/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.ledger;

import io.smilo.commons.HashHelper;
import io.smilo.commons.HashUtility;
import io.smilo.commons.db.Store;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.function.Function;


/**
 * This class provides all methods necessary to use an address after it has been generated.
 */
@Component
public class AddressUtility {

    private static final Logger LOGGER = Logger.getLogger(AddressUtility.class);
    private static final String CS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; //Character set used in Lamport Private Key Parts
    private static final Base32 base32 = new Base32();
    private static final Base64 base64 = new Base64();
    private static final String COLLECTION_NAME = "merkel";
    private final MerkleTreeGenerator treeGen;

    private Store store;
    private final Function<String, String> hashShort;
    private final Function<String, String> hash512;

    /**
     * Constructor ensures existance of address folder for storing the Merkle Trees. Also checks for availability of SHA1PRNG.
     */
    public AddressUtility(Store store, MerkleTreeGenerator treeGen) {
        this.store = store;
        this.treeGen = treeGen;
        try {
            SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR: NO SHA1PRNG SUPPORT! EXITING APPLICATION", e);
        }
        store.initializeCollection(COLLECTION_NAME);
        hashShort = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return HashUtility.digestSHA256ShortToBase64(s);
            }
        };
        hash512 = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return HashUtility.digestSHA512ToBase64(s);
            }
        };
    }

    /**
     * This method will verify that the supplied address signed the supplied message to generate the supplied signature.
     *
     * @param message   The message of which to verify the signature
     * @param signature The signature to verify
     * @param address   The address to check the signature against
     * @param index     The index of the Lamport Keypair used (position on bottom of Merkle tree)
     * @return boolean Whether the message was signed by the provided address using the provided index
     */
    public boolean verifyMerkleSignature(String message, String signature, String address, long index) {
        try {
            String lamportSignature = signature.substring(0, signature.indexOf(","));
            String merkleAuthPath = signature.substring(signature.indexOf(",") + 1);

            String[] lamportPublicKey = buildPublicKeyFromLamportSignature(message, lamportSignature);


            String leafStart = getLeafNode(lamportPublicKey);

            //Split on : in order to get the auth paths into a String array
            String[] merkleAuthPathComponents = merkleAuthPath.split(":");

            //Address matches, so signature is legitimate!
            if (checkMerkelPathAuthentication(address, index, leafStart, merkleAuthPathComponents)) {
                return true;
            }

        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("Failure: Incorrect MerkleSignature");
        } catch (NullPointerException e) {
            LOGGER.error("Failure: No MerkleSignature available");
        }catch (Exception e) {
            LOGGER.error("Failed to verify MerkleSignature", e);
        }
        //Fell through at some point, likely the address didn't match
        return false;
    }

    private String[] buildPublicKeyFromLamportSignature(String message, String lamportSignature) {
        //Holds 100 pairs, each pair containing one public and one private Lamport Key part
        String[] lamportSignaturePairs = lamportSignature.split("::");
        String[] lamportPublicKey = new String[200];

        //Lamport Signatures work with binary, so we need a binary string representing the hash of the message we want verify the signature of
        //Smilo Lamport Signatures sign the first 100 bytes of the hash. To generate a message colliding with the signature, one would need on average 2^99 tries
        final String binaryToCheck = HashUtility.digestSHA256ToBinaryHash(message).substring(0, 100);

        int i;
        for (i = 0; i < 99; i++) {
            String [] items = applyHashOnPrivateKey(binaryToCheck.charAt(i), lamportSignaturePairs[i].split(":"), hashShort);
            lamportPublicKey[2*i] = items[0];
            lamportPublicKey[2*i+1] = items[1];
        }

        String [] items = applyHashOnPrivateKey(binaryToCheck.charAt(i), lamportSignaturePairs[i].split(":"), hash512);
        lamportPublicKey[2*i] = items[0];
        lamportPublicKey[2*i+1] = items[1];

        return lamportPublicKey;
    }

    private String[] applyHashOnPrivateKey(char binary, String[] pair, Function<String,String> hash) {
        if (binary == '1') {
            pair[1] = hash.apply(pair[1]);
        } else {
            pair[0] = hash.apply(pair[0]);
        }
        return pair;
    }

    private boolean checkMerkelPathAuthentication(String address, long index, String leafStart, String[] merkleAuthPathComponents) {
        //This position variable will store where on the tree we are. Important for order of concatenation: rollingHash first, or Component first
        long position = index;
        //This rollingHash will contain the hash as we calculate up the hash tree
        String rollingHash;
        if (position % 2 == 0) { //Even; rollingHash goes first
            rollingHash = HashUtility.digestSHA256ToBase64(leafStart + merkleAuthPathComponents[0]);
        } else { //Odd; path component should go first
            rollingHash = HashUtility.digestSHA256ToBase64(merkleAuthPathComponents[0] + leafStart);
        }
        position /= 2;
        for (int i = 1; i < merkleAuthPathComponents.length - 1; i++) { //Go to merkleAuthPathComponents.length - 1 because the final hash is returned in base16 and is truncated
            //Combine the current hash with the next component, which visually would lie on the same Merkle Tree layer
            if (position % 2 == 0) { //Even; rollingHash goes first
                rollingHash = HashUtility.digestSHA256ToBase64(rollingHash + merkleAuthPathComponents[i]);
            } else { //Odd; path component should go first
                rollingHash = HashUtility.digestSHA256ToBase64(merkleAuthPathComponents[i] + rollingHash);
            }
            LOGGER.debug("rollingHash: " + rollingHash + " and auth component: " + merkleAuthPathComponents[i]);
            position /= 2;
        }
        //Final hash, done differently for formatting of address (base16, set length of 16 characters for the top of the Merkle Tree)

        if (position % 2 == 0) { //Even; rollingHash goes first
            rollingHash = AddressHelper.formatAddress(AddressHelper.getType(merkleAuthPathComponents.length+1), HashHelper.sha256((rollingHash + merkleAuthPathComponents[merkleAuthPathComponents.length - 1]).getBytes()));
        } else { //Odd; path component should go first
            rollingHash = AddressHelper.formatAddress(AddressHelper.getType(merkleAuthPathComponents.length+1), HashHelper.sha256((merkleAuthPathComponents[merkleAuthPathComponents.length - 1] + rollingHash).getBytes()));
        }

        if (address.toUpperCase().equals(rollingHash.toUpperCase())) { //Compare address and rolling hash
            return true;
        }
        return false;
    }

    private String getLeafNode(String[] lamportPublicKey) {
        String lamportPublicSignatureFull = "";
        //Populate full String to hash to get first leaf component
        for (int i = 0; i < lamportPublicKey.length; i++) {
            lamportPublicSignatureFull += lamportPublicKey[i];
        }
        LOGGER.debug("lmpSig: " + lamportPublicSignatureFull);
        //First leaf component; bottom layer of Merkle Tree
        return HashUtility.digestSHA256ToBase64(lamportPublicSignatureFull);
    }

    /**
     * This method will completely sign a message using the privateKey and Lamport Keypair Index supplied.
     *
     * @param message    The message to sign
     * @param privateKey The original private key
     * @param index      The index of the Lamport Keypair to sign with
     * @param address    The Smilo address
     * @return String The Merkle Signature consisting of a 200-part Lamport Signature along with the authentication path up the Merkle Tree
     */
    public String getMerkleSignature(String message, String privateKey, long index, String address) {
        String[][] merkelTree = null;
        try {
            merkelTree = getMerkelTreeInstance(privateKey, address);
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            LOGGER.error("ERROR: UNABLE TO (getMerkleSignature) READ INFORMATION FOR ADDRESS " + address + "!");
            return null;
        }

        //The 200 Lamport Private Key Parts, 100 of which will appear as-is in the final signature
        String[] lamportPrivateKeyParts = getLamportPrivateKeyParts(privateKey, index);

        //Lamport Signatures work with binary, so we need a binary string representing the hash of the message we want to sign
        String binaryToSign = HashUtility.digestSHA256ToBinaryHash(message);
        //Smilo Lamport Signatures sign the first 100 bytes of the hash. To generate a message colliding with the signature, one would need on average 2^99 tries
        binaryToSign = binaryToSign.substring(0, 100);

        String lamportSignature = getLamportSignature(lamportPrivateKeyParts, binaryToSign);

        String merklePath = getMerkelAuthenticationPath((int) index, address, merkelTree);

        return lamportSignature + "," + merklePath;
    }

    private String getLamportSignature(String[] lamportPrivateKeyParts, String binaryToSign) {
        String lamportSignature = "";
        for (int i = 0; i < binaryToSign.length(); i++) { //Add a public and private key part to signature for each digit of signable binary
            if (binaryToSign.charAt(i) == '0') { //A zero means we reveal the first private key
                if (i == binaryToSign.length() - 1) { //If it is part of the last pair, we want to use SHA512 (Full Length)
                    lamportSignature += lamportPrivateKeyParts[i * 2] + ":" + HashUtility.digestSHA512ToBase64(lamportPrivateKeyParts[i * 2 + 1]);
                } else {
                    lamportSignature += lamportPrivateKeyParts[i * 2] + ":" + HashUtility.digestSHA256ShortToBase64(lamportPrivateKeyParts[i * 2 + 1]);
                }
            } else if (binaryToSign.charAt(i) == '1') { //A one means we reveal the second private key
                if (i == binaryToSign.length() - 1) { //If it is part of the last pair, we want to use SHA512 (Full Length)
                    lamportSignature += HashUtility.digestSHA512ToBase64(lamportPrivateKeyParts[i * 2]) + ":" + lamportPrivateKeyParts[i * 2 + 1];
                } else { //If it is any one of the other previous pairs, use SHA256Short
                    lamportSignature += HashUtility.digestSHA256ShortToBase64(lamportPrivateKeyParts[i * 2]) + ":" + lamportPrivateKeyParts[i * 2 + 1];
                }
            } else { //Something has gone terribly wrong, our binary string isn't made of binary.
                LOGGER.error("CRITICAL ERROR: BINARY STRING IS NOT BINARY");
                System.exit(-4);
            }
            if (i < binaryToSign.length() - 1) { //Add a double-colon separator between pairs
                lamportSignature += "::";
            }
        }
        return lamportSignature;
    }

    private String getMerkelAuthenticationPath(int index, String address, String[][] merkelTree) {
        //Now we need to get the authentication path
        String merklePath = "";
        int layers = treeGen.getAddressNumberOfLayers(address);

        int[] authPathIndexes = getAuthenticationPathIndexes(index, layers);
        for (int i = 0; i < authPathIndexes.length; i++) {
            String layerData = merkelTree[i][authPathIndexes[i]];
            LOGGER.debug("We think the " + (authPathIndexes[i]) + "th index is " + layerData + ".");
            merklePath += layerData; //readLayerFile.nextLine() will now return the correct hash
            if (i < authPathIndexes.length - 1) //We want all elements in merklePath to be separated by a colon
            {
                merklePath += ":";
            }
        }
        return merklePath;
    }

    private String[][] getMerkelTreeInstance(String privateKey, String address) throws IOException, ClassNotFoundException {
        checkAndCreateMerkelTree(address, privateKey);
        byte val[] = store.get(COLLECTION_NAME, address.toUpperCase().getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(val);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return (String[][]) objectInputStream.readObject();
    }

    private void checkAndCreateMerkelTree(String address, String privateKey) {
        if (!treeGen.hasMerkelTree(address)) {
            String addressGen = treeGen.generateMerkleTree(privateKey, treeGen.getAddressNumberOfLayers(address), 16, 128);
            if (!address.equals(addressGen)) {
                LOGGER.error("Private key ("+privateKey+") walletFile is associated to a wrong address ("+address+")");
                LOGGER.debug("Generated address is ("+addressGen+")");
            }
        }
    }
    /**
     * This method returns a Long array of the required authentication path's locations. The authentication path represents, starting at layer 0, what element of each layer must be revealed to allow
     * peers to verify The signature as legitimate.
     *
     * @param startingIndex The index of the Lamport Keypair used
     * @param layers        The number of layers in the tree
     * @return long[] Long array holding the authentication path indexes generated from a given startingIndex and moving up for a given number of layers.
     */
    public int[] getAuthenticationPathIndexes(int startingIndex, int layers) {
        //Top layer will always be the address, no need to return this part, so only need layers-1 total layers.
        int[] authPath = new int[layers - 1];
        int workingIndex = startingIndex;
        for (int i = 0; i < layers - 1; i++) {
            // This if could be substituted by workingIndexˆ1. Bitwise xor.
            if (workingIndex % 2 == 0) //workingIndex is even
            {
                authPath[i] = workingIndex + 1;
            } else //workingIndex is odd
            {
                authPath[i] = workingIndex - 1;
            }
            workingIndex /= 2; // workingIndex integer division by 2 is equal to shift 1 bit.
        }
        return authPath;
    }

    /**
     * This method uses an original private key for an address, and returns the Lamport private key (capable of signing a 100-bit message) from the spot defined by index with each element separated by
     * a colon.
     *
     * @param privateKey The original private key of the Smilo address in question
     * @param index      The index of the Lamport Signature (bottom layer of Merkle Tree) to return
     * @return String[] A String[] containing the 200 Lamport Private Key Parts
     */
    public String[] getLamportPrivateKeyParts(String privateKey, long index) {
        try {
            LamportGenerator lamportGenerator = new LamportGenerator();

            return lamportGenerator.getLamportPrivateKeyList(lamportGenerator.getSeedForIthLamportKey(privateKey, index));
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR: UNABLE TO GENERATE LAMPORT PRIVATE KEY PARTS", e);
            System.exit(-2);
        }
        return null;
    }



    /**
     * This method checks an address to ensure proper formatting. Smilo address format: Prefix + TreeRoot + VerificationHash Prefix can be S1, S2, S3, S4, or S5. And P1, P2, P3, P4 and P5 for private
     * addresses. S1 means 14 layer, S2 means 15 layer, S3 means 16 layer, S4 means 17 layer, S5 means 18 layer. TreeRoot is an all-caps Base32 32-character-long SHA256 hash that represents the top of
     * the Merkle Tree for the respective address. VerificationHash is the first four digits of the Base32 SHA256 hash of TreeRoot, also in caps.
     *
     * @param address The address to test for validity
     * @return boolean Whether the address is formatted correctly
     */
    public boolean isAddressFormattedCorrectly(String address) {
        try {
//            String prefix = address.substring(0, 2); //Prefix is 2 characters long
//            if (!prefix.equals("S1") && !prefix.equals("S2") && !prefix.equals("S3") && !prefix.equals("S4") && !prefix.equals("S5")) {
//                return false;
//            }
//            LOGGER.trace("Address has correct prefix");
//            String treeRoot = address.substring(2, 34); //32 characters long. Should be all-caps Base32
//            String characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"; //Normal Base32 character set. All upper case! Omission of 1 is normal. :)
//            for (int i = 0; i < treeRoot.length(); i++) {
//                if (!characterSet.contains(treeRoot.charAt(i) + "")) {
//                    LOGGER.error("Address not valid!");
//                    return false;
//                }
//            }
//            LOGGER.trace("Address has correct characterset");
//            String givenEnding = address.substring(34); //Characters 34 to 37 should be all that's left. Remember we start counting at 0.
//            String correctEnding = HashUtility.digestSHA256ToBase32(prefix + treeRoot).substring(0, 4); //First four characters of Base32-formatted SHA256 of treeRoot
//            if (!correctEnding.equals(givenEnding)) {
//                LOGGER.debug("Address has incorrect ending, should be " + correctEnding + " instead of " + givenEnding);
//                return false;
//            }
            return AddressHelper.checkAddress(address);
        } catch (Exception e) { //Not printing exceptions or logging them on purpose. Any time an address too short is passed in, this will snag it.
            return false;
        }
    }
}

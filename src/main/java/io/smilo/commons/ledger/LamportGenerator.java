package io.smilo.commons.ledger;

import io.smilo.commons.HashUtility;
import org.apache.log4j.Logger;

import java.security.SecureRandom;


public class LamportGenerator {
    private static final Logger LOGGER = Logger.getLogger(LamportGenerator.class);
    private SecureRandom lmpPrivGen;
    private static final String CS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; //Character set used in Lamport Private Key Parts

    public byte[] getSeedForIthLamportKey(String privateKey, long index) {
        try {
            SecureRandom generatePrivateSeeds = SecureRandom.getInstance("SHA1PRNG");
            generatePrivateSeeds.setSeed(privateKey.getBytes());
            //Will loop through filling privateSeed until we reach the correct index
            byte[] privateSeed = new byte[100];
            for (int i = 0; i <= index; i++) {
                generatePrivateSeeds.nextBytes(privateSeed);
            }
            return privateSeed;
        } catch (Exception e) {
            LOGGER.error("Error getting SecureRandom object", e);
            return null;
        }
    }

    public String[] getLamportPrivateKeyList(byte[] seed) {
        try {
            lmpPrivGen = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            LOGGER.error("Error getting SecureRandom object", e);
        }
        lmpPrivGen.setSeed(seed);
        String [] keys = new String[200];
        for(int i = 0; i<keys.length; i++){
            keys[i] = getLamportPrivateKey();
        }
        return keys;
    }

    public String applyHashToPrivateKeyList(String [] privateKeys) {
        StringBuffer publicKey = new StringBuffer();
        int i;
        for (i = 0; i<privateKeys.length-2; i++){
            publicKey.append(HashUtility.digestSHA256ShortToBase64(privateKeys[i]));
        }
        publicKey.append(HashUtility.digestSHA512ToBase64(privateKeys[i++]));
        publicKey.append(HashUtility.digestSHA512ToBase64(privateKeys[i]));
        return publicKey.toString();
    }
    /**
     * Yeah, it's ugly. This is a manual unroll of the loops required to generate a Lamport Public Key. It used to be very pretty. This is 2x as fast.
     * The ugly code is worth the speedup.
     * This method takes a seed, creates a SecureRandom seeded with the input seed, and then directly generates the public key, without ever storing the
     * private key, as that is unnecessary. Each Lamport Private Key Part is 20 psuedo-random (from seeded SecureRandom) characters. There are 200
     * of these, to support signing a 100-bit message.
     * The Lamport Public Key is the 200 SHA256Short hashes of the 200 Lamport Private Keys concatenated together in order.
     * The Lamport Public Keys returned from calling this method many times build the bottom layer of the Merkle Tree.
     * Uses SHA256Short in order to reduce the total size of a 200-part Lamport Public Key down to reduce the size of the blockchain.
     *
     * @param seed The byte array seed for the desired Lamport Signature Keypair
     * @return String This is the Public key of the Lamport Signature defined by byte[] seed
     */
    public String getLamportPublicKey(byte[] seed) {
        return applyHashToPrivateKeyList(getLamportPrivateKeyList(seed));
    }

    /**
     * This method uses the lmpPrivGen object to generate the next Lamport Private Key part. Each Lamport Private Key Part is 20 pseudo-random characters.
     *
     * @return String The next 20-character Lamport Private Key part.
     */
    String getLamportPrivateKey() {
        int len = CS.length();
        return "" + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len))
                + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len))
                + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len))
                + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len)) + CS.charAt(lmpPrivGen.nextInt(len));
    }
}
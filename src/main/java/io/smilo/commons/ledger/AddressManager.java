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

import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionOutput;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;

@Component
public class AddressManager {

    private static final Logger LOGGER = Logger.getLogger(AddressManager.class);

    private final List<AbstractMap.SimpleEntry<String,String>> privateKeys;
    private final MerkleTreeGenerator treeGen;
    private Long defaultAddressOffset = 1L;
    private final AddressUtility addressUtility;
    private final PrivateKeyGenerator privateKeyGenerator;

    public AddressManager(@Value("${WALLET_FILE:wallet.keys}") String walletFileName,
                          AddressUtility addressUtility,
                          MerkleTreeGenerator merkleTreeGenerator,
                          PrivateKeyGenerator privateKeyGenerator) {
        /**
         * Loads in wallet private key. If none exist, generates an address.
         */
        this.privateKeyGenerator = privateKeyGenerator;
        this.treeGen = merkleTreeGenerator;
        this.addressUtility = addressUtility;
        this.privateKeys = new ArrayList<>();
        readOrRegenerateDatabase(walletFileName);
    }

    public final void readOrRegenerateDatabase(String walletFileName) {
        try {
            this.privateKeys.clear();

            File walletFile = new File(walletFileName);

            if (!walletFile.exists()) {
                LOGGER.info("Generating a new address...");
                String privateKey = privateKeyGenerator.getPrivateKey();
                String address = treeGen.generateMerkleTree(privateKey, 14, 16, 128);
                LOGGER.info("New address: " + address);
                PrintWriter out = new PrintWriter(walletFile);
                try {
                    out.println(address + ":" + privateKey);
                } catch (Exception e) {
                    LOGGER.error("Error writing walletFile", e);
                } finally {
                    out.close();
                }
                addToWallet(address, privateKey);
            } else {
                Scanner scan = new Scanner(walletFile);
                try {
                    while (scan.hasNextLine()) {
                        String input = scan.nextLine();
                        String address = input.substring(0, input.indexOf(":"));
                        String privateKey = input.substring(input.indexOf(":") + 1);
                        addToWallet(address, privateKey);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error reading walletFile", e);
                } finally {
                    scan.close();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Exception when readOrRegenerateDatabase", e);
        }
    }

    private void addToWallet(String address, String privateKey) {
        if(address != null) {
            privateKeys.add(new AbstractMap.SimpleEntry<> (address, privateKey));
        }
    }

    /**
     * This method returns the address index offset, which is used when signing transactions with the default address. If this were kept track of by the databaseManager, block generation would be
     * tricky. defaultAddressOffset is always at least 1, as it is added to the most-recently-used index (from the blockchain) to calculate which index should be used for signing the next transaction.
     *
     * @return long The offset for the default address
     */
    public Long getDefaultAddressIndexOffset() {
        return defaultAddressOffset;
    }

    /**
     * This method increments the defaultAddressIndexOffset in order to account for a signature being used.
     */
    public void incrementDefaultAddressIndexOffset() {
        defaultAddressOffset++;
    }

    /**
     * This method resets the defaultAddressOffset to 1; useful when the blockchain has caught up with the transactions we sent.
     */
    public void resetDefaultAddressIndexOffset() {
        defaultAddressOffset = 1L;
    }

    /**
     * This method returns the private key of the wallet's default address.
     *
     * @return String Private key of the daemon's default address.
     */
    public String getDefaultPrivateKey() {
        return privateKeys.get(0).getValue();
    }

    public String getAddressPrivateKey(String address) {
        return privateKeys.stream().filter(e -> e.getKey().equals(address)).findFirst().map(a -> a.getValue()).orElse(null);
    }

    public Transaction getSignedTransaction(String destinationAddress, BigInteger sendAmount, Long signatureIndex) {
        //TODO: Pass assetID, hash and fee!
        String transactionData = getDefaultAddress() + ";" + sendAmount + ";" + destinationAddress + ";" + sendAmount;
        String address = getDefaultAddress();
        String privateKey = getDefaultPrivateKey();
        String signature = addressUtility.getMerkleSignature(transactionData, privateKey, signatureIndex, address);
        return new Transaction(System.currentTimeMillis(), "", getDefaultAddress(), sendAmount, BigInteger.ZERO,
                Arrays.asList(new TransactionOutput(destinationAddress, sendAmount)), "", signature, signatureIndex);
    }

    /**
     * Returns the 'default' address, which is the first one from loading up the address file, or the one that was originally generated with the daemon first ran.
     *
     * @return String Default address
     */
    public String getDefaultAddress() {
        return privateKeys.get(0).getKey();
    }

    /**
     * Returns a new address
     *
     * @return String A new address
     */
    public String getNewAddress() {
        String privateKey = privateKeyGenerator.getPrivateKey();
        String address = treeGen.generateMerkleTree(privateKey, 14, 16, 128);
        addToWallet(address, privateKey);
        return address;
    }

    /**
     * Returns the public address of a imported private key
     *
     * @return String address
     */
    public String importPrivateKey(String privateKey) {
        String address = treeGen.generateMerkleTree(privateKey, 14, 16, 128);
        addToWallet(address, privateKey);
        return address;
    }

    /**
     * Check if an address exist in the wallet.
     *
     * @param address
     * @return Boolean
     */
    public boolean addressExistInWallet(String address){
        return privateKeys.stream().anyMatch(e -> e.getKey().equals(address));
    }

}

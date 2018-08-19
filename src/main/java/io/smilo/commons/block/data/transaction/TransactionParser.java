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

package io.smilo.commons.block.data.transaction;

import io.smilo.commons.HashUtility;
import io.smilo.commons.block.data.Parser;
import io.smilo.commons.block.data.Validator;
import io.smilo.commons.ledger.AddressUtility;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.pendingpool.StringBigIntegerPair;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.ArrayBufferInput;
import org.msgpack.core.buffer.MessageBufferInput;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TransactionUtility simplifies a few basic tasks dealing with transaction parsing and verification.
 */
@Component
public class TransactionParser implements Parser<Transaction>, Validator<Transaction> {

    private static final Logger LOGGER = Logger.getLogger(TransactionParser.class);
    private static final byte CURRENT_VERSION = (byte) 1;
    private final AddressUtility addressUtility;
    private final LedgerManager ledgerManager;

    private ArrayList<StringBigIntegerPair> accountBalanceDeltaTables;

    public TransactionParser(AddressUtility addressUtility,
                             LedgerManager ledgerManager) {
        this.addressUtility = addressUtility;
        this.ledgerManager = ledgerManager;
        this.accountBalanceDeltaTables = new ArrayList<>();
    }

    /**
     * Checks if the transaction is valid based on the following criteria:
     *
     * - DataHash is valid
     * - TransactionHash is valid
     * - Input address is formatted correctly
     * - Output addresses are valid
     * - Input amount equals output amounts
     * - Signature is correct
     * - There is no other spending transaction pending for this address
     * - Balance is sufficient to cover the spendings
     *
     * @param transaction transaction to check
     * @return true if valid, false if invalid
     */
    @Override
    public boolean isValid(Transaction transaction) {
        return isValid(transaction, false);
    }

    /**
     * Checks if the transaction is valid based on the following criteria:
     *
     * - DataHash is valid
     * - TransactionHash is valid
     * - Input address is formatted correctly
     * - Output addresses are valid
     * - Input amount equals output amounts
     * - Signature is correct
     * - There is no other spending transaction pending for this address
     * - Balance is sufficient to cover the spendings
     *
     * @param transaction transaction to check
     * @param allowMultipleSpendingTransactions if true, the method will not check if there are spending transactions pending.
     * @return true if valid, false if invalid
     */
    public boolean isValid(Transaction transaction, boolean allowMultipleSpendingTransactions) {
        boolean isValid = true;
        try {
            if(transaction.getDataHash().equals("")){
                LOGGER.error("Error validating Tx hash: " + transaction.getDataHash() + " not valid.");
                isValid = false;
            }

            if (transaction.getDataHash().equals(HashUtility.digestSHA256ToHEX(transaction.getRawTransactionData()))){
                LOGGER.info("Tx hash: " + transaction.getDataHash() + " is valid.");
            } else {
                LOGGER.error("Error validating Tx hash: " + transaction.getDataHash() + " not valid.");
                isValid = false;
            }

            if (!addressUtility.isAddressFormattedCorrectly(transaction.getInputAddress())) {
                LOGGER.error("Error validating transaction: input address " + transaction.getInputAddress() + " is misformatted.");
                isValid = false;
            }

            for (TransactionOutput output : transaction.getTransactionOutputs()) {
                if (!addressUtility.isAddressFormattedCorrectly(output.getOutputAddress())) {
                    LOGGER.error("Error validating transaction: output address " + output.getOutputAddress() + " is misformatted.");
                    isValid = false;
                }
            }

            if (transaction.getInputAmount().compareTo(transaction.getOutputTotal()) < 0) {
                LOGGER.debug("Input amount: " + transaction.getInputAmount() + " & Output amount: " + transaction.getOutputTotal());
                LOGGER.error("Input amount is smaller then output amount!");
                isValid = false;
                // Coins can't be created out of thin air!
            }

            if (transaction.getInputAmount().compareTo(transaction.getOutputTotal()) > 0) {
                LOGGER.debug("Input amount: " + transaction.getInputAmount() + " & Output amount: " + transaction.getOutputTotal());
                LOGGER.error("Input amount is bigger then output amount!");
                return false; //Where do they need to go? We don't have greedy miners.
            }

            if (!addressUtility.verifyMerkleSignature(transaction.getRawTransactionDataWithHash(), transaction.getSignatureData(), transaction.getInputAddress(), transaction.getSignatureIndex())) {
                LOGGER.error("Error validating transaction: Transaction signature does not match!");
                isValid = false;
            }

            if (!allowMultipleSpendingTransactions && ledgerManager.getPendingTransactions().stream().anyMatch(t -> t.getInputAddress().equals(transaction.getInputAddress()))) {
                LOGGER.error("Error validating transaction: already a pending transaction for this inputaddress!");
                return false;
            }

            //We need to check to make sure the input address isn't sending coins they don't own.
            //Check for the outstanding outgoing amount for this address
            BigInteger outstandingOutgoingAmount = BigInteger.ZERO;
            int indexOfDelta = -1;
            for (int i = 0; i < accountBalanceDeltaTables.size(); i++) {
                if (accountBalanceDeltaTables.get(i).getStringToHold().equals(transaction.getInputAddress())) {
                    outstandingOutgoingAmount = accountBalanceDeltaTables.get(i).getBigIntegerToHold();
                    indexOfDelta = i;
                    break;
                }
            }
            BigInteger previousBalance = ledgerManager.getAddressBalance(transaction.getInputAddress());
            // TODO: Check if pending transactions on account balance are properly counted on LedgerStore.
            if (previousBalance.compareTo(transaction.getInputAmount()) < 0) {
                LOGGER.info("Account " + transaction.getInputAddress() + " tried to spend " + transaction.getInputAmount() + " but only had " + (previousBalance.add(outstandingOutgoingAmount)) + " coins.");
                isValid = false;
            }
            if (indexOfDelta >= 0) {
                accountBalanceDeltaTables.get(indexOfDelta).addBigIntegerToHold(transaction.getInputAmount());
            } else {
                accountBalanceDeltaTables.add(new StringBigIntegerPair(transaction.getInputAddress(), transaction.getInputAmount())); //No existing entry in the pending delta tables, so we create an ew one
            }
        } catch (Exception e) {
            // Likely an error parsing a Long or performing some String manipulation task. Maybe array bounds exceptions.
            LOGGER.error("Exception when validating transaction ", e);
            isValid = false;

        }
        return isValid;
    }

    /**
     * Signs a Transaction built with the provided sending address and amount, and destination address(es) and amount(s).
     *
     * @param transaction The transaction to sign
     * @param privateKey  The private key for inputAddress
     * @param index       The signature index to use
     * @return String The full transaction, formatted for use in the Smilo network, including the signature and signature index. Returns null if transaction is incorrect for any reason.
     * timestamp;AssetID;InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;Outputamount2;...;...;txFee;txHash;SignatureData;SignatureIndex
     */
    // TODO: hash messages, smart contracts
    @Override
    public Transaction sign(Transaction transaction, String privateKey, int index) {
        try {
            if (transaction.getTimestamp() == null) {
                transaction.setTimestamp(System.currentTimeMillis());
            }

            if (transaction.getInputAddress() == null || transaction.getTransactionOutputs() == null || transaction.getTransactionOutputs().isEmpty() || transaction.getInputAmount().compareTo(BigInteger.ZERO) <= 0) { //Immediate red flags
                LOGGER.error("Error signing transaction!");
                return null;
            }

            transaction.setSignatureData(addressUtility.getMerkleSignature(transaction.getTransactionBody(), privateKey, index, transaction.getInputAddress()));
            transaction.setSignatureIndex((long) index);

            if (!isValid(transaction)) {
                LOGGER.error("Transaction not valid!");
                return null;
            }
        } catch (Exception ex) {
            LOGGER.error("Error signing transaction", ex);
        }
        return transaction;
    }

    @Override
    public void hash(Transaction transaction) {
        try {
            transaction.setDataHash(HashUtility.digestSHA256ToHEX(transaction.getRawTransactionData()));
        } catch (Exception ex) {
            LOGGER.error("Unable to create data hash for transaction", ex);
        }
    }
//        TRANSACTION timestamp;assetID;inputAddress;inputAmount;outputAddress1;outputAmount1;outputAddress2;outputAmount2;...;...;txFee;txHash;signatureData;signatureIndex

    @Override
    public Transaction deserialize(byte[] raw) {
        if (raw.length == 0) return null;
        MessageBufferInput data = new ArrayBufferInput(raw);
        MessageUnpacker msgpack = MessagePack.newDefaultUnpacker(data);
        Transaction transaction = null;
        try {
            msgpack.unpackByte(); // Skip version number
            Long timestamp = msgpack.unpackLong();
            String assetId = msgpack.unpackString();
            String inputAddress = msgpack.unpackString();
            BigInteger inputAmount = msgpack.unpackBigInteger();
            int items = msgpack.unpackArrayHeader();
            List<TransactionOutput> outputs = new ArrayList<>();
            for (int i = 0; i < items; i++) {
                int size = msgpack.unpackBinaryHeader();
                byte[] temp = msgpack.readPayload(size);
                outputs.add(parseOutput(temp));
            }
            BigInteger fee = msgpack.unpackBigInteger();
            int size = msgpack.unpackBinaryHeader();
            byte [] extraData = msgpack.readPayload(size);
            LOGGER.info("Unsupported extra data inside: " + Hex.encodeHexString(extraData));
            String hash = msgpack.unpackString();
            String signature = msgpack.unpackString();
            Long signatureIndex = msgpack.unpackLong();

            transaction = new Transaction(timestamp, assetId, inputAddress, inputAmount, fee, outputs, hash, signature, signatureIndex);
        } catch (ArrayIndexOutOfBoundsException | IOException ex) {
            LOGGER.error("Unable to deserialize transaction", ex);
        }
        return transaction;
    }

    private TransactionOutput parseOutput(byte[] raw) throws IOException {
        MessageBufferInput data = new ArrayBufferInput(raw);
        MessageUnpacker msgpack = MessagePack.newDefaultUnpacker(data);
        String outputAddress = msgpack.unpackString();
        BigInteger outputAmount = msgpack.unpackBigInteger();
        return new TransactionOutput(outputAddress,outputAmount);
    }

    @Override
    public byte[] serializeWithoutSignature(Transaction transaction) {
        return transaction.getRawTransactionData().getBytes(UTF_8);
    }

    @Override
    public byte[] serialize(Transaction transaction) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MessagePacker msgpack = MessagePack.newDefaultPacker(out)) {
            msgpack.packByte(CURRENT_VERSION);
            msgpack.packLong(transaction.getTimestamp());
            msgpack.packString(transaction.getAssetId());
            msgpack.packString(transaction.getInputAddress());
            msgpack.packBigInteger(transaction.getInputAmount());
            List<TransactionOutput> outputs = transaction.getTransactionOutputs();
            msgpack.packArrayHeader(outputs.size());
            for (TransactionOutput txout : outputs) {
                byte[] rawoutput = packOutput(txout);
                msgpack.packBinaryHeader(rawoutput.length);
                msgpack.addPayload(rawoutput);
            }
            msgpack.packBigInteger(transaction.getFee());
            msgpack.packBinaryHeader(0);
            msgpack.packString(transaction.getDataHash());
            msgpack.packString(transaction.getSignatureData());
            msgpack.packLong(transaction.getSignatureIndex());
            msgpack.flush();
            return out.toByteArray();
        } catch (ArrayIndexOutOfBoundsException | IOException ex) {
            LOGGER.error("Unable to serialize transaction", ex);
        }
        return null;
    }

    private byte[] packOutput(TransactionOutput txout) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MessagePacker msgpack = MessagePack.newDefaultPacker(out)) {
            msgpack.packString(txout.getOutputAddress());
            msgpack.packBigInteger(txout.getOutputAmount());
            msgpack.flush();
        }
        return out.toByteArray();
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Transaction.class.isAssignableFrom(clazz);
    }

    //    /**
//     * Transactions on the Smilo network from the same address must occur in a certain order, dictated by the signature index.
//     * As such, We want to order all transactions from the same address in order.
//     * The order of transactions from different addresses does not matter--coins will not be received and spent in the same transaction.
//     *
//     * @param transactionsToSort ArrayList<String> containing String representations of all the addresses to sort
//     *
//     * @return ArrayList<String> All of the transactions sorted in order for block inclusion, with any self-invalidating transactions removed.
//     */
//    public ArrayList<String> sortTransactionsBySignatureIndex(ArrayList<String> transactionsToSort) {
//        for (int i = 0; i < transactionsToSort.size(); i++) {
//            if (!isValid(transactionsToSort.get(i))) {
//                transactionsToSort.remove(i);
//                i--; //Compensate for changing ArrayList size
//            }
//        }
//        ArrayList<String> sortedTransactions = new ArrayList<String>();
//        for (int i = 0; i < transactionsToSort.size(); i++) {
//            System.out.println("spin1");
//            if (sortedTransactions.isEmpty()) {
//                sortedTransactions.add(transactionsToSort.get(0));
//            } else {
//                String address = transactionsToSort.get(i).split(";")[0];
//                long index = Long.parseLong(transactionsToSort.get(i).split(";")[transactionsToSort.get(i).split(";").length - 1]);
//                boolean added = false;
//                for (int j = 0; j < sortedTransactions.size(); j++) {
//                    System.out.println("spin2");
//                    if (sortedTransactions.get(j).split(";")[0].equals(address)) {
//                        String[] parts = sortedTransactions.get(j).split(";");
//                        int indexToGrab = parts.length - 1;
//                        String sigIndexToParse = sortedTransactions.get(j).split(";")[indexToGrab];
//                        long existingSigIndex = Long.parseLong(sigIndexToParse);
//                        if (index < existingSigIndex) {
//                            //Insertion should occur before the currently-studied element
//                            sortedTransactions.add(j, transactionsToSort.get(i));
//                            added = true;
//                            break;
//                        } else if (index == existingSigIndex) {
//                            //This should never happen--double-signed transaction. Discard the new one!
//                            j = sortedTransactions.size();
//                        }
//                    }
//                }
//                if (!added) {
//                    sortedTransactions.add(transactionsToSort.get(i));
//                }
//            }
//        }
//        return sortedTransactions;
//    }
}

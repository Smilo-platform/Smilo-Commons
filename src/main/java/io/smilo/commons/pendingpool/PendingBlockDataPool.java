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

package io.smilo.commons.pendingpool;

import io.smilo.commons.HashUtility;
import io.smilo.commons.block.AddResultType;
import io.smilo.commons.block.Block;
import io.smilo.commons.block.ParserProvider;
import io.smilo.commons.block.data.AddBlockDataResult;
import io.smilo.commons.block.data.BlockData;
import io.smilo.commons.block.data.Parser;
import io.smilo.commons.block.data.Validator;
import io.smilo.commons.block.data.message.Message;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionOutput;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.peer.PeerSender;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PendingBlockDataPool {

    private static final Logger LOGGER = Logger.getLogger(PendingBlockDataPool.class);

    public final Set<BlockData> allBroadcastBlockData = new HashSet<>();
    private Set<BlockData> pendingBlockData = new HashSet<>();

    private final PeerSender peerSender;
    private final ParserProvider parserProvider;
    private final LedgerManager ledgerManager;

    public PendingBlockDataPool(PeerSender peerSender,
                                ParserProvider parserProvider, LedgerManager ledgerManager) {
        this.peerSender = peerSender;
        this.parserProvider = parserProvider;
        this.ledgerManager = ledgerManager;
    }

    public void addMessage(String rawMessage) {
        Parser parser = parserProvider.getParser(Message.class);
        Message message = (Message) parser.deserialize(HashUtility.decodeFromBase64(rawMessage));
        addBlockDataToPool(message);
    }

    public void addTransaction(String rawTransaction) {
        Parser parser = parserProvider.getParser(Transaction.class);
        Transaction transaction = (Transaction) parser.deserialize(HashUtility.decodeFromBase64(rawTransaction));
        addBlockDataToPool(transaction);
    }

    public AddBlockDataResult addBlockData(BlockData blockData) {
        try {
            Validator validator = parserProvider.getValidator(blockData.getClass());
            boolean alreadyExists = pendingBlockData.contains(blockData);

            if (alreadyExists) {
                return new AddBlockDataResult(blockData, AddResultType.DUPLICATE, blockData.getClass().getSimpleName() + " is already pending");
            }

            if (!validator.isValid(blockData)) {
                LOGGER.info("Throwing out a message deemed invalid");
                return new AddBlockDataResult(blockData, AddResultType.VALIDATION_ERROR, "Throwing out a " + blockData.getClass().getSimpleName() + " deemed invalid");
            }

            pendingBlockData.add(blockData);
            if (blockData instanceof Transaction) {
                ledgerManager.addPendingTransaction((Transaction) blockData);
            }
            return new AddBlockDataResult(blockData, AddResultType.ADDED, "Added " + blockData.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.error("Exception when run addBlockData", e);
            return new AddBlockDataResult(blockData, AddResultType.UNKNOWN, "An exception has occurred");
        }
    }

    public AddBlockDataResult addBlockDataToPool(BlockData blockData) {
        try {
            boolean alreadyExists = allBroadcastBlockData.contains(blockData);

            //Data was not already received
            if (!alreadyExists) {

                allBroadcastBlockData.add(blockData);
                AddBlockDataResult result = addBlockData(blockData);

                if (result.getType().isSuccess()) {
                    LOGGER.info("New " + blockData.getClass().getSimpleName() + " on network:");
                    peerSender.broadcastContent(blockData);
                } else {
                    LOGGER.error("Not a good " + blockData.getClass().getSimpleName() + "!");
                }
                return result;
            }
            return new AddBlockDataResult(blockData, AddResultType.DUPLICATE, blockData.getClass().getSimpleName() + " is already pending");
        } catch (Exception ex) {
            LOGGER.error("Exception at addBlockDataToPool, a good " + blockData.getClass().getSimpleName() + "!", ex);
            return new AddBlockDataResult(blockData, AddResultType.UNKNOWN, "An exception has occurred");
        }
    }

    /**
     * Removes identical block data from the pending block data pool
     *
     * @param blockData The transaction to remove
     * @return boolean Whether removal was successful
     */
    public boolean removeBlockData(BlockData blockData) {
        if (blockData instanceof Transaction) {
            ledgerManager.removePendingTransaction((Transaction) blockData);
        }
        return pendingBlockData.remove(blockData);
    }

    /**
     * This method is the most useful method in this class--it allows the mass removal of all transactions from the pending transaction pool that were included in a network block, all in one call. The
     * returned boolean is not currently utilized in MainClass, proper handling of blocks with transaction issues will be addressed in a future alpha, probably 0.0.1a6/7 given my schedule.
     *
     * @param block The block holding transactions to remove
     * @return boolean Whether all transactions in the block were successfully removed
     */
    public boolean removeTransactionsInBlock(Block block) {
        //This try-catch method wraps around more than it needs to, in the name of easy code management, and making colors line up nicely in my IDE.
        try {
            /* Transaction format:
             * InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex
             *
             * We are removing only transactions that match the exact String from the block. If the block validation fails, NO transactions are removed from the pool.
             * In a late-night coding session, not removing any transactions of an invalid block seemed like the bset idea--transactions should never be discarded
             * if they haven't made it into the blockchain, and any block that doesn't isValid won't make it through Blockchain's block screening, so these transactions
             * that we aren't removing will never happen on-chain if we remove them from the pool when an invalid block says we should. Also closes a potential attack
             * vector where someone could submit false blocks in order to be a nuisance and empty the pending transaction pool.
             */
            List<Transaction> transactions = block.getTransactions();
            boolean allSuccessful = true;
            for (int i = 0; i < transactions.size(); i++) {
                if (!removeBlockData(transactions.get(i))) {
                    allSuccessful = false; //This might happen if a transaction was in a block before it made it across the network to a peer, so not always a big deal!
                }
            }
            return allSuccessful;
        } catch (Exception e) {
            LOGGER.error("Exception on removeTransactionsInBlock: ", e);
            return false;
        }
    }

    /**
     * This method scans through all of the pending transactions to calculate the total (net) balance change pending on an address. A negative value represents coins that were sent from the address in
     * question, and a positive value represents coins awaiting confirmations to arrive.
     *
     * @param address Smilo address to search the pending transaction pool for
     * @return long The pending total (net) change for the address in question
     */
    public BigInteger getPendingBalance(String address) {
        BigInteger totalChange = BigInteger.ZERO;
        List<Transaction> pendingTransactionsss = getPendingData(Transaction.class);

        for (int i = 0; i < pendingTransactionsss.size(); i++) {
            Transaction transaction = pendingTransactionsss.get(i);
            try {
                if (transaction.containsAddress(address)) {
                    String senderAddress = transaction.getInputAddress();
                    if (senderAddress.equals(address)) {
                        totalChange = totalChange.subtract(transaction.getInputAmount());
                    }
                    totalChange = totalChange.add(transaction.getTransactionOutputs().stream()
                            .filter(txOutput -> txOutput.getOutputAddress().equals(address))
                            .map(txOutput -> txOutput.getOutputAmount())
                            .reduce(BigInteger.ZERO, BigInteger::add));
                }
            } catch (Exception e) {
                LOGGER.error("Exception getPendingBalance, Major problem: Transaction in the pending transaction pool is incorrectly formatted! Transaction in question: " + transaction, e);
            }
        }
        return totalChange;
    }

    public <T extends BlockData> List<T> getPendingData(Class<T> clazz) {
        List<T> data = new ArrayList<>();
        pendingBlockData.stream().filter(b -> b.getClass().equals(clazz)).forEach(b -> data.add((T) b));
        return data;
    }

    public Set<BlockData> getPendingBlockData() {
        return pendingBlockData;
    }


    /**
     * Returns all pending transactions for the given address. A transaction is considered pending for the given address if:
     * - The address is equal to a transaction's input address
     * - The address is equal to any of the transaction's output addresses.
     *
     * @param address
     * @return
     */
    public List<Transaction> getPendingTransactionsForAddress(String address) {
        List<Transaction> pendingTransactions = getPendingData(Transaction.class);

        List<Transaction> pendingForAddress = new ArrayList<>();
        for (Transaction transaction : pendingTransactions) {
            if (transaction.getInputAddress().equals(address)) {
                pendingForAddress.add(transaction);
            } else {
                // It might be an output
                for (TransactionOutput output : transaction.getTransactionOutputs()) {
                    if (output.getOutputAddress().equals(address)) {
                        pendingForAddress.add(transaction);
                        break;
                    }
                }
            }
        }

        return pendingForAddress;
    }


    /**
     * Searches for and returns the Transaction with the given data hash. Null will be returned if no pending transaction was found.
     *
     * @param dataHash
     * @return
     */
    public Transaction getPendingTransaction(String dataHash) {
        List<Transaction> pendingTransactions = getPendingData(Transaction.class);

        for (Transaction transaction : pendingTransactions) {
            if (transaction.getDataHash().equals(dataHash))
                return transaction;
        }

        return null;
    }


}

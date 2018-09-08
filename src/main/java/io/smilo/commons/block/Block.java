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

package io.smilo.commons.block;


import io.smilo.commons.block.data.transaction.Transaction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * This class provides all functionality related to block verification and usage. A block contains: - Timestamp (Unix Epoch) - Block number - Previous block hash - ledgerHash - Transaction list
 * (Array) - blockHash - nodeSignature - nodeSignatureIndex
 */
public class Block extends Content {

    private long blockNum;
    private String previousBlockHash;
    private String redeemAddress;
    private String ledgerHash;
    private List<Transaction> transactions;
    private String blockHash;
    private String nodeSignature;
    private long nodeSignatureIndex;

    public Block() {}

    /**
     * Constructor for Block object. A block object is made for any confirmed or potential network block, and requires all pieces of data in this constructor to be a valid network block. The timestamp
     * is the result of the miner's initial call to System.currentTimeMillis(). When peers are receiving new blocks (synced with the network, not catching up) they will refuse any blocks that are more
     * than 2 hours off their internal adjusted time. This makes difficulty malleability impossible in the long-run, ensures that timestamps are reasonably accurate, etc. As a result, any clients far
     * off from the true network time will be forked off the network as they won't accept valid network blocks. Make sure your computer's time is set correctly!
     *
     * All blocks stack in one particular order, and each block contains the hash of the previous block, to clear any ambiguities about which chain a block belongs to during a fork.
     *
     * Blocks are hashed to create a block hash, which ensures blocks are not altered, and is used in block stacking. The data hashed is formatted as a String:
     * {timestamp:blockNum:previousBlockHash:redeemAddress},{ledgerHash},{transactions} Then, the full block (including the hash) is signed by the miner. So:
     * {timestamp:blockNum:previousBlockHash:redeemAddress},{ledgerHash},{transactions},{blockHash} will be hashed and signed by the redeemAddress, which should be held by the signing node. The final block format:
     * {timestamp:blockNum:previousBlockHash:redeemAddress},{ledgerHash},{transactions},{blockHash},{nodeSignature},{nodeSignatureIndex}
     *
     * Explicit transactions are represented as Strings in an ArrayList<String>. Each explicit transaction follows the following format:
     * InputAssetId;InputAddress;InputAmount;TX_Fee;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex At a bare minimum, ALL transactions must have an Input
     * Asset ID, InputAddress, InputAmount, TX_FEE and one OutputAddress and one OutputAmount The payment of transaction fees are collected into 1 explicit transaction and result into .
     *
     * @param timestamp Timestamp originally set into the block by the node
     * @param blockNum The block number
     * @param previousBlockHash The hash of the previous block
     * @param redeemAddress the user's public key
     * @param ledgerHash The hash of the ledger as it existed before this block's transactions occurred
     * @param transactions List<Transaction> of all the transactions included in the block
     * @param nodeSignature Node's signature of the block
     * @param nodeSignatureIndex Node's signature index used when generating nodeSignature
     */
    public Block(Long timestamp, long blockNum, String previousBlockHash, String redeemAddress, String ledgerHash, List<Transaction> transactions, String nodeSignature, int nodeSignatureIndex) {
        super(timestamp);
        this.blockNum = blockNum;
        this.previousBlockHash = previousBlockHash;
        this.ledgerHash = ledgerHash;
        this.transactions = transactions;
        this.nodeSignature = nodeSignature;
        this.nodeSignatureIndex = nodeSignatureIndex;
        this.redeemAddress = redeemAddress;
    }

    public String getRawBlockData() {
        String transactionsString = transactions.stream().filter(Transaction::hasContent).map(Transaction::getRawTransaction).collect(joining("*"));
        return "{" + getTimestamp() + ":" + blockNum + ":" + previousBlockHash + ":" + redeemAddress + "},{" + ledgerHash + "},{" + transactionsString + "}";
    }

    /**
     * Scans the block for any transactions that involve the provided address. Returns ArrayList<String> containing "simplified" transactions, in the format of sender:amount:asset:receiver Each of
     * these "simplified" transaction formats don't necessarily express an entire transaction, but rather only portions of a transaction which involve either the target address sending or receiving
     * coins.
     *
     * @param addressToFind Address to search through block transaction pool for
     *
     * @return List<String> Simplified-transaction-format list of all related transactions.
     */
    public List<String> getTransactionsInvolvingAddress(String addressToFind) {
        ArrayList<String> relevantTransactionParts = new ArrayList<>();
        for (Transaction tempTransaction: transactions) {
            //Transaction format: InputAssetId;InputAddress;InputAmount;TX_Fee;ToAddress1;Output1;ToAddress2;Output2;SignatureData;SignatureIndex
            String assetid = tempTransaction.getAssetId();
            String sender = tempTransaction.getInputAddress();
            if (sender.equalsIgnoreCase(addressToFind)) {
                tempTransaction.getTransactionOutputs()
                        .forEach(txOutput -> {
                            relevantTransactionParts.add(assetid + ":" + sender + ":" + txOutput.getOutputAddress() + ":" + txOutput.getOutputAmount());
                        });
            } else {
                tempTransaction.getTransactionOutputs().stream()
                        .filter(txOutput -> txOutput.getOutputAddress().equals(addressToFind))
                        .forEach(txOutput -> {
                            relevantTransactionParts.add(assetid + ":" + sender + ":" + txOutput.getOutputAddress() + ":" + txOutput.getOutputAmount());
                        });
            }
        }
        return relevantTransactionParts;
    }

    /**
     * Returns the raw String representation of the block, useful when saving the block or sending it to a peer.
     * 
     * @return String The raw block
     */
    public String getRawBlock() {
        return getRawBlockDataWithHash() + ",{" + nodeSignature + "},{" + nodeSignatureIndex + "}";
    }

    public long getBlockNum() {
        return blockNum;
    }

    public String getLedgerHash() {
        return ledgerHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public String getNodeSignature() {
        return nodeSignature;
    }

    public long getNodeSignatureIndex() {
        return nodeSignatureIndex;
    }

    public String getRedeemAddress() {
        return redeemAddress;
    }

    public boolean hasNoExplicitTransactions() {
        return transactions.isEmpty() || transactions.stream().allMatch(t -> t.isEmpty());
    }

    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
    }

    public void setPreviousBlockHash(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public void setLedgerHash(String ledgerHash) {
        this.ledgerHash = ledgerHash;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public void setNodeSignature(String nodeSignature) {
        this.nodeSignature = nodeSignature;
    }

    public void setNodeSignatureIndex(long nodeSignatureIndex) {
        this.nodeSignatureIndex = nodeSignatureIndex;
    }

    public void setRedeemAddress(String redeemAddress) {
        this.redeemAddress = redeemAddress;
    }

    public String getPrintableString() {
        return getRawBlock().substring(0, 30) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        return new EqualsBuilder()
                .append(blockNum, block.blockNum)
                .append(previousBlockHash, block.previousBlockHash)
                .append(blockHash, block.blockHash)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(blockNum)
                .append(previousBlockHash)
                .append(blockHash)
                .toHashCode();
    }

    public String getRawBlockDataWithHash() {
        return getRawBlockData() + ",{" + blockHash + "}";
    }

    public static String getSmallHash(String previousBlockHash){
        String currpreviousBlockHash = previousBlockHash;
        if (currpreviousBlockHash != null && currpreviousBlockHash.length() > 60) {
            currpreviousBlockHash = currpreviousBlockHash.substring(0, 30) + "..." + currpreviousBlockHash.substring(currpreviousBlockHash.length() - 30, currpreviousBlockHash.length());
        }
        return currpreviousBlockHash;

    }
    @Override
    public String toString() {
        return "Block{" +
                "blockNum=" + blockNum +
                ", blockHash='" + getSmallHash(blockHash )+ '\'' +
                ", previousBlockHash='" + getSmallHash(previousBlockHash) + '\'' +
                ", redeemAddress='" + redeemAddress + '\'' +
                ", ledgerHash='" + getSmallHash(ledgerHash )+ '\'' +
//                ", transactions=" + transactions +
                ", nodeSignature='" + getSmallHash(nodeSignature )+ '\'' +
                ", nodeSignatureIndex=" + nodeSignatureIndex +
                '}';
    }
}

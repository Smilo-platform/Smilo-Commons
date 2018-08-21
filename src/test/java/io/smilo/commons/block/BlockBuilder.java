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
import io.smilo.commons.ledger.AddressManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BlockBuilder {

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private BlockParser blockParser;

    @Autowired
    private AddressManager addressManager;

    public BlockBuildCommand blank() {
        return blank("redeemAddress", "ledgerHash", new ArrayList<>(), "nodeSignature,comma", 0);
    }

    public BlockBuildCommand blank(Block previousBlock, String redeemAddress, String ledgerHash, List<Transaction> transactions, String nodeSignature, int nodeSignatureIndex) {
        if (previousBlock == null) {
            return blank(0, "0000000000000000000000000000000000000000000000000000000000000000", redeemAddress, ledgerHash, transactions, nodeSignature, nodeSignatureIndex);
        }
        return blank(previousBlock.getBlockNum() + 1, previousBlock.getBlockHash(), redeemAddress, ledgerHash, transactions, nodeSignature, nodeSignatureIndex);
    }

    public BlockBuildCommand blank(String redeemAddress, String ledgerHash, List<Transaction> transactions, String nodeSignature, int nodeSignatureIndex) {
        return blank(blockStore.getLastBlock(), redeemAddress, ledgerHash, transactions, nodeSignature, nodeSignatureIndex);
    }

    public BlockBuildCommand blank(long blockNum, String blockHashString, String redeemAddress, String ledgerHash, List<Transaction> transactions, String nodeSignature, int nodeSignatureIndex) {
        return new BlockBuildCommand().blank(blockNum, blockHashString, redeemAddress, ledgerHash, transactions, nodeSignature, nodeSignatureIndex);
    }

    public BlockBuildCommand genesis() {
        return blank("redeemAddress", "ledgerHash", new ArrayList<>(), "nodeSignature", 0);
    }

    public class BlockBuildCommand {

        private Block block;
        
        public BlockBuildCommand blank(long blockNum, String blockHash, String redeemAddress, String ledgerHash, List<Transaction> transactions, String nodeSignature, int nodeSignatureIndex) {
            block = new Block(System.currentTimeMillis(),
                    blockNum,
                    blockHash,
                    redeemAddress,
                    ledgerHash,
                    transactions,
                    nodeSignature,
                    nodeSignatureIndex);
            return this;
        }

        public BlockBuildCommand withRedeemAddress(String address) {
            block.setRedeemAddress(address);
            return this;
        }

        public BlockBuildCommand withBlockNum(long blockNum) {
            block.setBlockNum(blockNum);
            return this;
        }

        public BlockBuildCommand withTransactions(List<Transaction> transactions) {
            block.setTransactions(transactions);
            return this;
        }

        public BlockBuildCommand withTimestamp(Long timestamp) {
            block.setTimestamp(timestamp);
            return this;
        }

        public BlockBuildCommand sign() {
            blockParser.sign(block, addressManager.getAddressPrivateKey(block.getRedeemAddress()), 0);
            return this;
        }
        
        public Block save() {
            if (block.getRedeemAddress() == null){
                block.setRedeemAddress(addressManager.getDefaultAddress());
            }
            blockParser.hash(block);
            blockStore.getLargestChain().addBlock(block);
            blockStore.saveToFile();
            return block;
        }

        public Block construct() {
            if (block.getRedeemAddress() == null){
                block.setRedeemAddress(addressManager.getDefaultAddress());
            }
            if (block.getBlockHash() == null){
                blockParser.hash(block);
            }

            return block;
        }

    }

}

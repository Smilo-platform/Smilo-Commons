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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smilo.commons.db.Store;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.ByteBuffer.allocateDirect;
import static org.ethereum.util.ByteUtil.longToBytes;

/**
 * Class is responsible for managing the current blockchain state and writing/reading it to the database.
 */
@Component
public class BlockStore {

    @Autowired
    private BlockParser blockParser;
    private static final Logger LOGGER = Logger.getLogger(BlockStore.class);
    protected final Store store;
    // TODO: we can't keep the chain in memory forever, find a way to write a part of the chain to db and only keep the last few blocks in memory
    // TODO: we currently don't write multiple forks to the database. Instead, every block for every chain is saved to the database, which will probably cause errors.
    private List<SmiloChain> chains;
    protected static final String COLLECTION_NAME = "block";

    public BlockStore(Store store) {
        this.chains = new ArrayList<>();
        this.store = store;
        store.initializeCollection(COLLECTION_NAME);
    }

    /**
     * Retrieves the largest fork of the blockchain.
     *
     * @return the largest known chain on this node
     */
    public SmiloChain getLargestChain() {
        return chains.stream()
                .max(Comparator.comparingInt(SmiloChain::getLength))
                .orElse(null);
    }

    /**
     * Returns the length of the tallest tree
     *
     * @return int Length of longest tree in smiloChain
     */
    public int getBlockchainLength() {
        return getLargestChain().getLength();
    }

    /**
     * Writes a block to the smiloChain file
     *
     * @param block to write
     */
    public void writeBlockToFile(Block block) {
        byte[] bytes = blockParser.serialize(block);
        store.put(COLLECTION_NAME, longToBytes(block.getBlockNum()), bytes);
    }

    /**
     * Saves entire smiloChain to a file, useful to save the state of the smiloChain so it doesn't have to be redownloaded later. Blockchain is stored to a file called SMILOCHAIN_DATA inside the
     * provided dbFolder.
     *
     * @return boolean Whether saving to file was successful.
     */
    public void saveToFile() {
        this.chains.forEach(chain -> {
            chain.getBlocks().forEach(this::writeBlockToFile);
        });
    }

    /**
     * Calls getTransactionsInvolvingAddress() on all Block objects in the current Blockchain to get all relevant transactions.
     *
     * @param addressToFind Address to search through all block transaction pools for
     * @return ArrayList<String> All transactions in simplified form blocknum:sender:amount:asset:receiver of
     */
    public List<String> getAllTransactionsInvolvingAddress(String addressToFind) {
        SmiloChain longestChain = getLargestChain();

        List<String> allTransactions = new ArrayList<>();

        for (int i = 0; i < longestChain.getLength(); i++) {
            List<String> transactionsFromBlock = longestChain.getBlockByIndex(i).getTransactionsInvolvingAddress(addressToFind);
            for (int j = 0; j < transactionsFromBlock.size(); j++) {
                allTransactions.add(longestChain.getBlockByIndex(i).getBlockNum() + ":" + transactionsFromBlock.get(j));
            }
        }
        return allTransactions;
    }

    /**
     * Returns the last block of the largest chain
     *
     * @return the last block of the largest chain
     */
    public Block getLastBlock() {
        SmiloChain chain = getLargestChain();
        if (chain == null) {
            return null;
        }
        return chain.getLastBlock();
    }

    /**
     * Retrieves all forks on this node
     *
     * @return all forks of the smilochain on this node
     */
    public List<SmiloChain> getAll() {
        return this.chains;
    }

    /**
     * Checks if any of the chain forks contains the given blockHash
     *
     * @param blockHash blockHash to check
     * @return
     */
    public boolean containsHash(String blockHash) {
        return chains.stream()
                .anyMatch(chain -> chain.containsHash(blockHash));
    }

    /**
     * Removes all forks that are 10 blocks behind the largest chain.
     */
    public void cleanUpChains() {
        chains = chains.stream().filter(c -> c.getLength() > getBlockchainLength() - 10).collect(Collectors.toList());
    }

    /**
     * Adds a chain to the list of forks.
     *
     * @param chain chain to add
     */
    public void addSmiloChain(SmiloChain chain) {
        chains.add(chain);
    }

    /**
     * Retrieves a block by blockNumb
     *
     * @param blockNum blockNum to query for
     * @return the block containing the given blockNum
     */
    public Block getBlock(long blockNum) {
        Block result = null;
        byte[] raw = store.get(COLLECTION_NAME, longToBytes(blockNum));
        if (raw != null && raw.length > 0) {
            result = blockParser.deserialize(raw);
        }
        return result;
    }

    /**
     * Retrieves the last block from the database
     *
     * @return the last block from the database
     */
    public Block getLatestBlockFromStore() {
        Map.Entry<byte[], byte[]> m = store.last(COLLECTION_NAME);
        if (m == null) {
            return null;
        }
        byte[] raw = m.getValue();
        return blockParser.deserialize(raw);
    }

    /**
     * Checks if there are any blocks stored in the database
     *
     * @return true if a block is stored in the database, otherwise false
     */
    public boolean blockInBlockStoreAvailable() {
        if (store.getEntries(COLLECTION_NAME) > 0L) {
            LOGGER.debug("Block has an entry");
            return true;
        } else {
            LOGGER.debug("Block has no entries");
            return false;
        }
    }


//
//    /*
// * Copyright (c) 2018 Smilo Platform B.V.
// *
// * Licensed under the Apache License, Version 2.0 (the “License”);
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an “AS IS” BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package io.smilo.api.block;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.smilo.api.address.AddressManager;
//import io.smilo.api.address.AddressStore;
//import io.smilo.api.db.Store;
//import org.apache.log4j.Logger;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//import static java.nio.ByteBuffer.allocateDirect;
//
//    @Component
//    public class BlockStore {
//
//        private static final Logger LOGGER = Logger.getLogger(BlockStore.class);
//        private final Store store;
//        protected static final String COLLECTION_NAME = "block";
//        private final ObjectMapper dataMapper;
//        private final AddressManager addressManager;
//        private final AddressStore addressStore;
//
//        private long latestBlockHeight = -1;
//        private String latestBlockHash = "0000000000000000000000000000000000000000000000000000000000000000";
//
//        public BlockStore(Store store, ObjectMapper dataMapper, AddressManager addressManager, AddressStore addressStore) {
//            this.addressManager = addressManager;
//            this.addressStore = addressStore;
//            this.store = store;
//            this.dataMapper = dataMapper;
//            store.initializeCollection(COLLECTION_NAME);
//        }
//
//        /**
//         * Initialises the height of the latest block
//         *
//         * @return Boolean
//         */
//        public void initialiseLatestBlock() {
//
//            // When there is a block in the BlockStore, load the latest.
//            // When there is a balance, do nothing, else set 200M Smilo on balance
//            if (blockInBlockStoreAvailable()) {
//                LOGGER.info("Loading block from DB...");
//                BlockDTO latestBlock = getLatestBlockFromStore();
//                latestBlockHeight = latestBlock.getBlockNum();
//                latestBlockHash = latestBlock.getBlockHash();
//            }
//        }
//
//        /**
//         * Returns the height of the latest block
//         *
//         * @return int height of latest block
//         */
//        public long getLatestBlockHeight() {
//            return latestBlockHeight;
//        }
//
//
//        /**
//         * Returns the hash of the latest block
//         *
//         * @return String hash the latests added block
//         */
//        public String getLatestBlockHash() {
//            return latestBlockHash;
//        }
//
//        /**
//         * Set the height of the latest block
//         */
//        public void setLatestBlockHeight(Long blockHeight) {
//            latestBlockHeight = blockHeight;
//        }
//
//
//        /**
//         * Set the hash of the latest block
//         */
//        public void setLatestBlockHash(String hash) {
//            latestBlockHash = hash;
//        }
//
//        /**
//         * Writes a block to the smiloChain file
//         *
//         * @param block to write
//         */
//        public void writeBlockToFile(Block block) {
//            final ByteBuffer key = allocateDirect(64);
//            final ByteBuffer val = allocateDirect(100000000);
//
//            try {
//                BlockDTO dto = BlockDTO.toDTO(block);
//                byte[] bytes = dataMapper.writeValueAsBytes(dto);
//
//                key.putLong(block.getBlockNum()).flip();
//                val.put(bytes).flip();
//                store.put(COLLECTION_NAME, key, val);
//            } catch (JsonProcessingException e) {
//                LOGGER.error("Unable to convert block to byte array " + e);
//            }
//        }
//
//        /**
//         * Get specific block from DB
//         * @Integer blockNum
//         * @return Block
//         */
//        public BlockDTO getBlock(long blockNum) {
//            final ByteBuffer key = allocateDirect(64);
//            key.putLong(blockNum).flip();
//
//            byte[] raw = store.get(COLLECTION_NAME, key);
//            BlockDTO result;
//            try {
//                result = dataMapper.readValue(raw, BlockDTO.class);
//            } catch (IOException e) {
//                LOGGER.debug("Unable to convert byte array to block" + e);
//                return null;
//            }
//
//            return result;
//        }
//
//        public BlockDTO getLatestBlockFromStore() {
//            byte[] raw = store.last(COLLECTION_NAME);
//
//            BlockDTO result = null;
//            try {
//                result = dataMapper.readValue(raw, BlockDTO.class);
//            } catch (Exception e) {
//                LOGGER.error("Unable to convert byte array to block" + e);
//                return null;
//            }
//            if (result == null) return null;
//
//            return result;
//        }
//
//        public Boolean blockInBlockStoreAvailable(){
//            if(store.getEntries(COLLECTION_NAME) > 0L){
//                LOGGER.debug("Block has an entry");
//                return true;
//            } else {
//                LOGGER.debug("Block has no entries");
//                return false;
//            }
//        }
//    }


}

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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a chain of blocks
 */
public class SmiloChain {

    // TODO: LinkedList/LinkedSet?
    private final List<Block> blocks;

    public SmiloChain() {
        this.blocks = new ArrayList<>();
    }

    /**
     * Finds the length of the chain in blocks
     * @return amount of blocks in the chain
     */
    public int getLength() {
        return blocks.size();
    }
    
    /**
     * Checks if the block is empty
     * @return true if no blocks are added, false if blocks are present
     */
    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /**
     * Finds a block by hash
     * @param hash Hash to query the blocks with
     * @return The block with the matching hash or an empty optional
     */
    public Optional<Block> getBlockByHash(String hash) {
        return blocks.stream().filter(block -> block.getBlockHash().equals(hash)).findFirst();
    }

    /**
     * Finds the last block in the chain. Returns an empty optional if no blocks are present in the block
     * @return last block or empty optional
     */
    public Block getLastBlock() {
        if (blocks.isEmpty()) {
            return null;
        }
        return this.blocks.get(blocks.size() -1);
    }

    /**
     * Adds a block to the chain
     * @param block block to add
     */
    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    /**
     * Returns all blocks in the chain
     * @return all blocks in the chain
     */
    public List<Block> getBlocks() {
        return this.blocks;
    }

    /**
     * checks if the chain contains a block with the given blockHash
     * @param blockHash blockHash to query with
     * @return true of the chain contains a block with the given blockHash
     */
    public boolean containsHash(String blockHash) {
        return this.blocks.stream().anyMatch(block -> block.getBlockHash().equals(blockHash));
    }

    /**
     * Returns the block by index throws ArrayOutOfBoundsException thrown when the index is greater than the amount of blocks - 1
     *
     * @param index block index
     * @return block at index
     */
    public Block getBlockByIndex(int index) {
        return blocks.get(index);
    }
}

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
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.peer.IPeer;
import io.smilo.commons.peer.PeerSender;
import io.smilo.commons.peer.PeerStore;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.INetworkState;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Only one SmiloChainService object is created per instance of the daemon. It keeps track of ALL possible chains, and internally handles chain reorganization. The decision to put LedgerManager as an
 * object owned by SmiloChainService was for purposes of simplicity--there's only one SmiloChainService, and only one LedgerManager. The SmiloChainService has fork management integrated, and should be
 * able to appropriately handle any unexpected circumstances. Fork management is one of the major things I'm watching for during 0.0.1a1. If you want to try to fork the network, please do. I'm sure
 * there are ways.
 * <p>
 * As the SmiloChainService has the most up-to-date information about smiloChain data, it makes perfect sense for the ledger, which is based purely on the smiloChain, to be managed by the
 * SmiloChainService object. Initial plans were to have separate SmiloChainService objects for each fork in a chain but the overhead of cloning SmiloChainServices seemed unwarranted. Significant
 * optimization still needs to be done on the fork management--climbing all the way down the shorter chain and back up the longer chain is NOT a permanant solution, and I hope to have respectable fork
 * management overhead by 1.0.0.
 * <p>
 * Additionally, there is no need to store identical blocks between multiple forks. The overhead of a fork suddenly requiring double the smiloChain storage is unacceptable. It works, I think. But
 * unacceptable for production code, so that'll certainly change, hopefully by 1.0.0, depending on my schedule.
 * <p>
 * As blocks are added to the smiloChain, the ledger is updated. While working beautifully in small-scale testing, I'm sure the signature count synchronization between signed transactions and blocks
 * will trip up at some point, and send the SmiloChainService into either a loop of dispair, or an irrecoverable error. Either is equally frightening.
 * <p>
 * Fault tolerance with desynchronization between ledger and smiloChain for signature accounts will be a 0.0.1a6/7 feature. I've gotta think long and hard about the best approaches that don't
 * compromise security in the name of fault-tolerance, while remaining usable, reliable, and fast.
 * <p>
 * You'll notice a loop which appears to retry transactions up to 10,000 times. Transactions must be executed in order--two transactions from the same address need to go in order of signature index,
 * otherwise the storage space required to maintain all used signature indexes would be astronomical.
 * <p>
 * As a caveat to the Merkle Tree signature scheme, Lamport Key reuse creates the potential for double-spend attacks, so once a Lamport Keypair is used, the network rejects all future signatures from
 * that keypair. Important to keep this in mind--
 * <p>
 * A SmiloChainService represents an entire chain of blocks. Only one is created in the entire execution of the program. All blocks will be added individually and in-order.
 * <p>
 * This SmiloChainService will handle all forks--it keeps a record of forks for ten blocks. After a fork falls more than 10 blocks behind, it is deleted.
 */
@Component
public class SmiloChainService {

    private static final Logger LOGGER = Logger.getLogger(SmiloChainService.class);
    private static final int MAX_LOOP_COUNT = 10000;

    //blockhash as key, set of node identifiers
    private Map<String, Set<String>> approvedBlocks = new HashMap<>();
    private final Set<Block> chainQueue;
    private final List<Block> blockQueue;

    // TODO: cleanup allBroadcastBlockHashes every now and then
    private final Set<String> allBroadcastBlockHashes = new HashSet<>();

    private final LedgerManager ledgerManager;
    private final BlockParser blockGenerator;
    private final BlockStore blockStore;
    private final PeerStore peerStore;
    private final INetworkState networkState;
    private final PendingBlockDataPool pendingBlockDataPool;
    private final PeerSender peerSender;

    public SmiloChainService(LedgerManager ledgerManager,
                             BlockParser blockGenerator,
                             BlockStore blockStore,
                             PeerStore peerStore,
                             INetworkState networkState,
                             PendingBlockDataPool pendingBlockDataPool,
                             PeerSender peerSender) {
        this.peerStore = peerStore;
        this.networkState = networkState;
        this.pendingBlockDataPool = pendingBlockDataPool;
        this.blockQueue = new ArrayList<>();
        this.chainQueue = new HashSet<>();
        this.ledgerManager = ledgerManager;
        this.blockGenerator = blockGenerator;
        this.blockStore = blockStore;
        this.peerSender = peerSender;
    }

    /**
     * This method attempts to add a block to the smiloChain. No upstream handling is required to make sure the block is valid, all of that is handled here. Additionally, the block will be
     * automatically placed onto the correct fork, or a new fork will be made if necessary.
     *
     * @param block Block to add to the smiloChain
     * @return boolean Whether adding the block was unsuccessful. Most common source of returning false is a block that doesn't verify.
     */
    private AddBlockResult addBlock(Block block) {
        LOGGER.info("Attempting to add block " + block.getBlockNum() + " with hash " + block.getBlockHash());
        try {
            //A bit of cleanup--remove chains that are more than 10 blocks shorter than the largest chain.
            SmiloChain largestChain = blockStore.getLargestChain();
            if (largestChain == null) {
                LOGGER.error("OMG, longest chain returned null ? let's try to get into catchupMode. ");
                networkState.updateCatchupMode();
                return new AddBlockResult(block, AddResultType.UNKNOWN, "Something went wrong adding the block!");
            }
            final int largestChainLength = largestChain.getLength();
            final String largestChainLastBlockHash = !largestChain.isEmpty() ? largestChain.getLastBlock().getBlockHash() : "";

            //Now we have the longest chain, we remove any chains that are less than largestChainLength - 10 in length.
            //TODO: why though? ಠ_ಠ
            blockStore.cleanUpChains();

            //Block looks fine on its own--we don't know how it's going to play with the chain. If the block's number is larger than the largest chain + 1, we'll put the block in a queue to attempt to add later.
            //Block numbering starts at 0.
            if (block.getBlockNum() > largestChain.getLastBlock().getBlockNum() + 1) {
                //Add it to the queue.

                Set<Block> list = new HashSet<>(blockQueue);
                boolean isAdded = list.add(block);
                if (isAdded) {
                    blockQueue.add(block);
                }
                if (!isAdded) {
                    LOGGER.error("THIS BLOCK IS ALREADY ON blockQueue, WILL IGNORE!! " + block.getBlockNum() + " with starting hash " + block.getBlockHash().substring(0, 20));
                    return new AddBlockResult(block, AddResultType.DUPLICATE, "Block has been added to the queue");
                }
                /*
                 * In the future, the addBlock() method may be changed to return an int, with values representing things like block above existing heights, validation error, block not on any chains, etc.
                 * For now, the boolean indicates simply whether immediate addition of the block to some internal smiloChain was successful.
                 */
                LOGGER.info("Block " + block.getBlockNum() + " with starting hash " + block.getBlockHash().substring(0, 20) + " added to queue...");
                LOGGER.info("LargestChainLength: " + largestChainLength);
                LOGGER.info("block.blockNum: " + block.getBlockNum());
                //Block wasn't added onto any chain (yet)
                return new AddBlockResult(block, AddResultType.QUEUED, "Block has been added to the queue");
            }

            //Then, we will see whether it goes well onto the ends of any existing chains.
            if (blockStore.getAll().stream()
                    .anyMatch(chain -> addBlockToChain(chain, block, largestChainLastBlockHash, largestChain))) {
                blockStore.writeBlockToFile(block);
                return new AddBlockResult(block, AddResultType.ADDED, "Added successfully");
            }

            boolean foundPlaceForBlock = findPlaceForBlock(block);
            if (foundPlaceForBlock) //Was put on a fork. We need to make sure that the dominant smiloChain is represented by the Ledger.
            {
                //Might be stuff here in the future...
            } else {
                //Didn't fit on any existing smiloChain. Probably really old.
                LOGGER.error("Block didn't fit on any existing smiloChain. Probably really old.");
                return new AddBlockResult(block, AddResultType.FORK_ERROR, "Block didn't fit!");
            }
        } catch (Exception e) {
            LOGGER.error("Exception when trying to addBlock", e);
            return new AddBlockResult(block, AddResultType.UNKNOWN, e.getMessage());
        }

        return new AddBlockResult(block, AddResultType.UNKNOWN, "Something went wrong adding the block!");
    }

    /**
     * Attempts to find a chain to place a block in
     *
     * @param block Block to add
     * @return returns true if the block fits on one of the chains, false if not
     */
    private boolean findPlaceForBlock(Block block) {
        for (int i = 0; i < blockStore.getAll().size(); i++) {
            SmiloChain tempChain = blockStore.getAll().get(i); //No working with this ArrayList indirectly; we've got a lot of work to do!
            // TODO: 11?
            for (int j = tempChain.getLength() - 11; j < tempChain.getLength(); j++) {
                if (j < 0) //Pathetically-early network forks handled
                {
                    j = 0; //Negative blocks aren't one of the features of 2.0
                }
                if (tempChain.getBlockByIndex(j).getBlockHash().equals(block.getPreviousBlockHash())) //Found where it forked!
                {
                    // TODO: newChain is never saved! Is this correct?
                    ArrayList<Block> newChain = new ArrayList<>();
                    //Add all of the old chain until we get to the fork
                    for (int k = 0; k <= j; k++) {
                        newChain.add(tempChain.getBlockByIndex(k));
                    }
                    //Add the fork block, which is an alternative to tempChain.get(j + 1)
                    newChain.add(block);
                    return true;
                    //As block didn't fit onto the end of another chain, we don't need to check for the new fork being longer.
                }
            }
        }
        return false;
    }

    // TODO: refactor

    /**
     * Adds a block to a chain
     *
     * @param chain                     chain to add the block to
     * @param block                     block to add
     * @param largestChainLastBlockHash last blockhash of the largest chain
     * @param largestChain              the largest chain on this node
     * @return true if the block was added, false if an error occurred
     */
    private boolean addBlockToChain(SmiloChain chain, Block block, final String largestChainLastBlockHash, SmiloChain largestChain) {
        //Block numbering starts at 0
        LOGGER.debug("Previous block hash according to chain: " + chain.getLastBlock().getBlockHash());
        LOGGER.debug("Previous block hash according to added block: " + block.getPreviousBlockHash());
        LOGGER.debug("Selected chain size: " + chain.getLength());
        LOGGER.debug("Should be equal to block num: " + block.getBlockNum());
        if (block.getPreviousBlockHash().equals(chain.getLastBlock().getBlockHash()) && chain.getLastBlock().getBlockNum() + 1 == block.getBlockNum()) {
            chain.addBlock(block);
            //We might have created a longer fork! We need to check.
            if (chain.getLength() > largestChain.getLength()) //We just created a longer chain--but it might be the original that we added to!
            {
                if (!chain.getBlockByIndex(chain.getLength() - 2).getBlockHash().equals(largestChainLastBlockHash)) //Then we didn't stack onto the correct chain... We need to reverse some blocks in the ledger
                {
                    //Future implementations will be MUCH more efficient--they'll reverse down the fork and ride it back up.
                    //However, during the developmental time squeeze that is two hours before 0.0.1a1 launch when I realized the logic I had here wasn't good, this seemed like a great idea.
                    for (int i = largestChain.getLength() - 1; i > 0; i--) {
                        List<Transaction> transactionsToReverse = largestChain.getBlockByIndex(i).getTransactions();
                        for (int k = 0; k < transactionsToReverse.size(); k++) {
                            ledgerManager.reverseTransaction(transactionsToReverse.get(k));
                        }
                        // ledgerManager.adjustAddressBalance(largestChain.get(j).redeemAddress, -100); // Todo: Reverse mining income...
                        ledgerManager.adjustAddressSignatureCount(largestChain.getBlockByIndex(i).getRedeemAddress(), -1);
                    }
                    //The ledger is completely empty, basically. Good job. Efficiency at its finest. Will be fixed during upcoming refactoring.
                    for (int i = 0; i < chain.getLength(); i++) {
                        addTransactions(block);
                        ledgerManager.adjustAddressSignatureCount(chain.getBlockByIndex(i).getRedeemAddress(), 1);
                    }
                } else //Great, we added to the longest chain!
                {
                    //We need to execute all the transactions...
                    //if (ledgerManager.getLastBlockNum() < block.getBlockNum()) //If the ledger was read in from a file, then we don't add the transactions again!
                    {
                        addTransactions(block);
                        //ledgerManager.adjustAddressBalance(block.redeemAddress, 100); // Todo: Pay mining fee
                        ledgerManager.adjustAddressSignatureCount(block.getRedeemAddress(), 1);
                    }
                }
            }
            addTransactions(block);
            return true;
        } else {
            LOGGER.error("Something went wrong with stacking...");
            LOGGER.error("Last block hash: " + chain.getLastBlock().getBlockHash());
            LOGGER.error("Previous block hash: " + block.getPreviousBlockHash());
            return false;
        }
    }

    /**
     * Creates a first chain and adds the genesis block
     *
     * @param block block to add
     */
    public void createInitialChain(Block block) {
        SmiloChain initial = new SmiloChain();
        blockStore.addSmiloChain(initial);
        initial.addBlock(block);

        if (blockStore.getLastBlock() == null) {
            addTransactions(block);
        }

        ledgerManager.adjustAddressSignatureCount(block.getRedeemAddress(), 1);
        blockStore.writeBlockToFile(block);
    }

    /**
     * Adds all transactions in a block to the ledgerManager
     *
     * @param block block to take the transactions from
     */
    private void addTransactions(Block block) {
        //We can't directly assign transactionsToApply to block.transactions as we are going to edit it, and we don't want to delete transactions from the actual block.
        List<Transaction> transactionsToApply = new ArrayList<>(block.getTransactions());

        int loopCount = 0;
        int transactionsApplied = 0;
        //Yippee let's add our first chunk of transactions if we need to!
        while (transactionsToApply.size() > transactionsApplied && !transactionsToApply.get(0).isEmpty()) {
            loopCount++;

            // Todo! Fix this, i made a quick fix.
            for (int k = 0; k < transactionsToApply.size(); k++) {
                if (ledgerManager.executeTransaction(transactionsToApply.get(k))) {
                    //Executed correctly
                    transactionsToApply.remove(k);
                    //Compensate for changed ArrayList size
                    k--;
                }
            }
            if (loopCount > MAX_LOOP_COUNT) {
                LOGGER.error("Infinite block detected! Hash: " + block.getBlockHash() + " and height: " + block.getBlockNum() + ", TransactionsToApply size " + transactionsToApply.size());
                System.exit(-1); // Todo: make this clean and stable
            }
        }
    }

    // TODO: refactor to object instead of string

    /**
     * Retrieves all transactions with a given address.
     *
     * @param addressToFind address to query for
     * @return All transactions involving the address, represented as a string
     */
    public List<String> getAllTransactionsInvolvingAddress(String addressToFind) {
        return blockStore.getAllTransactionsInvolvingAddress(addressToFind);
    }

    /**
     * If a block is new to the client, the client will attempt to add it to the smiloChain.
     * When added to the smiloChain, it may get added to a chain, put on a new fork, put on an existing, shorter-length chain that's forked less than 10 blocks back, or
     * it may end up being queued or deleted. Queued blocks are blocks that self-isValid (signatures match, etc.) but don't fit onto any chain.
     * They are often used when getting blocks from a peer, in case one arrives out of order.
     */
    public AddBlockResult addBlockToSmiloChain(Block block) {
        LOGGER.info("Attempting to add block...");

        if (hasSeenBefore(block.getBlockHash())) {
            LOGGER.info("Have seen block " + block.getBlockNum() + " before..." + block.getBlockHash() + " not adding.... ");
            tryBlockQueue();
            return new AddBlockResult(block, AddResultType.DUPLICATE, "Block has been seen before");
        } else {
            //Initially, check for duplicate blocks
            if (blockStore.containsHash(block.getBlockHash())) {
                //Duplicate block; block has already been added. This happens all the time, as multiple peers can all broadcast the same block.
                LOGGER.info("Duplicate block received from peer");
                return new AddBlockResult(block, AddResultType.DUPLICATE, "Duplicate block received from peer");
            }

            // TODO: change to preconditions
            if (!blockGenerator.isValid(block)) {
                LOGGER.info("Block validation failed!");
                return new AddBlockResult(block, AddResultType.VALIDATION_ERROR, "Block is not a valid block. Don't add it!");
            }
            //Block has not been previously received, so it will be added to the smiloChain (hopefully)
            LOGGER.info("addBlockToSmiloChain, new block from network! " + block.getBlockNum() + ", Block: " + block.getPrintableString());
            AddBlockResult result = addBlock(block);

            boolean isAdded = result.getType().isSuccess() && !networkState.getCatchupMode();
            if (isAdded) {
                LOGGER.info("Added block " + block.getBlockNum() + " with hash: [" + block.getBlockHash().substring(0, 30) + "..." + block.getBlockHash().substring(block.getBlockHash().length() - 30, block.getBlockHash().length() - 1) + "]");
            }

            if (result.getType() == AddResultType.ADDED) {
                allBroadcastBlockHashes.add(block.getBlockHash());
                LOGGER.debug("Remove processed transactions from BlockDataPool");
                //Remove all transactions from the pendingTransactionPool that appear in the block
                pendingBlockDataPool.removeTransactionsInBlock(block);
                chainQueue.remove(block);

                // Check Queue
                LOGGER.info("Processing queue");
                tryBlockQueue();
            } else if (result.getType() == AddResultType.QUEUED) {
                allBroadcastBlockHashes.add(block.getBlockHash());
                LOGGER.debug("GOING TO PROCESS BLOCK QUEUE, SIZE: " + blockQueue.size());
                tryBlockQueue();
            }

            LOGGER.info("Block : " + result.getType() + " " + result.getMessage() + " , Block: " + block.toString());
            return result;
        }
    }

    /**
     * Checks if a block has been seen before in the incoming broadcasts
     *
     * @param blockHash of the block to check
     * @return true if it has been seen before, false if not
     */
    public boolean hasSeenBefore(String blockHash) {
        return allBroadcastBlockHashes.stream().anyMatch(p -> p.equals(blockHash));
    }

    /**
     * Try to add blocks in the queue to the chain
     */
    private void tryBlockQueue() {
        boolean addedABlock;
        //Some blocks in the queue may be attempted before other dependency blocks, so while we are able to add blocks, we will continue to add them.
        do {
            addedABlock = false;
            for (int i = 0; i < blockQueue.size(); i++) {
                Block block = blockQueue.get(i);
                AddBlockResult result = addBlock(block);

                if (result.getType() == AddResultType.ADDED) {
                    //Remove all transactions from the pendingTransactionPool that appear in the block
                    pendingBlockDataPool.removeTransactionsInBlock(block);
                    addedABlock = true;
                }
                if (result.getType() != AddResultType.QUEUED) {
                    // if it was not queued, it has either been added to the chain or it was invalid.
                    blockQueue.remove(i);
                    i--; //Compensate for changing ArrayList size, don't want to skip an element!
                }
            }
        } while (addedABlock);

        networkState.updateCatchupMode();
    }

    /**
     * Retrieve the timestamp of the last block
     *
     * @return timestamp of the last block
     */
    public Long getLastBlockTimeStamp() {
        return blockStore.getLastBlock().getTimestamp();
    }

    /**
     * Adds a valid block to the chainQueue and checks the approval rate of the block
     *
     * @param block the block we add to the chainQueue
     * @return true if the block was added, false if an error occurred
     */
    public AddBlockResult addBlockToChainQueue(Block block) {
        LOGGER.info("Attempting to add block...");

        if (hasSeenBefore(block.getBlockHash())) {
            LOGGER.info("Have seen block " + block.getBlockHash() + " before... not adding.");
            return new AddBlockResult(block, AddResultType.DUPLICATE, "Block has been seen before");
        } else {
            //Initially, check for duplicate blocks
            if (blockStore.containsHash(block.getBlockHash())) {
                //Duplicate block; block has already been added. This happens all the time, as multiple peers can all broadcast the same block.
                LOGGER.info("Duplicate block received from peer");
                return new AddBlockResult(block, AddResultType.DUPLICATE, "Duplicate block received from peer");
            }

            // TODO: change to preconditions
            if (!blockGenerator.isValid(block)) {
                LOGGER.info("Block validation failed!");
                return new AddBlockResult(block, AddResultType.VALIDATION_ERROR, "Block is not a valid block. Don't add it!");
            }

            chainQueue.add(block);

            boolean isApproved = checkBlockApprovedStatus(block.getBlockHash());
            LOGGER.debug("if block not found on chain?");
            if (!isApproved && networkState.getTopBlock() + 1 < block.getBlockNum()) {
                LOGGER.error("BLOCK NOT FOUND, WILL UPDATE topBlock AND REQUEST_NET_STATE");
                networkState.setTopBlock(block.getBlockNum());
                List<IPeer> ls = new ArrayList<>(peerStore.getPeers());
                for (int i = 0; i < ls.size(); i++) {
                    IPeer p = ls.get(i);
                    if (p != null) {
                        p.write("REQUEST_NET_STATE");
                    }
                }
            }


            return new AddBlockResult(block, AddResultType.QUEUED, "Block added to chain queue");
        }
    }

    /**
     * Adds an approved block to approvedBlocks when a peer approves it. Afterwards we check if 66% approved the block
     *
     * @param block
     * @param peerIdentifier
     */
    public AddResultType addApprovedBlock(Block block, String peerIdentifier) {
        String blockHash = block.getBlockHash();
        if (!hasSeenBefore(blockHash)) {
            Set<String> identifiers = approvedBlocks.getOrDefault(blockHash, new HashSet<>());

            identifiers.add(peerIdentifier);
            approvedBlocks.put(blockHash, identifiers);
            boolean isApproved = checkBlockApprovedStatus(blockHash);

            if (!isApproved && networkState.getTopBlock() + 2 < block.getBlockNum()) {
                LOGGER.error(" ########### BLOCK "+networkState.getTopBlock() + 2 +" IS LOWER THAN TOPBLOCK FOUND IN CHAIN "+block.getBlockNum()+", WILL UPDATE topBlock AND REQUEST_NET_STATE  ########### ");
                networkState.setTopBlock(block.getBlockNum());
                List<IPeer> ls = new ArrayList<>(peerStore.getPeers());
                for (int i = 0; i < ls.size(); i++) {
                    IPeer p = ls.get(i);
                    if (p != null) {
                        p.write("REQUEST_NET_STATE");
                    }
                }
//                return AddResultType.VALIDATION_ERROR;
            }
        } else {
//            LOGGER.warn(" ########### BLOCK DUPLICATED  ########### ");
            boolean isApproved = checkBlockApprovedStatus(blockHash);
            if (!isApproved && networkState.getTopBlock() + 1 < block.getBlockNum()) {
                LOGGER.error(" ########### BLOCK NOT FOUND IN CHAIN, WILL UPDATE topBlock AND REQUEST_NET_STATE  ########### ");
                networkState.setTopBlock(block.getBlockNum());
                List<IPeer> ls = new ArrayList<>(peerStore.getPeers());
                for (int i = 0; i < ls.size(); i++) {
                    IPeer p = ls.get(i);
                    if (p != null) {
                        p.write("REQUEST_NET_STATE");
                    }
                }
//                return AddResultType.VALIDATION_ERROR;
            }
            return AddResultType.ADDED;
        }

        return AddResultType.ADDED;
    }

    /**
     * Checks with a blockhash if a block has been approved by 66% of the peers
     * If a block has 66% approval we'll retrieve the block from the chainQueue and add it to the smilochain
     * If a block doesn't have 66% approval we'll do nothing
     *
     * @param blockHash the blockhash to check
     */
    private boolean checkBlockApprovedStatus(String blockHash) {
        Set<String> identifiers = approvedBlocks.getOrDefault(blockHash, new HashSet<>());
        int peerSize = peerStore.getPeers().size();

        if ((identifiers.size() * 100 / peerSize) >= 66) {
            LOGGER.info("\033[32m66 ########### 66% approved block: " + blockHash + "########### \033[39m");
            Block block = chainQueue.stream().filter(b -> b.getBlockHash().equals(blockHash)).findFirst().orElse(null);
            if (block == null) {
                LOGGER.error(" ########### Block not found in chain queue ###########");
                return false;
            } else {
                AddBlockResult result = addBlockToSmiloChain(block);
                if (!result.getType().name().equals(AddResultType.ADDED.name()) && !result.getType().name().equals(AddResultType.QUEUED.name())) {
                    LOGGER.error(" ########### Could not add block to smiloChain -> " + result.getType().name() + " ###########");
                }
                LOGGER.debug("########### Going to broadcast COMMIT of the approved block " + block.toString() + " ###########");
                peerSender.broadcastContent(PayloadType.COMMIT, block);

                approvedBlocks.remove(blockHash);
                return true;
            }
        }

        return false;
    }

}
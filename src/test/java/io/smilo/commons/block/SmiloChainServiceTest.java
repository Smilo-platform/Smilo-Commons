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

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionBuilder;
import io.smilo.commons.ledger.AddressManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@Category({StableTests.class})
public class SmiloChainServiceTest extends AbstractSpringTest {

    @Autowired
    private SmiloChainService smiloChainService;
    
    @Autowired
    private BlockStore blockStore;
    
    @Autowired
    private TransactionBuilder transactionBuilder;
    
    @Autowired
    private BlockBuilder blockBuilder;

    @Autowired
    private BlockParser blockParser;

    @Autowired
    private AddressManager addressManager;
    
    @After
    public void cleanUp() {
        ReflectionTestUtils.setField(smiloChainService, "allBroadcastBlockHashes", new HashSet<>());
        ReflectionTestUtils.setField(smiloChainService, "blockQueue", new ArrayList<>());
    }
    
    @Test
    public void testAddBlockToSmiloChainSeenBefore() {
        Set<String> blockHashes = new HashSet<>();
        Block block = blockBuilder.blank("redeemAddress", "ledgerHash", new ArrayList<>(), "nodeSignature", 0).construct();
        blockHashes.add(block.getBlockHash());
        
        ReflectionTestUtils.setField(smiloChainService, "allBroadcastBlockHashes", blockHashes);
        
        smiloChainService.addBlockToSmiloChain(block);
        assertFalse(blockStore.getLastBlock().equals(block));
    }
    
    @Test
    public void testQueue() {
        String address = addressManager.getDefaultAddress();
        String privateKey = addressManager.getAddressPrivateKey(address);
        Block block1 = blockBuilder.blank(address, "ledgerHash", new ArrayList<>(), "nodeSignature", 0).construct();
        blockParser.hash(block1);
        blockParser.sign(block1, privateKey, 0);
        Block block2 = blockBuilder.blank(block1, address, "ledgerHash", new ArrayList<>(), "nodeSignature", 1).construct();
        blockParser.hash(block2);
        blockParser.sign(block2, privateKey, 1);
        
        // Add block2 before block1. This should queue block2 until block1 has been added
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block2);
        assertFalse(blockStore.getLastBlock().equals(block2));
        assertEquals(AddResultType.QUEUED, result.getType());
        
        AddBlockResult r = smiloChainService.addBlockToSmiloChain(block1);
        assertFalse(blockStore.getLastBlock().getBlockHash().equals(block2.getBlockHash()));
    }
    
    @Test
    public void testDuplicate() {
        Block block = blockBuilder.blank("redeemAddress", "ledgerHash", new ArrayList<>(), "nodeSignature", 0).save();
        
        int size = blockStore.getBlockchainLength();
        
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block);
        assertEquals(AddResultType.DUPLICATE, result.getType());
        
        assertEquals(size, blockStore.getBlockchainLength());
    }
    
    @Test
    public void testValidateError() {
        Transaction transaction = transactionBuilder.kelly_funds_robert_incorrect_hash().construct();
        
        Block block = blockBuilder.blank("redeemAddress", "ledgerHash", asList(transaction), "nodeSignature,0", 0).construct();
        
        int size = blockStore.getBlockchainLength();
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block);
        assertEquals(AddResultType.VALIDATION_ERROR, result.getType());
        assertEquals(size, blockStore.getBlockchainLength());
    }
    
    @Test
    public void testForkError() {
        // blocknum matches, previous block hash does not, causing a fork error
        String address = addressManager.getDefaultAddress();
        Block block = blockBuilder.blank(blockStore.getBlockchainLength(), "asdas", address, "ledgerHash", new ArrayList<>(), "nodeSignature", 0).construct();
        blockParser.hash(block);
        blockParser.sign(block, addressManager.getAddressPrivateKey(address), 0);
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block);
        assertEquals(AddResultType.FORK_ERROR, result.getType());
    }

    @Test
    public void testAddBlockToSmiloChain() {
        String address = addressManager.getDefaultAddress();
        Block block = blockBuilder.blank(address, "ledgerHash", new ArrayList<>(), "nodeSignature", 0)
                .construct();
        blockParser.hash(block);
        blockParser.sign(block, addressManager.getAddressPrivateKey(address), 0);
        int size = blockStore.getBlockchainLength();
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block);
        assertEquals(AddResultType.ADDED, result.getType());
        assertEquals(size + 1, blockStore.getBlockchainLength());
    }

    @Test
    public void testAddBlockToSmiloChainHighBlockNum() {
        String address = addressManager.getDefaultAddress();
        for (int i = 0; i < 130; i++) {
            blockBuilder.blank(address, "ledgerHash", new ArrayList<>(), "nodeSignature", 0)
                    .save();
        }

        Block block2 = blockBuilder.blank(address, "ledgerHash", new ArrayList<>(), "nodeSignature", 0)
                .construct();
        blockParser.hash(block2);
        blockParser.sign(block2, addressManager.getAddressPrivateKey(address), 0);
        int size = blockStore.getBlockchainLength();
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block2);
        assertEquals(AddResultType.ADDED, result.getType());
        assertEquals(size + 1, blockStore.getBlockchainLength());
    }
    
    @Test
    public void testGetAllTransactionsInvolvingAddress() {
        Transaction t1 = transactionBuilder.elkan_shares_wealth().construct();
        Transaction t2 = transactionBuilder.kelly_funds_robert_incorrect_hash().construct();
        
        Block block = blockBuilder.blank("redeemAddress", "ledgerHash", asList(t1, t2), "nodeSignature", 0).withBlockNum(1L).construct();

        blockStore.getLargestChain().addBlock(block);
        
        List<String> transactions = smiloChainService.getAllTransactionsInvolvingAddress(t2.getInputAddress());
        assertEquals(2, transactions.size());
        
        // TODO: Check for kelly().getAddress() and elkan.getAddress() instead of hardcoded values once builders are setup correctly
        assertTrue(transactions.stream().anyMatch(t -> t.contains("S1HY2JKBM44VJWCBZROTMOM5B3BYZWD7FVDVFS:100"))); // 1 from kelly to robert
        assertTrue(transactions.stream().anyMatch(t -> t.contains("S17LXTYN7HC4VHVTYZDENSTZOAD47HKV5FBEHR:1"))); // 100 from elkan to kelly
    }

    @Test
    public void testGetBlock() {
        Block block = blockBuilder.blank(addressManager.getDefaultAddress(), "myLedgerHash", new ArrayList<>(), "nodeSignature", 0).save();
        Block dto = blockStore.getBlock(block.getBlockNum());
        Assert.assertEquals(addressManager.getDefaultAddress(), block.getRedeemAddress());
        Assert.assertEquals("myLedgerHash", dto.getLedgerHash());
        Assert.assertEquals(block.getBlockNum(), dto.getBlockNum());
    }

    @Test
    public void testCreateInitialChain(){
        Block block = blockBuilder.blank("myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0).save();
        smiloChainService.createInitialChain(block);
        assertEquals(block,blockStore.getLastBlock());
    }

}

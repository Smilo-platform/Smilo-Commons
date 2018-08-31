/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smilo.commons.block;

import io.smilo.commons.pendingpool.PendingBlockDataPool;
import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.HashUtility;
import io.smilo.commons.StableTests;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionBuilder;
import io.smilo.commons.ledger.AddressManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@Category({StableTests.class})
public class BlockParsingTest extends AbstractSpringTest {

    @Autowired
    private TransactionBuilder transactionBuilder;

    @Autowired
    private BlockBuilder blockBuilder;

    @Autowired
    private AddressManager addressManager;

    @Autowired
    private PendingBlockDataPool pendingBlockDataPool;

    @Autowired
    private SmiloChainService smiloChainService;

    @Autowired
    private BlockParser blockParser;

    @Test
    public void testSerialize() {
        Block block = new Block(1527514557052L, 0, "abdcedfg", "myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        block.setBlockHash("XXXXXXXXXXXXXXX");
        byte[] serialized = blockParser.serialize(block);
        Assert.assertTrue(serialized.length > 0);
    }

    @Test
    public void testDeserializeEmptyTransactions() {
        Block block = new Block(1527514557052L, 0, "abdcedfg", "myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        block.setBlockHash("XXXXXXXXXXXXXXX");
        byte[] serialized = blockParser.serialize(block);

        Block result = blockParser.deserialize(serialized);
        assertEquals(1527514557052L, result.getTimestamp().longValue());
        assertEquals(0L, result.getBlockNum());
        assertEquals("abdcedfg", result.getPreviousBlockHash());
        assertEquals("myRedeemAddress", result.getRedeemAddress());
        assertEquals("XXXXXXXXXXXXXXX", result.getBlockHash());
        Assert.assertTrue(result.getTransactions().isEmpty());
        assertEquals("myLedgerHash", result.getLedgerHash());
        assertEquals("nodeSignature", result.getNodeSignature());
        assertEquals(0L, result.getNodeSignatureIndex());
    }

    @Test
    public void testHash() throws NoSuchAlgorithmException {
        Block block = new Block(1527514557052L, 0, "", "myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);

        String hash = HashUtility.digestSHA256ToHEX((block.getRawBlockData()));

        blockParser.hash(block);
        assertEquals(hash, block.getBlockHash());
    }

    @Test
    public void testSupports() {
        Block block = new Block(1527514557052L, 0, "", "myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        Assert.assertTrue(blockParser.supports(block.getClass()));
    }

    @Test
    public void testIfParsingBlockCleansBlockDataPool() {
        // Build transaction
        Transaction transaction = transactionBuilder.elkan_shares_wealth().construct();
        ArrayList<Transaction> txs = new ArrayList<>();
        txs.add(transaction);

        // Build block
        Block block = blockBuilder.blank().withRedeemAddress(addressManager.getDefaultAddress()).withTransactions(txs).sign().construct();

        // Add transaction to blockDataPool
        pendingBlockDataPool.addBlockDataToPool(transaction);

        assertEquals(1, pendingBlockDataPool.getPendingBlockData().size());

        // Process Block
        AddBlockResult result = smiloChainService.addBlockToSmiloChain(block);
        assertTrue(result.getType().isSuccess());

        assertEquals(0, pendingBlockDataPool.getPendingBlockData().size());
    }

}

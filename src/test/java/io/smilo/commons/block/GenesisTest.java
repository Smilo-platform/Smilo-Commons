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
import io.smilo.commons.block.genesis.GenesisLoader;
import io.smilo.commons.db.Store;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.ledger.LedgerStore;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class GenesisTest extends AbstractSpringTest {

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private GenesisLoader genesisLoader;

    @Autowired
    private LedgerManager ledgerManager;

    @Autowired
    private LedgerStore ledgerStore;

    @Autowired
    private Store store;

    private static final String COLLECTION_NAME = "block";

    @Before
    public void BlockDataStoreBefore() {
        store.clear(COLLECTION_NAME);
        ledgerStore.clearAccounts();
        ReflectionTestUtils.setField(blockStore, "chains", new ArrayList<>());
    }

    @Test
    public void testGetGenesisBlockFromStore() {
        genesisLoader.loadGenesis();
        assertTrue(blockStore.blockInBlockStoreAvailable());
        Block latest = blockStore.getLastBlock();
        System.out.println("DEBUG: " + latest.getBlockNum());
        System.out.println("DEBUG: " + latest.getBlockHash());
        System.out.println("DEBUG: " + latest.getRawBlockData());
        System.out.println("DEBUG: " + blockStore.getLargestChain().getLength());
        assertEquals(0, latest.getBlockNum());
    }

    @Test
    public void testGetLatestBlockFromStoreWhenNoBlockAvailable() {
        assertFalse(blockStore.blockInBlockStoreAvailable());
    }

    @Test
    public void testGetLatestBlockFromBlockStoreWhenNoBlockAvailable() {
        assertNull(store.last(COLLECTION_NAME));
    }

    @Test
    public void testGetLatestBlockFromBlockStoreWhenBlockAvailable() {
        genesisLoader.loadGenesis();
        assertNotNull(store.last(COLLECTION_NAME));
        Block latestBlock = blockStore.getLatestBlockFromStore();
        assertEquals(latestBlock.getBlockNum(), 0);
    }

    @Test
    public void testGenesisBalance() {
        genesisLoader.loadGenesis();
        assertEquals(ledgerManager.getAddressBalance("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR"),BigInteger.valueOf(200000000L));
    }

}

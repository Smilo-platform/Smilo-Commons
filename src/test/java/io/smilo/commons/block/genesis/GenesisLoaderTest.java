package io.smilo.core.block.genesis;

import io.smilo.commons.block.BlockBuilder;
import io.smilo.commons.block.SmiloChainService;
import io.smilo.commons.block.genesis.GenesisLoader;
import io.smilo.commons.peer.PeerStore;
import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.block.Block;
import io.smilo.commons.block.BlockStore;
import io.smilo.commons.block.data.transaction.TransactionBuilder;
import io.smilo.commons.peer.PeerBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.Assert.*;

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
public class GenesisLoaderTest extends AbstractSpringTest {

    @Autowired
    private GenesisLoader genesisLoader;

    @Autowired
    private TransactionBuilder transactionBuilder;

    @Autowired
    private BlockBuilder blockBuilder;

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private PeerBuilder peerBuilder;

    @Autowired
    private SmiloChainService smiloChainService;

    @Autowired
    private PeerStore peerStore;

    @Test
    public void loadGenesis() {
        super.cleanUpFiles();
        genesisLoader.loadGenesis();
        assertEquals(0, blockStore.getLastBlock().getBlockNum());
        // If anything changed, the hash below will change. Validating the hash should validate all data inside indirectly.
        assertEquals("550CCC58D66FB748B59CEA8314E396545A2BCD7DCCD2CEA6FAAE29F64FBD356D", blockStore.getLastBlock().getBlockHash());
    }

    @Test
    public void loadGenesisFromDatabase() {
        peerBuilder.peer_ready().save();

        transactionBuilder.elkan_shares_wealth().queue();
        Block block = blockBuilder.blank().construct();
        peerStore.getPeers().forEach(p -> {
            smiloChainService.addApprovedBlock(block.getBlockHash(), p.getIdentifier());
        });
        smiloChainService.addBlockToChainQueue(block);

        // reset the memory storage, in hopes that the genesisloader will load the blocks from database
        ReflectionTestUtils.setField(blockStore, "chains", new ArrayList<>());

        genesisLoader.loadGenesis();

        assertEquals(block.getBlockHash(), blockStore.getLastBlock().getBlockHash());
    }
}
package io.smilo.commons.block;

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.peer.MockPeer;
import io.smilo.commons.peer.PeerBuilder;
import io.smilo.commons.peer.PeerReceiver;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertTrue;
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
public class BlockSyncerTest extends AbstractSpringTest {

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private PeerBuilder peerBuilder;

    @Autowired
    private PeerReceiver peerReceiver;

    @Autowired
    private BlockSyncer blockSyncer;

    @Test
    public void testBroadcastNewBlockRequest() {
        int blockNum = blockStore.getBlockchainLength();
        MockPeer peer = peerBuilder.peer_ready().save();
        peer.write("NETWORK_STATE " + blockNum + 1);
        peerReceiver.getNewData();

        blockSyncer.broadcastNewBlockRequest();

        assertContainsMessage(peer, "GET_BLOCK " + (blockNum + 1));
    }

    private void assertContainsMessage(MockPeer peer, String message) {
        assertTrue(peer.getWrittenData().contains(message));
    }
}
package io.smilo.commons.peer;

import io.smilo.commons.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
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
public class PeerClientTest extends AbstractSpringTest {

    @Autowired
    private PeerClient peerClient;

    @Autowired
    private PeerBuilder peerBuilder;

    @Autowired
    public PeerStore peerStore;

    @Value("#{'${NODES_LIST}'.split(',')}")
    private List<String> nodes;

    @Before
    public void cleanUpPeers() {
        peerStore.clear();
    }

    @Test
    public void testConnectToPeer() {
        IPeer peer = peerBuilder.peer_uninitialized().construct();
        peerClient.connectToPeer(peer);

        assertTrue(peer.isInitialized());
        assertTrue(peerClient.getPeers().stream().anyMatch(p -> p.getIdentifier().equals(peer.getIdentifier())));
    }

    @Test
    public void testInitialize() {
        peerClient.initializePeers();
        Set<IPeer> peers = peerClient.getPeers();

        // Check if all peers from application-test.properties have been loaded correctly
        assertTrue(containsPeer(peers, "127.0.0.1", 8021));
        assertTrue(containsPeer(peers, "127.0.0.1", 8022));
        assertTrue(containsPeer(peers, "127.0.0.1", 8023));
        assertTrue(containsPeer(peers, "[::1]", 8024));
    }

    private boolean containsPeer(Set<IPeer> peers, String connectHost, int connectPort) {
        return peers.stream()
                .anyMatch(p -> p.getConnectHost().equals(connectHost) && p.getConnectPort() == connectPort);
    }

    @Test
    public void testBroadcast() {
        IPeer peer = peerBuilder.peer_ready().save();
        peerClient.broadcast("REQUEST_NET_STATE");
        assertTrue(((MockPeer) peer).getWrittenData().get(0).startsWith("REQUEST_NET_STATE"));
    }

    @Test
    public void testBroadcastIgnorePeer() {
        List<IPeer> peers = asList(peerBuilder.peer_ready().withConnectHost("localhost").withIdentifier("id1").save(), peerBuilder.peer_ready().withConnectHost("localhost").withIdentifier("id2").save());

        IPeer ignorePeer = peerBuilder.peer_ready().save();
        peerClient.broadcastIgnorePeer("REQUEST_NET_STATE", ignorePeer);
        for (IPeer peer : peers) {
            assertTrue(((MockPeer) peer).getWrittenData().get(0).startsWith("REQUEST_NET_STATE"));
        }
        assertEquals(0, ((MockPeer) ignorePeer).getWrittenData().size());
    }

    @Test
    public void testGetRandomPeer() {
        peerBuilder.peer_ready().save();
        peerBuilder.peer_ready().save();

        Set<IPeer> peers = peerClient.getPeers();
        IPeer randomPeer = peerClient.getRandomPeer();

        assertTrue(peers.contains(randomPeer));
    }


}
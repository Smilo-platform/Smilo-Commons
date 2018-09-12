package io.smilo.commons.peer;

import io.smilo.commons.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
public class PeerEncoderTest extends AbstractSpringTest {

    @Autowired
    private PeerBuilder peerBuilder;

    @Autowired
    private PeerEncoder peerEncoder;

    @Test
    public void testEncodeDecode() {
        IPeer peer = peerBuilder.peer_ready().withCapability(Capability.P2P, (byte)1).construct();
        byte[] encodedPeer = peerEncoder.encode(peer);
        IPeer decodedPeer = peerEncoder.decode(encodedPeer);

        assertEquals(peer.getConnectHost(), decodedPeer.getConnectHost());
        assertEquals(peer.getIdentifier(), decodedPeer.getIdentifier());
        assertEquals(peer.getConnectPort(), decodedPeer.getConnectPort());
        assertEquals(1, decodedPeer.getCapabilities().size());
        assertEquals(1, decodedPeer.getCapabilities().get(0).getVersion());
        assertEquals(Capability.P2P, decodedPeer.getCapabilities().get(0).getName());
    }

}
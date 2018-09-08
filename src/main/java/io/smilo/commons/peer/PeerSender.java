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

package io.smilo.commons.peer;

import io.smilo.commons.HashUtility;
import io.smilo.commons.block.Content;
import io.smilo.commons.block.ParserProvider;
import io.smilo.commons.block.data.Parser;
import io.smilo.commons.peer.network.Network;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class PeerSender {

    private static final Logger LOGGER = Logger.getLogger(PeerSender.class);

    private final PeerClient peerClient;
    private final ParserProvider parserProvider;

    public PeerSender(PeerClient peerClient, ParserProvider parserProvider) {
        this.peerClient = peerClient;
        this.parserProvider = parserProvider;
    }

    public void broadcast(PayloadType type, String data) {
        peerClient.broadcast(type.name() + " " + data);
    }

    public void broadcast(PayloadType type) {
        peerClient.broadcast(type.name());
    }

    public void broadcastContent(PayloadType type, Content content) {
        Parser parser = parserProvider.getParser(content.getClass());
        String data = HashUtility.encodeToBase64(parser.serialize(content));
        broadcast(type, data);
    }

    public void broadcastContent(Content content) {
        broadcastContent(PayloadType.valueOf(StringUtils.upperCase(content.getClass().getSimpleName())), content);
    }

    public void broadcastToNetwork(Network network, PayloadType payloadType, String data, boolean includeUnconfirmed) {
        broadcastToNetwork(network, payloadType.name() + " " + data, includeUnconfirmed);
    }

    public void broadcastToNetwork(Network network, PayloadType payloadType, String data) {
        broadcastToNetwork(network, payloadType, data, false);
    }

    public void broadcastToNetwork(Network network, PayloadType payloadType, boolean includeUnconfirmed) {
        broadcastToNetwork(network, payloadType.name(), includeUnconfirmed);
    }

    public void broadcastToNetwork(Network network, PayloadType payloadType) {
        broadcastToNetwork(network, payloadType, false);
    }

    private void broadcastToNetwork(Network network, String data, boolean includeUnconfirmed) {
        LOGGER.trace("Broadcasting " + data + " to " + network.getIdentifier());

        Set<String> peerIdentifiers = new HashSet<>(network.getPeerIdentifiers());
        if (includeUnconfirmed) {
            peerIdentifiers.addAll(network.getUnconfirmedPeerIdentifiers());
        }

        peerIdentifiers.stream()
                .map(peerClient::getPeerByIdentifier)
                .filter(Objects::nonNull)
                .forEach(peer -> {
                    peer.write(data);
                });
    }
}
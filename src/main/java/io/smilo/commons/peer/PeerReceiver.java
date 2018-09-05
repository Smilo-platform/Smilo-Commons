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

import io.smilo.commons.block.BlockStore;
import io.smilo.commons.peer.payloadhandler.PayloadHandlerProvider;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.INetworkState;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class PeerReceiver {

    private static final Logger LOGGER = Logger.getLogger(PeerReceiver.class);

    private final BlockStore blockStore;
    private final PeerClient peerClient;
    private final PayloadHandlerProvider payloadHandlerProvider;
    private final INetworkState networkState;

    private final Long pingInterval;
    private final int maxConnectionAttempts;
    private final INetworkUpdater networkUpdater;

    public PeerReceiver(BlockStore blockStore,
                        PeerClient peerClient,
                        PayloadHandlerProvider payloadHandlerProvider,
                        INetworkState networkState,
                        @Value("${PING_INTERVAL:4320000}") Long pingInterval,
                        @Value("${MAX_CONNECTION_ATTEMPTS:7}") int maxConnectionAttempts, INetworkUpdater networkUpdater) {
        this.blockStore = blockStore;
        this.peerClient = peerClient;
        this.payloadHandlerProvider = payloadHandlerProvider;
        this.networkState = networkState;
        this.pingInterval = pingInterval;
        this.maxConnectionAttempts = maxConnectionAttempts;
        this.networkUpdater = networkUpdater;
    }

    /**
     * Retrieve and handle new data from peers
     */
    public void getNewData() {
        LOGGER.trace("Look for new data from peers...");
        LOGGER.trace("Connected with " + peerClient.getPeers().size() + " threads...");

        // copy the peer list to make sure we don't get any concurrency issues when adding new received peers
        new ArrayList<>(peerClient.getPeers()).stream().filter(p -> p.isInitialized()).forEach(peer -> {
            List<String> input = peer.readData();

            /*
             * While taking up new transactions and blocks, the client will broadcast them to the network if they are new to the client.
             * As a result, if you are connected to 7 peers, you will get reverb 7 times for a broadcast of a block or transaction.
             * For now, this is done to MAKE SURE everyone is on the same page with block/transaction propagation.
             * In the future, much smarter algorithms for routing, perhaps sending "have you seen xxx transaction" or similar will be used.
             * No point in sending 4 KB when a 64-byte message (or less) could check to make sure a transaction hasn't already been sent.
             * Not wanting to complicate Proof of Concept, there are no fancy algorithms or means of telling if peers have already heard the news you are going to deliver.
             */
            input.stream().filter(Objects::nonNull).forEach(data -> {
                if (data.length() > 60) {
                    LOGGER.debug("got data: " + data.substring(0, 30) + "..." + data.substring(data.length() - 30, data.length()));
                } else {
                    LOGGER.debug("got data: " + data);
                }
                List<String> parts = Arrays.asList(data.split(" "));
                if (!parts.isEmpty()) {
                    try {
                        handlePayload(parts, peer);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        LOGGER.error("Incomplete message... " + data);
                        peer.write("ERROR the message was not complete!");
                    } catch (Exception e) {
                        LOGGER.error("No idea what happened here... MSG: " + data, e);
                    }
                }
            });
        });
    }

    /**
     * Broadcast a new block request if catchupMode is true
     */
    public void broadcastNewBlockRequest() {
        if (networkState.getCatchupMode()) {
            try {
                //Sleep for a bit, wait for responses before requesting more data.
                Thread.sleep(300);
                //Broadcast request for new block(s)
                for (int i = blockStore.getBlockchainLength(); i < networkState.getTopBlock(); i++) {
                    LOGGER.info("Requesting block " + i + "...");
                    peerClient.broadcast("GET_BLOCK " + i);
                }
            } catch (InterruptedException e) {
                //If this throws an error, something's terribly off.
                LOGGER.error("Exception when broadcastNewBlockRequest, something's terribly off.", e);
            }
        }
    }

    private void handlePayload(List<String> parts, IPeer peer) {
        try {
            peer.setLastSeen(System.currentTimeMillis());
            if (!isEmpty(peer.getIdentifier())) {
                peerClient.save(peer);
            }
            PayloadType type = PayloadType.valueOf(StringUtils.upperCase(parts.get(0)));
            payloadHandlerProvider.getPayloadHandler(type).handlePeerPayload(parts, peer);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown payload: " + StringUtils.upperCase(parts.get(0)) + ", do nothing.");
        }
    }

    /**
     * Update the data from the peer network
     */
    public void run() {
        getNewData();
        broadcastNewBlockRequest();
        pingPeers();
        networkUpdater.updateNetworks();
    }

    /**
     * Ping peers that have not been seen for longer than 12 hours
     */
    private void pingPeers() {
        peerClient.getPeers().stream()
                // Only ping every 5 seconds
                .filter(p -> p.getLastPing() == null || p.getLastPing() < System.currentTimeMillis() - 5000L)
                .filter(p -> p.getLastSeen() == null || p.getLastSeen() < System.currentTimeMillis() - pingInterval)
                .filter(p -> p.getConnectionAttempts() <= maxConnectionAttempts)
                .forEach(p -> {
                    LOGGER.debug("Pinging " + p.getIdentifier());
                    p.addConnectionAttempt();
                    p.setLastPing(System.currentTimeMillis());
                    p.write(PayloadType.PING.name());
                });

        peerClient.getPeers().stream()
                .filter(p -> p.getLastPing() != null)
                .filter(p -> p.getLastSeen() == null || p.getLastSeen() < System.currentTimeMillis() - pingInterval)
                .filter(p -> p.getConnectionAttempts() > maxConnectionAttempts)
                .forEach(p -> {
                    LOGGER.info("No response, disconnecting: " + p.getIdentifier());
                    peerClient.disconnect(p);
                });
    }

}
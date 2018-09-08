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
import io.smilo.commons.peer.payloadhandler.PayloadHandler;
import io.smilo.commons.peer.payloadhandler.PayloadHandlerProvider;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.INetworkState;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Thread.sleep;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class PeerReceiver {

    private static final Logger LOGGER = Logger.getLogger(PeerReceiver.class);

    private final BlockStore blockStore;
    private final PeerClient peerClient;
    private final PayloadHandlerProvider payloadHandlerProvider;
    private final INetworkState networkState;

    private final Long PING_INTERVAL;
    private final int MAX_CONNECTION_ATTEMPTS;
    private final INetworkUpdater networkUpdater;
    private final int MAX_BLOCKS_CATCHUP;

    public PeerReceiver(BlockStore blockStore,
                        PeerClient peerClient,
                        PayloadHandlerProvider payloadHandlerProvider,
                        INetworkState networkState,
                        @Value("${PING_INTERVAL:4320000}") Long PING_INTERVAL,
                        @Value("${MAX_BLOCKS_CATCHUP:25}") int MAX_BLOCKS_CATCHUP,
                        @Value("${MAX_CONNECTION_ATTEMPTS:7}") int MAX_CONNECTION_ATTEMPTS, INetworkUpdater networkUpdater) {
        this.blockStore = blockStore;
        this.peerClient = peerClient;
        this.payloadHandlerProvider = payloadHandlerProvider;
        this.networkState = networkState;
        this.PING_INTERVAL = PING_INTERVAL;
        this.MAX_BLOCKS_CATCHUP = MAX_BLOCKS_CATCHUP;
        this.MAX_CONNECTION_ATTEMPTS = MAX_CONNECTION_ATTEMPTS;
        this.networkUpdater = networkUpdater;
    }

    /**
     * Retrieve and handle new data from peers
     */
    public void getNewData() {
//        LOGGER.debug("Look for new data from peers...");
//        LOGGER.debug("Connected with " + peerClient.getPeers().size() + " threads...");

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
                    LOGGER.trace("got data: " + data.substring(0, 30) + "..." + data.substring(data.length() - 30, data.length()));
                } else {
                    LOGGER.trace("got data: " + data);
                }
                List<String> parts = Arrays.asList(data.split(" "));
                if (!parts.isEmpty()) {
                    try {
                        handlePayload(parts, peer);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        LOGGER.warn("Invalid message... " + data);
//                        peer.write("ERROR the message was not complete!");
                    } catch (Exception e) {
                        LOGGER.error("No idea what happened here... MSG: " + data, e);
                    }
                } else {
                    LOGGER.error("Invalid message... " + data);
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

                //Broadcast request for new block(s)
                int blockNum = blockStore.getBlockchainLength();
                long blockGoal = networkState.getTopBlock();
                int max_blocks = (int) Math.min(blockGoal - blockNum, MAX_BLOCKS_CATCHUP);

                for (int i = 0; i < max_blocks; ++i) {
                    int getBlock = blockStore.getBlockchainLength() + i;
                    LOGGER.info("Requesting block " + getBlock + "...");
                    peerClient.broadcast("GET_BLOCK " + getBlock);
                }

                //Sleep for a bit, wait for responses before requesting more data to prevent DDos
                sleep(500);
            } catch (InterruptedException e) {
                //If this throws an error, something's terribly off.
                LOGGER.error("Exception when broadcastNewBlockRequest, something's terribly off.", e);
            }
        }
    }

    private void handlePayload(List<String> parts, IPeer peer) {
        PayloadType type = null;
        try {
            peer.setLastSeen(System.currentTimeMillis());
            if (!isEmpty(peer.getIdentifier())) {
                peerClient.save(peer);
            }
            type = PayloadType.valueOf(StringUtils.upperCase(parts.get(0)));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown payload: " + StringUtils.upperCase(parts.get(0)) + " , do nothing. ");
        }

        if (type != null) {
            LOGGER.trace("Will handlePeerPayload after identify PayloadType " + type);
            try {
                PayloadHandler p = payloadHandlerProvider.getPayloadHandler(type);
                if (p != null) {
                    p.handlePeerPayload(parts, peer);
                } else {
                    LOGGER.error("Could not find a handler for the type " + type);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("COULD NOT HANDLE PAYLOAD " + type, e);
            }
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
                .filter(p -> p.getLastSeen() == null || p.getLastSeen() < System.currentTimeMillis() - PING_INTERVAL)
                .filter(p -> p.getConnectionAttempts() <= MAX_CONNECTION_ATTEMPTS)
                .forEach(p -> {
                    LOGGER.trace("Pinging " + p.getIdentifier());
                    p.addConnectionAttempt();
                    p.setLastPing(System.currentTimeMillis());
                    p.write(PayloadType.PING.name());
                });

        peerClient.getPeers().stream()
                .filter(p -> p.getLastPing() != null)
                .filter(p -> p.getLastSeen() == null || p.getLastSeen() < System.currentTimeMillis() - PING_INTERVAL)
                .filter(p -> p.getConnectionAttempts() > MAX_CONNECTION_ATTEMPTS)
                .forEach(p -> {
                    LOGGER.warn("No response, disconnecting: " + p.getIdentifier());
                    InetAddress address = p.getAddress();
                    int remotePort = p.getRemotePort();
                    Socket newSocket;
                    try {
                        newSocket = new Socket(address, remotePort);
                    } catch (IOException e) {
                        LOGGER.error("Could not retry to connect to peer :( " + p.toString(), e);
                        return;
                    }
                    peerClient.disconnect(p);
//                    try {
//                        sleep(1000);
//                    } catch (InterruptedException e) {
//
//                    }
//                    Peer newPeer = new Peer(p.getIdentifier(), newSocket);
//                    peerClient.connectToPeer(newPeer);
                });
    }

}
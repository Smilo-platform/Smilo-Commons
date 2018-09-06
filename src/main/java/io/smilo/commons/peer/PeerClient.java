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

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class PeerClient {

    private static final Logger LOGGER = Logger.getLogger(PeerClient.class);
    private static final int DEFAULT_PORT = 8020;

    private final Set<String> nodes;

    private String hostname;
    private int port;

    private final int listenPort;
    private boolean shouldRun = true;

    // TODO: check if it's random enough
    private final Random random = new Random();
    private TaskExecutor taskExecutor;
    private AddressManager addressManager;
    private PeerInitializer peerInitializer;
    private PeerStore peerStore;
    private Set<IPeer> pendingPeers;

    public PeerClient(@Value("#{'${NODES_LIST}'.split(',')}") Set<String> nodes,
                      ThreadPoolTaskExecutor taskExecutor,
                      AddressManager addressManager, PeerInitializer peerInitializer,
                      PeerStore peerStore) {
        this.taskExecutor = taskExecutor;
        this.addressManager = addressManager;
        this.peerInitializer = peerInitializer;
        this.peerStore = peerStore;
        this.listenPort = DEFAULT_PORT;
        this.nodes = nodes;
        this.pendingPeers = new HashSet<>();
        initializePeers();
        listenToSocket();
    }

    /**
     * Pauses listening to the peer network
     */
    public void pauseListening() {
        this.shouldRun = false;
    }

    /**
     * Resumes listening to the peer network
     */
    public void resumeListening() {
        this.shouldRun = true;
    }

    /**
     * Initialize peers
     *
     * 1. read peers form database
     * 2. if no peers are found in the database, add the peers from the property file
     */
    public void initializePeers() {
        Set<IPeer> peersFromDatabase = new HashSet<>(peerStore.getPeers());
        if (peersFromDatabase.isEmpty()) {
            LOGGER.info("Peer list does not exist.. Writing to database..");
            /*
             * In future networks, these will route to servers running the daemon. For now, it's just the above nodes.
             */
            nodes.stream()
                    .map(node -> {
                        try {
                            String addressPart = node.substring(0, node.lastIndexOf(':'));
                            int port = Integer.parseInt(node.substring(node.lastIndexOf(':') + 1));

                            // can be IPv6, IPv4 or a hostname
                            InetAddress address = InetAddress.getByName(addressPart);
                            IPeer peer = peerInitializer.initializePeer("", address, port);
                            return peer;
                        } catch (UnknownHostException | StringIndexOutOfBoundsException e) {
                            LOGGER.error("Invalid formatting or unknown hostname " + node + "! Not adding to the database");
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(this::connectToPeer);
        } else {
            peersFromDatabase.forEach(this::connectToPeer);
        }
    }

    private void listenToSocket() {
        taskExecutor.execute(() -> {
            try (ServerSocket listenSocket = new ServerSocket(listenPort)) {
                while (shouldRun) //Doesn't actually quit right when shouldRun is changed, as while loop is pending.
                {
                    IPeer peer = peerInitializer.initializePeer("", listenSocket.accept());
                    connectToPeer(peer);
                }

            } catch (java.net.BindException e) {
                LOGGER.error("Port " + listenPort + " already in use", e);
                System.exit(-1);
            } catch (Exception e) {
                LOGGER.error("Exception when listenToSocket", e);
            }
        });
    }

    /**
     * Attempts a connection to an external peer
     *
     * @param peer Peer to connect to
     */
    public void connectToPeer(IPeer peer) {
        LOGGER.info("Connecting to " + peer.getConnectHost());
        try {
            taskExecutor.execute(() -> {
                peer.run();
            });

            // should be connected within 3 seconds, we don't want to block the flow
            int amountOfRetries = 30;
            while (!peer.isInitialized()) {
               sleep(100);
               amountOfRetries--;
               if (amountOfRetries == 0) {
                   // TODO: review error handling
                   LOGGER.error("Failed to connect to peer " + peer.getAddress().getHostAddress() + ":" + peer.getRemotePort());
                   return;
               }
            }

            if (isEmpty(peer.getIdentifier())) {
                peer.write(PayloadType.REQUEST_IDENTIFIER.name());
                pendingPeers.add(peer);
                LOGGER.info("Requesting identity from " + peer.getAddress().getHostAddress() + ":" + peer.getRemotePort());
            } else {
                LOGGER.info("Connected to " + peer.getIdentifier());
                peerStore.save(peer);
            }

        } catch (Exception e) {
            LOGGER.warn("Unable to connect to " + peer.getAddress().getHostAddress() + ":" + peer.getRemotePort());
        }
    }

    /**
     * Announces the same message to all peers simultaneously. Useful when re-broadcasting messages.
     *
     * @param toBroadcast String to broadcast to peers
     */
    public void broadcast(String toBroadcast) {
        Collection<IPeer> peers = peerStore.getPeers();
        if (toBroadcast.length() > 60) {
            LOGGER.info("Broadcasting " + toBroadcast.substring(0, 30) + "..." + toBroadcast.substring(toBroadcast.length() - 30, toBroadcast.length()) + " to " + peers.size() + " peer" + (peers.size() != 1 ? "s": ""));

        } else {
            LOGGER.info("Broadcasting " + toBroadcast + " to " + peers.size() + " peer" + (peers.size() != 1 ? "s": ""));

        }
        peers.stream().filter(p -> !p.getIdentifier().equals(addressManager.getDefaultAddress()))
                .forEach(peer -> {
                    LOGGER.trace("Sent:: " + toBroadcast);
                    peer.write(toBroadcast);
                });
    }

    /**
     * Announces the same message to all peers except the ignored one simultaneously. Useful when re-broadcasting messages. Peer ignored as it's the peer that sent you info.
     *
     * @param toBroadcast  String to broadcast to peers
     * @param peerToIgnore Peer to not send broadcast too--usually the peer who sent information that is being rebroadcast
     */
    public void broadcastIgnorePeer(String toBroadcast, IPeer peerToIgnore) {
        peerStore.getPeers().stream()
                .filter(p -> !p.getIdentifier().equals(peerToIgnore.getIdentifier()))
                .forEach(peer -> {
                    LOGGER.trace("Sent:: " + toBroadcast);
                    peer.write(toBroadcast);
                });
    }

    /*
     * Returns a random peer host/port combo to the querying peer.
     * Future versions will detect dynamic ports and not send peers likely to not support direct connections.
     * While not part of GET_PEER, very-far-in-the-future-versions may support TCP punchthrough assists.
     */
    public IPeer getRandomPeer() {
        List<IPeer> peers = new ArrayList<>(peerStore.getPeers());
        return peers.get(random.nextInt(peers.size()));
    }

    public IPeer getRandomPeerNotPeer(IPeer peer) {
        List<IPeer> peers = peerStore.getPeers().stream().filter(p -> !p.equals(peer)).collect(Collectors.toList());
        if (peers.isEmpty()) {
            LOGGER.info("No peers found!");
            return null;
        }
        return peers.get(random.nextInt(peers.size()));
    }

    public Set<IPeer> getPeers() {
        Set<IPeer> peers = new HashSet<>(peerStore.getPeers());
        peers.addAll(pendingPeers);
        return peers;
    }

    public void savePendingPeer(IPeer peer) {
         pendingPeers.stream().filter(p -> p.equals(peer))
                .findFirst()
                .ifPresent(found -> {
                    LOGGER.debug("Removing: " + peer.getIdentifier() + " from pending peers");
                    pendingPeers.remove(found);
                    LOGGER.debug("Saving pending peer: " + peer.getIdentifier() + " to peer store");
                    peerStore.save(peer);
                });
    }

    public void disconnect(IPeer peer) {
        LOGGER.info("Disconnecting: " + peer.getIdentifier());
        peer.closePeer();
        pendingPeers.remove(peer);

        if (!isEmpty(peer.getIdentifier())) {
            LOGGER.debug("Removing: " + peer.getIdentifier() + " from peer store");
            peerStore.remove(peer);
        }
    }

    public void save(IPeer peer) {
        peerStore.save(peer);
    }

    public IPeer getPeerByIdentifier(String identifier) {
        return peerStore.getPeer(identifier);
    }

    public String getExternalIP() {
        String externalIp = null;
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            externalIp = in.readLine();
        } catch(Exception e1) {
            LOGGER.warn("Unable to retrieve external IP from checkip.amazonaws.com", e1);
            LOGGER.info("Attempt 2 to retrieve external IP");
            try {
                URL whatismyip = new URL("http://icanhazip.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));

                externalIp = in.readLine();
            } catch(Exception e2) {
                LOGGER.warn("Unable to retrieve external IP from icanhazip.com", e2);
                LOGGER.info("Attempt 3 to retrieve external IP");

                try {
                    URL whatismyip = new URL("http://ifconfig.me/ip");
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            whatismyip.openStream()));

                    externalIp = in.readLine();
                } catch(Exception e3) {
                    LOGGER.warn("Unable to retrieve external IP from ifconfig.me/ip", e3);
                    LOGGER.info("Attempt 4 to retrieve external IP");

                    try {
                        URL whatismyip = new URL("http://wtfismyip.com/text");
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                whatismyip.openStream()));

                        externalIp = in.readLine();
                    } catch(Exception e4) {
                        LOGGER.warn("Unable to retrieve external IP from wtfismyip.com/text", e4);
                    }
                }
            }
        }
        return externalIp;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

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

package io.smilo.commons.peer.network.link;

import io.smilo.commons.peer.PeerSender;
import io.smilo.commons.peer.network.Network;
import io.smilo.commons.peer.network.NetworkStatus;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.NetworkState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NetworkLinker {

    private static final Logger LOGGER = Logger.getLogger(NetworkLinker.class);

    private final ThreadPoolTaskScheduler taskScheduler;
    private final PeerSender peerSender;
    private final NetworkState networkState;

    private boolean isLinking = false;
    private List<Network> networks = new ArrayList<>();

    private long timeToFindNetworks;

    public NetworkLinker(ThreadPoolTaskScheduler taskScheduler,
                         PeerSender peerSender,
                         NetworkState networkState,
                         @Value("${NETWORK_LINK_TIMEOUT}") Long networkLinkTimeout) {
        this.taskScheduler = taskScheduler;
        this.peerSender = peerSender;
        this.networkState = networkState;
        this.timeToFindNetworks = networkLinkTimeout;
    }

    public void startLinkingProcess() {
        LOGGER.info("Starting linking process!");
        isLinking = true;
        peerSender.broadcast(PayloadType.READY_TO_LINK);


        // wait for [timeToFindNetworks] and assume that's all networks we will get
        taskScheduler.schedule(() -> {
            LOGGER.info("Received a total of " + networks.size() + " networks.");

            // filter networks that we already know
            networks = networks.stream()
                    .filter(n -> !networkState.getNetworks().contains(n))
                    .collect(Collectors.toList());

            if (networks.isEmpty()) {
                createNewNetwork();
            } else {
                // simply join the first, we might need to choose a specific network in the future
                joinNetwork(networks.get(0));
            }

            networks.clear();
            isLinking = false;
            LOGGER.info("NetworkLinker is done working");
        }, new Timestamp(System.currentTimeMillis() + timeToFindNetworks));
    }

    private void joinNetwork(Network network) {
        LOGGER.info("Joining existing network " + network.getIdentifier() + " with " + network.getPeerIdentifiers().size() + " peers");
        network.setNetworkStatus(NetworkStatus.NONE);
        networkState.addNetwork(network);
    }

    private void createNewNetwork() {
        LOGGER.info("Creating new network!");
        Network network = new Network();
        network.setIdentifier(UUID.randomUUID().toString());
        network.setNetworkStatus(NetworkStatus.LINKED);
        networkState.addNetwork(network);
    }

    public void addNetworks(Set<Network> networks) {
        this.networks.addAll(networks);
    }

    public boolean isLinking() {
        return isLinking;
    }
}

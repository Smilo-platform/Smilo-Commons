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
package io.smilo.commons.peer.network.join;

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.PeerSender;
import io.smilo.commons.peer.network.ConsensusFinishedListener;
import io.smilo.commons.peer.network.Network;
import io.smilo.commons.peer.network.NetworkConsensus;
import io.smilo.commons.peer.network.NetworkStatus;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.NetworkState;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class NetworkJoiner implements ConsensusFinishedListener {

    private static final Logger LOGGER = Logger.getLogger(NetworkJoiner.class);

    private final PeerSender peerSender;
    private final NetworkState networkState;
    private final AddressManager addressManager;
    private final NetworkConsensus networkConsensus;

    public NetworkJoiner(PeerSender peerSender, NetworkState networkState, AddressManager addressManager, NetworkConsensus networkConsensus) {
        this.peerSender = peerSender;
        this.networkState = networkState;
        this.addressManager = addressManager;
        this.networkConsensus = networkConsensus;
    }

    /**
     * Try to become a full node in a network
     * Must already be LINKED in this network.
     *
     * @param network network to become a full node in
     */
    public void becomeFullNode(Network network) {
        LOGGER.info("Becoming full node in " + network.getIdentifier());
        if (network.getPeerIdentifiers().size() + network.getUnconfirmedPeerIdentifiers().size() == 1) {
            LOGGER.info("Looks like I'm the only node, I'll set myself to full node for now");
            consensusApproved(network, addressManager.getDefaultAddress(), new HashSet<>());
        } else {
            if (!isAlreadyApproved(network.getIdentifier(), addressManager.getDefaultAddress())) {
                peerSender.broadcastToNetwork(network, PayloadType.READY_FULL_NODE, network.getIdentifier(), true);
                networkConsensus.startConsensus(network, addressManager.getDefaultAddress(), this);
            }
        }
    }

    public void becomeFullNode(String networkIdentifier, String peerIdentifier) {
        Optional<Network> networkOptional = networkState.getNetworkByIdentifier(networkIdentifier);
        if (networkOptional.isPresent() && !isAlreadyApproved(networkIdentifier, peerIdentifier)) {
            LOGGER.info("Starting to register votes for " + networkIdentifier + " for peer " + peerIdentifier);
            networkConsensus.startConsensus(networkOptional.get(), peerIdentifier, this);
        } else {
            LOGGER.warn("Unknown network " + networkIdentifier + " when starting to register votes for becoming a full node for peer " + peerIdentifier);
        }
    }

    public void addApprovedNode(String networkIdentifier, String peerIdentifier, String approver) {
        networkState.getNetworkByIdentifier(networkIdentifier)
                .ifPresent(n -> networkConsensus.addApproval(n, peerIdentifier, approver));
    }


    public boolean isJoining(String networkIdentifier, String peerIdentifier) {
        return networkState.getNetworkByIdentifier(networkIdentifier)
                .map(n -> networkConsensus.consensusActive(n, peerIdentifier))
                .orElse(false);
    }

    private boolean isAlreadyApproved(String networkIdentifier, String peerIdentifier) {
        return networkState.getNetworkByIdentifier(networkIdentifier)
                .filter(network -> network.getPeerIdentifiers().contains(peerIdentifier))
                .isPresent();
    }

    @Override
    public void consensusApproved(Network network, String identifier, Set<String> approvals) {
         if (identifier.equals(addressManager.getDefaultAddress())) {
            LOGGER.info("I am a full node for this network! :)");
            network.setNetworkStatus(NetworkStatus.FULL_NODE);
        } else {
            LOGGER.info(identifier + " became a full node in network " + network.getIdentifier() + "!");
        }

        network.getUnconfirmedPeerIdentifiers().remove(identifier);
        network.getPeerIdentifiers().add(identifier);

    }
}

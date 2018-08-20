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

/**
 * Step 1 is becoming linked to a network, step 2 is becoming a full node, so that you can can participate in SPoRT
 * and create blocks when you are elected and help securing the network by approving and declining messages.
 *
 * The NetworkJoiner can be used to join a network:
 *
 * - Call becomeFullNode to start joining a network. READY_FULL_NODE will be broadcasted and the consensus mechanism will start
 * - Call addApprovedNode or addDeclinedNode whenever a peer within the network has approved or declined
 * - When the consensus has finished, the network will be either removed from networkState, or the network status will be changed to FULL_NODE
 */
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

    /**
     * Start the process of another peer in the network becoming a full node
     *
     * @param networkIdentifier network to start the consensus in
     * @param peerIdentifier peer to be accepted or declined
     */
    public void becomeFullNode(String networkIdentifier, String peerIdentifier) {
        Optional<Network> networkOptional = networkState.getNetworkByIdentifier(networkIdentifier);
        if (networkOptional.isPresent() && !isAlreadyApproved(networkIdentifier, peerIdentifier)) {
            LOGGER.info("Starting to register votes for " + networkIdentifier + " for peer " + peerIdentifier);
            networkConsensus.startConsensus(networkOptional.get(), peerIdentifier, this);
        } else {
            LOGGER.warn("Unknown network " + networkIdentifier + " when starting to register votes for becoming a full node for peer " + peerIdentifier);
        }
    }

    /**
     * Add approval for node to join network
     *
     * @param networkIdentifier consensus network
     * @param peerIdentifier consensus peer
     * @param approver public address who approved
     */
    public void addApprovedNode(String networkIdentifier, String peerIdentifier, String approver) {
        networkState.getNetworkByIdentifier(networkIdentifier)
                .ifPresent(n -> networkConsensus.addApproval(n, peerIdentifier, approver));
    }

    /**
     * Add approval for node to join network
     *
     * @param networkIdentifier consensus network
     * @param peerIdentifier consensus peer
     * @param decliner public address who declined
     */
    public void addDeclineNode(String networkIdentifier, String peerIdentifier, String decliner) {
        networkState.getNetworkByIdentifier(networkIdentifier)
                .ifPresent(n -> networkConsensus.addDecline(n, peerIdentifier, decliner));
    }

    /**
     * Checks if a peer is trying to join a network
     *
     * @param networkIdentifier consensus network
     * @param peerIdentifier consensus peer
     * @return true if a peer is trying to join, false if not
     */
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

    @Override
    public void consensusDeclined(Network network, String identifier, Set<String> declines) {
        if (identifier.equals(addressManager.getDefaultAddress())) {
            LOGGER.info("I have been declined from network " + network.getIdentifier() + " with " + declines.size() + " votes against!");
            networkState.getNetworks().remove(network);
        } else {
            network.getUnconfirmedPeerIdentifiers().remove(identifier);
            LOGGER.info("Removing " + identifier + " from network " + network.getIdentifier() + ", it has been declined by the network with " + declines.size() + " votes!");
        }
    }

}

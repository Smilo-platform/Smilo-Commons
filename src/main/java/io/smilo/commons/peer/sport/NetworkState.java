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

package io.smilo.commons.peer.sport;

import io.smilo.commons.block.BlockStore;
import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.PeerSender;
import io.smilo.commons.peer.network.Network;
import io.smilo.commons.peer.network.NetworkStatus;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class NetworkState {

    private final static Logger LOGGER = Logger.getLogger(NetworkState.class);

    private boolean catchupMode = false;
    private long topBlock = 0;
    private Set<Network> networks = new HashSet<>();

    private final BlockStore blockStore;
    private final PeerSender peerSender;
    private final AddressManager addressManager;

    public NetworkState(BlockStore blockStore, PeerSender peerSender, AddressManager addressManager) {
        this.blockStore = blockStore;
        this.peerSender = peerSender;
        this.addressManager = addressManager;
    }

    /**
     * Checks if the chain in the database has the same length as the top block. If the top block is higher than the database, set catchupMode to true
     */
    public void updateCatchupMode() {
        /*
         * Current chain is shorter than peer chains.
         * Chain starts counting at 0, so a chain height of 100, for example, means there are 100 blocks, and the top block's index is 99.
         * So we need to catch up!
         *
         */
        if (topBlock > blockStore.getBlockchainLength()) {
            LOGGER.info("Updating CatchupMode currentChainHeight: " + blockStore.getBlockchainLength() + ", topBlock: " + topBlock);
            catchupMode = true;
        } else {
            if (catchupMode) {
                LOGGER.info("Caught up with network."); //Probably won't be seen with block-add spam.
            }
            catchupMode = false;
        }
    }

    /**
     * Checks if the current network block is higher than the local database block.
     *
     * @return true if the current network block is higher than the local database block
     */
    public boolean getCatchupMode() {
        return catchupMode;
    }

    /**
     * Finds the highest blocknum from the network
     *
     * @return the highest blocknum from the network
     */
    public long getTopBlock() {
        return topBlock;
    }

    public void setTopBlock(int topBlock) {
        this.topBlock = topBlock;
        updateCatchupMode();
    }

    public Set<Network> getNetworks() {
        return networks;
    }

    public Optional<Network> getNetworkByIdentifier(String networkIdentifier) {
        return this.getNetworks().stream()
                .filter(network -> network.getIdentifier().equals(networkIdentifier))
                .findFirst();
    }

    public void addNetwork(Network network) {
        networks.add(network);
        peerSender.broadcastToNetwork(network, PayloadType.LINK_NETWORK, network.getIdentifier());

        // if it's linked, it means we created the network ourselves. In that case, we can assume that we're confirmed.
        if (network.getNetworkStatus() == NetworkStatus.LINKED) {
            network.getPeerIdentifiers().add(addressManager.getDefaultAddress());
        } else {
            network.getUnconfirmedPeerIdentifiers().add(addressManager.getDefaultAddress());
        }

        LOGGER.info("Network with ID " + network.getIdentifier() + " added!");
    }
}

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
package io.smilo.commons.peer.network;

import java.util.HashSet;
import java.util.Set;

public class Network {

    private String identifier;
    private Set<String> peerIdentifiers = new HashSet<>();
    private Set<String> unconfirmedPeerIdentifiers = new HashSet<>();
    private NetworkStatus networkStatus;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Set<String> getPeerIdentifiers() {
        return peerIdentifiers;
    }

    public void setPeerIdentifiers(Set<String> peerIdentifiers) {
        this.peerIdentifiers = peerIdentifiers;
    }

    public Set<String> getUnconfirmedPeerIdentifiers() {
        return unconfirmedPeerIdentifiers;
    }

    public void setUnconfirmedPeerIdentifiers(Set<String> unconfirmedPeerIdentifiers) {
        this.unconfirmedPeerIdentifiers = unconfirmedPeerIdentifiers;
    }

    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(NetworkStatus networkStatus) {
        this.networkStatus = networkStatus;
    }

    /**
     * Returns amount of peers, both full nodes and unconfirmed peers.
     * @return total amount of peers
     */
    public int getAmountOfPeers() {
        return peerIdentifiers.size() + unconfirmedPeerIdentifiers.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Network network = (Network) o;

        return identifier != null ? identifier.equals(network.identifier) : network.identifier == null;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

}

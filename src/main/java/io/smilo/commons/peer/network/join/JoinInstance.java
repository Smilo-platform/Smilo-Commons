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


import io.smilo.commons.peer.network.Network;

public class JoinInstance {

    private final Network network;
    private final String peerAddress;

    public JoinInstance(Network network, String peerAddress) {
        this.network = network;
        this.peerAddress = peerAddress;
    }

    public Network getNetwork() {
        return network;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoinInstance that = (JoinInstance) o;

        if (!network.equals(that.network)) return false;
        return peerAddress.equals(that.peerAddress);
    }

    @Override
    public int hashCode() {
        int result = network.hashCode();
        result = 31 * result + peerAddress.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JoinInstance{" +
                "network='" + network + '\'' +
                ", peerAddress='" + peerAddress + '\'' +
                '}';
    }
}

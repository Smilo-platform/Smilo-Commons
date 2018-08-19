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

public class Consensus {

    private final Network network;
    private final String identifier;
    private final Set<String> approvedIdentifiers = new HashSet<>();
    private final ConsensusFinishedListener listener;

    public Consensus(Network network, String identifier, ConsensusFinishedListener listener) {
        this.identifier = identifier;
        this.listener = listener;
        this.network = network;
    }

    public Set<String> getApprovedIdentifiers() {
        return approvedIdentifiers;
    }

    public void finish() {
        listener.consensusApproved(network, identifier, approvedIdentifiers);
    }
}

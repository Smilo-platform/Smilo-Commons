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

import io.smilo.commons.peer.network.join.JoinInstance;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class NetworkConsensus {

    private static final Logger LOGGER = Logger.getLogger(NetworkConsensus.class);

    private Map<JoinInstance, Consensus> approvedNodes = new HashMap<>();

    public void startConsensus(Network network, String identifier, ConsensusFinishedListener listener) {
        JoinInstance joinInstance = new JoinInstance(network, identifier);
        approvedNodes.put(joinInstance, new Consensus(network, identifier, listener));
    }

    public void endConsensus(Network network, String identifier) {
        JoinInstance joinInstance = new JoinInstance(network, identifier);
        approvedNodes.remove(joinInstance);
    }

    public boolean consensusActive(Network network, String identifier) {
        JoinInstance joinInstance = new JoinInstance(network, identifier);
        return approvedNodes.containsKey(joinInstance);
    }

    public Set<String> getApprovals(Network network, String identifier) {
        Consensus consensus =  approvedNodes.get(new JoinInstance(network, identifier));
        if (consensus != null) {
            return consensus.getApprovedIdentifiers();
        } else {
            return new HashSet<>();
        }
    }

    public void addApproval(Network network, String identifier, String approver) {
        LOGGER.info("Registering approval for " + network.getIdentifier() + " for " + identifier + " from " + approver);
        JoinInstance joinInstance = new JoinInstance(network, identifier);
        Set<String> identifiers = getApprovals(network, identifier);
        if (identifiers != null) {
            identifiers.add(approver);
            checkNodeApprovedStatus(joinInstance);
        } else {
            LOGGER.warn("I didn't know about identifier " + identifier + " in network " + network.getIdentifier() + "!");
        }
    }

    private void checkNodeApprovedStatus(JoinInstance joinInstance) {
        Set<String> identifiers = getApprovals(joinInstance.getNetwork(), joinInstance.getPeerAddress());
        int numberOfApprovers = identifiers.size();
        if ((numberOfApprovers * 100 / joinInstance.getNetwork().getPeerIdentifiers().size()) >= 66) {
            LOGGER.info("66 Percent approved node: " + joinInstance.toString());
            approvedNodes.get(joinInstance).finish();
            endConsensus(joinInstance.getNetwork(), joinInstance.getPeerAddress());
        } else {
            LOGGER.info((numberOfApprovers * 100 / joinInstance.getNetwork().getPeerIdentifiers().size()) + " percent approved (" + numberOfApprovers + "/" + joinInstance.getNetwork().getPeerIdentifiers().size() + ")");
        }
    }


}

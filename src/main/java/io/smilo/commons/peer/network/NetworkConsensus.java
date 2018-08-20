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

import io.smilo.commons.peer.network.join.ConsensusInstance;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to keep track of running consensus in networks.
 * Consensus key is a combination of network and identifier, which can be a block hash, peer identifier or something arbitrary
 *
 * The class can keep track of multiple consensus instances:
 *
 * - Call startConsensus with a network and identifier (could be block hash, peer identifier, etc)
 * - Call addApproval/addDecline every time a peer in the network has approved or declined
 * - As soon as 66% of the network has approved, consensusApproved of the ConsensusFinishedListener will be called and the consensus will be ended.
 * - As soon as 33% of the network has declined, consensusDeclined of the ConsensusFinishedListener will be called and the consensus will be ended.
 *
 * - getApprovals or getDeclined can be called to view to progress of the consensus
 * - endConsensus can be called to end the consensus. consensusApproved or consensusDeclined will not be called.
 */
@Component
public class NetworkConsensus {

    private static final Logger LOGGER = Logger.getLogger(NetworkConsensus.class);

    private Map<ConsensusInstance, Consensus> approvedNodes = new HashMap<>();

    public void startConsensus(Network network, String identifier, ConsensusFinishedListener listener) {
        ConsensusInstance consensusInstance = new ConsensusInstance(network, identifier);
        approvedNodes.put(consensusInstance, new Consensus(network, identifier, listener));
    }

    /**
     * Consensus will be ended and the progress will be removed
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     */
    public void endConsensus(Network network, String identifier) {
        ConsensusInstance consensusInstance = new ConsensusInstance(network, identifier);
        approvedNodes.remove(consensusInstance);
    }

    /**
     * Checks if a consensus is active for the combination of network and identifier
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     * @return true if the consensus is active, otherwise false
     */
    public boolean consensusActive(Network network, String identifier) {
        ConsensusInstance consensusInstance = new ConsensusInstance(network, identifier);
        return approvedNodes.containsKey(consensusInstance);
    }

    /**
     * Returns the addresses of all approvers for this consensus
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     * @return set of approvers
     */
    public Set<String> getApprovals(Network network, String identifier) {
        Consensus consensus =  approvedNodes.get(new ConsensusInstance(network, identifier));
        if (consensus != null) {
            return consensus.getApprovedIdentifiers();
        } else {
            return new HashSet<>();
        }
    }

    /**
     * Returns the addresses of all declines for this consensus
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     * @return set of declines
     */
    public Set<String> getDeclines(Network network, String identifier) {
        Consensus consensus =  approvedNodes.get(new ConsensusInstance(network, identifier));
        if (consensus != null) {
            return consensus.getDeclinedIdentifiers();
        } else {
            return new HashSet<>();
        }
    }

    /**
     * Registers an approval for a consensus instance
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     * @param approver public address of the approver
     */
    public void addApproval(Network network, String identifier, String approver) {
        LOGGER.info("Registering approval for " + network.getIdentifier() + " for " + identifier + " from " + approver);
        ConsensusInstance consensusInstance = new ConsensusInstance(network, identifier);
        Set<String> identifiers = getApprovals(network, identifier);
        if (identifiers != null) {
            identifiers.add(approver);
            checkNodeApprovedStatus(consensusInstance);
        } else {
            LOGGER.warn("I didn't know about identifier " + identifier + " in network " + network.getIdentifier() + "!");
        }
    }

    /**
     * Registers an decline for a consensus instance
     *
     * @param network Consensus network
     * @param identifier consensus identifier
     * @param decliner public address of the decliner
     */
    public void addDecline(Network network, String identifier, String decliner) {
        LOGGER.info("Registering decline for " + network.getIdentifier() + " for " + identifier + " from " + decliner);
        ConsensusInstance consensusInstance = new ConsensusInstance(network, identifier);
        Set<String> identifiers = getDeclines(network, identifier);
        if (identifiers != null) {
            identifiers.add(decliner);
            checkNodeApprovedStatus(consensusInstance);
        } else {
            LOGGER.warn("I didn't know about identifier " + identifier + " in network " + network.getIdentifier() + "!");
        }
    }

    /**
     * Checks if the consensus instance can be ended (66% approval or 33% decline)
     *
     * @param consensusInstance consensus instance to check
     */
    private void checkNodeApprovedStatus(ConsensusInstance consensusInstance) {
        Set<String> approvedIdentifiers = getApprovals(consensusInstance.getNetwork(), consensusInstance.getPeerAddress());
        Set<String> declineIdentifiers = getDeclines(consensusInstance.getNetwork(), consensusInstance.getPeerAddress());
        int numberOfApprovers = approvedIdentifiers.size();
        int numberOfDecliners = declineIdentifiers.size();
        int networkSize = consensusInstance.getNetwork().getPeerIdentifiers().size();

        double approval = (numberOfApprovers * 100 / networkSize);
        double decline = (numberOfDecliners * 100 / networkSize);

        if (approval >= 66) {
            LOGGER.info(approval + " Percent approved node: " + consensusInstance.toString());
            approvedNodes.get(consensusInstance).approve();
            endConsensus(consensusInstance.getNetwork(), consensusInstance.getPeerAddress());
        } else {
            LOGGER.info(approval + " percent approved (" + numberOfApprovers + "/" + networkSize + ")");
        }
        if (decline >= 33) {
            LOGGER.info(decline + " Percent declined node: " + consensusInstance.toString());
            approvedNodes.get(consensusInstance).decline();
            endConsensus(consensusInstance.getNetwork(), consensusInstance.getPeerAddress());
        } else {
            LOGGER.info(decline + " percent declined (" + numberOfDecliners + "/" + networkSize + ")");
        }
    }

}

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

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.PeerSender;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
public class SportState {

    private static final Logger LOGGER = Logger.getLogger(SportState.class);

    private final NetworkState networkState;

    private final PeerSender peerSender;

    private final AddressManager addressManager;

    private final TreeMap<Long, SportRound> rounds = new TreeMap<>();

    public SportState(NetworkState networkState, PeerSender peerSender, AddressManager addressManager) {
        this.networkState = networkState;
        this.peerSender = peerSender;
        this.addressManager = addressManager;
    }

    /**
     * Initializes all rounds to the passed address
     * @param address to set the speaker to for all rounds
     */
    public void initialize(String address) {
        if (rounds.isEmpty()) {
            LOGGER.debug("Initializing SPoRT");
            SportRound sportRound = new SportRound(address, networkState.getTopBlock());
            rounds.put(sportRound.getTopBlock(), sportRound);
        }
    }

    /**
     * Finds the speaker address of the passed round / blockNum. returns null if none is known.
     * @param blockNum blockNum or round to check for speaker
     * @return speaker address of the round or null if none is known
     */
    public String getSpeakerAddress(long blockNum) {
        // TODO: if round was not present in the current rounds, we should probably do something like we do in smiloChainService,
        // storing the speakers that do not fit on the current stack of SPoRT rounds.
        SportRound round = getRound(blockNum);
        return round.getSpeakerAddress();
    }

    /**
     * Finds the speaker address of the current round
     * @return the current speaker address
     */
    public String getSpeakerAddress() {
        Map.Entry<Long, SportRound> lastEntry = rounds.lastEntry();
        if (lastEntry == null) {
            return null;
        } else {
            return lastEntry.getValue().getSpeakerAddress();
        }
    }

    public SportRound startNewRound(Long roundNum, String address) {
        SportRound round = rounds.get(roundNum);
        if (rounds.get(roundNum) == null) {
            LOGGER.debug("Initializing round " + roundNum + " with speaker " + address);
            round = new SportRound(address, roundNum);
            rounds.put(roundNum, round);
        } else {
            LOGGER.debug("Round "+ roundNum + " is already set!");
        }
        return round;
    }

    /**
     * Adds a speaker to the round, either to the speaker address or the list of backups.
     * The new speaker will be broadcasted if the current node is the speaker.
     *
     * @param roundNum top block num of the round
     * @param speakerAddress address of the speaker
     */
    public void addNewSpeaker(long roundNum, String speakerAddress) {
        SportRound round = getRound(roundNum);
        SportRound previousRound = rounds.get(round.getTopBlock() - 1);

        LOGGER.info(round);

        if (round.getSpeakerAddress() == null) {
            LOGGER.info("Adding " + speakerAddress + " as speaker to round " + roundNum);
            round.setSpeakerAddress(speakerAddress);

            // broadcast new speaker if I am speaker of the previous round
            if (previousRound != null && addressManager.getDefaultAddress().equals(previousRound.getSpeakerAddress())) {
                peerSender.broadcast(PayloadType.NEW_SPEAKER, roundNum + " " + speakerAddress);
            }
        } else if (!round.getSpeakerAddress().equals(speakerAddress)){
            Integer index = round.addSecondarySpeaker(speakerAddress);
            LOGGER.info("Adding " + speakerAddress + " as secondary speaker to round " + roundNum + " with index " + index);
            if (previousRound != null && addressManager.getDefaultAddress().equals(previousRound.getSpeakerAddress())) {
                peerSender.broadcast(PayloadType.SECONDARY_SPEAKER, roundNum + " " + speakerAddress + " " + index);
            }
        } else {
            LOGGER.debug("Speaker was already added!");
        }

        rounds.put(roundNum, round);
    }

    /**
     * Adds a secondary speaker to the specified round at the specified index if this index is still available
     * Does not rebroadcast the secondary speaker
     *
     * @param roundNum round number to add the speaker to
     * @param speakerAddress address to add
     * @param index index to add the speaker at
     */
    public void addSecondarySpeaker(long roundNum, String speakerAddress, int index) {
        LOGGER.info("Adding secondary " + speakerAddress + " at index "+ index + " for round " + roundNum);
        SportRound round = getRound(roundNum);
        round.addSecondarySpeaker(index, speakerAddress);
    }

    /**
     * Finds the round by round number. Creates a new empty round if it was not found.
     * @param blockNum round number/ block number
     * @return the round by round number or an empty new round
     */
    public SportRound getRound(long blockNum) {
        return rounds.values().stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getTopBlock() == blockNum)
                .findFirst()
                .orElseGet(() -> {
            LOGGER.info("Creating new round " + blockNum);
            SportRound round = new SportRound(blockNum);
            rounds.put(blockNum, round);
            return round;
        });
    }

    /**
     * Finds the current SPoRT round
     * @return the current SPoRT round
     */
    public SportRound getCurrentRound() {
        Map.Entry<Long, SportRound> last = rounds.lastEntry();
        if (last == null) {
            return null;
        } else {
            return last.getValue();
        }
    }

    /**
     * Finds the previous SPoRT round
     * @return the previous SPoRT round
     */
    public SportRound getPreviousRound() {
        return rounds.get(getCurrentRound().getTopBlock() - 1);
    }

    public void reset() {
        this.rounds.clear();
    }
}

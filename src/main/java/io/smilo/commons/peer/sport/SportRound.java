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

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SportRound {

    private static final Logger LOGGER = Logger.getLogger(SportRound.class);

    private String speakerAddress;
    private long topBlock;
    private Map<Integer, String> secondarySpeakers = new HashMap<>();

    private short difficulty = 21;
    private String sportChallengeData;

    public SportRound(String speakerAddress, long topBlock) {
        this.speakerAddress = speakerAddress;
        this.topBlock = topBlock;
    }

    public SportRound(long blockNum) {
        this.topBlock = blockNum;
    }

    public String getSpeakerAddress() {
        return speakerAddress;
    }

    public void setSpeakerAddress(String speakerAddress) {
        this.speakerAddress = speakerAddress;
    }

    public long getTopBlock() {
        return topBlock;
    }

    public Map<Integer, String> getSecondarySpeakers() {
        return secondarySpeakers;
    }

    public Integer addSecondarySpeaker(String secondarySpeaker) {
        Integer highestIndex = secondarySpeakers.keySet().stream().max(Integer::compare).orElse(-1);
        secondarySpeakers.put(highestIndex + 1, secondarySpeaker);
        return highestIndex + 1;
    }

    public Integer addSecondarySpeaker(Integer index, String secondarySpeaker) {
        if (secondarySpeakers.get(index) != null && !secondarySpeakers.get(index).equals(secondarySpeaker)) {
            LOGGER.error("A secondary speaker (" + secondarySpeakers.get(index) + ") was already present at index " + index);
            return null;
        }
        this.secondarySpeakers.put(index, secondarySpeaker);
        return index;
    }

    public String generateSportChallengeData() {
        this.sportChallengeData = UUID.randomUUID().toString();
        return sportChallengeData;
    }

    public String getSportChallengeData() {
        return sportChallengeData;
    }

    public short getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(short difficulty) {
        this.difficulty = difficulty;
    }

    public void setSportChallengeData(String sportChallengeData) {
        this.sportChallengeData = sportChallengeData;
    }

    @Override
    public String toString() {
        return "SportRound{" +
                "speakerAddress='" + speakerAddress + '\'' +
                ", topBlock=" + topBlock +
                ", difficulty=" + difficulty +
                ", sportChallengeData='" + sportChallengeData + '\'' +
                '}';
    }
}

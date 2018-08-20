/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.ledger;

import io.smilo.commons.HashUtility;
import org.apache.log4j.Logger;

import java.util.Arrays;

public class LamportThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(LamportThread.class);
    private final LamportGenerator lamportGenerator = new LamportGenerator();

    private byte[][] seeds;
    private int count;
    private String[] publicKeys;

    public LamportThread() {

    }

    public boolean checkSeedGenerated(String privateKey, int position, byte[] seed) {
        byte [] seedLamportGenerator = lamportGenerator.getSeedForIthLamportKey(privateKey, position);
        if (!Arrays.equals(seed, seedLamportGenerator)) {

            LOGGER.error("Seeds differ:");
            LOGGER.error("Merkel Seed:" + seed);
            LOGGER.error("Signing Seed:" + seedLamportGenerator);
            return false;
        }
        LOGGER.error("Equal Seeds..");
        return true;
    }
    /**
     * Sets the 2D-byte-array seeds and count
     *
     * @param seeds 2D byte array of seeds
     * @param count Number of keys per thread to run
     */
    public void setData(byte[][] seeds, int count) {
        if (publicKeys == null) {
            publicKeys = new String[count];
        }
        this.seeds = seeds;
        this.count = count;
    }

    /**
     * Returns the public keys of the Lamport Signature pair
     *
     * @return String[] Public keys
     */
    public String[] getPublicKeys() {
        return publicKeys;
    }

    public void run() {
        try {
            for (int i = 0; i < count; i++) {
                publicKeys[i] = HashUtility.digestSHA256ToBase64(lamportGenerator.getLamportPublicKey(seeds[i]));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to run LamportThread", e);
        }
    }

}

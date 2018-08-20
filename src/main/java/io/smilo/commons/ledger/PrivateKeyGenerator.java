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

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PrivateKeyGenerator {

    /**
     * Todo: There is NO WAY this method will appear in any recognizable form in the actual release. This IS NOT a safe way to generate private keys--the output of Java's default Random number
     * generator isn't sufficient. Final release will use a variety of system information including mouse movements, timing of process threads, and SecureRandom.
     *
     * String privateKey = ""; for (int i = 0; i < 32; i++) { privateKey += characterSet.charAt(random.nextInt(characterSet.length())); } return privateKey; }* @return String An insecure private key
     * (seed for Lamport Signatures)
     */
    public String getPrivateKey() {
        String characterSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String privateKey = "";
        for (int i = 0; i < 32; i++) {
            privateKey += characterSet.charAt(random.nextInt(characterSet.length()));
        }
        return privateKey;
    }

}

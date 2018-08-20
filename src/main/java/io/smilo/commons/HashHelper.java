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

package io.smilo.commons;

import org.ethereum.crypto.cryptohash.Keccak256;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
    private static MessageDigest md256;
    private static MessageDigest md512;

    public static byte[] sha256(byte[] msg) {
        return md256.digest(msg);
    }

    public static byte[] sha512(byte[] msg) {
        return md512.digest(msg);
    }

    public static byte[] keccak256(byte[] msg) {
        Keccak256 digest =  new Keccak256();
        digest.update(msg);
        return digest.digest();
    }

    static {
        try {
            md256 = MessageDigest.getInstance("SHA-256"); //Initializes md for SHA256 functions to use
            md512 = MessageDigest.getInstance("SHA-512"); //Initializes md512 for SHA-512
        } catch (NoSuchAlgorithmException error) {
            System.exit(-1);
        }
    }
}

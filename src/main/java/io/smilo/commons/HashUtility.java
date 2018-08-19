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
package io.smilo.commons;

import org.apache.commons.codec.binary.Base32;
import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.StringUtils.isEmpty;

public class HashUtility {

    private static final Logger LOGGER = Logger.getLogger(HashUtility.class);

    public static String digestSHA256ToHEX(String data) {
        return toHEXString(digestSHA256(data));
    }

    public static String digestSHA256ToBase64(String data) {
        return encodeToBase64(digestSHA256(data));
    }

    public static String digestSHA512ToBase64(String data) {
        return encodeToBase64(digestSHA512(data));
    }

    public static byte[] digestSHA256(String data) {
        return digestSHA256(data.getBytes(UTF_8));
    }

    /**
     * This SHA256 function returns a base64 String repesenting the full SHA256 hash of the String toHash
     * The full-length SHA256 hashes are used for the non-Lamport and non-Address levels of the Merkle Tree
     *
     * @param data The bytes to hash using SHA256
     * @return the SHA256 hash in bytes
     */
    public static byte[] digestSHA256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            LOGGER.error("Exception when trying to digest SHA-256 hash ", e);
            return null;
        }
    }

    /**
     * This SHA512 function returns the full-length SHA512 hash of the String toHash. SHA512 is used for the last 2 elements of the Lamport Signature, in order to require any attacker to break one
     * SHA512 hash if they were to crack a Lamport Public Key.
     *
     * @param data The String to hash using SHA512
     * @return String the 128-character resulting from hashing data
     */
    public static byte[] digestSHA512(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            return md.digest(data.getBytes(UTF_8));
        } catch (Exception e) {
            LOGGER.error("Exception when trying to digest SHA-512 hash ", e);
            return null;
        }
    }

    public static String encodeToBase64(byte[] bytes) {
        try {
            return new String(Base64.getEncoder().encode(bytes));
        } catch (Exception e){
            LOGGER.error("Exception when trying to encode byte array to Base64 String", e);
            return null;
        }
    }

    public static byte[] decodeFromBase64(String toDecode) {
        try {
            return Base64.getDecoder().decode(toDecode);
        } catch (Exception e){
            LOGGER.error("Exception when trying to decode Base64 String", e);
            return null;
        }
    }

    public static String encodeToBase32(byte[] bytes) {
        try {
            Base32 base32 = new Base32();
            return new String(base32.encode(bytes));
        } catch (Exception e){
            LOGGER.error("Exception when trying to encode byte array to Base32 String", e);
            return null;
        }
    }

    private static String toHEXString(byte[] data) {
        try {
            return DatatypeConverter.printHexBinary(data);
        } catch (Exception e) {
            LOGGER.error("Exception when trying to convert byte array to HEX string", e);
            return null;
        }
    }

    /**
     * This SHA256 function returns a 16-character, base64 String. The String is shortened to reduce space on the blockchain, and is sufficiently long for security purposes.
     *
     * @param data The String to hash using SHA256
     * @return String The 16-character base64 String resulting from hashing toHash and truncating
     */
    public static String digestSHA256ShortToBase64(String data) {
       String result = digestSHA256ToBase64(data);
        if (!isEmpty(result)) {
            return result.substring(0, 16);
        }
        return null;
    }

    /**
     * Used for the generation of an address. Base32 is more practical for real-world addresses, due to a more convenient ASCII charset.
     * Shortened to 32 characters, as that provides 32^32=1,461,501,637,330,902,918,203,684,832,716,283,019,655,932,542,976 possible addresses.
     * @param data The String to hash using SHA256
     * @return String the base32-encoded String representing the entire SHA256 hash of toHash
     */
    public static String digestSHA256ToBase32(String data) {
        String result = encodeToBase32(digestSHA256(data));
        if (!isEmpty(result)) {
            return result.substring(0, 32);
        }
        return null;
    }

    /**
     * This SHA256 function returns a 256-character binary String representing the full SHA256 hash of the String toHash This binary String is useful when signing a message with a Lamport Signature.
     *
     * @param data The String to hash using SHA256
     * @return String The binary String representing the entire SHA256 hash of toHash
     */
    public static String digestSHA256ToBinaryHash(String data) {
        try {
            byte[] messageHash = digestSHA256(data);
            return new BigInteger(1, messageHash).toString(2);
        } catch (Exception e) {
            LOGGER.error("Unable to create SHA256 binary hash", e);
        }
        return null;
    }
}

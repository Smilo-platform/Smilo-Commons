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

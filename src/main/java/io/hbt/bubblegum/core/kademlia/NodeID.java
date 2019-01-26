package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static io.hbt.bubblegum.core.Configuration.KEY_BIT_LENGTH;

/**
 * Definition class for DHT node identifiers.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class NodeID {

    public static final int KEY_BYTE_LENGTH = KEY_BIT_LENGTH >> 3;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final byte[] key;

    /**
     * Constructor.
     * Builds randomly-generated key.
     */
    public NodeID() {
        this.key = NodeID.generateRandomKey();
    }

    /**
     * Constructor.
     * Builds from hex string.
     * @param id The hex string to use as input.
     * @throws MalformedKeyException
     */
    public NodeID(String id) throws MalformedKeyException {
        byte[] parsedKey = NodeID.keyFromHex(id);
        if(parsedKey == null) throw new MalformedKeyException();
        else this.key = parsedKey;
    }

    /**
     * Constructor.
     * Builds from byte array representation.
     * @param newKey The byte array to build from.
     */
    private NodeID(byte[] newKey) {
        this.key = newKey;
    }

    /**
     * Getter for the byte array representation of this key instance.
     * @return
     */
    public byte[] getKey() {
        return this.key.clone();
    }

    /**
     * Internal. Builds from a hex string.
     * @param id The hex string to build from.
     * @return The byte array derived from the hex input.
     * @throws MalformedKeyException
     */
    private static byte[] keyFromHex(String id) {
        try {
            if(id.length() == KEY_BIT_LENGTH >> 2) return NodeID.hexToBytes(id);
            else return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Helper method to generate a randomly populated byte array of length KEY_BYTE_LENGTH.
     * @return The resulting byte array.
     */
    public static byte[] generateRandomKey() {
        byte[] randomKey = new byte[KEY_BYTE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomKey);
        return randomKey;
    }

    /**
     * Converts a hex string to an array of bytes.
     * @param s The string to convert.
     * @return The resulting byte array.
     * @throws NumberFormatException
     */
    public static byte[] hexToBytes(String s) throws NumberFormatException {
        byte[] b = new byte[s.length() >> 1];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte)v;
        }
        return b;
    }

    /**
     * Converts a byte array to its hex representation.
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length << 1];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j << 1] = hexArray[v >>> 4];
            hexChars[(j << 1)+1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).intern();
    }

    /**
     * Generate an XOR distance between two NodeID instances.
     * @param node The NodeID to compare this instance to.
     * @return The XOR distance in byte array form.
     */
    public byte[] xorDistance(NodeID node) {
        byte[] xored = new byte[KEY_BYTE_LENGTH];
        for(int i = 0; i < KEY_BYTE_LENGTH; i++) {
            xored[i] = (byte)(node.key[i] ^ this.key[i]);
        }
        return xored;
    }

    /**
     * Calculate the shared prefix length of two NodeID instances using the XOR distance metric.
     * @param node The NodeID to compare this instance against.
     * @return The sisze of the shared prefix.
     */
    public int sharedPrefixLength(NodeID node) {
        byte[] xored = this.xorDistance(node);
        int byteIndex = 0;
        int bitIndex = 0;

        while(byteIndex < KEY_BYTE_LENGTH && xored[byteIndex] == 0x00) byteIndex++;

        // Assert: byteIndex is either =/= 0 or > num bytes
        if(byteIndex < KEY_BYTE_LENGTH) {
            byte toTest = xored[byteIndex];
            int currentTest = 128; // 1000 0000
            while(currentTest >= 0) {
                if((currentTest & toTest) > 0) break; // found the first differing bit
                bitIndex++;
                currentTest >>= 1;
            }
        }
        return (byteIndex << 3) + bitIndex;
    }

    /**
     * Helper method to generate a NodeID which shares a prefix of a specified length with this instance.
     * @param length The number of shared prefix bits.
     * @return The resulting NodeID.
     */
    public NodeID generateIDWithSharedPrefixLength(int length) {

        if(length >= this.key.length << 3) return new NodeID(this.key.clone());

        int sharedBytes = length >> 3;
        int sharedBits = length % 8;

        byte[] newID = new byte[this.key.length];
        Configuration.rand.nextBytes(newID);

        // Copy shared whole bytes
        for(int i = 0; i < sharedBytes; i++) newID[i] = this.key[i];

        // Calculate the byte with the bit split
        byte mask = (byte)(0xFF << (8 - sharedBits));
        newID[sharedBytes] = (byte)((this.key[sharedBytes] & mask) + ~(this.key[sharedBytes] | mask));

        return new NodeID(newID);
    }

    /**
     * Generate a NodeID from the SHA1 hash of a string key.
     * @param input The string key.
     * @return The resulting NodeID.
     */
    public static NodeID hash(String input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(input.getBytes());
            NodeID newID = new NodeID();
            for (int i = 0; i < Math.min(result.length, KEY_BYTE_LENGTH); i++) newID.key[i] = result[i];
            return newID;

        } catch (NoSuchAlgorithmException e) {
            System.err.println("This JVM installation does not support SHA1.\nUnable to continue.");
            System.exit(-1);
        }
        return null;
    }

    /**
     * Helper method to directly hash long primitives.
     * @param num The number to hash.
     * @return The resulting NodeID.
     */
    public static NodeID hash(long num) {
        return NodeID.hash("" + num);
    }

    /**
     * Getter for the String representation of this key in hex.
     * @return
     */
    @Override
    public String toString() {
        return bytesToHex(this.key);
    }

    /**
     * NodeID equality operator.
     * @param obj The object to judge equality for.
     * @return Equality judgement.
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NodeID) {
           return (this.hashCode() == obj.hashCode());
        }
        return false;
    }

    /**
     * Helper function to generate NodeID instance hash codes.
     * @return
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.key);
    }

    /**
     * XOR distance (from this key) comparator between two RouterNode instances.
     * @return
     */
    public Comparator<RouterNode> getKeyDistanceComparator() {
       return (o1, o2) -> {
           byte[] o1Distance = this.xorDistance(o1.getNode());
           byte[] o2Distance = this.xorDistance(o2.getNode());
           return new BigInteger(1, o1Distance).abs().compareTo(new BigInteger(1, o2Distance).abs());
       };
    }

} // end NodeID class

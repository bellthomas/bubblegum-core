package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;

import java.security.SecureRandom;
import java.util.Arrays;

public class NodeID {
    public static final int KEY_BIT_LENGTH = 160;
    public final byte[] key;

    public NodeID() {
        this.key = NodeID.generateRandomKey();
    }

    public NodeID(String id) throws MalformedKeyException {
        byte[] parsedKey = NodeID.keyFromHex(id);
        if(parsedKey == null) throw new MalformedKeyException();
        else this.key = parsedKey;
    }

    private static byte[] keyFromHex(String id) throws MalformedKeyException {
        try {
            if(id.length() == KEY_BIT_LENGTH / 4) return NodeID.hexToBytes(id);
            else return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static byte[] generateRandomKey() {
        byte[] randomKey = new byte[KEY_BIT_LENGTH / 8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomKey);
        return randomKey;
    }

    public static byte[] hexToBytes(String s) throws NumberFormatException {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte)v;
        }
        return b;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length << 1];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j << 1] = hexArray[v >>> 4];
            hexChars[(j << 1)+1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public int sharedPrefixLength(NodeID node) {
        byte[] xored = new byte[KEY_BIT_LENGTH / 8];
        for(int i = 0; i < KEY_BIT_LENGTH / 8; i++) {
            xored[i] = (byte)(node.key[i] ^ this.key[i]);
        }

        int byteIndex = 0;
        int bitIndex = 0;
        while(byteIndex < KEY_BIT_LENGTH / 8 && xored[byteIndex] == 0x00) byteIndex++;

        // byteIndex is either =/= 0 or > num bytes
        if(byteIndex < KEY_BIT_LENGTH / 8) {
            byte toTest = xored[byteIndex];
            int currentTest = 128;
            while(currentTest >= 0) {
                if((currentTest & toTest) > 0) break;
                bitIndex++;
                currentTest >>= 1;
            }
        }
        return (byteIndex << 3) + bitIndex;
    }

    @Override
    public String toString() {
        return bytesToHex(this.key);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NodeID) {
           return (this.hashCode() == obj.hashCode());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.key);
    }
}

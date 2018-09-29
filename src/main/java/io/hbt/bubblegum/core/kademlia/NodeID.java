package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;

import java.security.SecureRandom;

public class NodeID {
    private byte[] key;

    public NodeID() {
        this.key = NodeID.generateRandomKey();
    }

    public NodeID(String id) throws MalformedKeyException {
        if(id.length() != 40) throw new MalformedKeyException();
        try {
            key = hexToBytes(id);
        } catch (NumberFormatException nfe) {
            throw new MalformedKeyException();
        }
    }

    public static byte[] generateRandomKey() {
        byte[] randomKey = new byte[20];
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
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public String toString() {
        return bytesToHex(this.key);
    }
}

package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class NodeID {
    public static final int KEY_BIT_LENGTH = 64;
    public static final int KEY_BYTE_LENGTH = KEY_BIT_LENGTH >> 3;

    private final byte[] key;

    public NodeID() {
        this.key = NodeID.generateRandomKey();
    }

    public NodeID(String id) throws MalformedKeyException {
        byte[] parsedKey = NodeID.keyFromHex(id);
        if(parsedKey == null) throw new MalformedKeyException();
        else this.key = parsedKey;
    }

    private NodeID(byte[] newKey) {
        this.key = newKey;
    }

    public byte[] getKey() {
        return this.key.clone();
    }

    private static byte[] keyFromHex(String id) throws MalformedKeyException {
        try {
            if(id.length() == KEY_BIT_LENGTH >> 2) return NodeID.hexToBytes(id);
            else return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static byte[] generateRandomKey() {
        byte[] randomKey = new byte[KEY_BYTE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomKey);
        return randomKey;
    }

    public static byte[] hexToBytes(String s) throws NumberFormatException {
        byte[] b = new byte[s.length() >> 1];
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

    public byte[] xorDistance(NodeID node) {
        byte[] xored = new byte[KEY_BYTE_LENGTH];
        for(int i = 0; i < KEY_BYTE_LENGTH; i++) {
            xored[i] = (byte)(node.key[i] ^ this.key[i]);
        }
        return xored;
    }

    public int sharedPrefixLength(NodeID node) {
        byte[] xored = this.xorDistance(node);
        int byteIndex = 0;
        int bitIndex = 0;

        while(byteIndex < KEY_BYTE_LENGTH && xored[byteIndex] == 0x00) byteIndex++;

        // byteIndex is either =/= 0 or > num bytes
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

    public NodeID generateIDWithSharedPrefixLength(int length) {

        if(length >= this.key.length << 3) return new NodeID(this.key.clone());

        int sharedBytes = length >> 3;
        int sharedBits = length % 8;

        byte[] newID = new byte[this.key.length];
        new Random().nextBytes(newID);

        // Copy shared whole bytes
        for(int i = 0; i < sharedBytes; i++) newID[i] = this.key[i];

        // Calculate the byte with the bit split
        byte mask = (byte)(0xFF << (8 - sharedBits));
        newID[sharedBytes] = (byte)((this.key[sharedBytes] & mask) + ~(this.key[sharedBytes] | mask));

        return new NodeID(newID);
    }

    public String getKeyBitsString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : this.key) {
            sb.append(Integer.toBinaryString(b & 255 | 256).substring(1) + " ");
        }
        return sb.toString();
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

    public Comparator<RouterNode> getKeyDistanceComparator() {
       return (o1, o2) -> {
//           BigInteger a1 = new BigInteger(1, o1.getIdentifier().getKey());
//           BigInteger a2 = new BigInteger(1, o2.getIdentifier().getKey());
//           a1 = a1.xor(new BigInteger(1, key));
//           a2 = a2.xor(new BigInteger(1, key));
//           return a1.abs().compareTo(a2.abs());

           byte[] o1Distance = this.xorDistance(o1.getNode());
           byte[] o2Distance = this.xorDistance(o2.getNode());
           return new BigInteger(1, o1Distance).abs().compareTo(new BigInteger(1, o2Distance).abs());
       };
    }

}

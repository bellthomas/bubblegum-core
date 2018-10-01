package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;

public class NodeID {
    public static final int KEY_BIT_LENGTH = 160;
    public static final int KEY_BYTE_LENGTH = KEY_BIT_LENGTH >> 3;
    public final byte[] key;

    public NodeID() {
        this.key = NodeID.generateRandomKey();
    }

    public NodeID(String id) throws MalformedKeyException {
        byte[] parsedKey = NodeID.keyFromHex(id);
        if(parsedKey == null) throw new MalformedKeyException();
        else this.key = parsedKey;
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

    public Comparator<BubblegumNode> getKeyDistanceComparator() {
       return (o1, o2) -> {
//           BigInteger a1 = new BigInteger(1, o1.getIdentifier().getKey());
//           BigInteger a2 = new BigInteger(1, o2.getIdentifier().getKey());
//           a1 = a1.xor(new BigInteger(1, key));
//           a2 = a2.xor(new BigInteger(1, key));
//           return a1.abs().compareTo(a2.abs());

           byte[] o1Distance = this.xorDistance(o1.getIdentifier());
           byte[] o2Distance = this.xorDistance(o2.getIdentifier());
           return new BigInteger(1, o1Distance).abs().compareTo(new BigInteger(1, o2Distance).abs());
       };
    }

}

package io.hbt.bubblegum.core.auxiliary;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Helper class to process large set of byte arrays.
 */
public class ComparableBytePayload implements Comparable<ComparableBytePayload> {
    private final byte[] payload;

    /**
     * Constructor.
     * @param payload The byte array being represented.
     */
    public ComparableBytePayload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * Retrieve the payload.
     * @return The payload being represented.
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * Wrap many byte arrays from a collection.
     * @param collection The collection of byte arrays.
     * @return A set of wrapped payloads.
     */
    public static Set<ComparableBytePayload> fromCollection(Collection<? extends byte[]> collection) {
        Set<ComparableBytePayload> result = new HashSet<>();
        for(byte[] item : collection) result.add(new ComparableBytePayload(item));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ComparableBytePayload) {
            return this.customComparison(((ComparableBytePayload) obj).getPayload());
        }
        else if(obj instanceof byte[]) {
            return this.customComparison((byte[])obj);
        }
        else {
            return false;
        }
    }

    /**
     * Custom byte array comparison.
     * Much faster than the standard Arrays equality function.
     * @param toCompare The byte array to compare to this one.
     * @return Whether the byte arrays are the same.
     */
    private boolean customComparison(byte[] toCompare) {
        if(this.payload.length != toCompare.length) return false;
        else {
            for(int i = 0; i < this.payload.length; i++) {
                if(this.payload[i] != toCompare[i]) return false;
            }
            return true;
        }
    }

    @Override
    public int compareTo(ComparableBytePayload o) {
        return Arrays.compare(this.payload, o.getPayload());
    }

} // end ComparableBytePayload class

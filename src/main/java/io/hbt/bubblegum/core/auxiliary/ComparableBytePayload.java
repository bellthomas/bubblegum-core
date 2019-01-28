package io.hbt.bubblegum.core.auxiliary;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ComparableBytePayload implements Comparable<ComparableBytePayload> {
    private final byte[] payload;
    public ComparableBytePayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public static Set<ComparableBytePayload> fromCollection(Collection<? extends byte[]> collection) {
        Set<ComparableBytePayload> result = new HashSet<>();
        for(byte[] item : collection) result.add(new ComparableBytePayload(item));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ComparableBytePayload) {
//            return Arrays.equals(this.payload, ((ComparableBytePayload) obj).getPayload());
            return this.customComparison(((ComparableBytePayload) obj).getPayload());
        }
        else if(obj instanceof byte[]) {
//            return Arrays.equals(this.payload, (byte[])obj);
            return this.customComparison((byte[])obj);
        }
        else {
            return false;
        }
    }

    // TODO evaluate
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
}

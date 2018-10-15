package io.hbt.bubblegum.core.social;

import java.util.HashSet;

public class Database {
    public HashSet<String> db = new HashSet<>();

    public boolean hasKey(String key) {
        return this.db.contains(key);
    }

    public byte[] valueForKey(String key) {
        return new byte[] { 0x01 };
    }

    public void add(String key) {
        this.db.add(key);
    }
}

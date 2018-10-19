package io.hbt.bubblegum.core.social;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.Arrays;
import java.util.HashMap;

public class Database {

    private HashMap<String, byte[]> db = new HashMap<>();
    private BubblegumNode localNode;

    public Database(BubblegumNode localNode) {
        this.localNode = localNode;
    }

    public boolean hasKey(String key) {
        return this.db.containsKey(key);
    }

    public byte[] valueForKey(String key) {
        return this.db.get(key);
    }

    public boolean add(String key, byte[] value) {
        this.db.put(key, value);
        this.print("[Database] Saved " + key + " -> " + Arrays.toString(value));
        return true;
    }

    private void print(String msg) {
        this.localNode.log(msg);
    }
}

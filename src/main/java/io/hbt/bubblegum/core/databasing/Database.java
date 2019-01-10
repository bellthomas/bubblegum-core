package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ComparableBytePayload;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Database {

    private ConcurrentHashMap<String, HashMap<String, List<Pair<ComparableBytePayload, Long>>>> db;

    private static Database instance;
    private static ContentDatabase cdbInstance;
    private static MasterDatabase masterDatabase;

    protected static final String DB_FOLDER_PATH = ".databases/";
    protected static final long EXPIRY_AGE = 30 * 60; // seconds

    private final ConcurrentHashMap<String, Pair<String, Long>> lastNetworkUpdates;

    private Database() {
        this.checkDatabasesDirectory();
        this.db = new ConcurrentHashMap<>();
        this.lastNetworkUpdates = new ConcurrentHashMap<>();
        Database.cdbInstance = ContentDatabase.getInstance();
        Database.masterDatabase = MasterDatabase.getInstance();
    }

    public synchronized static Database getInstance() {
        if(Database.instance == null) Database.instance = new Database();
        return Database.instance;
    }

    public void initialiseExpiryScheduler(ActivityExecutionContext context) {
        if(context != null) {
            context.scheduleTask("system", () -> this.checkForExpiredPosts(), 30, 30, TimeUnit.SECONDS);
        }
    }


    public Map<Integer, List<NetworkDetails>> loadNetworksFromDatabase() {
        return Database.masterDatabase.loadNetworksFromDatabase();
    }

    public void updateNodeInDatabase(BubblegumNode node) {
        Database.masterDatabase.updateNetwork(node);
    }

    public void updateNodesInDatabase(List<BubblegumNode> nodes) {
        Database.masterDatabase.updateNetworks(nodes);
    }

    public boolean hasKey(String node, String key) {
        if(!this.db.containsKey(node)) return false;
        else return this.db.get(node).containsKey(key);
    }

    public List<byte[]> valueForKey(String node, String key) {
        if(!this.db.containsKey(node)) return null;
        else return this.db.get(node).get(key).stream().map((p) -> p.getFirst().getPayload()).collect(Collectors.toList());
    }

    public boolean add(String node, String key, byte[] value) {
        if(!this.db.containsKey(node)) this.db.put(node, new HashMap<>());
        if(!this.db.get(node).containsKey(key)) this.db.get(node).put(key, new ArrayList<>());

        ComparableBytePayload newPayload = new ComparableBytePayload(value);
        synchronized (this.db.get(node).get(key)) {
            // If we have an old version, remove it and save the new one
            this.db.get(node).get(key).removeIf(p -> p.getFirst().equals(newPayload));
            this.db.get(node).get(key).add(new Pair<>(new ComparableBytePayload(value), System.currentTimeMillis()));
        }

        this.print("[Database] Saved " + key + " -> " + Arrays.toString(value));
        return true;
    }

    public void saveUserMeta(BubblegumNode node, String content) {
        Database.cdbInstance.saveMeta("username", node, content);
    }

    // TODO broken
    public void publishEntityMeta(BubblegumNode node, String key, String globalPostIdentifier) {
        node.getExecutionContext().addCompoundActivity(node.getIdentifier(), () -> {
            String uniquifier = ":" + UUID.randomUUID().toString();
            if(node.store(NodeID.hash(key), globalPostIdentifier.getBytes())) {
                this.lastNetworkUpdates.put(globalPostIdentifier + uniquifier, new Pair<>(key, System.currentTimeMillis()));
            } else {
//                System.out.println("Failed to store value");
                this.lastNetworkUpdates.put(globalPostIdentifier + uniquifier, new Pair<>(key, 0L));
            }
        });
    }

    public Post savePost(BubblegumNode node, String content) {
        return this.savePost(node, content, "");
    }

    public Post savePost(BubblegumNode node, String content, String inResponseTo) {
        Post saved = Database.cdbInstance.savePost(node, content, inResponseTo);
        long epochBin = saved.getTimeCreated() / Configuration.BIN_EPOCH_DURATION;
        String globalPostIdentifier = Database.globalIdentifierForPost(node, saved);
        this.publishEntityMeta(node, Long.toString(epochBin), globalPostIdentifier);

//        System.out.println(epochBin + " -> " + globalPostIdentifier);
        return saved;
    }

    public Post getPost(BubblegumNode node, String id) {
        return Database.cdbInstance.getPost(node, id);
    }

    public List<Post> getAllPosts(BubblegumNode node) {
        return Database.cdbInstance.getPosts(node);
    }

    public List<Post> queryPosts(BubblegumNode node, long from, long to, List<String> ids) {
        return Database.cdbInstance.queryPosts(node, from, to, ids);
    }

    private void checkDatabasesDirectory() {
        File directory = new File(".databases");
        if (!directory.exists()) directory.mkdir();
    }

    public void checkForExpiredPosts() {
        long current = System.currentTimeMillis();
        this.db.forEach((k1,v1) -> {
            v1.forEach((k2,v2) -> {
                v2.removeAll(
                    v2.stream()
                        .filter((p) -> ((p.getSecond() + Database.EXPIRY_AGE * 1000) < current)) // older than the expiry age
                        .collect(Collectors.toList())
                );
            });
        });
    }

    public void refreshExpiringPosts(BubblegumNode node, int margin) {
        long cutoff = System.currentTimeMillis() - (Database.EXPIRY_AGE * 1000) + margin;
        List<Pair<String, String>> ids = this.lastNetworkUpdates.entrySet()
            .stream()
            .filter((e) -> e.getValue().getSecond() < cutoff && e.getKey().startsWith(node.getNodeIdentifier().toString()))
            .map((e) -> {
                String[] keyParts = e.getKey().split(":");
                if(keyParts.length == 3) return new Pair<>(keyParts[0]+":"+keyParts[1], e.getValue().getFirst());
                else return null;
            })
            .collect(Collectors.toList());

        for(Pair<String, String> p : ids) {
            if (p != null) this.publishEntityMeta(node, p.getSecond(), p.getFirst());
        }
    }

    public static String globalIdentifierForPost(BubblegumNode node, Post post) {
        return node.getNodeIdentifier().toString() + ":" + post.getID();
    }

    public void reset() {
        // TODO finish
        Database.masterDatabase.resetDatabases();
    }

    private void print(String msg) {
//        this.localNode.log(msg);
    }
}

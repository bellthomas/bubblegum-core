package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ComparableBytePayload;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
            Iterator<Pair<ComparableBytePayload, Long>> iterator = this.db.get(node).get(key).iterator();
            while(iterator.hasNext()) {
                if(iterator.next().getFirst().equals(newPayload)) iterator.remove();
            }

            this.db.get(node).get(key).add(new Pair<>(new ComparableBytePayload(value), System.currentTimeMillis()));
        }

        return true;
    }

    public void saveUserMeta(BubblegumNode node, String metaName, String content) {
        Database.cdbInstance.saveMeta(metaName, node, content);
    }

    // TODO broken
    public void publishEntityMeta(BubblegumNode node, String key, String globalPostIdentifier, String oldUniquifier) {
        node.getExecutionContext().addCompoundActivity(node.getIdentifier(), () -> {
            String uniquifier = oldUniquifier == null ? ":" + UUID.randomUUID().toString() : ":" + oldUniquifier;
            if(node.store(NodeID.hash(key), globalPostIdentifier.getBytes())) {
                this.lastNetworkUpdates.put(globalPostIdentifier + uniquifier, new Pair<>(key, System.currentTimeMillis()));
            } else {
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
        this.publishEntityMeta(node, Long.toString(epochBin), globalPostIdentifier, null);

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
        File directory = new File(Configuration.DB_FOLDER_PATH);
        if (!directory.exists()) directory.mkdir();
    }

    public void checkForExpiredPosts() {
        long current = System.currentTimeMillis();
        List<Pair<ComparableBytePayload, Long>> toRemove = new ArrayList<>();
        for(Map.Entry<String, HashMap<String, List<Pair<ComparableBytePayload, Long>>>> entry : this.db.entrySet()) {
            for(Map.Entry<String, List<Pair<ComparableBytePayload, Long>>> innerEntry : entry.getValue().entrySet()) {
                for(Pair<ComparableBytePayload, Long> p : innerEntry.getValue()) {
                    if((p.getSecond() + Configuration.DB_ENTITY_EXPIRY_AGE) < current) {
                        toRemove.add(p);
                    }
                }

                innerEntry.getValue().removeAll(toRemove);
                toRemove.clear();
            }
        }

//        this.db.forEach((k1,v1) -> {
//            v1.forEach((k2,v2) -> {
//                v2.removeAll(
//                    v2.stream()
//                        .filter((p) -> ((p.getSecond() + Configuration.DB_ENTITY_EXPIRY_AGE) < current)) // older than the expiry age
//                        .collect(Collectors.toList())
//                );
//            });
//        });
    }

    // TODO cleanup
    public void refreshExpiringPosts(BubblegumNode node, int margin) {
        long cutoff = System.currentTimeMillis() - Configuration.DB_ENTITY_EXPIRY_AGE + margin;

        Iterator<Map.Entry<String, Pair<String, Long>>> iterator = this.lastNetworkUpdates.entrySet().iterator();
        Map.Entry<String, Pair<String, Long>> currentEntry;
        String globalID, uniquifier;
        float localRandomRefreshProbability = 10000 / Configuration.RANDOM_POST_REFRESH_PROBABILITY;
        while(iterator.hasNext()) {
            currentEntry = iterator.next();
            if(currentEntry.getKey().startsWith(node.getNodeIdentifier().toString())) {
               if(currentEntry.getValue().getSecond() < cutoff ||
                  Configuration.random(10000) < localRandomRefreshProbability) {
                   globalID = currentEntry.getKey().substring(0, currentEntry.getKey().length() - 37);
                   uniquifier = currentEntry.getKey().substring(currentEntry.getKey().length() - 36);
                   currentEntry.getValue().setSecond(System.currentTimeMillis());
                   this.publishEntityMeta(node, currentEntry.getValue().getFirst(), globalID, uniquifier);
               }
            }
        }


//        List<String[]> ids = this.lastNetworkUpdates.entrySet()
//            .stream()
//            .filter((e) -> (
//                e.getValue().getSecond() < cutoff ||
//                Configuration.random(10000) < (10000.0 / (Configuration.DB_ENTITY_EXPIRY_AGE / Configuration.POST_EXPIRY_REFRESH_CHECK)))
//                && e.getKey().startsWith(node.getNodeIdentifier().toString())
//            )
//            .map((e) -> {
//                String[] keyParts = e.getKey().split(":");
//                if (keyParts.length == 3) return new String[] {e.getValue().getFirst(), keyParts[0]+":"+keyParts[1], keyParts[2]};
//                else return null;
//            })
//            .collect(Collectors.toList());
//
//        for(String[] p : ids) {
//            if (p != null && p.length == 3) this.publishEntityMeta(node, p[0], p[1], p[2]);
//        }
    }

    public static String globalIdentifierForPost(BubblegumNode node, Post post) {
        return node.getNodeIdentifier().toString() + ":" + post.getID();
    }

    public void reset() {
        // TODO finish
        Database.masterDatabase.resetDatabases();
    }

}

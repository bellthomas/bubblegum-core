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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * The main databse implementation for Bubblegum instances.
 */
public class Database {

    private HashMap<String, HashMap<String, List<Pair<ComparableBytePayload, Long>>>> db;
    private static Database instance;
    private static ContentDatabase cdbInstance;
    private final HashMap<String, Pair<String, Long>> lastNetworkUpdates;

    /**
     * Constructor.
     */
    private Database() {
        this.checkDatabasesDirectory();
        this.db = new HashMap<>();
        this.lastNetworkUpdates = new HashMap<>();
        Database.cdbInstance = ContentDatabase.getInstance();
    }

    /**
     * Singleton getInstance().
     * @return The singleton instance.
     */
    public synchronized static Database getInstance() {
        if(Database.instance == null) Database.instance = new Database();
        return Database.instance;
    }

    /**
     * Initialise a maintenance task to check for expired posts.
     * @param context
     */
    public void initialiseExpiryScheduler(ActivityExecutionContext context) {
        if(context != null) {
            context.scheduleTask("system", () -> {
                this.checkForExpiredPosts();
                System.gc();
            }, 30, 30, TimeUnit.SECONDS);
        }
    }

    /**
     * Check if the database holds a particular key.
     * @param node The owning node's identifier.
     * @param key The key to check for.
     * @return The result.
     */
    public boolean hasKey(String node, String key) {
        if(!this.db.containsKey(node)) return false;
        else return this.db.get(node).containsKey(key);
    }

    /**
     * Fetch the values stored against a given key.
     * @param node The owning node's identifier.
     * @param key The key to retrieve.
     * @return The found values.
     */
    public List<byte[]> valueForKey(String node, String key) {
        if(!this.db.containsKey(node)) return null;
        else return this.db.get(node).get(key).stream().map((p) -> p.getFirst().getPayload()).collect(Collectors.toList());
    }

    /**
     * Store a value in the Database.
     * @param node The owning node's identifier.
     * @param key The key.
     * @param value The value.
     * @return Whether add was successful.
     */
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

    /**
     * Save a user's meta value.
     * @param node The user's node's identifier.
     * @param metaName The meta key.
     * @param content The meta value.
     */
    public void saveUserMeta(BubblegumNode node, String metaName, String content) {
        Database.cdbInstance.saveMeta(metaName, node, content);
    }

    /**
     * Publish/republish post indices to the network.
     * @param node The owning node's identifier.
     * @param key The index key.
     * @param globalPostIdentifier The post's identifier.
     * @param oldUniquifier Previous data.
     */
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

    /**
     * Save a post.
     * @param node The publishing node's identifier.
     * @param content The post's content.
     * @return Whether the operation was successful.
     */
    public Post savePost(BubblegumNode node, String content) {
        return this.savePost(node, content, "");
    }

    /**
     * Save a post.
     * @param node The publishing node's identifier.
     * @param content The post's content.
     * @param inResponseTo The ID of the post this one is in response to.
     * @return Whether the operation was successful.
     */
    public Post savePost(BubblegumNode node, String content, String inResponseTo) {
        Post saved = Database.cdbInstance.savePost(node, content, inResponseTo);
        long epochBin = saved.getTimeCreated() / Configuration.BIN_EPOCH_DURATION;
        String globalPostIdentifier = Database.globalIdentifierForPost(node, saved);
        this.publishEntityMeta(node, Long.toString(epochBin), globalPostIdentifier, null);
        return saved;
    }

    /**
     * Fetch a local post.
     * @param node The owning node's identifier.
     * @param id The post's identifier.
     * @return the Post instnace or null if not found.
     */
    public Post getPost(BubblegumNode node, String id) {
        return Database.cdbInstance.getPost(node, id);
    }

    /**
     * Fetch all local posts for a node.
     * @param node The owning node's identifier.
     * @return The list of all Posts.
     */
    public List<Post> getAllPosts(BubblegumNode node) {
        return Database.cdbInstance.getPosts(node);
    }

    /**
     * Query posts.
     * @param node The local BubblegumNode.
     * @param from Query: start UNIX time.
     * @param to Query: end UNIX time.
     * @param ids Query: the set of post IDs.
     * @return The found set of Post instances.
     */
    public List<Post> queryPosts(BubblegumNode node, long from, long to, List<String> ids) {
        return Database.cdbInstance.queryPosts(node, from, to, ids);
    }

    /**
     * Create the databases directory is required.
     */
    private void checkDatabasesDirectory() {
        File directory = new File(Configuration.DB_FOLDER_PATH);
        if (!directory.exists()) directory.mkdir();
    }

    /**
     * Scan the database for expired posts.
     */
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
    }

    /**
     * Republish persistent keys if they are about to expire.
     * @param node The owning node's identifier.
     * @param margin How close to expiration posts need to be to be republished,
     */
    public static void refreshExpiringPosts(BubblegumNode node, int margin) {
        long cutoff = System.currentTimeMillis() - Configuration.DB_ENTITY_EXPIRY_AGE + margin;

        Iterator<Map.Entry<String, Pair<String, Long>>> iterator = Database.getInstance().lastNetworkUpdates.entrySet().iterator();
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
                   Database.getInstance().publishEntityMeta(node, currentEntry.getValue().getFirst(), globalID, uniquifier);
               }
            }
        }
    }

    /**
     * Calculate the global ID for a Post instance.
     * @param node The owning node.
     * @param post The Post instance.
     * @return
     */
    public static String globalIdentifierForPost(BubblegumNode node, Post post) {
        return node.getNodeIdentifier().toString() + ":" + post.getID();
    }

    public Map<Integer, List<NetworkDetails>> loadNetworksFromDatabase() {
        // return Database.masterDatabase.loadNetworksFromDatabase();
        return new HashMap<>();
    }

    public void updateNodeInDatabase(BubblegumNode node) {
        // Database.masterDatabase.updateNetwork(node);
    }

    public void updateNodesInDatabase(List<BubblegumNode> nodes) {
        // Database.masterDatabase.updateNetworks(nodes);
    }

    public void reset() {
        // Database.masterDatabase.resetDatabases();
    }

} // end Database class

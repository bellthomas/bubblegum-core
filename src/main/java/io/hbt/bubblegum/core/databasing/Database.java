package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.auxiliary.ComparableBytePayload;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Database {

    private ConcurrentHashMap<String, HashMap<String, List<Pair<ComparableBytePayload, Long>>>> db;

    private static Database instance;
    private static ContentDatabase cdbInstance;
    private static MasterDatabase masterDatabase;

    protected static final String DB_FOLDER_PATH = ".databases/";
    protected static final long EXPIRY_AGE = 60; // seconds

    private final ConcurrentHashMap<String, Pair<Long, Long>> lastNetworkUpdates;

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
            context.scheduleTask(() -> this.checkForExpiredPosts(), 10, 10, TimeUnit.SECONDS);
        }
    }


//    public Database(BubblegumNode localNode) {
//
////        Connection connection = null;
////        try
////        {
////            this.checkDatabasesDirectory();
////
//            // create a databasing connection
////            connection = DriverManager.getConnection("jdbc:sqlite:.databases/" + this.localNode.getNodeIdentifier().toString() + ".db");
////            Statement statement = connection.createStatement();
////            statement.setQueryTimeout(30);  // set timeout to 30 sec.
//
////            statement.executeUpdate("drop table if exists person");
////            statement.executeUpdate("create table person (id integer, name string)");
////            statement.executeUpdate("insert into person values(1, 'leo')");
////            statement.executeUpdate("insert into person values(2, 'yui')");
////            ResultSet rs = statement.executeQuery("select * from person");
////            while(rs.next())
////            {
////                // read the result set
////                System.out.println("name = " + rs.getString("name"));
////                System.out.println("id = " + rs.getInt("id"));
////            }
////        }
////        catch(SQLException e)
////        {
////            // if the error message is "out of memory",
////            // it probably means no databasing file is found
////            System.err.println(e.getMessage());
////            e.printStackTrace();
////        }
////        finally
////        {
////            try {
////                if(connection != null) connection.close();
////            }
////            catch(SQLException e) {
////                // connection close failed.
////                System.err.println(e);
////                e.printStackTrace();
////            }
////        }
//
//    }

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

        // TODO error on removeAll line?
        // If we have an old version, remove it
        ComparableBytePayload newPayload = new ComparableBytePayload(value);
        synchronized (this.db) {
            this.db.get(node).get(key).removeAll(
                this.db.get(node).get(key).stream().filter((p) -> p.getFirst().equals(newPayload)).collect(Collectors.toList())
            );
        }

        // Save new version
        this.db.get(node).get(key).add(new Pair<>(new ComparableBytePayload(value), System.currentTimeMillis()));

        this.print("[Database] Saved " + key + " -> " + Arrays.toString(value));
        return true;
    }

    public void saveUserMeta(BubblegumNode node, String content) {
        Database.cdbInstance.saveMeta("username", node, content);
    }

    private void savePostMeta(BubblegumNode node, long epochBin, String globalPostIdentifier) {
        if(node.store(NodeID.hash(epochBin), globalPostIdentifier.getBytes())) {
            this.lastNetworkUpdates.put(globalPostIdentifier, new Pair<>(epochBin, System.currentTimeMillis()));
        } else {
            this.lastNetworkUpdates.put(globalPostIdentifier, new Pair<>(epochBin, 0L));
        }
    }

    public Post savePost(BubblegumNode node, String content) {
        Post saved = Database.cdbInstance.savePost(node, content);
        long epochBin = saved.getTimeCreated() / BubblegumNode.epochBinDuration;
        String globalPostIdentifier = node.getNodeIdentifier().toString() + ":" + saved.getID();
        this.savePostMeta(node, epochBin, globalPostIdentifier);

        System.out.println(epochBin + " -> " + globalPostIdentifier);
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
        List<Pair<String, Long>> ids = this.lastNetworkUpdates.entrySet()
            .stream()
            .filter((e) -> e.getValue().getSecond() < cutoff && e.getKey().startsWith(node.getNodeIdentifier().toString()))
            .map((e) -> new Pair<>(e.getKey(), e.getValue().getFirst()))
            .collect(Collectors.toList());

        if(ids.size() > 0) {
            System.out.println("Refreshing " + ids.size() + " posts");
        }
        ids.forEach((p) -> this.savePostMeta(node, p.getSecond(), p.getFirst()));
    }

    public void reset() {
        // TODO finish
        Database.masterDatabase.resetDatabases();
    }

    private void print(String msg) {
//        this.localNode.log(msg);
    }
}

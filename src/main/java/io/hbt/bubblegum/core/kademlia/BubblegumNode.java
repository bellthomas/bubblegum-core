package io.hbt.bubblegum.core.kademlia;

import co.paralleluniverse.fibers.Suspendable;
import io.hbt.bubblegum.core.BubblegumCellServer;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.logging.Logger;
import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.MasterDatabase;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.databasing.SnapshotDatabase;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.BootstrapActivity;
import io.hbt.bubblegum.core.kademlia.activities.DiscoveryActivity;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.activities.QueryActivity;
import io.hbt.bubblegum.core.kademlia.activities.StoreActivity;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is local node
 * One per social network a part of
 */
public class BubblegumNode {
    private String identifier, networkIdentifier;
    private SocialIdentity socialIdentity;
    private NodeID nodeIdentifier;
    private RoutingTable routingTable;
    private BubblegumCellServer server;
    private ActivityExecutionContext executionContext;
    private Database db;
    private Logger logger;
    private ScheduledExecutorService internalScheduler;

    public static long epochBinDuration = 1000 * 60 * 15; // 15 minutes

    private BubblegumNode(
        String identifier,
        String networkIdentifier,
        SocialIdentity socialIdentity,
        ActivityExecutionContext context,
        BubblegumCellServer server,
        Logger logger,
        NodeID nid,
        int port
    ) {
        this.identifier = identifier;
        this.networkIdentifier = networkIdentifier;
        this.socialIdentity = socialIdentity;
        this.nodeIdentifier = nid;
        this.executionContext = context;
        this.logger = logger;
        this.server = server;
        this.server.registerNewLocalNode(this);

        this.routingTable = new RoutingTable(this);
        this.db = Database.getInstance();
        this.db.saveUserMeta(this, "Harri");

        this.setupInternalScheduling();

        this.log("Constructed BubblegumNode: " + this.nodeIdentifier.toString());
    }

    private void setupInternalScheduling() {
//        this.internalScheduler = Executors.newSingleThreadScheduledExecutor();

        // Setup Router snapshots
//        this.internalScheduler.scheduleAtFixedRate(
//            () -> {
//                this.log("[Scheduled] Saving router snapshot");
//                this.executionContext.addActivity(this.getNodeIdentifier().toString(), () -> this.saveRouterSnapshot());
//            },
//            3, 5, TimeUnit.MINUTES
//        );

        // Setup bucket refreshes
//        this.internalScheduler.scheduleAtFixedRate(
//            () -> {
//                this.log("[Scheduled] Saving router snapshot");
//                this.executionContext.addActivity(this.getNodeIdentifier().toString(), () -> this.routingTable.refreshBuckets());
//            },
//            1, 1, TimeUnit.MINUTES
//        );

        // Refresh/delete content as it expires
        this.getExecutionContext().scheduleTask(() -> {
            this.db.refreshExpiringPosts(this, 15);
        }, 60, 30, TimeUnit.SECONDS);

    }

    public String getIdentifier() {
        return this.identifier;
    }

    public NodeID getNodeIdentifier() {
        return this.nodeIdentifier;
    }

    @Override
    public String toString() {
        return nodeIdentifier.toString();
    }

    @Suspendable
    public boolean bootstrap(InetAddress address, int port, String foreignRecipient) {

        RouterNode to = new RouterNode(new NodeID(), address, port);
        BootstrapActivity boostrapActivity = new BootstrapActivity(this, to, foreignRecipient, (networkID) -> this.networkIdentifier = networkID);
        boostrapActivity.run(); // sync

        if(boostrapActivity.getSuccess()) {
            this.saveRouterSnapshot();
            Database.getInstance().updateNodeInDatabase(this);
            SnapshotDatabase.saveRouterSnapshot(this, this.getRoutingTable());
            return true;
        }
        else {
            return false;
        }
    }

    public Set<String> discover(InetAddress address, int port) {
        RouterNode to = new RouterNode(new NodeID(), address, port);
        DiscoveryActivity discoveryActivity = new DiscoveryActivity(this, to);
        discoveryActivity.run();

        if(discoveryActivity.getComplete() && discoveryActivity.getSuccess()) {
            return discoveryActivity.getEntries();
        }

        return null;
    }

    public List<byte[]> lookup(NodeID id) {
        LookupActivity lookupActivity = new LookupActivity(this, id, 5, true);
        lookupActivity.run();

        if(lookupActivity.getComplete() && lookupActivity.getSuccess()) {
            return lookupActivity.getResult();
        }
        else {
            return null;
        }
    }

    public boolean store(NodeID id, byte[] value) {
        StoreActivity storeActivity = new StoreActivity(this, id.toString(), value);
        storeActivity.run();
        return (storeActivity.getComplete() && storeActivity.getSuccess());
    }

    public List<Post> query(NodeID id, long start, long end, List<String> ids) {

        // Check for local query
        if(id.toString().equals(this.getNodeIdentifier().toString())) {
            return this.db.queryPosts(this, start, end, ids);
        }

        LookupActivity lookupActivity = new LookupActivity(this, id, 1, false);
        lookupActivity.run();

        if(lookupActivity.getComplete() && lookupActivity.getSuccess()) {
            if(lookupActivity.getClosestNodes().size() > 0) {
                RouterNode to = null;
                for(RouterNode node : lookupActivity.getClosestNodes()) {
                    if(node.getNode().equals(id)) {
                        to = node;
                        break;
                    }
                }

                if(to == null) return null;

                // Got the destination
                QueryActivity queryActivity = new QueryActivity(this, to, start, end, ids);
                queryActivity.run();

                if(queryActivity.getComplete() && queryActivity.getSuccess()) {
                    return queryActivity.getResults();
                }
                else {
                    return null;
                }
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public String getNetworkIdentifier() {
        return this.networkIdentifier;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public BubblegumCellServer getServer() {
        return this.server;
    }

    public Database getDatabase() {
        return this.db;
    }

    public String getRecipientID() {
        return this.networkIdentifier + ":" + this.nodeIdentifier.toString();
    }

    public void log(String message) {
        this.logger.logMessage(message);
    }

    /* Database */
    public boolean databaseHasKey(String key) {
        return this.db.hasKey(this.identifier, key);
    }

    public List<byte[]> databaseRetrieveValue(String key) {
        return this.db.valueForKey(this.identifier, key);
    }

    public Post savePost(String content) {
        if(content != null) return this.db.savePost(this, content);
        return null;
    }

    public Post getPost(String id) {
        return this.db.getPost(this, id);
    }

    public List<Post> getAllPosts() {
        return this.db.getAllPosts(this);
    }

    public List<Post> queryPosts(long start, long end, List<String> ids) {
        return this.db.queryPosts(this, start, end, ids);
    }

    /* Router */
    public void restoreRouterFromSnapshot() {

    }

    public void saveRouterSnapshot() {
        SnapshotDatabase.saveRouterSnapshot(this, this.getRoutingTable());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BubblegumNode) {
            if(obj == this) return true;
            else return this.nodeIdentifier.equals(((BubblegumNode)obj).getNodeIdentifier());
        }
        return false;
    }



    /**  BubblegumNode.Builder **/
    public static class Builder {
        private String identifier, networkIdentifier;
        private SocialIdentity socialIdentity;
        private NodeID nodeIdentifier;
        private ActivityExecutionContext executionContext;
        private BubblegumCellServer server;
        private Logger logger;
        private int port = 0;

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setNetworkIdentifier(String networkIdentifier) {
            this.networkIdentifier = networkIdentifier;
            return this;
        }

        public Builder setSocialIdentity(SocialIdentity socialIdentity) {
            this.socialIdentity = socialIdentity;
            return this;
        }

        public Builder setNodeIdentifier(NodeID nodeIdentifier) {
            this.nodeIdentifier = nodeIdentifier;
            return this;
        }

        public Builder setExecutionContext(ActivityExecutionContext context) {
            this.executionContext = context;
            return this;
        }

        public Builder setServer(BubblegumCellServer server) {
            this.server = server;
            return this;
        }

        public Builder setLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public BubblegumNode build() {
            // Check required
            if(this.socialIdentity == null || this.executionContext == null || this.logger == null || this.server == null)
                return null;

            // Fill in optionals
            if(this.identifier == null) this.identifier = UUID.randomUUID().toString();
            if(this.networkIdentifier == null) this.networkIdentifier = UUID.randomUUID().toString();
            if(this.nodeIdentifier == null) this.nodeIdentifier = new NodeID();
            if(!NetworkingHelper.validPort(this.port)) this.port = 0;

            return new BubblegumNode(
                this.identifier, this.networkIdentifier, this.socialIdentity,
                this.executionContext, this.server, this.logger, this.nodeIdentifier, this.port
            );
        }
    }
}

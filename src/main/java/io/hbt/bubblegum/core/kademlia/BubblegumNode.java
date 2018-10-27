package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.BubblegumServer;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.logging.Logger;
import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.MasterDatabase;
import io.hbt.bubblegum.core.databasing.SnapshotDatabase;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.BootstrapActivity;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.activities.StoreActivity;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
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
    private BubblegumServer server;
    private ActivityExecutionContext executionContext;
    private Database db;
    private Logger logger;
    private ScheduledExecutorService internalScheduler;

    private BubblegumNode(
        String identifier,
        String networkIdentifier,
        SocialIdentity socialIdentity,
        ActivityExecutionContext context,
        BubblegumServer server,
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
        this.db = new Database(this);
        this.db.add(nid.toString(), new byte[] {0x01}); // responds only to self

        this.setupInternalScheduling();

        this.log("Constructed BubblegumNode: " + this.nodeIdentifier.toString());
    }

    private void setupInternalScheduling() {
        this.internalScheduler = Executors.newSingleThreadScheduledExecutor();

        // Setup Router snapshots
        this.internalScheduler.scheduleAtFixedRate(
            () -> {
                this.log("[Scheduled] Saving router snapshot");
                this.executionContext.addActivity(this.getNodeIdentifier().toString(), () -> this.saveRouterSnapshot());
            },
            3, 5, TimeUnit.MINUTES
        );

        // Setup bucket refreshes
        this.internalScheduler.scheduleAtFixedRate(
                () -> {
                    this.log("[Scheduled] Saving router snapshot");
                    this.executionContext.addActivity(this.getNodeIdentifier().toString(), () -> this.routingTable.refreshBuckets());
                },
                5, 5, TimeUnit.MINUTES
        );

        // Refresh/delete content as it expires

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

    public boolean bootstrap(InetAddress address, int port, String foreignNetwork) {

        RouterNode to = new RouterNode(new NodeID(), address, port);
        BootstrapActivity boostrapActivity = new BootstrapActivity(this, to, foreignNetwork, (networkID) -> this.networkIdentifier = networkID);
        boostrapActivity.run(); // sync

        if(boostrapActivity.getSuccess()) {
            this.saveRouterSnapshot();
            MasterDatabase.getInstance().updateNetwork(this);
            return true;
        }
        else {
            return false;
        }
    }

    public byte[] lookup(NodeID id) {
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

    public String getNetworkIdentifier() {
        return this.networkIdentifier;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public BubblegumServer getServer() {
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
        return this.db.hasKey(key);
    }

    public byte[] databaseRetrieveValue(String key) {
        return this.db.valueForKey(key);
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
        private BubblegumServer server;
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

        public Builder setServer(BubblegumServer server) {
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

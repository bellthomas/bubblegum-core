package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.BubblegumCellServer;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.ObjectResolver;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ObjectResolutionDetails;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.databasing.SnapshotDatabase;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.BootstrapActivity;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.activities.QueryActivity;
import io.hbt.bubblegum.core.kademlia.activities.ResourceRequestActivity;
import io.hbt.bubblegum.core.kademlia.activities.StoreActivity;
import io.hbt.bubblegum.core.kademlia.activities.SyncActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSealedPayload.KademliaSealedPayload;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.simulator.Simulator;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Representation of a virtual Kademlia node.
 */
public class BubblegumNode {
    private String identifier, networkIdentifier;
    private NodeID nodeIdentifier;
    private RoutingTable routingTable;
    private BubblegumCellServer server;
    private ActivityExecutionContext executionContext;
    private Database db;
    private KeyManager keyManager;
    private ObjectResolver objectResolver;
    private int numberOfPosts = 0;
    private boolean setupPostRefreshing = false;

    // region Initialisation
    private BubblegumNode(
        String identifier, String networkIdentifier,
        ActivityExecutionContext context, BubblegumCellServer server,
        NodeID nid, ObjectResolver objectResolver) {
        this.identifier = identifier;
        this.networkIdentifier = networkIdentifier;
        this.nodeIdentifier = nid;
        this.executionContext = context;
        this.server = server;
        this.server.registerNewLocalNode(this);
        this.routingTable = new RoutingTable(this);
        this.db = Database.getInstance();
        this.db.saveUserMeta(this, "username", this.nodeIdentifier.toString());
        this.keyManager = new KeyManager(this);
        this.objectResolver = objectResolver;
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
        if(!Simulator.isCurrentlySimulating()) {
            this.getExecutionContext().scheduleTask(this.getIdentifier(), () -> {
                this.routingTable.refreshBuckets();
            }, Configuration.random(Configuration.REFRESH_BUCKETS_TIMER), Configuration.REFRESH_BUCKETS_TIMER, TimeUnit.MILLISECONDS);
        }
    }
    //endregion

    //region Primitive Operations
    public Set<String> discover(InetAddress address, int port) {
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

    public boolean sync(RouterNode node) {
        if(Configuration.ENABLE_PGP) {
            SyncActivity syncActivity = new SyncActivity(this, node);
            syncActivity.run();
            if(syncActivity.getComplete() && !syncActivity.getSuccess()) {
                if(syncActivity.getFailedWebOfTrustVerification()) {
                    System.err.println("SYNC rejected: failed WoT verification.");
                }
            }
            return (syncActivity.getComplete() && syncActivity.getSuccess());
        }
        else return true;
    }

    public void requestSync(RouterNode node) {
        if(Configuration.ENABLE_PGP) {
            this.executionContext.addCallbackActivity("system", () -> this.sync(node));
        }
    }
    //endregion

    //region Compound Operations
    public boolean bootstrap(InetAddress address, int port, String foreignRecipient) {

        RouterNode to = new RouterNode(new NodeID(), address, port);
        BootstrapActivity boostrapActivity = new BootstrapActivity(this, to, foreignRecipient, (networkID) -> this.networkIdentifier = networkID);
        boostrapActivity.run(); // sync

        if(boostrapActivity.getSuccess()) {
            // this.saveRouterSnapshot();
            Database.getInstance().updateNodeInDatabase(this);
            // SnapshotDatabase.saveRouterSnapshot(this, this.getRoutingTable());
            return true;
        }
        else {
            return false;
        }
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

    public List<Post> queryMeta(NodeID id, List<String> keys) {
        List<String> metakeys = keys.stream().map((k) -> "_" + k + "_" + id.toString()).collect(Collectors.toList());
        return this.query(id, -1, -1, metakeys);
    }

    public ObjectResolutionDetails requestResource(String hash, String uri) {
        if(hash.equals(this.getNodeIdentifier().toString())) {
            // Local file, no need to request
            return this.objectResolver.getLocalResource(this, uri);
        }
        try {
            NodeID nid = new NodeID(hash);
            LookupActivity lookupActivity = new LookupActivity(this, nid, 1, false);
            lookupActivity.run();
            if(lookupActivity.getComplete() && lookupActivity.getSuccess()) {
                Set<RouterNode> nodes = lookupActivity.getClosestNodes();
                RouterNode to = null;
                for(RouterNode found : nodes) {
                    if(found.getNode().equals(nid)) to = found;
                }

                if(to != null) {
                    ResourceRequestActivity resourceRequestActivity = new ResourceRequestActivity(this, to, uri);
                    resourceRequestActivity.run();
                    if(resourceRequestActivity.getComplete() && resourceRequestActivity.getSuccess()) {
                        return resourceRequestActivity.getResolutionDetails();
                    }
                }
            }
        } catch (MalformedKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
    //endregion

    //region Getters
    public String getIdentifier() {
        return this.identifier;
    }

    public NodeID getNodeIdentifier() {
        return this.nodeIdentifier;
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

    public int getNumberOfPosts() {
        return this.numberOfPosts;
    }

    public void log(String message) {
//        this.logger.logMessage(message);
    }
    //endregion

    //region Database
    private void initialisePostRefreshing() {
        // Refresh/delete content as it expires
        if (!this.setupPostRefreshing) {
            this.getExecutionContext().scheduleTask(this.getIdentifier(), () -> {
                Database.refreshExpiringPosts(this, Configuration.POST_EXPIRY_REFRESH_CHECK);
            }, Configuration.random(Configuration.POST_EXPIRY_REFRESH_CHECK), Configuration.POST_EXPIRY_REFRESH_CHECK, TimeUnit.MILLISECONDS);
            this.setupPostRefreshing = true;
        }
    }

    public boolean databaseHasKey(String key) {
        return this.db.hasKey(this.identifier, key);
    }

    public List<byte[]> databaseRetrieveValue(String key) {
        return this.db.valueForKey(this.identifier, key);
    }

    public Post savePost(String content) {
        if(content != null && content.trim().length() > 0) {
            Post p = this.db.savePost(this, content);
            if(p != null) {
                if(this.numberOfPosts == 0) this.initialisePostRefreshing();
                this.numberOfPosts++;
                return p;
            }
        }
        return null;
    }

    public Post saveResponse(String content, String inReponseTo) {
        if(inReponseTo == null || inReponseTo.trim().length() == 0) return this.savePost(content);

        if(content != null && content.trim().length() > 0) {
            Post saved = this.db.savePost(this, content, inReponseTo);
            String key = "responses_" + inReponseTo;
            String globalIdentifier = Database.globalIdentifierForPost(this, saved);
            this.db.publishEntityMeta(this, key, globalIdentifier, null);
            return saved;
        }

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

    public void updateMeta(String key, String value) {
        this.db.saveUserMeta(this, key, value);
    }
    //endregion

    //region Router
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
    //endregion


    //region PGP
    public boolean ensurePGPKeyIsLocal(String keyID, String pgpID) {
       return this.keyManager.ensurePGPKeyIsLocal(keyID, pgpID);
    }

    public KademliaSealedPayload encryptPacket(RouterNode node, byte[] payload) {
        return this.keyManager.encryptPacket(node, payload);
    }

    public byte[] decryptPacket(RouterNode node, KademliaSealedPayload sealed) {
        return this.keyManager.decryptPacket(node, sealed);
    }

    public byte[] encryptPrivate(byte[] payload) {
        return this.keyManager.encryptWithPrivate(payload);
    }

    public byte[] encryptPublic(byte[] payload) {
        return this.keyManager.encryptWithPublic(this.keyManager.getPublicKey(null), payload);
    }

    public byte[] encryptForNode(RouterNode node, byte[] payload) {
        if(this.keyManager.getPublicKey(node.toPGPUID()) != null) {
            return this.keyManager.encryptWithPublic(this.keyManager.getPublicKey(node.toPGPUID()), payload);
        }
        return null;
    }

    public byte[] decryptPublic(byte[] payload) {
        return this.keyManager.decryptWithPrivate(payload);
    }

    public byte[] decryptForNode(RouterNode node, byte[] payload) {
        if(this.keyManager.getPublicKey(node.toPGPUID()) != null) {
            return this.keyManager.decryptWithPublic(this.keyManager.getPublicKey(node.toPGPUID()), payload);
        }
        return null;
    }

    public String getPGPKeyID() {
        return this.keyManager.getPGPKeyID();
    }

    public boolean syncIfRequired(RouterNode node) {
        if(Configuration.ENABLE_PGP) {
            if (this.keyManager.getPublicKey(node.toPGPUID()) == null) {
                return this.sync(node);
            }
        }
        return true;
    }

    public boolean haveKeyForPGPID(String pgpID) {
        return (this.keyManager.getPublicKey(pgpID) != null);
    }

    public void declareSybilImpersonator(String pgpID) {
        this.keyManager.declareSybilImpersonator(pgpID);
    }
    public boolean checkForSybilImpersonator(String pgpID) {
        return this.keyManager.checkForSybilImpersonator(pgpID);
    }
    //endregion


    //region Builder
    public static class Builder {
        private String identifier, networkIdentifier;
        private NodeID nodeIdentifier;
        private ActivityExecutionContext executionContext;
        private BubblegumCellServer server;
        private ObjectResolver objectResolver;
        private int port = 0;

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setNetworkIdentifier(String networkIdentifier) {
            this.networkIdentifier = networkIdentifier;
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

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setObjectResolver(ObjectResolver objectResolver) {
            this.objectResolver = objectResolver;
            return this;
        }

        public BubblegumNode build() {
            // Check required
            if(this.executionContext == null || this.server == null)
                return null;

            // Fill in optionals
            if(this.identifier == null) this.identifier = "local-" + UUID.randomUUID().toString();
            if(this.networkIdentifier == null) this.networkIdentifier = "network-" + UUID.randomUUID().toString();
            if(this.nodeIdentifier == null) this.nodeIdentifier = new NodeID();
            if(!NetworkingHelper.validPort(this.port)) this.port = 0;

            return new BubblegumNode (
                this.identifier, this.networkIdentifier, this.executionContext,
                this.server, this.nodeIdentifier, this.objectResolver
            );
        }
    }
    //endregion

    //region Misc
    public Pair<Socket, InputStream> getResourceClient(ObjectResolutionDetails details) {
        return this.objectResolver.client(details);
    }

    public BgKademliaMessage.KademliaMessage newResourceRequest(RouterNode to, String eid, String origin, String originLocal, String uri) {
        return this.objectResolver.newRequest(this, to, eid, origin, originLocal, uri);
    }

    public String toPGPUID() {
        return String.join(":", this.server.getLocal().getHostAddress(), this.server.getPort()+"", this.nodeIdentifier.toString());
    }

    @Override
    public String toString() {
        return nodeIdentifier.toString();
    }

    //endregion
}

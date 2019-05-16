package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.BubblegumCellServer;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.ObjectResolver;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ObjectResolutionDetails;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.Post;
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

    /**
     * Constructor.
     * Should be built via the BubblegumNode.Builder class.
     * @param identifier The local UUID identifier of the instance.
     * @param networkIdentifier The assigned network identifier.
     * @param context The Bubblegum instance's shared ActivityExecutionContext.
     * @param server The node's cell's server instance.
     * @param nid The NodeID assigned to this node.
     * @param objectResolver The bubblegum instance's ObjectResolver instance.
     */
    private BubblegumNode(
        String identifier, String networkIdentifier,
        ActivityExecutionContext context, BubblegumCellServer server,
        NodeID nid, ObjectResolver objectResolver) {
        System.out.println("Instantiating BubblegumNode...");
        this.identifier = identifier;
        this.nodeIdentifier = nid;
        this.networkIdentifier = networkIdentifier;
        this.executionContext = context;
        this.server = server;
        this.routingTable = new RoutingTable(this);
        this.db = Database.getInstance();
        this.db.saveUserMeta(this, "username", this.nodeIdentifier.toString());
        this.objectResolver = objectResolver;
        this.keyManager = new KeyManager(this, (publicKeyBytes) -> {
            if(Configuration.NODE_ID_FROM_PUBLIC_KEY) {
                this.nodeIdentifier = NodeID.hash(new String(publicKeyBytes));
            }
        });
        this.server.registerNewLocalNode(this);

        this.setupInternalScheduling();
        System.out.println("Constructed BubblegumNode: " + this.nodeIdentifier.toString());
    }

    /**
     * Initialise internal maintenance tasks.
     */
    private void setupInternalScheduling() {
        // Setup bucket refreshes
        if(!Simulator.isCurrentlySimulating()) {
            this.getExecutionContext().scheduleTask(this.getIdentifier(), () -> {
                this.routingTable.refreshBuckets();
            }, Configuration.random(Configuration.REFRESH_BUCKETS_TIMER), Configuration.REFRESH_BUCKETS_TIMER, TimeUnit.MILLISECONDS);
        }
    }
    //endregion

    //region Primitive Operations

    /**
     * Perform a lookup operation on a given identifier.
     * @param id The ID to lookup.
     * @return The byte[] results returned from the network. Null on failure.
     */
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

    /**
     * Perform a store operation for a key-value pair.
     * @param id The pair's key.
     * @param value The pair's value.
     * @return Whether the operation was successful.
     */
    public boolean store(NodeID id, byte[] value) {
        StoreActivity storeActivity = new StoreActivity(this, id.toString(), value);
        storeActivity.run();
        return (storeActivity.getComplete() && storeActivity.getSuccess());
    }

    /**
     * If enabled, perform a synchronisation with a node.
     * @param node The node to synchronise with.
     * @return Whether the operation was successful.
     */
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

    /**
     * Schedule sync() for asynchronous operation.
     * @param node The peer to queue synchronisation with.
     */
    public void requestSync(RouterNode node) {
        if(Configuration.ENABLE_PGP) {
            this.executionContext.addCallbackActivity("system", () -> this.sync(node));
        }
    }
    //endregion

    //region Compound Operations

    /**
     * Synchronously perform a bootstrap operation with a node.
     * @param address The IP address of the node.
     * @param port The port of the node.
     * @param foreignRecipient The network identifier based key of the node.
     * @return Whether the operation was successful.
     */
    public boolean bootstrap(InetAddress address, int port, String foreignRecipient) {
        RouterNode to = new RouterNode(new NodeID(), address, port);
        BootstrapActivity boostrapActivity = new BootstrapActivity(this, to, foreignRecipient, (networkID) -> this.networkIdentifier = networkID);
        boostrapActivity.run();

        if(boostrapActivity.getSuccess()) {
            Database.getInstance().updateNodeInDatabase(this);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Perform a query operation on a node.
     * @param id The peer's NodeID.
     * @param start The start time of the query. UNIX time (seconds). Unused = -1.
     * @param end The end time of the query. UNIX time (seconds). Unused = -1.
     * @param ids The list of entity IDs to be returned. Unused = null.
     * @return Returned Post object or null on failure.
     */
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

    /**
     * Retrieve nodes' meta keys.
     * @param id The peer's NodeID.
     * @param keys The meta keys to request.
     * @return The returned Post objects or null on failure.
     */
    public List<Post> queryMeta(NodeID id, List<String> keys) {
        List<String> metakeys = keys.stream().map((k) -> "_" + k + "_" + id.toString()).collect(Collectors.toList());
        return this.query(id, -1, -1, metakeys);
    }

    /**
     * Execute a RESOLVE RPC exchange for a given file.
     * @param hash The node identifier hash of the remote node.
     * @param uri The file's URI being requested.
     * @return Retrieval details or null on failure.
     */
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

    /**
     * Retrieve the node's internal identifier.
     * @return the node's identifier.
     */
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Retrieve the NodeID of the node.
     * @return The node's NodeID.
     */
    public NodeID getNodeIdentifier() {
        return this.nodeIdentifier;
    }

    /**
     * Retrieve the node's networkIdentifier.
     * @return The node's networkIdentifier.
     */
    public String getNetworkIdentifier() {
        return this.networkIdentifier;
    }

    /**
     * Retrieve the node's routing table instance.
     * @return The node's routing table.
     */
    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    /**
     * Retrieve the node's AEC instance.
     * @return The node's AEC instance.
     */
    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    /**
     * Retrieve the node's server instance.
     * @return The node's server instance.
     */
    public BubblegumCellServer getServer() {
        return this.server;
    }

    /**
     * Retrieve the node's database instance.
     * @return The node's database instance.
     */
    public Database getDatabase() {
        return this.db;
    }

    /**
     * Generate the node's recipient ID string.
     * @return The node's recipient ID.
     */
    public String getRecipientID() {
        return this.networkIdentifier + ":" + this.nodeIdentifier.toString();
    }

    /**
     * Get the number of locally published posts.
     * @return The number of posts.
     */
    public int getNumberOfPosts() {
        return this.numberOfPosts;
    }

    /**
     * Internal logging method.
     * @param message The message to log.
     */
    public void log(String message) {
        // this.logger.logMessage(message);
    }
    //endregion

    //region Database

    /**
     * Initialise the internal task for cleaning up invalid <key, value> pairs.
     */
    private void initialisePostRefreshing() {
        // Refresh/delete content as it expires
        if (!this.setupPostRefreshing) {
            this.getExecutionContext().scheduleTask(this.getIdentifier(), () -> {
                Database.refreshExpiringPosts(this, Configuration.POST_EXPIRY_REFRESH_CHECK);
            }, Configuration.random(Configuration.POST_EXPIRY_REFRESH_CHECK), Configuration.POST_EXPIRY_REFRESH_CHECK, TimeUnit.MILLISECONDS);
            this.setupPostRefreshing = true;
        }
    }

    /**
     * Check to see if the database has a key-value pair.
     * @param key The key to check for.
     * @return Whether the key is present.
     */
    public boolean databaseHasKey(String key) {
        return this.db.hasKey(this.identifier, key);
    }

    /**
     * Retrieve the values associated with a given key.
     * @param key The query key.
     * @return The found values.
     */
    public List<byte[]> databaseRetrieveValue(String key) {
        return this.db.valueForKey(this.identifier, key);
    }

    /**
     * Save a post in the local private database.
     * @param content The post's content.
     * @return The created Post object.
     */
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

    /**
     * Save a response to a post locally and setup maintaining additional index keys.
     * @param content The response's content.
     * @param inReponseTo The ID of the post being responded to.
     * @return The Post object of the response.
     */
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

    /**
     * Retrieve a local post.
     * @param id The post's identifier.
     * @return the Post object.
     */
    public Post getPost(String id) {
        return this.db.getPost(this, id);
    }


    /**
     * Get all local posts.
     * @return The list of all Post objects.
     */
    public List<Post> getAllPosts() {
        return this.db.getAllPosts(this);
    }

    /**
     * Perform a local query on posts in the private data store.
     * @param start The UNIX start time of the query.
     * @param end The UNIX end time of the query.
     * @param ids Specific IDs being requested.
     * @return Posts objects matching the query.
     */
    public List<Post> queryPosts(long start, long end, List<String> ids) {
        return this.db.queryPosts(this, start, end, ids);
    }

    /**
     * Update a local meta value.
     * @param key The meta value's key.
     * @param value The meta value.
     */
    public void updateMeta(String key, String value) {
        this.db.saveUserMeta(this, key, value);
    }
    //endregion

    //region Misc

    /**
     * Open a secure TCP socket using a resolved file's details.
     * @param details The file's details.
     * @return The secure TCP socket and corresponding InputStream.
     */
    public Pair<Socket, InputStream> getResourceClient(ObjectResolutionDetails details) {
        return this.objectResolver.client(details);
    }

    /**
     * Register a new request for a file.
     * @param to the request's origin peer.
     * @param eid The transmission's exchangeIdentifier.
     * @param origin The declared origin IP.
     * @param originLocal The declared proxy/local address.
     * @param uri The file's URI.
     * @return The file's URI.
     */
    public BgKademliaMessage.KademliaMessage newResourceRequest(RouterNode to, String eid, String origin, String originLocal, String uri) {
        return this.objectResolver.newRequest(this, to, eid, origin, originLocal, uri);
    }


    @Override
    public String toString() {
        return nodeIdentifier.toString();
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

    /**
     * Generate this node's PGP ID string.
     * @return The node's PGP ID.
     */
    public String toPGPUID() {
        return String.join(";", this.server.getLocal().getHostAddress(), this.server.getPort()+"", this.nodeIdentifier.toString());
    }

    /**
     * Retrieve and verify a PGP key from the CA.
     * @param keyID The key's ID.
     * @param pgpID The expected PGP UID.
     * @return Whether the operation was successful.
     */
    public boolean ensurePGPKeyIsLocal(String keyID, String pgpID) {
       return this.keyManager.ensurePGPKeyIsLocal(keyID, pgpID);
    }

    /**
     * Encrypt a binary payload for transmission.
     * @param node The node the packet is destined for.
     * @param payload The unencrypted payload.
     * @return The sealed payload instance.
     */
    public KademliaSealedPayload encryptPacket(RouterNode node, byte[] payload) {
        return this.keyManager.encryptPacket(node, payload);
    }

    /**
     * Decrypt a received packet.
     * @param node The sending node.
     * @param sealed The sealed payload instance.
     * @return The decrypted byte[] or null on failure.
     */
    public byte[] decryptPacket(RouterNode node, KademliaSealedPayload sealed) {
        return this.keyManager.decryptPacket(node, sealed);
    }

    /**
     * Encrypt a payload with the local private key.
     * @param payload The byte[] to encrypt.
     * @return The encrypted payload.
     */
    public byte[] encryptPrivate(byte[] payload) {
        return this.keyManager.encryptWithPrivate(payload);
    }

    /**
     * Encrypt a payload with the local public key.
     * @param payload The byte[] payload to encrypt.
     * @return The encrypted payload.
     */
    public byte[] encryptPublic(byte[] payload) {
        return this.keyManager.encryptWithPublic(this.keyManager.getPublicKey(null), payload);
    }

    /**
     * Encrypt a payload with a peer's public key.
     * @param node The peer who's key to use.
     * @param payload The byte[] payload to encrypt.
     * @return The encrypted payload.
     */
    public byte[] encryptForNode(RouterNode node, byte[] payload) {
        if(this.keyManager.getPublicKey(node.toPGPUID()) != null) {
            return this.keyManager.encryptWithPublic(this.keyManager.getPublicKey(node.toPGPUID()), payload);
        }
        return null;
    }

    /**
     * Decrypt a payload using the local private key.
     * @param payload The payload to decrypt.
     * @return The decrypted payload.
     */
    public byte[] decryptPublic(byte[] payload) {
        return this.keyManager.decryptWithPrivate(payload);
    }

    /**
     * Decrypt a payload using a peer's public key.
     * @param node The peer who's key will be used.
     * @param payload The byte[] payload to decrypt.
     * @return The decrypted payload.
     */
    public byte[] decryptForNode(RouterNode node, byte[] payload) {
        if(this.keyManager.getPublicKey(node.toPGPUID()) != null) {
            return this.keyManager.decryptWithPublic(this.keyManager.getPublicKey(node.toPGPUID()), payload);
        }
        return null;
    }

    /**
     * Get this node's generated PGP ID.
     * @return The node's PGP ID.
     */
    public String getPGPKeyID() {
        return this.keyManager.getPGPKeyID();
    }

    /**
     * If required perform a synchronous synchronisation operation.
     * @param node The node to synchronise with.
     * @return Whether the operation was successful.
     */
    public boolean syncIfRequired(RouterNode node) {
        if(Configuration.ENABLE_PGP) {
            if (this.keyManager.getPublicKey(node.toPGPUID()) == null) {
                return this.sync(node);
            }
        }
        return true;
    }

    /**
     * Check if we has the public key of a peer held locally.
     * @param pgpID The PGP ID of the peer.
     * @return Whether the key is in the local cache.
     */
    public boolean haveKeyForPGPID(String pgpID) {
        return (this.keyManager.getPublicKey(pgpID) != null);
    }

    /**
     * On failing Web-of-Trust validation, temporarily block a peer.
     * @param pgpID The peer's PGP ID.
     */
    public void declareSybilImpersonator(String pgpID) {
        this.keyManager.declareSybilImpersonator(pgpID);
    }

    /**
     * Validate a PGP ID, checking for impersonation.
     * @param pgpID The PGP ID to check.
     * @return Whether the PGP ID passed the check or not.
     */
    public boolean checkForSybilImpersonator(String pgpID) {
        return this.keyManager.checkForSybilImpersonator(pgpID);
    }
    //endregion


    //region Builder

    /**
     * Builder pattern constructor for BubblegumNode.
     */
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

        /**
         * Having set all necessary fields, build the corresponding BubblegumNode.
         * @return
         */
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

    } // end BubblegumNode.Builder class

    //endregion

} // end BubblegumNode class

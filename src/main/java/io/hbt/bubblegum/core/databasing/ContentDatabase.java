package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * In-memory implementation of bubblegum-core's private data store.
 */
class ContentDatabase implements ContentDatabaseInterface {
    private static ContentDatabase instance;
    private List<Post> posts = new ArrayList<>();
    private HashMap<String, Post> idIndexed = new HashMap<>();

    /**
     * Constructor.
     */
    private ContentDatabase() { /* Singleton */ }

    /**
     * Singleton getInstance().
     * @return The singleton instance.
     */
    protected synchronized static ContentDatabase getInstance() {
        if(ContentDatabase.instance == null) ContentDatabase.instance = new ContentDatabase();
        return ContentDatabase.instance;
    }

    /**
     * Save an entity.
     * @param id Unique identifier.
     * @param node Owning node.
     * @param content Entity content.
     * @return The created Post object.
     */
    private Post saveEntity(String id, BubblegumNode node, String content) {
        return this.saveEntity(id, node, content, "");
    }

    /**
     * Save an entity.
     * @param id Unique identifier.
     * @param node Owning node.
     * @param content Entity content.
     * @param response The ID of the post being responded to .
     * @return The created Post object.
     */
    private Post saveEntity(String id, BubblegumNode node, String content, String response) {
        if(content == null || node == null) return null;
        Post p = new Post(
            id,
            node.getNodeIdentifier().toString(),
            node.getNetworkIdentifier(),
            content,
            response,
            System.currentTimeMillis()
        );

        this.posts.add(p);
        this.idIndexed.put(id, p);
        return p;
    }

    /**
     * Save a post.
     * @param node Owning node.
     * @param content The post's content.
     * @param inResponseTo the post being responded to.
     * @return The created Post object.
     */
    public Post savePost(BubblegumNode node, String content, String inResponseTo) {
        return this.saveEntity("post-"+UUID.randomUUID().toString(), node, content, inResponseTo);
    }

    /**
     * Save a meta value.
     * @param key The meta key.
     * @param node The owning node.
     * @param content The meta value.
     * @return The created Post instance.
     */
    public Post saveMeta(String key, BubblegumNode node, String content) {
        return this.saveEntity("_"+ key + "_" + node.getNodeIdentifier().toString(), node, content);
    }

    /**
     * Fetch a local post.
     * @param node The owning node.
     * @param id The post's identifier.
     * @return The Post instance or null if not found.
     */
    public Post getPost(BubblegumNode node, String id) {
        if(id == null || node == null) return null;

        if(this.idIndexed.containsKey(id)) return this.idIndexed.get(id);
        else return null;
    }

    /**
     * Get all posts created by a node.
     * @param node The owning node.
     * @return All the node's posts.
     */
    public List<Post> getPosts(BubblegumNode node) {
        if(node == null) return new ArrayList<>();
        return this.posts.stream().filter((p) -> p.getOwner().equals(node.getIdentifier())).collect(Collectors.toList());
    }

    /**
     * Query posts.
     * @param node The owning node.
     * @param start Query: start UNIX time.
     * @param end Query: end UNIX time.
     * @param ids Query: set of post identifier.
     * @return The found list of Posts.
     */
    public List<Post> queryPosts(BubblegumNode node, long start, long end, List<String> ids) {
        if(ids != null && ids.size() > 0) return queryPostsByIDs(node, ids);
        return queryPostsByTime(node, start, end);
    }

    /**
     * Query posts by time.
     * @param node The owning node.
     * @param start Query: start UNIX time.
     * @param end Query: end UNIX time.
     * @return The found list of Posts.
     */
    public List<Post> queryPostsByTime(BubblegumNode node, long start, long end) {
        if(start < 0 || end < 0 || end < start) return new ArrayList<>();
        return this.posts.stream().filter((p) -> p.getTimeCreated() >= start && p.getTimeCreated() < end).collect(Collectors.toList());
    }

    /**
     * Query posts by IDs.
     * @param node The owning node.
     * @param ids Query: set of post identifier.
     * @return The found list of Posts.
     */
    public List<Post> queryPostsByIDs(BubblegumNode node, List<String> ids) {
        if(ids == null || ids.size() == 0) return new ArrayList<>();
        ArrayList results = new ArrayList();
        for(String id : ids) {
            if(this.idIndexed.containsKey(id)) results.add(this.idIndexed.get(id));
        }

        return results;
    }

} // end ContentDatabase class

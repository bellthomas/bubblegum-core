package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class ContentDatabase implements ContentDatabaseInterface {
    private static ContentDatabase instance;
    private List<Post> posts = new ArrayList<>();
    private HashMap<String, Post> idIndexed = new HashMap<>();

    private ContentDatabase() { /* Singleton */ }

    protected synchronized static ContentDatabase getInstance() {
        if(ContentDatabase.instance == null) ContentDatabase.instance = new ContentDatabase();
        return ContentDatabase.instance;
    }

    private Post saveEntity(String id, BubblegumNode node, String content) {
        return this.saveEntity(id, node, content, "");
    }

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

    public Post savePost(BubblegumNode node, String content, String inResponseTo) {
        return this.saveEntity("post-"+UUID.randomUUID().toString(), node, content, inResponseTo);
    }

    public Post saveMeta(String key, BubblegumNode node, String content) {
        return this.saveEntity("_"+ key + "_" + node.getNodeIdentifier().toString(), node, content);
    }

    public Post getPost(BubblegumNode node, String id) {
        if(id == null || node == null) return null;

        if(this.idIndexed.containsKey(id)) return this.idIndexed.get(id);
        else return null;
    }

    public List<Post> getPosts(BubblegumNode node) {
        if(node == null) return new ArrayList<>();
        return this.posts.stream().filter((p) -> p.getOwner().equals(node.getIdentifier())).collect(Collectors.toList());
    }

    public List<Post> queryPosts(BubblegumNode node, long start, long end, List<String> ids) {
        if(ids != null && ids.size() > 0) return queryPostsByIDs(node, ids);
        return queryPostsByTime(node, start, end);
    }

    public List<Post> queryPostsByTime(BubblegumNode node, long start, long end) {
        if(start < 0 || end < 0 || end < start) return new ArrayList<>();
        return this.posts.stream().filter((p) -> p.getTimeCreated() >= start && p.getTimeCreated() < end).collect(Collectors.toList());
    }

    public List<Post> queryPostsByIDs(BubblegumNode node, List<String> ids) {
        if(ids == null || ids.size() == 0) return new ArrayList<>();
        ArrayList results = new ArrayList();
        for(String id : ids) {
            if(this.idIndexed.containsKey(id)) results.add(this.idIndexed.get(id));
        }

        return results;
    }

}

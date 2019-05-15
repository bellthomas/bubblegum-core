package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.List;

/**
 * General interface for the private data store used by Bubblegum.
 */
public interface ContentDatabaseInterface {
    Post savePost(BubblegumNode node, String content, String inResponseTo);
    Post saveMeta(String key, BubblegumNode node, String content);
    Post getPost(BubblegumNode node, String id);
    List<Post> getPosts(BubblegumNode node);
    List<Post> queryPosts(BubblegumNode node, long start, long end, List<String> ids);
    List<Post> queryPostsByTime(BubblegumNode node, long start, long end);
    List<Post> queryPostsByIDs(BubblegumNode node, List<String> ids);
}
package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AsyncNetworkQuery {

    public enum Method { TIME_OPTIMISED, CALL_OPTIMISED }

    private BubblegumNode node;
    private List<String> ids;
    private List<Runnable> callbacks;
    private boolean completed = false;

    private final List<Post> results = new ArrayList<>();

    private final static Map<String, HashMap<String, String>> meta = new HashMap<>();
    private final static Map<String, Post> cached = new HashMap<>();

    public AsyncNetworkQuery(BubblegumNode node) {
        this.node = node;
        this.ids = new ArrayList<>();
        this.callbacks = new ArrayList<>();
    }

    public AsyncNetworkQuery(BubblegumNode node, List<String> ids) {
        this.node = node;
        this.ids = ids;
        this.callbacks = new ArrayList<>();
    }

    public AsyncNetworkQuery(BubblegumNode node, List<String> ids, Runnable r) {
        this.node = node;
        this.ids = ids;
        this.callbacks = new ArrayList<>() {{ add(r); }};
    }

    public boolean getCompleted() {
        return this.completed;
    }

    public void addID(String id) {
        this.ids.add(id);
    }

    public void addID(long id) {
        this.ids.add(id + "");
    }

    public void onChange(Runnable r) {
        this.callbacks.add(r);
    }

    public int getNumberOfResults() {
        return this.results.size();
    }

    public void run() {
        final List<String> ids = this.ids;
        this.node.getExecutionContext().addActivity(node.getIdentifier(), () -> {

            List<byte[]> values;
            TreeSet<String> globalIdentifiers = new TreeSet<>();
            for(String id : ids) {
                values = this.node.lookup(NodeID.hash(id));
                if(values != null) {
                    globalIdentifiers.addAll(
                        values
                            .stream()
                            .map((b) -> new String(b))
                            .filter((s) -> !AsyncNetworkQuery.cached.containsKey(s))
                            .collect(Collectors.toList())
                    );
                }
            }

            List<String> foreignUsersPosts = new ArrayList<>();
            if(globalIdentifiers.size() > 0) {
                String first;
                String[] gidParts, temp;
                while(globalIdentifiers.size() > 0) {
                    foreignUsersPosts.clear();
                    first = globalIdentifiers.pollFirst();
                    gidParts = first.split(":");
                    if(gidParts.length == 2) {
                        String owner = gidParts[0];
                        foreignUsersPosts.add(gidParts[1]);

                        while(globalIdentifiers.size() > 0 && foreignUsersPosts.size() < 50 && globalIdentifiers.first().startsWith(owner)) {
                            temp = globalIdentifiers.pollFirst().split(":");
                            if(temp.length == 2) foreignUsersPosts.add(temp[1]);
                        }

                        //
                        try {
                            NodeID nid = new NodeID(owner);

                            if(!AsyncNetworkQuery.meta.containsKey(nid.toString())) {
                                this.node.getExecutionContext().addCompoundActivity(node.getIdentifier(), () -> {
                                    this.retrieveMetadata(this.node, nid);
                                    this.changeOccurred();
                                });
                            }


                            if(foreignUsersPosts.size() > 0) {
                                this.node.getExecutionContext().addCompoundActivity(node.getIdentifier(), () -> {
                                    List<Post> posts = this.node.query(nid, -1, -1, foreignUsersPosts);
                                    if (posts != null) {
                                        for (Post p : posts) {
                                            AsyncNetworkQuery.cached.put(p.getOwner() + ":" + p.getID(), p);
                                            this.results.add(p);
                                        }
                                        this.changeOccurred();
                                    }
                                });
                            }

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void retrieveMetadata(BubblegumNode node, NodeID nid) {
        if(!AsyncNetworkQuery.meta.containsKey(nid.toString())) {
            AsyncNetworkQuery.meta.put(nid.toString(), new HashMap<>());

            List<String> metaKeys = new ArrayList<>();
            metaKeys.add("_username");

            List<Post> payload = node.query(nid, -1, -1, metaKeys);
            if(payload != null) {
                for(Post p : payload) {
                    if(p != null) {
                        AsyncNetworkQuery.meta.get(nid.toString()).put(p.getID(), p.getContent());
                    }
                }
            }
        }
    }

    private void changeOccurred() {
        for(Runnable r : this.callbacks) r.run();
    }
}

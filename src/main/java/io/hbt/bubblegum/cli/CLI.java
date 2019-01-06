package io.hbt.bubblegum.cli;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CLI {

    private Bubblegum bb = new Bubblegum(false);
    private int selectedNetwork = -1; // changes to indicate selected node
    private int currentIndex = 0; // count nodes
    private HashMap<Integer, String> networkIndicies;
    private Terminal terminal;
    private String prompt = "\n> ";
    private static int helpFirstCol = 18;


    public CLI() {

        System.out.println("Starting Bubblegum...");

        networkIndicies = new HashMap<>();
        for(String network : bb.getNodeIdentifiers()) {
            networkIndicies.put(this.currentIndex, network);
            this.currentIndex++;
        }

        try {
            this.terminal = TerminalBuilder.terminal();

            LineReader reader = LineReaderBuilder.builder().build();

            BubblegumNode current;
            boolean running = true;
            while (running) {
                String line = null;
                try {
                    line = reader.readLine(this.prompt);
                    if(line != null && line.length() > 0) {
                        String[] commandParts = line.split("\\s+");
                        if(commandParts.length == 0) continue;
                        switch (commandParts[0].trim().toLowerCase()) {
                            case "q":
                                running = false;
                                break;
                            case "help":
                                this.printHelp("q", "Quit the program");
                                this.printHelp("select", "Select a node by its index");
                                this.printHelp("current", "Print the selected node");
                                this.printHelp("list", "List all nodes");
                                this.printHelp("count", "Count the number of current nodes");
                                this.printHelp("create", "Create a new node");
                                this.printHelp("bootstrap", "Bootstrap selected node to anoter node");
                                this.printHelp("bootstrapi", "Bootstrap selected node to an internally hosted node");
                                this.printHelp("store", "Store a key value pair to the current node's network");
                                this.printHelp("lookup", "Retrieve a value for a key from the current node's network");
                                this.printHelp("construct", "Synchronously build a network locally");
                                this.printHelp("constructparallel", "In parallel, build a network locally");
                                this.printHelp("discover", "");
                                this.printHelp("discoveri", "");
                                this.printHelp("bucketsize", "");
                                this.printHelp("refreshbuckets", "");
                                this.printHelp("teststore", "");
                                this.printHelp("savepost", "");
                                this.printHelp("getpost", "");
                                this.printHelp("getallposts", "");
                                this.printHelp("discoveri", "");
                                this.printHelp("refresh", "");
                                this.printHelp("reset", "Delete and start again");
                                break;
                            case "current":
                                this.current();
                                break;
                            case "list":
                                this.list();
                                break;
                            case "count":
                                this.print(this.networkIndicies.size() + "");
                                break;
                            case "refresh":
                                this.refresh();
                                break;
                            case "reset":
                                this.reset();
                                break;
                            case "select":
                                this.select(commandParts);
                                break;
                            case "create":
                                this.create();
                                break;
                            case "bootstrapi":
                                current = this.getCurrentNode();
                                if(current != null) this.bootstrapInternal(current, commandParts);
                                break;
                            case "bootstrap":
                                current = this.getCurrentNode();
                                if(current != null) this.bootstrapExternal(current, commandParts);
                                break;
                            case "store":
                                current = this.getCurrentNode();
                                if(current != null) this.store(current, commandParts);
                                break;
                            case "lookup":
                                current = this.getCurrentNode();
                                if(current != null) this.lookup(current, commandParts);
                                break;
                            case "construct":
                                this.construct(commandParts);
                                break;
                            case "constructparallel":
                                this.constructParallel(commandParts);
                                break;
                            case "discoveri":
                                current = this.getCurrentNode();
                                if(current != null) this.discoverInternal(current, commandParts);
                                break;
                            case "discover":
                                current = this.getCurrentNode();
                                if(current != null) this.discoverExternal(current, commandParts);
                                break;
                            case "bucketsize":
                                this.averageSizeOfBucket();
                                break;
                            case "refreshbuckets":
                                current = this.getCurrentNode();
                                if(current != null) this.refreshBuckets(current);
                                break;
                            case "teststore":
                                this.testKeyOnAllNodes(commandParts);
                                break;
                            case "savepost":
                                current = this.getCurrentNode();
                                if(current != null) this.savePost(current, commandParts);
                                break;
                            case "getpost":
                                current = this.getCurrentNode();
                                if(current != null) this.getPost(current, commandParts);
                                break;
                            case "getallposts":
                                current = this.getCurrentNode();
                                if(current != null) this.getAllPosts(current);
                                break;
                            case "queryposts":
                                current = this.getCurrentNode();
                                if(current != null) this.queryPosts(current, commandParts);
                                break;
                            case "query":
                                current = this.getCurrentNode();
                                if(current != null) this.query(current, commandParts);
                                break;
                            case "queryid":
                                current = this.getCurrentNode();
                                if(current != null) this.queryid(current, commandParts);
                                break;
                            case "oneday":
                                current = this.getCurrentNode();
                                if(current != null) this.oneday(current, commandParts);
                                break;
                            case "reply":
                                current = this.getCurrentNode();
                                if(current != null) this.reply(current, commandParts);
                                break;
                            default:
                                this.print("Invalid command");
                                break;
                        }
                    }

                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void printHelp(String commandName, String desc) {
        String first = String.format("%1$-" + this.helpFirstCol + "s", commandName);
        this.print(first + " " + desc);
    }

    private void current() {
        BubblegumNode c = this.getCurrentNode();
        if(c != null) {
            this.print(this.selectedNetwork + " -> [" + c.getRecipientID() + "] {"+ c.getServer().getLocal().getHostAddress() + ":" + c.getServer().getPort() +"} (" + c.getIdentifier() + ")");
        }
    }

    private void select(String[] command) {
        if(command.length >= 2) {
            Integer i = this.toInt(command[1]);
            if(i != null) {
                if(networkIndicies.containsKey(i)) {
                    this.selectedNetwork = i;
                    this.current();
                }
                else {
                    this.print("Network " + i + " doesn't exist");
                }
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void list() {
        BubblegumNode current;
        if(this.networkIndicies.size() == 0) this.print("No nodes present");
        for(Map.Entry<Integer, String> entry : this.networkIndicies.entrySet()) {
            current = this.getNode(entry.getValue());
            if(current != null) {
                this.print(entry.getKey() + " -> [" + current.getRecipientID() + "] (" + current.getIdentifier() + ")");
            }
        }
    }

    private void reset() {
        this.bb.reset();
        this.currentIndex = 0;
        this.print("Bubblegum instance reset");
        this.refresh();
    }

    private void refresh() {
        networkIndicies = new HashMap<>();
        for(String network : bb.getNodeIdentifiers()) {
            networkIndicies.put(this.currentIndex, network);
            this.currentIndex++;
        }
        this.selectedNetwork = -1;
        this.print("Bubblegum CLI instance refreshed");
    }

    private void create() {
        BubblegumNode n = this.bb.createNode();
        this.print("Created " + n.getIdentifier() + ", NID: " + n.getNetworkIdentifier());
        this.networkIndicies.put(this.currentIndex++, n.getIdentifier());
    }

    private void bootstrapInternal(BubblegumNode current, String[] command) {
        if(command.length >= 3) {
            Integer i = this.toInt(command[1]);
            if(i != null) {
                if(this.networkIndicies.containsKey(i)) {
                    final BubblegumNode bootstrapTo = this.getNode(this.networkIndicies.get(i));
                    current.getExecutionContext().addActivity(current.getIdentifier(), () -> {
                        if (current.bootstrap(bootstrapTo.getServer().getLocal(), bootstrapTo.getServer().getPort(), command[2])) {
                            this.print("Bootstrap successful");
                        } else {
                            this.print("Bootstrap failed");
                        }
                    });
                }
                else {
                    this.print("Invalid network index");
                }
            } else {
                this.print("Invalid arguments");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void bootstrapExternal(BubblegumNode current, String[] command) {
        if(command.length >= 4) {
            try {
                InetAddress addr = InetAddress.getByName(command[1]);
                Integer i = this.toInt(command[2]);
                if (i != null) {
                    if(current.bootstrap(addr, i, command[3])) {
                        this.print("Bootstrap successful");
                    }
                    else {
                        this.print("Bootstrap failed");
                    }
                }
                else {
                    this.print("Invalid port");
                }
            } catch (UnknownHostException e) {
                this.print("Invalid InetAddress");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void store(BubblegumNode current, String[] command) {
        if(command.length >= 3) {
            String key = command[1];
            String value = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
            if(current.store(NodeID.hash(key), value.getBytes())) {
                this.print("Stored '"+ key +"' -> '"+ value +"'");
            }
            else {
                this.print("Store failed");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void lookup(BubblegumNode current, String[] command) {
        if(command.length >= 2) {
            String key = command[1];
            List<byte[]> results = current.lookup(NodeID.hash(key));
            if(results != null && results.size() > 0) {
                results.forEach((b) -> this.print(" - " + new String(b)));
            }
            else {
                this.print("No value found");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void discoverExternal(BubblegumNode current, String[] command) {
        if(command.length >= 3) {
            try {
                InetAddress addr = InetAddress.getByName(command[1]);
                Integer i = this.toInt(command[2]);
                if (i != null) {
                    Set<String> networks = current.discover(addr, i);
                    if (networks != null && networks.size() > 0) {
                        this.print("Found: ");
                        for (String network : networks) this.print(network);
                    } else {
                        this.print("No networks found");
                    }
                }
            } catch (UnknownHostException e) {
                this.print("Invalid InetAddress");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void discoverInternal(BubblegumNode current, String[] command) {
        if(command.length >= 2) {
            Integer i = this.toInt(command[1]);
            if(i != null) {
                if(this.networkIndicies.containsKey(i)) {
                    BubblegumNode toDiscover = this.getNode(this.networkIndicies.get(i));
                    Set<String> networks = current.discover(toDiscover.getServer().getLocal(), toDiscover.getServer().getPort());
                    if(networks != null && networks.size() > 0) {
                        this.print("Found: ");
                        for(String network : networks) this.print(network);
                    }
                    else {
                        this.print("No networks found");
                    }
                }
                else {
                    this.print("Invalid network index");
                }
            } else {
                this.print("Invalid arguments");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    /*
    int randomNum = ThreadLocalRandom.current().nextInt(0, currentIndex);
    BubblegumNode randomNode = this.bb.getNode(this.networkIndicies.get(randomNum));
    node.bootstrap(
        randomNode.getServer().getLocal(),
        randomNode.getServer().getPort(),
        randomNode.getRecipientID()
    );
    this.networkIndicies.put(this.currentIndex++, node.getIdentifier());
     */

    private void constructParallel(String[] command) {
        if(command.length >= 3) {
            Integer i = this.toInt(command[1]);
            Integer p = this.toInt(command[2]);
            Thread[] ts = new Thread[p];
            if(i != null) {
                long start = System.currentTimeMillis();
                for(int x = 0; x < p; x++) {
                    if(x == p-1) ts[x] = new Thread(() -> construct(new String[]{"", "" + (i - (i / p)*(p-1)), "f"}));
                    else ts[x] = new Thread(() -> construct(new String[]{"", "" + i / p, "f"}));
                }

                for(Thread t : ts) t.start();

                for(Thread t : ts) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Time taken: " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private void construct(String[] command) {
        String time = "t";
        if(command.length >= 3) time = command[2];

        if(command.length >= 2) {
            Integer i = this.toInt(command[1]);
            if(i != null) {
                List<BubblegumNode> nodes = this.bb.buildNodes(i);
                int x = 0;
                long start = System.currentTimeMillis();

                for(BubblegumNode node : nodes) {
                    if(this.currentIndex == 0)  {
                        this.addNode(node);
                    }
                    else {
                        int randomNum = Math.max(currentIndex - 10, 0);
                        if(randomNum > 0) randomNum = ThreadLocalRandom.current().nextInt(0, randomNum);
                        BubblegumNode randomNode = this.bb.getNode(this.networkIndicies.get(randomNum));

                        node.bootstrap(
                            randomNode.getServer().getLocal(),
                            randomNode.getServer().getPort(),
                            randomNode.getRecipientID()
                        );

                        this.addNode(node);
                    }
                }

                if(time.equals("t")) System.out.println("Time taken: " + (System.currentTimeMillis() - start) + "ms");

            } else {
                this.print("Invalid arguments");
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private synchronized void addNode(BubblegumNode node) {
        if(node != null) {
            this.print("Node " + this.currentIndex);
            this.networkIndicies.put(this.currentIndex++, node.getIdentifier());
        }
    }



    private void averageSizeOfBucket() {
        int total = 0;
        int number = 0;
        int max = 0;
        int min = Integer.MAX_VALUE;
        ArrayList<Integer> vals = new ArrayList<>();
        for(Map.Entry<Integer, String> entry : this.networkIndicies.entrySet()) {
            BubblegumNode node = this.bb.getNode(entry.getValue());
            int size = node.getRoutingTable().getSize();
            vals.add(size);
            max = Integer.max(max, size);
            min = Integer.min(min, size);
            total += size;
            number++;
        }

        this.print("Average Bucket Size = " + (double)total / number);
        this.print("Minimum Bucket Size = " + min);
        this.print("Maximum Bucket Size = " + max);

        StringBuilder sb = new StringBuilder();
        Collections.sort(vals);
        for(Integer i : vals) sb.append(i + ", ");
        this.print(sb.toString());

        for(int x = 0; x < currentIndex; x++) {
            String id = this.networkIndicies.get(x);
            BubblegumNode node = this.bb.getNode(id);
            this.print(x + " -> " + node.getRoutingTable().getSize() + " nodes");
        }

    }

    private void testKeyOnAllNodes(String[] command) {
        if(command.length >= 3) {
            NodeID key = NodeID.hash(command[1]);
            String value = String.join(" ", Arrays.copyOfRange(command, 2, command.length));

            for(Map.Entry<Integer, String> entry : this.networkIndicies.entrySet()) {
                List<byte[]> result = this.bb.getNode(entry.getValue()).lookup(key);
                if(result != null)  {
                    List<String> rString = result.stream().map((b) -> new String(b)).collect(Collectors.toList());
                    if(!rString.contains(value)) this.print("Fail: ["+entry.getKey()+"] did not return the specified value");
                    else this.print("["+entry.getKey()+"] Success");
                }
                else {
                    this.print("Fail: ["+entry.getKey()+"] gave no result");
                }
            }
        }
        else {
            this.print("Invalid number of arguments");
        }
    }

    private void savePost(BubblegumNode node, String[] command) {
        if(command.length >= 2) {
            String content = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
            System.out.println(node.savePost(content));
        }
    }

    private void getPost(BubblegumNode node, String[] command) {
        if(command.length >= 2) {
            String id = command[1];
            Post p = node.getPost(id);
            if(p == null) System.out.println("No post found");
            else System.out.println(p);
        }
    }

    private void getAllPosts(BubblegumNode node) {
        List<Post> posts = node.getAllPosts();
        if(posts.size() == 0) System.out.println("No posts found");
        else {
            for(Post post : posts) System.out.println(post);
        }
    }

    private void queryPosts(BubblegumNode node, String[] command) {
        if(command.length >= 3) {
            Long start = toLong(command[1]);
            Long end = toLong(command[2]);
            if(start != null && end != null) {
                List<Post> posts = node.queryPosts(start, end, new ArrayList<>());
                if (posts.size() == 0) this.print("No posts found");
                else for (Post p : posts) this.print(p.toString());
            }
        }
    }

    private void query(BubblegumNode node, String[] command) {
        if(command.length >= 4) {
            try {
                NodeID id = new NodeID(command[1]);
                Long start = toLong(command[2]);
                Long end = toLong(command[3]);
                if(start != null && end != null) {
                    List<Post> posts = node.query(id, start, end, new ArrayList<>());
                    if(posts == null) this.print("Failed to query remote posts");
                    else if(posts.size() == 0) this.print("No remote posts found");
                    else for (Post p : posts) this.print(p.toString());
                }

            } catch (MalformedKeyException e) {
                this.print("Failed to parse node ID");
            }
        }
    }

    private void queryid(BubblegumNode node, String[] command) {
        if(command.length >= 2) {
            String[] idParts = command[1].split(":");
            if(idParts.length == 2) {
                try {
                    NodeID id = new NodeID(idParts[0]);
                    List<Post> posts = node.query(id, -1, -1, new ArrayList<>() {{ add(idParts[1]); }});
                    if (posts == null) this.print("Failed to query remote posts");
                    else if (posts.size() == 0) this.print("No remote posts found");
                    else for (Post p : posts) this.print(p.toString());
                }
                catch (MalformedKeyException e) {
                    this.print("Failed to parse node ID");
                }
            }
            else {
                this.print("Invalid post ID");
            }
        }
    }

    private void oneday(BubblegumNode node, String[] command) {
        long lowEpochBin = (System.currentTimeMillis() - (24*60*60*1000)) / Configuration.BIN_EPOCH_DURATION;
        long highEpochBin = System.currentTimeMillis() / Configuration.BIN_EPOCH_DURATION;
        System.out.println(lowEpochBin);
        System.out.println(highEpochBin);

//        ArrayList<String> ids = new ArrayList<>();
        for(long i = highEpochBin; i >= lowEpochBin; i--) {
//            ids.add(Long.toString(i));
            List<byte[]> idBytes = node.lookup(NodeID.hash(i));
            List<String> ids = idBytes.stream().map((b) -> new String(b)).collect(Collectors.toList());
            for(String id : ids) {
                String[] idParts = id.split(":");
                if(idParts.length == 2) {
                    try {
                        NodeID nid = new NodeID(idParts[0]);
                        List<Post> posts = node.query(nid, -1, -1, new ArrayList<>() {{ add(idParts[1]); }});
                        if (posts == null) this.print("Failed to query remote posts");
                        else if (posts.size() == 0) this.print("No remote posts found");
                        else for (Post p : posts) this.print(p.toString() + "\n-----------");
                    } catch (MalformedKeyException e) {
                        this.print("Failed to parse node ID");
                    }
                }
                else {
                    System.out.println("Failed to retrieve: " + id);
                }
            }
        }

//        new AsyncNetworkQuery(node, ids, () -> System.out.println("Change")).run();
    }

    private void reply(BubblegumNode node, String[] command) {
        if(command.length >= 3) {
            String inResponseTo = command[1];
            String content = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
            System.out.println(node.saveResponse(content, inResponseTo));
        }
    }


    private void refreshBuckets(BubblegumNode node) {
        node.getRoutingTable().refreshBuckets();
    }

    private BubblegumNode getNode(String identifier) {
        return this.bb.getNode(identifier);
    }

    private BubblegumNode getCurrentNode() {
        if(this.currentIndex >= 0 && this.selectedNetwork >= 0) {
            return this.getNode(this.networkIndicies.get(this.selectedNetwork));
        }
        else {
            this.print("No node selected");
            return null;
        }
    }

    private void print(String message) {
        this.terminal.writer().println(message);
        this.terminal.writer().flush();
    }

    private Integer toInt(String i) {
        try {
            return Integer.valueOf(i);
        }
        catch (NumberFormatException nfe) {
            this.print("Integer conversion failed");
            return null;
        }
    }

    private Long toLong(String i) {
        try {
            return Long.valueOf(i);
        }
        catch (NumberFormatException e) {
            this.print("Long conversion failed");
            return null;
        }
    }


    public static void main(String[] args) {
        new CLI();
    }
}

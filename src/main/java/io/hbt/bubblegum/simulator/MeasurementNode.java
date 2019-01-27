package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.activities.PrimitiveStoreActivity;
import io.hbt.bubblegum.core.kademlia.activities.QueryActivity;
import io.hbt.bubblegum.core.kademlia.activities.StoreActivity;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class MeasurementNode {

    private Bubblegum bubblegum;
    private BubblegumNode node;
    private BubblegumNode[] helperNodes;
    private int iterations = 5;

    public MeasurementNode(InetAddress addr, int port, String recipient) {
        this.bubblegum = new Bubblegum(false);
        this.node = this.bubblegum.createNode();
        if(!this.node.bootstrap(addr, port, recipient) || this.node.getRoutingTable().getSize() < 2) {
            System.out.println("Bootstrap failed");
            return;
        }
        System.out.println("Built primary node");

        this.helperNodes = new BubblegumNode[this.iterations];
        for(int i = 0; i < this.iterations; i++) {
            this.helperNodes[i] = this.bubblegum.createNode();
            if(!this.helperNodes[i].bootstrap(addr, port, recipient) || this.helperNodes[i].getRoutingTable().getSize() < 2) {
                System.out.println("Helper node bootstrap failed");
                return;
            }
            System.out.println("Built helper node " + i);
        }

        Metrics.startRecording();
        Metrics.startRecordEvents();

        System.out.println("\nStarting tests...");

        for(int i = 0; i < 3; i++) {
            System.out.println("[Run "+i+"]-----------------------------------------------");
            this.runPingTest();
            this.runFindNodeTest();
            this.runFindValueTest();
            this.runPrimitiveStoreTest();
            this.runLookupTest(false);
            this.runLookupTest(true);
            this.runStoreTest();
            this.runQueryTest();
            System.out.println("\n");
        }

        Metrics.stopRecording();
        Metrics.startRecordEvents();
        this.bubblegum = null;
        System.exit(0);
    }

    private void runTests(BiFunction<Integer, RouterNode, Metrics.Event> f) {
        int successes = 0;
        long durations = 0;
        long meta = 0;
        for (int i = 0; i < this.iterations; i++) {
            RouterNode candidate = this.getCandidate();
            if (candidate == null) {
                System.out.println("["+i+"] Aborted - null candidate node");
                return;
            }

            Metrics.clearEvents();
            Metrics.Event e = f.apply(i, candidate);
            if(e != null) {
                successes++;
                durations += e.duration;
                meta += e.delay;
                System.out.println("["+i+"] " + (double)e.duration/1000000 + "ms, " + e.delay);
            }
        }

        System.out.println("Averages - Duration = " + ((double)durations / successes)/1000000 + "ms, Meta = " + meta / successes);
    }

    private void runPingTest() {
        System.out.println("\n--[ PING ]--");

        this.runTests((i, candidate) -> {
            PingActivity pingActivity = new PingActivity(this.node, candidate);
            pingActivity.run();
            if (pingActivity.getComplete() && pingActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event pa = null;
                long bytes = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("PingActivity")) pa = event;
                    if(event.title.equals("Server")) bytes += event.delay;
                }

                if(pa == null) return null;
                else return new Metrics.Event("Ping", pa.duration, bytes, false);
            } else {
                System.out.println("["+i+"] Aborted - ping failed");
            }
            return null;
        });
    }

    private void runFindTest(boolean value) {
        this.runTests((i, candidate) -> {
            FindActivity findActivity = new FindActivity(this.node, candidate, new NodeID().toString(), value);
            findActivity.run();
            if (findActivity.getComplete() && findActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event fa = null;
                long bytes = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("FindActivity")) fa = event;
                    if(event.title.equals("Server")) bytes += event.delay;
                }

                if(fa == null) return null;
                else return new Metrics.Event("Find", fa.duration, bytes, false);
            } else {
                System.out.println("["+i+"] Aborted - find failed");
            }
            return null;
        });
    }

    private void runFindNodeTest() {
        System.out.println("\n--[ FIND_NODE ]--");
        this.runFindTest(false);
    }

    private void runFindValueTest() {
        System.out.println("\n--[ FIND_VALUE ]--");
        this.runFindTest(true);
    }


    private void runPrimitiveStoreTest() {
        System.out.println("\n--[ STORE ]--");

        this.runTests((i, candidate) -> {
            PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(this.node, candidate, new NodeID().toString(), "lkasjflkjhasflkjhaslfkjhaslkfjhaslkjfhalskjfhalskjfdhalksjfdhasf".getBytes());
            storeActivity.run();
            if (storeActivity.getComplete() && storeActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event sa = null;
                long bytes = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("PrimitiveStoreActivity")) sa = event;
                    if(event.title.equals("Server")) bytes += event.delay;
                }

                if(sa == null) return null;
                else return new Metrics.Event("Store", sa.duration, bytes, false);
            } else {
                System.out.println("["+i+"] Aborted - store failed");
            }
            return null;
        });
    }


    private void runLookupTest(boolean value) {
        System.out.println("\n--[ Lookup (value="+value+") ]--");

        this.runTests((i, candidate) -> {
            NodeID nid = this.helperNodes[i].getNodeIdentifier();
            if(value) {
                nid = new NodeID();
                this.helperNodes[i].store(nid, "kajhdkjhadkjhakjhakjha".getBytes());
            }
            LookupActivity lookupActivity = new LookupActivity(this.node, nid, 5, value);
            long start = System.nanoTime();
            lookupActivity.run();
            long duration = System.nanoTime() - start;
            if (lookupActivity.getComplete() && lookupActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event sa = null;
                int other = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("LookupActivity")) sa = event;
                    else other++;
                }

                if(sa == null) return null;
                else return new Metrics.Event("Lookup", duration, other, false);
            } else {
                System.out.println("["+i+"] Aborted - lookup failed");
            }
            return null;
        });
    }


    private void runStoreTest() {
        System.out.println("\n--[ Store ]--");

        this.runTests((i, candidate) -> {
            StoreActivity storeActivity = new StoreActivity(this.node, new NodeID().toString(), "kjbakjbcaskjhdkajhdkjahdkjhaskjcaskjbavkjbcakjbvsavas".getBytes());
            long start = System.nanoTime();
            storeActivity.run();
            long duration = System.nanoTime() - start;
            if (storeActivity.getComplete() && storeActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event sa = null;
                int other = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("StoreActivity")) sa = event;
                    else other++;
                }

                if(sa == null) return null;
                else return new Metrics.Event("Store", duration, other, false);
            } else {
                System.out.println("["+i+"] Aborted - store failed");
            }
            return null;
        });
    }


    private void runQueryTest() {
        System.out.println("\n--[ Query ]--");

        this.runTests((i, candidate) -> {
            QueryActivity queryActivity = new QueryActivity(this.node, candidate, System.currentTimeMillis() - 5*60*100, System.currentTimeMillis(), null);
            long start = System.nanoTime();
            queryActivity.run();
            long duration = System.nanoTime() - start;
            if (queryActivity.getComplete() && queryActivity.getSuccess()) {
                List<Metrics.Event> events = Metrics.getEvents();
                Metrics.Event qa = null;
                long bytes = 0;
                for (Metrics.Event event : events) {
                    if(event.title.equals("QueryActivity")) qa = event;
                    if(event.title.equals("Server")) bytes += event.delay;
                }

                if(qa == null) return null;
                else return new Metrics.Event("Find", duration, bytes, false);
            } else {
                System.out.println("["+i+"] Aborted - find failed");
            }
            return null;
        });
    }

    private RouterNode getCandidate() {
        Set<RouterNode> candidates = this.node.getRoutingTable().getNodesClosestToKey(new NodeID(), 1);
        if(candidates.size() > 0) return candidates.iterator().next();
        else return null;
    }

    public static void main(String[] args) {
        if(args.length >= 3) {
            try {
                InetAddress addr = NetworkingHelper.getInetAddress(args[0]);
                int port = Integer.valueOf(args[1]);
                new MeasurementNode(addr, port, args[2]);

            } catch (NumberFormatException nfe) {
                System.out.println("Invalid port");
            } catch (UnknownHostException e) {
                System.out.println("Invalid address");
            }
        }
        else System.out.println("Not enough arguments!");
    }
}

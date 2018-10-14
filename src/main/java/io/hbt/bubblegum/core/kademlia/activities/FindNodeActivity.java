package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeRequest.KademliaFindNodeRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeResponse.KademliaFindNodeResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class FindNodeActivity extends NetworkActivity {

    private final String search;
    private final Set<KademliaNode> results = new HashSet<>();
    private KademliaFindNodeRequest request;
    private String requestingHash;

    private final static int RESULTS_REQUESTED = 8;

    public FindNodeActivity(KademliaServer server, BubblegumNode self, RouterNode to, RoutingTable routingTable, String search) {
        super(server, self, to, routingTable);
        this.search = search;
    }

    public void setResponse(String responseID, KademliaFindNodeRequest request, String requestingHash) {
        super.setResponse(responseID);
        this.request = request;
        this.requestingHash = requestingHash;
    }

    @Override
    public void run() {

        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        message.setOriginHash(this.localNode.getIdentifier().toString());
        message.setOriginIP(this.server.getLocal().getHostAddress());
        message.setOriginPort(this.server.getPort());
        message.setExchangeID(this.exchangeID);

        if(this.isResponse) {
            this.print("Replying to FIND_NODE from " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());

            KademliaFindNodeResponse.Builder findNodeResponse = KademliaFindNodeResponse.newBuilder();
            // TODO check not null
            findNodeResponse.setRequest(this.request);

            Set<RouterNode> results =  this.routingTable.getNodesClosestToKeyWithExclusions(
                    this.request.getSearchHash(),
                    this.request.getNumberRequested(),
                    new HashSet<>(Arrays.asList(this.requestingHash))
            );

            for(RouterNode result : results) {
                KademliaNode.Builder pbNode = KademliaNode.newBuilder();
                pbNode.setHash(result.getNode().toString());
                pbNode.setIpAddress(result.getIPAddress().getHostAddress());
                pbNode.setPort(result.getPort());
                findNodeResponse.addResults(pbNode);
            }

            message.setFindNodeResponse(findNodeResponse);
        }
        else {
            this.print("Starting FIND_NODE(" + this.search + ") activity to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());

            KademliaFindNodeRequest.Builder findNodeRequest = KademliaFindNodeRequest.newBuilder();
            findNodeRequest.setSearchHash(this.search);
            findNodeRequest.setNumberRequested(FindNodeActivity.RESULTS_REQUESTED);
            message.setFindNodeRequest(findNodeRequest);
        }

        Consumer<KademliaMessage> callback = this.isResponse ? null : (kademliaMessage) -> {
            if(kademliaMessage.hasFindNodeResponse()) {
                KademliaFindNodeResponse response = kademliaMessage.getFindNodeResponse();
                StringBuilder logMessage = new StringBuilder();
                logMessage.append(kademliaMessage.getOriginHash() + " returned " + response.getResultsCount() + " results:\n");
                for(KademliaNode node : response.getResultsList()) {
                    logMessage.append("- " + node.getHash() + " @ " + node.getIpAddress() + ":" + node.getPort() + "\n");

                    // Only ping if not found or stale
                    RouterNode destination = this.routingTable.getRouterNodeForID(this.to.getNode());
                    if(destination == null || System.currentTimeMillis() - destination.getLatestResponse() > 600000000000L) {
                        PingActivity nodePing = new PingActivity(this.server, this.localNode, RouterNode.fromKademliaNode(node), this.routingTable);
                        this.localNode.getExecutionContext().addPingActivity(this.localNode.getIdentifier().toString(), nodePing);
                    }
                }

                this.print(logMessage.toString());
                this.results.addAll(response.getResultsList());
                this.complete = true;

                // TODO handle response
            }
            else {
                this.print("Invalid");
            }
        };

        this.server.sendDatagram(this.to, message.build(), callback);
        this.timeoutOnComplete();
    }

    public Set<KademliaNode> getResults() {
        return this.results;
    }
}

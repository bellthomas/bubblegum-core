package io.hbt.bubblegum.core.kademlia.activities;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeResponse.KademliaFindNodeResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindValueResponse.KademliaFindValueResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class FindActivity extends NetworkActivity {

    private final String search;
    private final Set<KademliaNode> resultNodes = new HashSet<>();
    private KademliaFindRequest request;
    private String requestingHash;
    private boolean returnValue;
    private byte[] value;

    private final static int RESULTS_REQUESTED = 8;

    public FindActivity(BubblegumNode self, RouterNode to, String search, boolean returnValue) {
        super(self, to);
        this.search = search;
        this.returnValue = returnValue;
    }

    public void setResponse(String responseID, KademliaFindRequest request, String requestingHash) {
        super.setResponse(responseID);
        this.request = request;
        this.requestingHash = requestingHash;
        this.returnValue = request.getReturnValue();
    }

    @Override
    public void run() {

        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        message.setOriginHash(this.localNode.getNodeIdentifier().toString());
        message.setOriginIP(this.server.getLocal().getHostAddress());
        message.setOriginPort(this.server.getPort());
        message.setExchangeID(this.exchangeID);

        if(this.isResponse) {
            this.print("Replying to " + (this.returnValue ? "FIND_VALUE" : "FIND_NODE") + " from "
                    + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());

            if(this.returnValue && this.localNode.databaseHasKey(this.request.getSearchHash())) {
                // We want to return the value
                KademliaFindValueResponse.Builder findValueResponse = KademliaFindValueResponse.newBuilder();
                findValueResponse.setRequest(this.request);

                // TODO validate
                findValueResponse.setValue(ByteString.copyFrom(this.localNode.databaseRetrieveValue(this.request.getSearchHash())));
                message.setFindValueResponse(findValueResponse);
            }
            else {

                // Return close nodes
                KademliaFindNodeResponse.Builder findNodeResponse = KademliaFindNodeResponse.newBuilder();
                // TODO check not null
                findNodeResponse.setRequest(this.request);

                Set<RouterNode> results = this.routingTable.getNodesClosestToKeyWithExclusions(
                        this.request.getSearchHash(),
                        this.request.getNumberRequested(),
                        new HashSet<>(Arrays.asList(this.requestingHash))
                );

                for (RouterNode result : results) {
                    KademliaNode.Builder pbNode = KademliaNode.newBuilder();
                    pbNode.setHash(result.getNode().toString());
                    pbNode.setIpAddress(result.getIPAddress().getHostAddress());
                    pbNode.setPort(result.getPort());
                    findNodeResponse.addResults(pbNode);
                }

                message.setFindNodeResponse(findNodeResponse);
            }
        }
        else {
            this.print("Starting " + (this.returnValue ? "FIND_VALUE" : "FIND_NODE") + "(" + this.search + ") activity to "
                    + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());

            KademliaFindRequest.Builder findNodeRequest = KademliaFindRequest.newBuilder();
            findNodeRequest.setSearchHash(this.search);
            findNodeRequest.setNumberRequested(FindActivity.RESULTS_REQUESTED);
            findNodeRequest.setReturnValue(this.returnValue);
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
                    if(destination == null || !destination.isFresh()) {
                        PingActivity nodePing = new PingActivity(this.localNode, this.localNode.getRoutingTable().fromKademliaNode(node));
                        this.localNode.getExecutionContext().addPingActivity(this.localNode.getNodeIdentifier().toString(), nodePing);
                    }
                }

                this.print(logMessage.toString());
                this.resultNodes.addAll(response.getResultsList());
                this.insertSenderNode(kademliaMessage.getOriginHash(), kademliaMessage.getOriginIP(), kademliaMessage.getOriginPort());
                this.onSuccess();

                // TODO handle response
            }
            else if(kademliaMessage.hasFindValueResponse()) {
                KademliaFindValueResponse findValueResponse = kademliaMessage.getFindValueResponse();
                byte[] value = findValueResponse.getValue().toByteArray();
                this.value = value;
                this.print("FIND_VALUE on " + findValueResponse.getRequest().getSearchHash() + " returned " + value.length + " bytes");
                this.onSuccess();
            }
            else {
                this.print("Invalid");
                this.onFail();
            }
        };

        this.server.sendDatagram(this.to, message.build(), callback);
        if(!this.isResponse) this.timeoutOnComplete();
        else this.onSuccess();
    }

    private void insertSenderNode(String hash, String ip, int port) {
        try {
            RouterNode sender = this.routingTable.getRouterNodeForID(new NodeID(hash));
            if(sender == null) {
                sender = new RouterNode(
                    new NodeID(hash),
                    InetAddress.getByName(ip),
                    port
                );
            }

            this.routingTable.insert(sender);

        } catch (MalformedKeyException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Set<KademliaNode> getFindNodeResults() {
        return this.resultNodes;
    }

    public byte[] getFindValueResult() {
        return this.value;
    }

    public RouterNode getDestination() {
        return this.to;
    }
}

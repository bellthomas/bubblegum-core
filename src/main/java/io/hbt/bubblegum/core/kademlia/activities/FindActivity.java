package io.hbt.bubblegum.core.kademlia.activities;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeResponse.KademliaFindNodeResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindValueResponse.KademliaFindValueResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FindActivity extends NetworkActivity {

    private final String search;
    private final Set<RouterNode> resultNodes = new HashSet<>();
    private KademliaFindRequest request;
    private String requestingHash;
    private boolean returnValue;
    private List<byte[]> values;

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
        KademliaMessage message = null;

        if(this.isResponse) {
            this.print("Replying to " + (this.returnValue ? "FIND_VALUE" : "FIND_NODE") + " from "
                    + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());

            if(this.returnValue && this.localNode.databaseHasKey(this.request.getSearchHash())) {

                // TODO validate
                List<byte[]> value = this.localNode.databaseRetrieveValue(this.request.getSearchHash());

                message = ProtobufHelper.buildFindValueResponse(
                    this.localNode,
                    this.to,
                    this.exchangeID,
                    this.request,
                    value
                );
            }
            else {

                Set<RouterNode> results = this.routingTable.getNodesClosestToKeyWithExclusions(
                    this.request.getSearchHash(),
                    this.request.getNumberRequested(),
                    new HashSet<>(Arrays.asList(this.requestingHash))
                );

                message = ProtobufHelper.buildFindNodeResponse(
                    this.localNode,
                    this.to,
                    this.exchangeID,
                    this.request,
                    results
                );
            }
        }
        else {
            this.print("Starting " + (this.returnValue ? "FIND_VALUE" : "FIND_NODE") + "(" + this.search + ") activity to "
                    + this.localNode.getNetworkIdentifier() + ":" + this.to.getNode());


            message = ProtobufHelper.buildFindRequest(
                this.localNode,
                this.to,
                this.exchangeID,
                this.search,
                FindActivity.RESULTS_REQUESTED,
                this.returnValue
            );
        }

        Consumer<KademliaMessage> callback = this.isResponse ? null : (kademliaMessage) -> {
            if(kademliaMessage.hasFindNodeResponse()) {
                KademliaFindNodeResponse response = kademliaMessage.getFindNodeResponse();
                StringBuilder logMessage = new StringBuilder();
                logMessage.append(kademliaMessage.getOriginHash() + " returned " + response.getResultsCount() + " results:\n");
                for(KademliaNode node : response.getResultsList()) {
                    logMessage.append("- " + node.getHash() + " @ " + node.getIpAddress() + ":" + node.getPort() + "\n");

                    // Only ping if not found or stale
                    RouterNode destination = this.routingTable.fromKademliaNode(node);
                    this.resultNodes.add(destination);
                }

//                this.print(logMessage.toString());
                this.insertSenderNode(kademliaMessage.getOriginHash(), kademliaMessage.getOriginIP(), kademliaMessage.getOriginPort());
                this.onSuccess();

                // TODO handle response?
            }
            else if(kademliaMessage.hasFindValueResponse()) {
                KademliaFindValueResponse findValueResponse = kademliaMessage.getFindValueResponse();
                List<ByteString> byteStringValues = findValueResponse.getValueList();
                List<byte[]> byteArrayValues = byteStringValues.stream().map((bs) -> bs.toByteArray()).collect(Collectors.toList());
                this.values = byteArrayValues;
                this.print("FIND_VALUE on " + findValueResponse.getRequest().getSearchHash() + " returned " + this.values.size() + " elements");
                this.insertSenderNode(kademliaMessage.getOriginHash(), kademliaMessage.getOriginIP(), kademliaMessage.getOriginPort());
                this.onSuccess();
            }
            else {
                this.print("Invalid");
                this.onFail();
            }
        };

        if(message != null) this.server.sendDatagram(this.localNode, this.to, message, callback);

        if(!this.isResponse) this.timeoutOnComplete();
        else this.onSuccess();
    }

    private void insertSenderNode(String hash, String ip, int port) {
        try {
            RouterNode sender = this.routingTable.getRouterNodeForID(new NodeID(hash));
            if(sender == null) {
                sender = new RouterNode(
                    new NodeID(hash),
                    NetworkingHelper.getInetAddress(ip),
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

    public Set<RouterNode> getFindNodeResults() {
        return this.resultNodes;
    }

    public List<byte[]> getFindValueResult() {
        return this.values;
    }

    public RouterNode getDestination() {
        return this.to;
    }

    public void pingToValidate() {
        if(this.resultNodes != null) {
            for(RouterNode node : this.resultNodes) {
                PingActivity nodePing = new PingActivity(this.localNode, node); // kademliaMessage.getOriginNetwork() + ":" + node.getHash()
                this.localNode.getExecutionContext().addPingActivity(this.localNode.getIdentifier(), nodePing);
            }
        }
    }
}

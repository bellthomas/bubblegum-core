package io.hbt.bubblegum.core.kademlia.activities;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaBinaryPayload.KademliaBinaryPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeResponse.KademliaFindNodeResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindValueResponse.KademliaFindValueResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Implementation of the FIND_NODE and FIND_VALUE RPCs.
 */
public class FindActivity extends NetworkActivity {

    private final String search;
    private final Set<RouterNode> resultNodes = new HashSet<>();
    private KademliaFindRequest request;
    private String requestingHash;
    private boolean returnValue;
    private List<byte[]> values;

    private final static int RESULTS_REQUESTED = 8;

    /**
     * Constructor.
     * @param self The owning BubblegumNode.
     * @param to The peer being queried.
     * @param search The search key.
     * @param returnValue Whether to return values if the peer has any.
     */
    public FindActivity(BubblegumNode self, RouterNode to, String search, boolean returnValue) {
        super(self, to);
        this.search = search;
        this.returnValue = returnValue;
    }

    /**
     * Declare that this activity was created in response to another message.
     * @param responseID The exchangeIdentifier of the original message.
     * @param request The payload of the original message.
     * @param requestingHash The requestor's NodeID hash.
     */
    public void setResponse(String responseID, KademliaFindRequest request, String requestingHash) {
        super.setResponse(responseID);
        this.request = request;
        this.requestingHash = requestingHash;
        this.returnValue = request.getReturnValue();
    }

    /**
     * Execute the RPC's logic.
     */
    @Override
    public void run() {
        super.run();
        if(this.aborted || !this.localNode.syncIfRequired(this.to)) {
            this.onFail("Aborted/Sync Failed");
            return;
        }

        KademliaMessage message = null;

        if(this.isResponse) {
            if(this.returnValue && this.localNode.databaseHasKey(this.request.getSearchHash())) {
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
            KademliaBinaryPayload payload = KademliaServerWorker.extractPayload(this.localNode, this.to, kademliaMessage);
            if(payload != null) {
                if (payload.hasFindNodeResponse()) {
                    KademliaFindNodeResponse response = payload.getFindNodeResponse();

                    for (KademliaNode node : response.getResultsList()) {
                        // Only ping if not found or stale
                        RouterNode destination = this.routingTable.fromKademliaNode(node);
                        this.resultNodes.add(destination);
                    }

                    this.routingTable.insert(KademliaServerWorker.getFromOriginHash(this.localNode, kademliaMessage));
                    this.onSuccess();

                } else if (payload.hasFindValueResponse()) {

                    KademliaFindValueResponse findValueResponse = payload.getFindValueResponse();
                    List<ByteString> byteStringValues = findValueResponse.getValueList();
                    List<byte[]> byteArrayValues = byteStringValues.stream().map((bs) -> bs.toByteArray()).collect(Collectors.toList());
                    this.values = byteArrayValues;

                    this.routingTable.insert(KademliaServerWorker.getFromOriginHash(this.localNode, kademliaMessage));
                    this.onSuccess();
                } else {
                    this.onFail();
                }
            }
            else {
                this.onFail();
            }
        };

        if(message != null) this.server.sendDatagram(this.localNode, this.to, message, callback);
        else this.onFail();

        if(!this.isResponse) this.timeoutOnComplete();
        else this.onSuccess();
    }

    /**
     * Retrieve the found peers.
     * @return Found peers.
     */
    public Set<RouterNode> getFindNodeResults() {
        return this.resultNodes;
    }

    /**
     * Retrieve the values found.
     * @return Found values.
     */
    public List<byte[]> getFindValueResult() {
        return this.values;
    }

    /**
     * Retrieve the peer being sent to.
     * @return The RouterNode instance of the peer.
     */
    public RouterNode getDestination() {
        return this.to;
    }

} // end FindActivity class

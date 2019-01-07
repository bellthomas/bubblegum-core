package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QueryActivity extends NetworkActivity {

    private long periodStart, periodEnd;
    private List<String> ids;

    private List<Post> results;

    public QueryActivity(BubblegumNode self, RouterNode to, long periodStart, long periodEnd, List<String> ids) {
        super(self, to);
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.ids = (ids == null) ? new ArrayList<>() : ids;
        this.results = new ArrayList<>();
    }

    public void setResponse(String responseID, KademliaQueryRequest request) {
        super.setResponse(responseID);
        this.periodStart = request.getFromTime();
        this.periodEnd = request.getToTime();
        this.ids = request.getIdListList();
    }

    @Override
    public void run() {
        super.run();
        if(this.periodStart < 0 && this.periodEnd < 0 && (this.ids == null || this.ids.size() == 0)) {
            this.onFail();
            return;
        }

        KademliaMessage message = null;

        if(this.isResponse) {
            List<Post> posts = this.localNode.queryPosts(periodStart, periodEnd, ids);
            message = ProtobufHelper.buildQueryResponse(this.localNode, this.to, this.exchangeID, posts);
        }

        else {
            message = ProtobufHelper.buildQueryRequest(
                this.localNode,
                this.to,
                this.exchangeID,
                this.periodStart,
                this.periodEnd,
                this.ids
            );
        }

        Consumer<KademliaMessage> callback = (this.isResponse) ? null : (kademliaMessage -> {
            if(kademliaMessage.hasQueryResponse()) {
                KademliaQueryResponse response = kademliaMessage.getQueryResponse();

                for(KademliaQueryResponseItem item : response.getItemsList()) {
                    this.results.add(Post.fromKademliaQueryResponseItem(item));
                }

                this.onSuccess();
            }
            else {
                this.onFail("Invalid response");
            }
        });

        if(message != null) this.server.sendDatagram(this.localNode, this.to, message, callback);

        if(!this.isResponse) this.timeoutOnComplete();
        else this.onSuccess();
    }

    public List<Post> getResults() {
        return this.results;
    }
}

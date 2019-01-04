package io.hbt.bubblegum.core.kademlia;

import co.paralleluniverse.fibers.Suspendable;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.activities.PrimitiveStoreActivity;
import io.hbt.bubblegum.core.kademlia.activities.QueryActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.UnknownHostException;

public class KademliaServerWorker {

    @Suspendable
    public static void accept(BubblegumNode node, KademliaMessage message) {

        if(message.hasPingMessage() && !message.getPingMessage().getReply()) {
//            node.log("PING received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]");
            try {
                RouterNode sender = node.getRoutingTable().getRouterNodeForID(new NodeID(message.getOriginHash()));
                if(sender == null) sender = new RouterNode(
                    new NodeID(message.getOriginHash()),
                    NetworkingHelper.getInetAddress(message.getOriginIP()),
                    message.getOriginPort()
                );
                node.getRoutingTable().insert(sender);

                PingActivity pingReply = new PingActivity(node, sender, message.getOriginNetwork() + ":" + message.getOriginHash());
                pingReply.setResponse(message);
                node.getExecutionContext().addCallbackActivity(node.getIdentifier(), pingReply);

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        else if(message.hasFindRequest()) {
            boolean returnValue = message.getFindRequest().getReturnValue();
//            node.log((returnValue ? "FIND_VALUE" : "FIND_NODE") + " received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getFindRequest().getSearchHash());

            try {
                RouterNode sender = node.getRoutingTable().getRouterNodeForID(new NodeID(message.getOriginHash()));
                if(sender == null) sender = new RouterNode(
                    new NodeID(message.getOriginHash()),
                    NetworkingHelper.getInetAddress(message.getOriginIP()),
                    message.getOriginPort()
                );
                // TODO insert?
                node.getRoutingTable().insert(sender);

                FindActivity findNodeReply = new FindActivity(
                    node,
                    sender,
                    message.getFindRequest().getSearchHash(),
                    returnValue
                );
                findNodeReply.setResponse(message.getExchangeID(), message.getFindRequest(), message.getOriginHash());
                node.getExecutionContext().addCallbackActivity(node.getIdentifier(), findNodeReply);

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        else if(message.hasStoreRequest()) {
//            node.log("STORE received[" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getStoreRequest().getKey());
            try {
                RouterNode sender = node.getRoutingTable().getRouterNodeForID(new NodeID(message.getOriginHash()));
                if(sender == null) sender = new RouterNode(
                    new NodeID(message.getOriginHash()),
                    NetworkingHelper.getInetAddress(message.getOriginIP()),
                    message.getOriginPort()
                );
                // TODO insert?
                node.getRoutingTable().insert(sender);

                PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(node, sender, message.getStoreRequest().getKey(), message.getStoreRequest().getValue().toByteArray());
                storeActivity.setResponse(message.getExchangeID());
                node.getExecutionContext().addCallbackActivity(node.getIdentifier(), storeActivity);

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        else if(message.hasQueryRequest()) {
            //            node.log("STORE received[" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getStoreRequest().getKey());
            try {
                RouterNode sender = node.getRoutingTable().getRouterNodeForID(new NodeID(message.getOriginHash()));
                if (sender == null) sender = new RouterNode(
                    new NodeID(message.getOriginHash()),
                    NetworkingHelper.getInetAddress(message.getOriginIP()),
                    message.getOriginPort()
                );
                // TODO insert?
                node.getRoutingTable().insert(sender);

                QueryActivity queryActivity = new QueryActivity(node, sender, 0, 0, null);
                queryActivity.setResponse(message.getExchangeID(), message.getQueryRequest());
                node.getExecutionContext().addCallbackActivity(node.getIdentifier(), queryActivity);

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}
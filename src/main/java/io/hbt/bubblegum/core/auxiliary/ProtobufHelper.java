package io.hbt.bubblegum.core.auxiliary;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaBinaryPayload.KademliaBinaryPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindNodeResponse.KademliaFindNodeResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindValueResponse.KademliaFindValueResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaPing.KademliaPing;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSealedPayload.KademliaSealedPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaStoreRequest.KademliaStoreRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaStoreResponse.KademliaStoreResponse;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSync.KademliaSync;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProtobufHelper {

    public static KademliaMessage.Builder constructKademliaMessage(BubblegumNode localNode, String to, String exchangeID) {
        return constructKademliaMessage(localNode, to, exchangeID, localNode.getNetworkIdentifier() + ":" + to);
    }

    public static KademliaMessage.Builder constructKademliaMessage(BubblegumNode localNode, String to, String exchangeID, String foreignRecipient) {
        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        message.setOriginNetwork(localNode.getNetworkIdentifier());
        message.setOriginHash(localNode.getNodeIdentifier().toString());
        message.setOriginIP(localNode.getServer().getLocal().getHostAddress());
        message.setOriginPort(localNode.getServer().getPort());
        message.setExchangeID(exchangeID);
        message.setRecipient(foreignRecipient);
        return message;
    }

    public static KademliaMessage buildStoreRequest(BubblegumNode localNode, RouterNode to, String exchangeID, String key, byte[] payloads) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaStoreRequest.Builder storeRequest = KademliaStoreRequest.newBuilder();
        storeRequest.setKey(key);
        storeRequest.setValue(ByteString.copyFrom(payloads));
        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setStoreRequest(storeRequest);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildStoreResponse(BubblegumNode localNode, RouterNode to, String exchangeID, boolean accepted) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaStoreResponse.Builder storeResponse = KademliaStoreResponse.newBuilder();
        storeResponse.setAccepted(accepted);
        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setStoreResponse(storeResponse);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildPingMessage(BubblegumNode localNode, String to, String exchangeID, boolean response, String foreignRecipient) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to, exchangeID, foreignRecipient);
        KademliaPing.Builder pingMessage = KademliaPing.newBuilder();
        pingMessage.setReply(response);
        message.setPingMessage(pingMessage);
        return message.build();
    }

    public static KademliaMessage buildFindValueResponse(BubblegumNode localNode, RouterNode to, String exchangeID, KademliaFindRequest request, List<byte[]> values) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaFindValueResponse.Builder findValueResponse = KademliaFindValueResponse.newBuilder();
        findValueResponse.setRequest(request);

        // Ensure no packet overflow
        int maxSize = Math.max(0, Configuration.DATAGRAM_BUFFER_SIZE - 1000);
        int currentSize = 0;
        Collections.shuffle(values);
        for(byte[] v : values) {
            if(currentSize + v.length < maxSize) {
                currentSize += v.length;
                findValueResponse.addValue(ByteString.copyFrom(v));
            }
        }

        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setFindValueResponse(findValueResponse);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildFindNodeResponse(BubblegumNode localNode, RouterNode to, String exchangeID, KademliaFindRequest request, Set<RouterNode> foundNodes) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaFindNodeResponse.Builder findNodeResponse = KademliaFindNodeResponse.newBuilder();
        findNodeResponse.setRequest(request);

        for (RouterNode result : foundNodes) {
            KademliaNode.Builder pbNode = KademliaNode.newBuilder();
            pbNode.setHash(result.getNode().toString());
            pbNode.setIpAddress(result.getIPAddress().getHostAddress());
            pbNode.setPort(result.getPort());
            findNodeResponse.addResults(pbNode);
        }

        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setFindNodeResponse(findNodeResponse);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildFindRequest(BubblegumNode localNode, RouterNode to, String exchangeID, String searchHash, int numRequested, boolean returnValue) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaFindRequest.Builder findRequest = KademliaFindRequest.newBuilder();
        findRequest.setSearchHash(searchHash);
        findRequest.setNumberRequested(numRequested);
        findRequest.setReturnValue(returnValue);
        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setFindRequest(findRequest);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildQueryRequest(BubblegumNode localNode, RouterNode to, String exchangeID, long start, long end, List<String> ids) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaQueryRequest.Builder queryRequest = KademliaQueryRequest.newBuilder();
        queryRequest.setFromTime(start);
        queryRequest.setToTime(end);
        queryRequest.addAllIdList(ids);
        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setQueryRequest(queryRequest);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaMessage buildQueryResponse(BubblegumNode localNode, RouterNode to, String exchangeID, List<Post> posts) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaQueryResponse.Builder queryResponse = KademliaQueryResponse.newBuilder();
        for(Post p : posts) queryResponse.addItems(buildQueryResponseItem(p));

        KademliaBinaryPayload.Builder payload = KademliaBinaryPayload.newBuilder();
        payload.setQueryResponse(queryResponse);
        message.setPayload(buildSealedPayload(localNode, to, payload.build().toByteArray()));
        return message.build();
    }

    public static KademliaQueryResponseItem buildQueryResponseItem(Post post) {
        KademliaQueryResponseItem.Builder responseItem = KademliaQueryResponseItem.newBuilder();
        responseItem.setId(post.getID());
        responseItem.setContent(post.getContent());
        responseItem.setResponse(post.getResponse());
        responseItem.setTime(post.getTimeCreated());
        responseItem.setNetwork(post.getNetwork());
        responseItem.setOwner(post.getOwner());
        return responseItem.build();
    }

    public static KademliaMessage buildSyncMessage(BubblegumNode localNode, RouterNode to, String exchangeID, int stage, String label, byte[] payload) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, to.getNode().toString(), exchangeID);
        KademliaSync.Builder sync = KademliaSync.newBuilder();
        sync.setStage(stage);
        sync.setLabel(label);
        sync.setEncrypted(ByteString.copyFrom(payload));
        message.setSyncMessage(sync);
        return message.build();
    }

    public static KademliaSealedPayload buildSealedPayload(BubblegumNode node, RouterNode dest, byte[] data) {
        if(Configuration.ENABLE_PGP) {
            return node.encryptPacket(dest, data);
        } else {
            KademliaSealedPayload.Builder payload = KademliaSealedPayload.newBuilder();
            payload.setData(ByteString.copyFrom(data));
            payload.setKeyA(ByteString.EMPTY);
            payload.setKeyB(ByteString.EMPTY);
            return payload.build();
        }
    }

}

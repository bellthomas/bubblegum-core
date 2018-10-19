package io.hbt.bubblegum.core.auxiliary;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaPing;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaStoreRequest.KademliaStoreRequest;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaStoreResponse.KademliaStoreResponse;

public class ProtobufHelper {

    public static KademliaMessage.Builder constructKademliaMessage(BubblegumNode localNode, String exchangeID) {
        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        message.setOriginHash(localNode.getIdentifier().toString());
        message.setOriginIP(localNode.getServer().getLocal().getHostAddress());
        message.setOriginPort(localNode.getServer().getPort());
        message.setExchangeID(exchangeID);
        return message;
    }

    public static KademliaMessage buildStoreRequest(BubblegumNode localNode, String exchangeID, String key, byte[] payload) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, exchangeID);
        KademliaStoreRequest.Builder storeRequest = KademliaStoreRequest.newBuilder();
        storeRequest.setKey(key);
        storeRequest.setValue(ByteString.copyFrom(payload));
        message.setStoreRequest(storeRequest);
        return message.build();
    }

    public static KademliaMessage buildStoreResponse(BubblegumNode localNode, String exchangeID, boolean accepted) {
        KademliaMessage.Builder message = constructKademliaMessage(localNode, exchangeID);
        KademliaStoreResponse.Builder storeResponse = KademliaStoreResponse.newBuilder();
        storeResponse.setAccepted(accepted);
        message.setStoreResponse(storeResponse);
        return message.build();
    }

}

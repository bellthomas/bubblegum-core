package io.hbt.bubblegum.core.databasing;

/**
 *
 */
public class NetworkDetails {
    private final String id, network, hash;
    private final int port;

    protected NetworkDetails(String id, String network, String hash, int port) {
        this.id = id;
        this.network = network;
        this.hash = hash;
        this.port = port;
    }

    public String getID() {
        return this.id;
    }

    public String getNetwork() {
        return this.network;
    }

    public String getHash() {
        return this.hash;
    }

    public int getPort() {
        return this.port;
    }
}
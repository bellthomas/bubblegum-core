package io.hbt.bubblegum.core.auxiliary;

public class ObjectResolutionDetails {
    public final String hostname, requestKey, encryptionKey, mimeType;
    public final int port;
    public ObjectResolutionDetails(String hostname, int port, String requestKey, String encryptionKey, String mimeType) {
        this.hostname = hostname;
        this.port = port;
        this.requestKey = requestKey;
        this.encryptionKey = encryptionKey;
        this.mimeType = mimeType;
    }
}
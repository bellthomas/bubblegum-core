package io.hbt.bubblegum.core.auxiliary;

/**
 * Details format for information retrieved by the RESOLVE RPC.
 */
public class ObjectResolutionDetails {
    public final String hostname, requestKey, encryptionKey, mimeType;
    public final int port;

    /**
     * Constructor.
     * @param hostname The server's address.
     * @param port The server's port.
     * @param requestKey The fetch key.
     * @param encryptionKey The encryption key.
     * @param mimeType The file's MIME type.
     */
    public ObjectResolutionDetails(String hostname, int port, String requestKey, String encryptionKey, String mimeType) {
        this.hostname = hostname;
        this.port = port;
        this.requestKey = requestKey;
        this.encryptionKey = encryptionKey;
        this.mimeType = mimeType;
    }

} // end ObjectResolutionDetails class
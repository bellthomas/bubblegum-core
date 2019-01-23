package io.hbt.bubblegum.core;

/**
 * System Configuration.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class Configuration {

    public static final int KEY_BIT_LENGTH = 80;

    public static final int BIN_EPOCH_DURATION = 1000 * 60 * 5; // 5 minutes

    public static final int LOOKUP_TIMEOUT = 1000 * 5; // 5 seconds

    public static final int NETWORK_ACTIVITY_RETRIES = 3; // 5 seconds
    public static final int ACTIVITY_TIMEOUT = 1000 * 10; // 10 seconds
    public static final int ACTIVITY_MAX_DELAY = 1000 * 10; // 10 seconds

    protected final static int TIMEOUT = 10000; // ms
    protected final static int MAX_START_DELAY = 10000; // ms

    //region Server
    public static final int MAX_BUBBLEGUM_CELLS = 30;
    public static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // TODO 64KB
    //endregion


    private Configuration() { /* Non instantiatable */ }

    public void reload() {

    }
}

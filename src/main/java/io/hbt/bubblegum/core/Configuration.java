package io.hbt.bubblegum.core;

import java.util.Random;

/**
 * System Configuration.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class Configuration {

    public static final int KEY_BIT_LENGTH = 80;

    public static final String DB_FOLDER_PATH = ".databases/";
    public static final long DB_ENTITY_EXPIRY_AGE = 30 * 1000; // ms
    public static final int BIN_EPOCH_DURATION = 1000 * 60 * 5; // 5 minutes
    public static final int POST_EXPIRY_REFRESH_CHECK = 10 * 1000; // ms

    public static final int LOOKUP_TIMEOUT = 5 * 1000; // 5 seconds

    public static final int NETWORK_ACTIVITY_RETRIES = 3; // 5 seconds
    public static final int ACTIVITY_TIMEOUT = 1000 * 10; // 10 seconds
    public static final int ACTIVITY_MAX_DELAY = 1000 * 10; // 10 seconds

    protected final static int TIMEOUT = 10000; // ms
    protected final static int MAX_START_DELAY = 10000; // ms

    //region Server
    public static final int MAX_BUBBLEGUM_CELLS = 30;
    public static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // kb
    //endregion

    public static final int EXECUTION_CONTEXT_MAX_THREADS = 200;
    public static final int EXECUTION_CONTEXT_GENERAL_PARALLELISM = 8;
    public static final int EXECUTION_CONTEXT_COMPOUND_PARALLELISM = 5;
    public static final int EXECUTION_CONTEXT_CALLBACK_PARALLELISM = 7;

    public static Random rand = new Random();

    private Configuration() { /* Non instantiatable */ }

    public static int random(int max) {
        return Configuration.rand.nextInt(max);
    }
}

package io.hbt.bubblegum.core;

import java.util.Random;

/**
 * System Configuration.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class Configuration {

    //region Kademlia ID and Router
    public static final int KEY_BIT_LENGTH = 80;
    public static final int ROUTER_BUCKET_SIZE = 8;
    public static final int ROUTER_NODE_FRESH_EXPIRY = 60 * 1000; // ms
    public static final int ROUTER_NODE_FRESH_OVERRIDE_PERCENTAGE = 5; // %
    public static final int REFRESH_BUCKETS_TIMER = 5 * 60 * 1000; // ms
    //endregion

    //region Database
    public static final String DB_FOLDER_PATH = ".databases/";
    public static final String CDB_FILE_NAME = "_content.db";
    public static final long DB_ENTITY_EXPIRY_AGE = 10 * 1000; // ms
    public static final int BIN_EPOCH_DURATION = 5 * 60 * 1000; // ms
    public static final int POST_EXPIRY_REFRESH_CHECK = 250; // ms
    public static final float RANDOM_POST_REFRESH_PROBABILITY = (float)Configuration.DB_ENTITY_EXPIRY_AGE / Configuration.POST_EXPIRY_REFRESH_CHECK;
    //endregion

    //region Activities/RPCs
    public static final int NETWORK_ACTIVITY_RETRIES = 3; // attempts
    public static final int ACTIVITY_TIMEOUT = 10 * 1000; // ms
    public static final int ACTIVITY_MAX_DELAY = 10 * 1000; // ms
    public static final int LOOKUP_TIMEOUT = 5 * 1000; // ms
    public final static int LOOKUP_ALPHA = 5; // parallelism
    //endregion

    //region Server
    public static final int MAX_BUBBLEGUM_CELLS = 10;
    public static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // kb
    //endregion

    //region Execution Context
    public static final int EXECUTION_CONTEXT_MAX_THREADS = 50;
    public static final int EXECUTION_CONTEXT_GENERAL_PARALLELISM = 8;
    public static final int EXECUTION_CONTEXT_COMPOUND_PARALLELISM = 5;
    public static final int EXECUTION_CONTEXT_CALLBACK_PARALLELISM = 7;
    public static final int EXECUTION_CONTEXT_MAX_PENDING_QUEUE = 100;
    //endregion

    public static Random rand = new Random();

    private Configuration() { /* Non instantiatable */ }

    public static int random(int max) {
        return Configuration.rand.nextInt(max);
    }
}

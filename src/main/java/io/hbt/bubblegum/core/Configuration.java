package io.hbt.bubblegum.core;

public class Configuration {

    public static final int BIN_EPOCH_DURATION = 1000 * 60 * 5; // 5 minutes

    //region Server
    public static final int MAX_BUBBLEGUM_CELLS = 30;
    public static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // TODO 64KB
    //endregion


    private Configuration() { /* Non instantiatable */ }

    public void reload() {

    }
}

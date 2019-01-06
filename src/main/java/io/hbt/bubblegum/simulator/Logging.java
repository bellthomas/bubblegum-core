package io.hbt.bubblegum.simulator;

public class Logging {

    private static Logging instance;

    private Logging() { /* Non instantiable */ }

    public synchronized Logging getInstance() {
        if(Logging.instance == null) Logging.instance = new Logging();
        return Logging.instance;
    }
}

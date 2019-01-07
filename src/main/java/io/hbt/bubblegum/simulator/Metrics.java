package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.kademlia.activities.NetworkActivity;
import io.hbt.bubblegum.core.kademlia.activities.SystemActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metrics {

    private static Metrics instance;
    private static boolean recording = false;

    private Metrics() { /* Non instantiable */ }

    protected synchronized Metrics getInstance() {
        if(Metrics.instance == null) Metrics.instance = new Metrics();
        return Metrics.instance;
    }

    public static boolean isRecording() {
        return Metrics.recording;
    }

    protected static void startRecording() {
        Metrics.recording = true;
    }

    protected static void stopRecording() {
        Metrics.recording = false;
    }

    private static final Logger logger = LoggerFactory.getLogger(Metrics.class);

    public static void activitySubmission(SystemActivity activity, long init, long start, long end, boolean success) {
        String response = "";
        if(activity instanceof NetworkActivity && ((NetworkActivity) activity).isResponse()) response = " (response)";

        logger.info(activity.getClass().getSimpleName() + ": " + (end-start) + "ms after a " + (start-init) + "ms wait" + response);
    }

    public static void serverSubmission(int bytes, boolean incoming) {
        if(incoming) logger.info("[Server] IN " + bytes + " bytes");
        else logger.info("[Server] OUT " + bytes + " bytes");
    }
}

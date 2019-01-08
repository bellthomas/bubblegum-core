package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.kademlia.activities.NetworkActivity;
import io.hbt.bubblegum.core.kademlia.activities.SystemActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Metrics {

    private static Metrics instance;
    private static boolean recording = false;
    private static boolean internal = true;
    private static final Logger logger = LoggerFactory.getLogger(Metrics.class);
    private static List<Event> events = new ArrayList<>();

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

    protected static void startInternalLogging() { Metrics.internal = true; }

    protected static void startDiskLogging() { Metrics.internal = false; }


    public static void activitySubmission(SystemActivity activity, long init, long start, long end, boolean success) {
        if (Metrics.internal) {
            boolean response = false;
            if (activity instanceof NetworkActivity && ((NetworkActivity) activity).isResponse()) response = true;
            events.add(new Event(activity.getClass().getSimpleName(), end-start, start-init, response));
        } else {
            String response = "";
            if (activity instanceof NetworkActivity && ((NetworkActivity) activity).isResponse())
                response = " (response)";
            logger.info(activity.getClass().getSimpleName() + ": " + (end - start) + "ms after a " + (start - init) + "ms wait" + response);
        }
    }

    public static void serverSubmission(int bytes, boolean incoming) {
        if(Metrics.internal) {
            events.add(new Event("Server", 0, bytes, incoming));
        } else {
            if (incoming) logger.info("[Server] IN " + bytes + " bytes");
            else logger.info("[Server] OUT " + bytes + " bytes");
        }
    }

    public static List<Event> getEvents() {
        List<Event> toReturn = events;
        events = new ArrayList<>();
        return toReturn;
    }

    public static void clearEvents() {
        events = new ArrayList<>();
    }

    public static class Event {
        public final String title;
        public final long duration;
        public final long delay;
        public final boolean flag;
        public Event(String title, long duration, long delay, boolean flag) {
            this.title = title;
            this.duration = duration;
            this.delay = delay;
            this.flag = flag;
        }
    }
}

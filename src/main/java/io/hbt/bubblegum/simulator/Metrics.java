package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.kademlia.activities.NetworkActivity;
import io.hbt.bubblegum.core.kademlia.activities.SystemActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Metrics {

    private static boolean recording = false;
    private static final Logger logger = LoggerFactory.getLogger(Metrics.class);
    private static final Marker marker = MarkerFactory.getMarker(LocalDateTime.now().toString());

    private static List<Event> events = new ArrayList<>();

    private static AtomicLong numCalls = new AtomicLong(0L);
    private static AtomicInteger atomicSuccesses = new AtomicInteger(0);
    private static AtomicInteger atomicFailures = new AtomicInteger(0);
    private static AtomicLong atomicBytesIn = new AtomicLong(0L);
    private static AtomicLong atomicBytesOut = new AtomicLong(0L);

    private static StringBuilder logMessages = new StringBuilder();
    private static long startOfRecording = 0;
    private static long lastFlush = System.currentTimeMillis();
    private static final int flushEvery = 10000; // ms

    private Metrics() { /* Non instantiable */ }

    public static boolean isRecording() {
        return Metrics.recording;
    }

    protected static void startRecording() {
        Metrics.startOfRecording = System.currentTimeMillis();
        Metrics.recording = true;
    }

    protected static void stopRecording() {
        Metrics.recording = false;
    }

    public static void activitySubmission(SystemActivity activity, long init, long start, long end, boolean success) {
        if(Metrics.recording) {
            Metrics.addSF(success);
            numCalls.incrementAndGet();

            boolean response = false;
            if (activity instanceof NetworkActivity && ((NetworkActivity) activity).isResponse()) response = true;
            events.add(new Event(activity.getClass().getSimpleName(), end - start, start - init, response));
        }
    }

    public static void serverSubmission(int bytes, boolean incoming) {
        if(Metrics.recording) {
            events.add(new Event("Server", 0, bytes, incoming));
            if(incoming) atomicBytesIn.addAndGet(bytes);
            else atomicBytesOut.addAndGet(bytes);
        }
    }

    public static List<Event> getEvents() {
        List<Event> toReturn = events;
        events = new ArrayList<>();
        return toReturn;
    }

    private static void addSF(boolean success) {
        if (success) atomicSuccesses.incrementAndGet();
        else atomicFailures.incrementAndGet();
    }

    private static String sfFlush() {
        long totalActivityTime = numCalls.getAndSet(0L);
        int successes = atomicSuccesses.getAndSet(0);
        int failures = atomicFailures.getAndSet(0);
        long bytesIn = atomicBytesIn.getAndSet(0L);
        long bytesOut = atomicBytesOut.getAndSet(0L);

        double successRatio;
        if(successes > 0 || failures > 0) successRatio = (double) successes / (successes + failures);
        else successRatio = 1.0;

        return totalActivityTime + "," + successes + "," + failures + "," + successRatio + "," + bytesIn + "," + bytesOut;
    }

//    public static void checkForSFFlush(long callTime) {
//        if(callTime > nextEpoch) {
//            synchronized (successes2) {
//                if(callTime > nextEpoch) {
//                    if(nextEpoch == -1) {
//                        initialEpoch = System.currentTimeMillis();
//                        nextEpoch = initialEpoch + epochDuration;
//                    }
//                    else {
//                        int successes = successes2.get();
//                        int failures = failures2.get();
//                        lastRecord = nextEpoch - epochDuration - initialEpoch;
//                        if(successes > 0 || failures > 0)
//                            successRates.put(lastRecord, (double) successes / (successes + failures));
//                        else
//                            successRates.put(lastRecord, 1.0);
//                        sfCounts.put(lastRecord, successes + "," + failures);
//                        nextEpoch += epochDuration;
//                        successes2.set(0);
//                        failures2.set(0);
//                    }
//                }
//            }
//        }
//    }

//    public static double getLatestSuccessRate() {
//        if(lastRecord == -1) return -1;
//        else return successRates.get(lastRecord);
//    }
//
//    public static String getLatestSFCount() {
//        if(lastRecord == -1) return "-,-";
//        else return sfCounts.get(lastRecord);
//    }



    protected static void addLogMessage(String msg) {
        synchronized (logMessages) {
            if(System.currentTimeMillis() > lastFlush + flushEvery) flushLogMessages();
            logMessages.append(msg + "\n");
        }
    }

    private static void flushLogMessages() {
        String toWrite;
        synchronized (logMessages) {
            toWrite = logMessages.toString();
            logMessages.setLength(0);
            lastFlush = System.currentTimeMillis();
        }

        if(toWrite != null && toWrite.length() > 0) logger.info(marker, toWrite.trim());
    }

    public static void runPeriodicCalls(String additional) {
        writeStatusToLog(additional);
        if(System.currentTimeMillis() > lastFlush + flushEvery) flushLogMessages();
    }

    public static String getStatusLogHeader() {
        return "time,rpcs,successes,failures,successRate,bytesIn,bytesOut,numProcessors,systemLoadAvg,processCPULoad,systemCPULoad,freeMemory,maxMemory,totalMemory";
    }

    public static void writeStatusToLog(String additional) {
        // time,totalRPC,localSuccess,localFailures,localSuccessRatio,localBytesIn,localBytesOut
        StringBuilder toWrite = new StringBuilder();
        long timeHeader = System.currentTimeMillis() - Metrics.startOfRecording;
        OperatingSystemMXBean operatingSystemMXBeanJLang = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean operatingSystemMXBeanSun =
            ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();

        toWrite.append(timeHeader + ",");
        toWrite.append(sfFlush() + ",");

        toWrite.append(operatingSystemMXBeanJLang.getAvailableProcessors() + ",");
        double sysLoadAvg = operatingSystemMXBeanJLang.getSystemLoadAverage();
        toWrite.append(sysLoadAvg < 0 ? 0 : sysLoadAvg + ",");

        double processCPULoad = operatingSystemMXBeanSun.getProcessCpuLoad();
        toWrite.append(processCPULoad < 0 ? 0 : processCPULoad + ",");
        double sysCPULoad = operatingSystemMXBeanSun.getSystemCpuLoad();
        toWrite.append(sysCPULoad < 0 ? 0 : sysCPULoad + ",");

        toWrite.append(runtime.freeMemory() + ",");
        toWrite.append(runtime.maxMemory() + ",");
        toWrite.append(runtime.totalMemory());

        if(additional.length() > 0) toWrite.append("," + additional);
        addLogMessage(toWrite.toString());
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

package io.hbt.bubblegum.core.auxiliary.logging;

import java.io.File;
import java.util.HashMap;

public class LoggingManager {
    private static LoggingManager instance;
    private HashMap<String, Logger> loggers;
    protected final String logFolder = ".logs/";

    private LoggingManager() {
        this.loggers = new HashMap<>();
        File logFolder = new File(this.logFolder);
        if(!logFolder.exists() || !logFolder.isDirectory()) logFolder.mkdirs();
        for(File file: logFolder.listFiles()) {
            if (!file.isDirectory()) file.delete();
        }
    }

    public static synchronized LoggingManager getInstance() {
        if(LoggingManager.instance == null) LoggingManager.instance = new LoggingManager();
        return LoggingManager.instance;
    }

    public static synchronized Logger getLogger(String id) {
        String idx = "asd";
        if(!LoggingManager.getInstance().loggers.containsKey(idx)) LoggingManager.getInstance().loggers.put(idx, new Logger(idx));
        return LoggingManager.getInstance().loggers.get(idx);
    }
}

package io.hbt.bubblegum.core.auxiliary.logging;

import java.io.File;
import java.util.HashMap;

public class LoggingManager {
    private static LoggingManager instance;
    private HashMap<String, Logger> loggers;
    protected final String logFolder = "logs/";

    private LoggingManager() {
        this.loggers = new HashMap<>();
        for(File file: new File(logFolder).listFiles()) {
            if (!file.isDirectory()) file.delete();
        }
    }

    public static synchronized LoggingManager getInstance() {
        if(LoggingManager.instance == null) LoggingManager.instance = new LoggingManager();
        return LoggingManager.instance;
    }

    public static synchronized Logger getLogger(String id) {
        if(!LoggingManager.getInstance().loggers.containsKey(id)) LoggingManager.getInstance().loggers.put(id, new Logger(id));
        return LoggingManager.getInstance().loggers.get(id);
    }
}

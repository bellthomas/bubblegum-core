package io.hbt.bubblegum.core.auxiliary.logging;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private String id;
    private File f;
    protected Logger(String id) {
        this.id = id;
        this.f = new File(".logs/" + id + ".log");
        this.f.getParentFile().mkdirs();
    }

    public synchronized void logMessage(String message) {
        try (FileOutputStream fos = new FileOutputStream(this.f,true)) {
            try (PrintWriter out = new PrintWriter(fos)) {
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Date date = new Date();
                out.println("[" + dateFormat.format(date) + "] " + message);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

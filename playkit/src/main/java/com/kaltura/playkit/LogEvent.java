package com.kaltura.playkit;

/**
 * Created by zivilan on 24/11/2016.
 */

public class LogEvent implements PKEvent {
    public final String log;
    public final LogType type;

    public enum LogType
    {LogEvent};

    public LogEvent(String log) {
        this.type = LogType.LogEvent;
        this.log = log;
    }

    @Override
    public Enum eventType() {
        return this.type;
    }

    public interface Listener {
        void onLogEvent(String log);
    }
}

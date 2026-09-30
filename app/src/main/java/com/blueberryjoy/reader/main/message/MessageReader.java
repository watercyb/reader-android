package com.blueberryjoy.reader.main.message;

public record MessageReader(String action, int message) implements MessageNode {
    public final static String ACTION_CONTEXT = "context";
    public final static String ACTION_PAUSE = "pause";
    public final static String ACTION_START = "start";
    public final static String ACTION_ERROR = "error";
    public final static int ACTION_ERROR_TTS = 0;

}

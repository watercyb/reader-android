package com.blueberryjoy.reader.main.message;

public record MessageBluetooth(String action, String message) implements MessageNode {
    public final static String ACTION_DISCONNECT = "disconnect";
    public final static String ACTION_CONNECT = "connect";

}

package com.blueberryjoy.reader.utils;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class RunTime {
    private static final HashMap<Integer, Long> times = new HashMap<>();

    public static void start(int key) {
        times.put(key, System.nanoTime());
    }

    @NonNull
    public static String end(int key) {
        long endTime = System.nanoTime();
        Long startTime = times.get(key);
        times.put(key, endTime);
        if (startTime == null)
            return "N/A";
        long time = (endTime - startTime);
        if (time < 10000)
            return time + "ns";
        time /= 1000;
        if (time < 10000)
            return time + "μs";
        time /= 1000;
        if (time < 10000)
            return time + "ms";
        time /= 1000;
        return time + "s";
    }

    public static void endAndShow(int key) {
        Message.log(end(key));
    }
}

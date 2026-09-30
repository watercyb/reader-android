package com.blueberryjoy.reader.utils;

import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.settings.ReaderSettings;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ad_Blocker {
    private static final ConcurrentHashMap.KeySetView<String, Boolean> adHosts = ConcurrentHashMap.newKeySet(500000);
    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String, Boolean> adHostCache = new ConcurrentHashMap<>();

    public static void initialize() {
        new Thread(() -> {
            try {
                InputStream is = App.Companion.getAppContext().getAssets().open("hosts.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("//")) {
                        adHosts.add(line);
                    }
                }
                reader.close();
                isLoaded.set(true);
            } catch (IOException e) {
                Message.toastErr("Unable to load ad blocking hosts.");
            }
        }).start();
    }

    public static boolean isAdHost(String url) {
        if (!ReaderSettings.getIsBlockAds()) return false;
        try {
            String host = new URI(url).getHost();
            if (host == null) return false;
            if (adHostCache.containsKey(host))
                return Boolean.TRUE.equals(adHostCache.get(host));
            if (checkHost(host)) {
                adHostCache.put(host, true);
                return true;
            } else if (isLoaded.get()) {
                adHostCache.put(host, false);
            }
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean checkHost(String host) {
        host = host.toLowerCase();
        int idx = host.lastIndexOf('.');
        while (idx >= 0) {
            if (adHosts.contains(host.substring(idx + 1)))
                return true;
            idx = host.lastIndexOf('.', idx - 1);
        }
        return adHosts.contains(host);
    }

    @NonNull
    public static WebResourceResponse createEmptyWebResourceResponse() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}

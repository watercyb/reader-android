package com.blueberryjoy.reader.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.App;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Favicon {
    @NonNull
    private static final HashMap<String, Bitmap> icons = new HashMap<>();

    public static void initial() {
        icons.clear();
        icons.put("null", getDefaultFavicon());
    }

    public static void setIcon(String k, Bitmap bitmap) {
        icons.put(k, bitmap);
    }

    @Nullable
    public static Bitmap getIconFromK(@NonNull String k) {
        if (icons.containsKey(k)) {
            return icons.get(k);
        } else {
            Bitmap bitmap = FileStorage.getIconFromFile(k);
            if (bitmap == null) return icons.get("null");
            setIcon(k, bitmap);
            return bitmap;
        }
    }

    @Nullable
    public static Bitmap getIconFromUrl(@NonNull String url) {
        String k = FileStorage.urlToFilename(url);
        return getIconFromK(k);
    }

    @Nullable
    public static Bitmap getDefaultFavicon() {
        AssetManager assetManager = App.Companion.getAppContext().getResources().getAssets();
        try (InputStream inputStream = assetManager.open("fav.png")) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }
}

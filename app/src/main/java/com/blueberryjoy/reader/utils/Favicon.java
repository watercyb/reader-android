package com.blueberryjoy.reader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.R;

import java.util.HashMap;

public class Favicon {
    @NonNull
    private static final HashMap<String, Bitmap> icons = new HashMap<>();

    public static void initial() {
        icons.clear();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        icons.put("null", BitmapFactory.decodeResource(App.Companion.getAppContext().getResources(), R.mipmap.ic_favicon, options));
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
            setIcon(k, bitmap);
            return bitmap;
        }
    }

    @Nullable
    public static Bitmap getIconFromUrl(@NonNull String url) {
        String k = FileStorage.urlToFilename(url);
        return getIconFromK(k);
    }
}

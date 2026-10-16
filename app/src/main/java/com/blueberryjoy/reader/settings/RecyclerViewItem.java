package com.blueberryjoy.reader.settings;

import android.graphics.Bitmap;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RecyclerViewItem {
    @NonNull
    final String locale;
    final String category;
    final String name;
    @NonNull
    final String title;
    @Nullable
    Bitmap icon;
    final int voiceIdx;
    int count;

    public RecyclerViewItem(@NonNull Voice voice, Bitmap icon, int voiceIdx, int count) {
        locale = voice.getLocale().toString();
        category = locale.split("_")[0];
        name = voice.getName();
        title = locale + ": " + name;
        this.icon = icon;
        this.voiceIdx = voiceIdx;
        this.count = count;
    }

    public boolean isItem() {
        return count == 0;
    }
}

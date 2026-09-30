package com.blueberryjoy.reader.settings;

import android.graphics.Bitmap;
import android.speech.tts.Voice;

public class RecyclerViewItem {
    final String locale;
    final String category;
    final String name;
    final String title;
    Bitmap icon;
    final int voiceIdx;
    int count;

    public RecyclerViewItem(Voice voice, Bitmap icon, int voiceIdx, int count) {
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

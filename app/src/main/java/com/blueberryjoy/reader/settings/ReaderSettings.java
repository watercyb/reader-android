package com.blueberryjoy.reader.settings;

import static android.content.Context.MODE_PRIVATE;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.blueberryjoy.reader.App;

import java.util.Locale;

public class ReaderSettings {
    @Nullable
    private static String voiceName;
    private static float voiceSpeed = 1;
    private static float voicePitch = 1;
    @Nullable
    public static String TTSEngine;
    private static boolean isShowReadingText;
    private static boolean isShowContentInNotification;
    private static boolean isStopWhenAudioFocusChanged;
    private static boolean isRequireAudioFocusWhenPlay;
    private static boolean isReloadPageAfterJS = false;
    @Nullable
    private static String lang;
    private static boolean isStopWhenBlueToothDisconnected = true;
    private static boolean isSkipURL = true;
    private static boolean isBlockAds = true;
    /*
    0: url
    1: play button
    2: import_export_bookmark
    3: bookmark open in new tab
    4: download open in new tab
    5: reload
    6: voice setting
    7: choose voice engine
    8: voice testing text
    9: choose voice
    10: history open in new tab
    11: delete suggestion
    12: scroll to text
    13: text not matching.
    */
    private static long tips = 0;
    private static boolean isShowWelcome;
    private static boolean isReduceRepeatText = true;

    public static void initial() {
        SharedPreferences sharedPref = App.Companion.getAppContext().getSharedPreferences("data", MODE_PRIVATE);
        voiceName = sharedPref.getString("voiceName", "en-US-language");
        voiceSpeed = sharedPref.getFloat("voiceSpeed", 1);
        voicePitch = sharedPref.getFloat("voicePitch", 1);
        TTSEngine = sharedPref.getString("TTSEngine", "");
        isShowReadingText = sharedPref.getBoolean("isShowReadingText", false);
        isShowContentInNotification = sharedPref.getBoolean("isShowContentInNotification", true);
        isStopWhenAudioFocusChanged = sharedPref.getBoolean("isStopWhenAudioFocusChanged", true);
        isRequireAudioFocusWhenPlay = sharedPref.getBoolean("isRequireAudioFocusWhenPlay", true);
        isReloadPageAfterJS = sharedPref.getBoolean("isReloadPageAfterJS", false);
        lang = sharedPref.getString("lang", "System");
        checkLang();
        isStopWhenBlueToothDisconnected = sharedPref.getBoolean("isStopWhenBlueToothDisconnected", true);
        if (isStopWhenBlueToothDisconnected && noBluetoothPermission()) {
            isStopWhenBlueToothDisconnected = false;
            sharedPref.edit().putBoolean("isStopWhenBlueToothDisconnected", isStopWhenBlueToothDisconnected).apply();
        }
        isSkipURL = sharedPref.getBoolean("isSkipURL", true);
        isBlockAds = sharedPref.getBoolean("isBlockAds", true);
        tips = sharedPref.getLong("tips", 0);
        isShowWelcome = sharedPref.getBoolean("isShowWelcome", true);
        isReduceRepeatText = sharedPref.getBoolean("isReduceRepeatText", true);
    }

    @Nullable
    public static String getTTSEngine() {
        return TTSEngine;
    }

    public static void saveEngine(String engine) {
        ReaderSettings.TTSEngine = engine;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putString("TTSEngine", engine).apply();
    }

    public static void saveVoice(String voiceName) {
        ReaderSettings.voiceName = voiceName;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putString("voiceName", voiceName).apply();
    }

    public static String getVoice() {
        if (voiceName == null)
            voiceName = App.Companion.getAppContext().getSharedPreferences("data", MODE_PRIVATE).getString("voiceName", "en-US-language");
        return voiceName;
    }

    public static float getSpeed() {
        return ReaderSettings.voiceSpeed;
    }

    public static void saveSpeed(float voiceSpeed) {
        ReaderSettings.voiceSpeed = voiceSpeed;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putFloat("voiceSpeed", voiceSpeed).apply();
    }


    public static float getPitch() {
        return ReaderSettings.voicePitch;
    }

    public static void savePitch(float voicePitch) {
        ReaderSettings.voicePitch = voicePitch;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putFloat("voicePitch", voicePitch).apply();
    }

    public static boolean getIsShowReadingText() {
        return isShowReadingText;
    }

    public static void saveIsShowReadingText(boolean isShowReadingText) {
        ReaderSettings.isShowReadingText = isShowReadingText;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isShowReadingText", isShowReadingText).apply();
    }

    public static boolean getIsShowContentInNotification() {
        return isShowContentInNotification;
    }

    public static void saveIsShowContentInNotification(boolean isShowContentInNotification) {
        ReaderSettings.isShowContentInNotification = isShowContentInNotification;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isShowContentInNotification", isShowContentInNotification).apply();
    }

    public static boolean getIsStopWhenAudioFocusChanged() {
        return isStopWhenAudioFocusChanged;
    }

    public static void saveIsStopWhenAudioFocusChanged(boolean isStopWhenAudioFocusChanged) {
        ReaderSettings.isStopWhenAudioFocusChanged = isStopWhenAudioFocusChanged;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isStopWhenAudioFocusChanged", isStopWhenAudioFocusChanged).apply();
    }

    public static boolean getIsRequireAudioFocusWhenPlay() {
        return isRequireAudioFocusWhenPlay;
    }

    public static void saveIsRequireAudioFocusWhenPlay(boolean isRequireAudioFocusWhenPlay) {
        ReaderSettings.isRequireAudioFocusWhenPlay = isRequireAudioFocusWhenPlay;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isRequireAudioFocusWhenPlay", isRequireAudioFocusWhenPlay).apply();
    }

    public static boolean getIsReloadPageAfterJS() {
        return isReloadPageAfterJS;
    }

    public static void saveIsReloadPageAfterJS(boolean isReloadPageAfterJS) {
        ReaderSettings.isReloadPageAfterJS = isReloadPageAfterJS;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isReloadPageAfterJS", isReloadPageAfterJS).apply();
    }

    @Nullable
    public static String getLang() {
        return lang;
    }

    public static void saveLang(@NonNull String lang) {
        if (!lang.equals(ReaderSettings.lang)) {
            ReaderSettings.lang = lang;
            App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putString("lang", lang).apply();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }
    }

    private static void checkLang() {
        if (lang == null || lang.equals("System")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LocaleManager localeManager = (LocaleManager) App.Companion.getAppContext().getSystemService(Context.LOCALE_SERVICE);
                LocaleList systemLocales = localeManager.getSystemLocales();
                String languageTag = systemLocales.get(0).toLanguageTag();
                ReaderSettings.saveLang(purseLang(languageTag));
            } else {
                String languageTag = Locale.getDefault().toLanguageTag();
                ReaderSettings.saveLang(purseLang(languageTag));
            }
        } else {
            String languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            if (!lang.equals(purseLang(languageTag)))
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }
    }

    public static String purseLang(String lang) {
        if (lang.startsWith("zh")) {
            String langTag = Locale.getDefault().toLanguageTag();
            if (langTag.contains("Hant")) return "zh-TW";
            if (langTag.contains("Hans")) return "zh";
            if (langTag.endsWith("TW") || langTag.endsWith("HK") || langTag.endsWith("MO"))
                return "zh-TW";
            return "zh";
        } else {
            return "en";
        }
    }

    public static boolean getIsStopWhenBlueToothDisconnected() {
        return isStopWhenBlueToothDisconnected;
    }

    public static boolean saveIsStopWhenBlueToothDisconnected(boolean isStopWhenBlueToothDisconnected) {
        if (isStopWhenBlueToothDisconnected && noBluetoothPermission()) return false;
        ReaderSettings.isStopWhenBlueToothDisconnected = isStopWhenBlueToothDisconnected;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isStopWhenBlueToothDisconnected", isStopWhenBlueToothDisconnected).apply();
        return true;
    }

    private static boolean noBluetoothPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(App.Companion.getAppContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED;
    }

    public static boolean getIsSkipURL() {
        return isSkipURL;
    }

    public static void saveIsSkipURL(boolean isSkipURL) {
        if (ReaderSettings.isSkipURL != isSkipURL) {
            ReaderSettings.isSkipURL = isSkipURL;
            App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isSkipURL", isSkipURL).apply();
        }
    }

    public static boolean getIsBlockAds() {
        return isBlockAds;
    }

    public static void saveIsBlockAds(boolean isBlockAds) {
        if (ReaderSettings.isBlockAds != isBlockAds) {
            ReaderSettings.isBlockAds = isBlockAds;
            App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isBlockAds", isBlockAds).apply();
        }
    }

    public static boolean getTips(int i) {
        return (tips & (1L << i)) != 0;
    }

    public static void saveTips(int i) {
        tips |= 1L << i;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putLong("tips", tips).apply();
    }

    public static void resetTips() {
        tips = 0;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putLong("tips", tips).apply();
    }

    public static boolean getIsShowWelcome() {
        return isShowWelcome;
    }

    public static void saveIsShowWelcome(boolean isShowWelcome) {
        ReaderSettings.isShowWelcome = isShowWelcome;
        App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isShowWelcome", isShowWelcome).apply();
    }

    public static String getVoiceName() {
        if (voiceName == null) return "";
        return voiceName;
    }

    public static boolean getIsReduceRepeatText() {
        return isReduceRepeatText;
    }

    public static void saveIsReduceRepeatText(boolean isReduceRepeatText) {
        if (ReaderSettings.isReduceRepeatText != isReduceRepeatText) {
            ReaderSettings.isReduceRepeatText = isReduceRepeatText;
            App.Companion.getAppContext().getSharedPreferences("data", 0).edit().putBoolean("isReduceRepeatText", isReduceRepeatText).apply();
        }
    }
}
package com.blueberryjoy.reader.utils;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.BuildConfig;

public class Message {

    private static Toast toast;
    private static Toast findInPage;

    public static void toast(String str) {
        if (BuildConfig.DEBUG) {
            if (toast != null) toast.cancel();
            toast = Toast.makeText(App.Companion.getAppContext(), str, Toast.LENGTH_SHORT);
            toast.show();
            LogList.appendToInternalFile("(" + App.Companion.getAppContext().getClass().getSimpleName() + ")" + str);
        }
    }

    public static void toastMSG(String str) {
        Toast.makeText(App.Companion.getAppContext(), str, Toast.LENGTH_SHORT).show();
    }

    public static void toastErr(String str) {
        Toast.makeText(App.Companion.getAppContext(), str, Toast.LENGTH_SHORT).show();
        if (BuildConfig.DEBUG)
            LogList.appendToInternalFile("(ERR " + App.Companion.getAppContext().getClass().getSimpleName() + ")" + str);
    }

    public static void toastFindInPage(String str) {
        if (findInPage != null) findInPage.cancel();
        findInPage = Toast.makeText(App.Companion.getAppContext(), str, Toast.LENGTH_SHORT);
        findInPage.show();
    }

    public static void log(@NonNull String str) {
        if (BuildConfig.DEBUG) {
            Log.w("testLog", str);
            LogList.appendToInternalFile(str);
        }
    }
}

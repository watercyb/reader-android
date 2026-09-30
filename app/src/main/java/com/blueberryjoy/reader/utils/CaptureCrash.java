package com.blueberryjoy.reader.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CaptureCrash implements Thread.UncaughtExceptionHandler {

    public CaptureCrash(Context context) {
        //api<30 do not need this permission?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder
                    .setTitle("Need Files Access Permission")
                    .setCancelable(false)
                    .setPositiveButton("ok", (dialog, id) -> {
                        Intent requireFilesAccessPermission = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                        requireFilesAccessPermission.setData(uri);
                        context.startActivity(requireFilesAccessPermission);
                    })
                    .setNegativeButton("cancel", (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        saveCrashLog(e);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void saveCrashLog(@NonNull Throwable e) {
        try {
            FileStorage.checkDirExist(Environment.getExternalStorageDirectory() + "/Reader/");
            File logFile = new File(Environment.getExternalStorageDirectory() + "/Reader/crash_log_" + getCurrentDate() + ".log");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(logFile, true));
            outputStreamWriter.write(getCurrentDate());
            outputStreamWriter.write("\n");
            printFullStackTrace(e, outputStreamWriter);
            outputStreamWriter.close();
        } catch (IOException err) {
            Message.log("CrashLog: " + err);
        }
    }

    private void printFullStackTrace(@NonNull Throwable e, @NonNull OutputStreamWriter outputStreamWriter) throws IOException {
        outputStreamWriter.write(e.toString());
        for (StackTraceElement element : e.getStackTrace()) {
            outputStreamWriter.write(element.toString() + "\n");
        }
        Throwable cause = e.getCause();
        if (cause != null) {
            outputStreamWriter.write("Caused by:\t");
            printFullStackTrace(cause, outputStreamWriter);
        }
        outputStreamWriter.write("\n\n");
    }

    @NonNull
    private String getCurrentDate() {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        return date.format(new Date());
    }
}
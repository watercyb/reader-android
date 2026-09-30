package com.blueberryjoy.reader.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.settings.ReaderSettings;

public class GlobalTipDialog {

    private static Dialog currentDialog;

    public static void show(Context context, View view, String content, int id, int x, int y, int gravity, boolean isFirst) {
        if (ReaderSettings.getTips(id)) return;
        if (currentDialog != null && currentDialog.isShowing()) dismiss();

        currentDialog = new Dialog(context);
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        @SuppressLint("InflateParams") View tip = LayoutInflater.from(context).inflate(R.layout.dialog_tip, null);

        TextView tvContent = tip.findViewById(R.id.tvContent);
        TextView btnCancel = tip.findViewById(R.id.btnCancel);

        tvContent.setText(content);

        btnCancel.setOnClickListener(v -> {
            ReaderSettings.saveTips(id);
            currentDialog.dismiss();
        });

        currentDialog.setContentView(tip);
        currentDialog.setCancelable(true);

        Window window = currentDialog.getWindow();
        if (window != null) {
            float density = context.getResources().getDisplayMetrics().density;
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = gravity;
            params.x = (int) (x * density);
            params.y = (int) (y * density);
            window.setAttributes(params);
        }

        currentDialog.show();

        if (view != null)
            shake(view, isFirst);
    }

    public static void show(Context context, View[] views, String[] content, int[] ids, int[] xs, int[] ys, int[] gravities, int idx, boolean isFirst) {
        if (ReaderSettings.getTips(ids[idx])) {
            if (idx < ids.length - 1)
                show(context, views, content, ids, xs, ys, gravities, idx + 1, isFirst);
            return;
        }

        if (currentDialog != null && currentDialog.isShowing()) dismiss();

        currentDialog = new Dialog(context);
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.dialog_tip, null);

        TextView tvContent = view.findViewById(R.id.tvContent);
        TextView btnCancel = view.findViewById(R.id.btnCancel);

        tvContent.setText(content[idx]);

        btnCancel.setOnClickListener(v -> {
            ReaderSettings.saveTips(ids[idx]);
            currentDialog.dismiss();
            if (idx < ids.length - 1)
                show(context, views, content, ids, xs, ys, gravities, idx + 1, false);
        });


        currentDialog.setContentView(view);
        currentDialog.setCancelable(true);

        currentDialog.setOnCancelListener(dialog -> {
            if (idx < ids.length - 1)
                show(context, views, content, ids, xs, ys, gravities, idx + 1, false);
        });

        Window window = currentDialog.getWindow();
        if (window != null) {
            float density = context.getResources().getDisplayMetrics().density;
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = gravities[idx];
            params.x = (int) (xs[idx] * density);
            params.y = (int) (ys[idx] * density);
            window.setAttributes(params);
        }

        currentDialog.show();
        if (views[idx] != null)
            shake(views[idx], isFirst);
    }

    private static void shake(View view, boolean isFirst) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator shake1 = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, -15f, 10f, -10f, 0f);
        shake1.setDuration(500);
        ObjectAnimator shake2 = shake1.clone();
        shake2.setDuration(500);
        if (isFirst) {
            ObjectAnimator shake3 = shake1.clone();
            shake3.setDuration(500);
            set.playSequentially(shake1, ObjectAnimator.ofFloat(view, "translationX", 0f).setDuration(200), shake2, ObjectAnimator.ofFloat(view, "translationX", 0f).setDuration(200), shake3);
        } else {
            set.playSequentially(shake1, ObjectAnimator.ofFloat(view, "translationX", 0f).setDuration(200), shake2);
        }
        set.start();
    }

    public static void dismiss() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }
}
package com.blueberryjoy.reader.main.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcel;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.main.utils.JavaScriptHandler;
import com.blueberryjoy.reader.main.window.WindowList;

public class MyWebView extends NestedScrollWebView {
    private int idx = -1;
    private CountDownTimer timer;
    private int touched_x = 0;
    private int touched_y = 0;
    private float scale = 1;

    public OnDoubleTapListener onDoubleTapListener;
    private GestureDetector gestureDetector;

    public MyWebView(@NonNull Context context, boolean isPrivate) {
        super(context);
        initial(isPrivate);
    }

    public MyWebView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        initial(false);
    }

    public MyWebView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                touched_x = (int) ((event.getX() + getScrollX()) / getScaleFactor());
                touched_y = (int) ((event.getY() + getScrollY()) / getScaleFactor());
                evaluateJavascript("(function(x,y) { if (typeof pageReader !== 'undefined') return pageReader.getElementByPosition(x,y); })(" + touched_x + ", " + touched_y + ");", html -> {
                    if (html.equals("null")) return;
                    int id;
                    try {
                        id = Integer.parseInt(html);
                    } catch (Exception e) {
                        id = -1;
                    }
                    if (id >= 0) {
                        idx = id;
                        timer.start();
                    }
                });
                break;
            case (MotionEvent.ACTION_UP):
                performClick();
                break;
            case (MotionEvent.ACTION_MOVE):
            case (MotionEvent.ACTION_CANCEL):
                break;
        }
        gestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    public interface OnDoubleTapListener {
        void onDoubleTap(int idx, int x, int y);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initial(boolean isPrivate) {
        WebSettings webSettings = getSettings();
        webSettings.setDefaultFontSize(16);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        if (isPrivate) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE))
                WebViewCompat.setProfile(this, "incognito_profile_" + WindowList.getPrivateProfileIdx());
        }

        webSettings.setTextZoom(100);

        gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent event) {
                if (onDoubleTapListener != null) {
                    onDoubleTapListener.onDoubleTap(idx, touched_x, touched_y);
                    idx = -1;
                }
                return true;
            }
        });

        timer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                idx = -1;
            }
        };

        scale = getResources().getDisplayMetrics().density;
    }

    public void setScaleFactor(float scale) {
        this.scale = scale;
    }

    public float getScaleFactor() {
        return scale;
    }

    public void updateThemeColor() {
        int currentNightMode = App.Companion.getAppContext().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                JavaScriptHandler.updateThemeColor(this, false);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                JavaScriptHandler.updateThemeColor(this, true);
                break;
        }
    }

    public String getState() {
        Bundle bundle = new Bundle();
        this.saveState(bundle);
        Parcel parcel = Parcel.obtain();
        bundle.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public void setState(String state) {
        byte[] bytes = Base64.decode(state, Base64.DEFAULT);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        Bundle bundle = Bundle.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        this.restoreState(bundle);
    }
}

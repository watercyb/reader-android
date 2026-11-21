package com.blueberryjoy.reader.main.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcel;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.main.utils.JavaScriptHandler;
import com.blueberryjoy.reader.main.window.WindowList;
import com.blueberryjoy.reader.utils.FileStorage;
import com.blueberryjoy.reader.utils.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

public class MyWebView extends NestedScrollWebView {
    private int idx = -1;
    private CountDownTimer timer;
    private int touched_x = 0;
    private int touched_y = 0;
    private float scale = 1;
    private final int[] touchedPosition = new int[4];
    private boolean isPrivate = false;

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

                JavaScriptHandler.getElementByPosition(this, touched_x, touched_y, result -> {
                    int id;
                    try {
                        id = new JSONObject(result).getInt("idx");
                    } catch (JSONException ignored) {
                        id = -1;
                    }
                    if (id >= 0) {
                        idx = id;
                        timer.start();
                    }
                });

                touchedPosition[0] = (int) event.getX();
                touchedPosition[1] = (int) event.getY();
                touchedPosition[2] = touched_x;
                touchedPosition[3] = touched_y;

                break;
            case (MotionEvent.ACTION_UP):
                if (hasNoUrl() && onDoubleTapListener != null)
                    onDoubleTapListener.onDoubleTap(-1, -1, -1);
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
        this.isPrivate = isPrivate;

        WebSettings webSettings = getSettings();
        webSettings.setDefaultFontSize(16);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        setBackgroundColor(Color.TRANSPARENT);

        if (isPrivate) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE))
                WebViewCompat.setProfile(this, "incognito_profile_" + WindowList.getPrivateProfileIdx());
        }

        webSettings.setTextZoom(100);

        gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent event) {
                if (onDoubleTapListener != null && !hasNoUrl()) {
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

        HashSet<String> set = new HashSet<>();
        set.add("*");
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(this, FileStorage.getJSCode("tracker.js"), set);
        } else {
            Message.log("Cannot inject DOCUMENT_START_SCRIPT");
        }
    }

    public void setScaleFactor(float scale) {
        this.scale = scale;
    }

    public float getScaleFactor() {
        return scale;
    }

    public void updateThemeColor() {
        int currentNightMode = App.Companion.getAppContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
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

    public int[] getTouchedPosition() {
        return touchedPosition;
    }

    public void performDoubleTap(int idx, int x, int y) {
        if (onDoubleTapListener != null) onDoubleTapListener.onDoubleTap(idx, x, y);
    }

    public void longClickAt(int x, int y) {
        long holdMs = ViewConfiguration.getLongPressTimeout() + 200L;
        final long downTime = SystemClock.uptimeMillis();

        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        dispatchTouchEvent(down);
        down.recycle();

        postDelayed(() -> {
            long upTime = SystemClock.uptimeMillis();
            MotionEvent up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0);
            up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            dispatchTouchEvent(up);
            up.recycle();
        }, holdMs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!hasNoUrl()) return;
        if (isPrivate) {
            setPrivateBackground(canvas);
        } else {
            setNormalBackground(canvas);
        }
    }

    public boolean hasNoUrl() {
        String currentUrl = getUrl();
        return currentUrl == null || currentUrl.trim().isEmpty();
    }

    private void setPrivateBackground(Canvas canvas) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int backgroundColor = isDarkMode ? Color.rgb(24, 27, 33) : Color.rgb(242, 243, 245);
        int titleColor = isDarkMode ? Color.WHITE : Color.rgb(48, 52, 59);
        int descriptionColor = isDarkMode ? Color.rgb(175, 182, 195) : Color.rgb(102, 109, 120);
        int hintColor = isDarkMode ? Color.rgb(140, 150, 165) : Color.rgb(133, 139, 149);

        // Background
        canvas.drawColor(backgroundColor);

        // icon
        float density = getResources().getDisplayMetrics().density;
        float centerX = getWidth() / 2f;
        float iconY = getHeight() * 0.24f;
        float iconSize = 80f * density;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_open_private);
        float left = centerX - iconSize / 2f;
        float top = iconY - iconSize / 2f;
        RectF dst = new RectF(left, top, left + iconSize, top + iconSize);
        canvas.drawBitmap(bitmap, null, dst, paint);

        // Title
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(titleColor);
        paint.setTextSize(22f * density);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        float titleY = iconY + iconSize / 2f + 32f * density;
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_title_private), centerX, titleY, paint);

        // Description
        paint.setColor(descriptionColor);
        paint.setTextSize(14f * density);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        float lineHeight = 22f * density;
        float textY = titleY + 32f * density;
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_text_private_1), centerX, textY, paint);
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_text_private_2), centerX, textY + lineHeight, paint);

        // Bottom hint
        paint.setColor(hintColor);
        paint.setTextSize(12f * density);
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_hint), centerX, textY + lineHeight * 3f, paint);
    }

    public void setNormalBackground(Canvas canvas) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int backgroundColor = isDarkMode ? Color.rgb(30, 33, 40) : Color.rgb(255, 255, 255);
        int titleColor = isDarkMode ? Color.WHITE : Color.rgb(48, 52, 59);
        int descriptionColor = isDarkMode ? Color.rgb(175, 182, 195) : Color.rgb(102, 109, 120);
        int hintColor = isDarkMode ? Color.rgb(140, 150, 165) : Color.rgb(133, 139, 149);

        // Background
        canvas.drawColor(backgroundColor);

        // Globe icon
        float density = getResources().getDisplayMetrics().density;
        float centerX = getWidth() / 2f;
        float iconY = getHeight() * 0.24f;
        float iconSize = 80f * density;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_emply_page_icon);
        float left = centerX - iconSize / 2f;
        float top = iconY - iconSize / 2f;
        RectF dst = new RectF(left, top, left + iconSize, top + iconSize);
        canvas.drawBitmap(bitmap, null, dst, paint);

        // Title
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(titleColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(22f * density);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        float titleY = iconY + iconSize / 2f + 32f * density;
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_title), centerX, titleY, paint);

        // Description
        paint.setColor(descriptionColor);
        paint.setTextSize(14f * density);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        float descriptionY = titleY + 30f * density;
        float lineHeight = 22f * density;
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_text), centerX, descriptionY, paint);

        // Hint
        paint.setColor(hintColor);
        paint.setTextSize(12f * density);
        canvas.drawText(App.Companion.getAppContext().getString(R.string.empty_page_hint), centerX, descriptionY + lineHeight + 18f * density, paint);
    }
}

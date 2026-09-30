package com.blueberryjoy.reader.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.main.MainActivity;
import com.blueberryjoy.reader.main.webview.MyWebView;
import com.blueberryjoy.reader.main.window.WindowList;
import com.blueberryjoy.reader.utils.Message;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class GeneralSetting extends AppCompatActivity {
    private boolean[] isChanged;
    private Spinner lang;
    private final String[] langNames = new String[]{"English", "中文(zh-CN)", "中文(zh-TW)"};
    private final String[] langCodes = new String[]{"en", "zh", "zh-TW"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        isChanged = new boolean[2];

        CheckBox readingBox = findViewById(R.id.readingBox);
        CheckBox notification = findViewById(R.id.notification);
        CheckBox audioFocusChanged = findViewById(R.id.audioFocusChanged);
        CheckBox requireAudioFocus = findViewById(R.id.requireAudioFocus);
        CheckBox reloadAfterJS = findViewById(R.id.reloadAfterJS);
        lang = findViewById(R.id.lang);
        CheckBox stopAfterBTDisc = findViewById(R.id.stopAfterBTDisc);
        CheckBox skipURL = findViewById(R.id.skipURL);
        CheckBox blockAds = findViewById(R.id.blockAds);
        TextView resetTips = findViewById(R.id.resetTips);
        TextView clearBrowsingData = findViewById(R.id.clearBrowsingData);

        readingBox.setChecked(ReaderSettings.getIsShowReadingText());
        notification.setChecked(ReaderSettings.getIsShowContentInNotification());
        requireAudioFocus.setChecked(ReaderSettings.getIsRequireAudioFocusWhenPlay());
        if (!ReaderSettings.getIsRequireAudioFocusWhenPlay()) {
            ReaderSettings.saveIsStopWhenAudioFocusChanged(false);
            audioFocusChanged.setEnabled(false);
        }
        audioFocusChanged.setChecked(ReaderSettings.getIsStopWhenAudioFocusChanged());
        reloadAfterJS.setChecked(ReaderSettings.getIsReloadPageAfterJS());
        stopAfterBTDisc.setChecked(ReaderSettings.getIsStopWhenBlueToothDisconnected());
        skipURL.setChecked(ReaderSettings.getIsSkipURL());
        blockAds.setChecked(ReaderSettings.getIsBlockAds());

        initialLang();

        readingBox.setOnClickListener(v -> {
            ReaderSettings.saveIsShowReadingText(readingBox.isChecked());
            isChanged[0] ^= true;
        });

        notification.setOnClickListener(v -> {
            ReaderSettings.saveIsShowContentInNotification(notification.isChecked());
            isChanged[1] ^= true;
        });

        audioFocusChanged.setOnClickListener(v -> ReaderSettings.saveIsStopWhenAudioFocusChanged(audioFocusChanged.isChecked()));

        requireAudioFocus.setOnClickListener(v -> {
            ReaderSettings.saveIsRequireAudioFocusWhenPlay(requireAudioFocus.isChecked());
            if (!requireAudioFocus.isChecked()) {
                ReaderSettings.saveIsStopWhenAudioFocusChanged(false);
                audioFocusChanged.setChecked(false);
                audioFocusChanged.setEnabled(false);
            } else {
                audioFocusChanged.setEnabled(true);
            }
        });

        reloadAfterJS.setOnClickListener(v -> ReaderSettings.saveIsReloadPageAfterJS(reloadAfterJS.isChecked()));

        stopAfterBTDisc.setOnClickListener(v -> {
            if (!ReaderSettings.saveIsStopWhenBlueToothDisconnected(stopAfterBTDisc.isChecked())) {
                stopAfterBTDisc.setChecked(false);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(GeneralSetting.this);
                alertDialogBuilder.setTitle(getString(R.string.general_setting_title_BT_permission)).setMessage(getString(R.string.general_setting_text_BT_permission)).setCancelable(false).setPositiveButton(getString(R.string.button_close), (dialog, id) -> {
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        skipURL.setOnClickListener(v -> ReaderSettings.saveIsSkipURL(skipURL.isChecked()));

        blockAds.setOnClickListener(v -> ReaderSettings.saveIsBlockAds(blockAds.isChecked()));

        resetTips.setOnClickListener(v -> {
            ReaderSettings.resetTips();
            Message.toastMSG(getString(R.string.general_setting_reset_tips_text));
        });

        clearBrowsingData.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.general_setting_clear_browsing_data_title)
                .setMessage(getString(R.string.general_setting_clear_browsing_data_message) + "\n\n" + getString(R.string.general_setting_clear_browsing_data_history_note))
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.general_setting_clear, (dialog, which) -> {
                    clearBrowsingData();
                    Message.toastMSG(getString(R.string.general_setting_browsing_data_cleared));
                })
                .show());

        initializeAd();
    }

    @Override
    public void finish() {
        Intent intent = new Intent(MainActivity.ACTIVITY_RESULT_GENERAL_SETTING);
        intent.putExtra("isChanged", isChanged);
        setResult(RESULT_OK, intent);
        super.finish();
    }

    private void initialLang() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langNames);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ReaderSettings.saveLang(langCodes[(int) id]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        lang.setAdapter(arrayAdapter);
        String currentLang = ReaderSettings.getLang();
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentLang)) {
                lang.setSelection(i);
                break;
            }
        }
    }

    private void clearBrowsingData() {
        MyWebView webView = WindowList.getWebView();
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        webView.clearSslPreferences();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        WebStorage.getInstance().deleteAllData();
    }

    private void initializeAd() {
        AdView adView = findViewById(R.id.adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                //Message.log("Ad failed to load: " + adError.getMessage());
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }
}
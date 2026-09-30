package com.blueberryjoy.reader.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.utils.TextToSpeechManager;
import com.blueberryjoy.reader.utils.GlobalTipDialog;
import com.blueberryjoy.reader.utils.Message;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class VoiceSetting extends AppCompatActivity {
    private RecyclerView voices;
    private TextView textView;
    private Spinner spinner;
    private TextView sample;
    private RecyclerViewAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voicesetting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        voices = findViewById(R.id.recyclerView);
        textView = findViewById(R.id.textView);
        spinner = findViewById(R.id.spinner);
        sample = findViewById(R.id.textView1);

        //Different if api >= 30.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                    v.setPadding(0, 0, 0, insets.getInsets(WindowInsets.Type.ime()).bottom);
                    return insets;
                }
            });
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(this.getWindow().getDecorView(), new OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                @SuppressWarnings("deprecation")
                public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                    v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            sample.setText(extras.getString("textSample", ""));

        updateVoiceList();
        updateSpinner();

        findViewById(R.id.button).setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VoiceSetting.this);
            EditText decimal = new EditText(VoiceSetting.this);
            decimal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            decimal.setText(String.valueOf(ReaderSettings.getSpeed()));
            decimal.setGravity(Gravity.CENTER);
            alertDialogBuilder.setView(decimal);
            alertDialogBuilder
                    .setTitle(getString(R.string.voice_setting_title_change_speed))
                    .setMessage(getString(R.string.voice_setting_text_change_speed))
                    .setPositiveButton(getString(R.string.button_save), (dialog, id) -> {
                        String str = decimal.getText().toString();
                        if (str.isEmpty()) {
                            str = "0";
                        } else {
                            if (str.charAt(0) == '.') str = "0" + str;
                            if (str.charAt(str.length() - 1) == '.') str = str + "0";
                        }
                        float speed = Float.parseFloat(str);
                        if (speed >= 2) {
                            speed = 2;
                        } else if (speed <= 0.5) {
                            speed = 0.5f;
                        }
                        Message.toastMSG(getString(R.string.message_text_change_speed) + speed);
                        TextToSpeechManager.setSpeed(speed);
                    })
                    .setNegativeButton(getString(R.string.button_cancel), (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

        findViewById(R.id.button1).setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VoiceSetting.this);
            EditText decimal = new EditText(VoiceSetting.this);
            decimal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            decimal.setText(String.valueOf(ReaderSettings.getPitch()));
            decimal.setGravity(Gravity.CENTER);
            alertDialogBuilder.setView(decimal);
            alertDialogBuilder
                    .setTitle(getString(R.string.voice_setting_title_change_pitch))
                    .setMessage(getString(R.string.voice_setting_text_change_pitch))
                    .setPositiveButton(getString(R.string.button_save), (dialog, id) -> {
                        String str = decimal.getText().toString();
                        if (str.isEmpty()) {
                            str = "0";
                        } else {
                            if (str.charAt(0) == '.') str = "0" + str;
                            if (str.charAt(str.length() - 1) == '.') str = str + "0";
                        }
                        float pitch = Float.parseFloat(str);
                        Message.toastMSG(getString(R.string.message_text_change_pitch) + pitch);
                        TextToSpeechManager.setPitch(pitch);
                    })
                    .setNegativeButton(getString(R.string.button_cancel), (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

        sample.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(@NonNull Editable s) {
                for (int i = s.length() - 1; i >= 0; i--) {
                    if (s.charAt(i) == '\n') {
                        s.delete(i, i + 1);
                        sample.clearFocus();
                    }
                }
            }
        });

        sample.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(sample, 0);
            } else {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(sample.getWindowToken(), 0);
            }
        });

        View[] views = {spinner, sample, voices};
        int[] ids = {7, 8, 9};
        int[] xs = {0, 0, 0};
        int[] ys = {32, 160, 128};
        int[] gravities = {Gravity.TOP | Gravity.START, Gravity.BOTTOM | Gravity.START, Gravity.TOP | Gravity.START};
        String[] contents = {getString(R.string.tip_choose_engine), getString(R.string.tip_input_text), getString(R.string.tip_choose_voice)};
        GlobalTipDialog.show(VoiceSetting.this, views, contents, ids, xs, ys, gravities, 0, true);

        initializeAd();
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if (sample.isFocused()) {
            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();
            Rect rect = new Rect();
            sample.getGlobalVisibleRect(rect);
            if (!rect.contains(x, y))
                sample.clearFocus();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initializeVoiceList() {
        textView.setText(ReaderSettings.getVoice());
        voices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = getRecyclerViewAdapter();
        voices.setAdapter(recyclerViewAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(voices.getContext(), LinearLayoutManager.VERTICAL);
        voices.addItemDecoration(dividerItemDecoration);
        voices.scrollToPosition(recyclerViewAdapter.getVoiceIdx());
    }

    @NonNull
    private RecyclerViewAdapter getRecyclerViewAdapter() {
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerViewAdapter.setClickListener((idx) -> {
            textView.setText(TextToSpeechManager.setVoice(idx));
            String textSample = getString(R.string.voice_setting_text_hint);
            if (sample.getText().length() > 0)
                textSample = sample.getText().toString();
            TextToSpeechManager.speak(textSample);
        });
        return recyclerViewAdapter;
    }

    private void updateVoiceList() {
        if (recyclerViewAdapter == null) {
            initializeVoiceList();
        } else {
            textView.setText(ReaderSettings.getVoice());
            recyclerViewAdapter.update();
            voices.scrollToPosition(recyclerViewAdapter.getVoiceIdx());
        }
    }

    private void updateSpinner() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TextToSpeechManager.getEngineList());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextToSpeechManager.setEngine(() -> initializeVoiceList(), (int) id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(TextToSpeechManager.getTTSEngineIdx());
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
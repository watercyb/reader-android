package com.blueberryjoy.reader.utils;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blueberryjoy.reader.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loglist);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.button).setOnClickListener(v -> {
            File file = new File(FileStorage.getDir() + "/log.txt");
            if (file.exists() && file.delete())
                readFromInternalFile();
        });

        readFromInternalFile();
    }

    private void readFromInternalFile() {
        File file = new File(FileStorage.getDir() + "/log.txt");
        if (!file.exists()) try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.close();
        } catch (IOException ignored) {
        }
        StringBuilder SB = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                SB.append(line).append("\n\n");
            }
        } catch (IOException ignored) {
        }
        ((TextView) findViewById(R.id.textView)).setText(SB.toString());
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    public static void appendToInternalFile(String str) {
        File file = new File(FileStorage.getDir() + "/log.txt");
        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            bufferedWriter.write(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ": " + str);
            bufferedWriter.newLine();
        } catch (IOException ignored) {
        }
    }
}
package com.blueberryjoy.reader.main.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.settings.ReaderSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentText {

    // Repeated sequences without separators, e.g. 你好你好你好...
    private static final Pattern SEQUENCE_REPEAT = Pattern.compile("(.{1,}?)(?:\\1){4,}", Pattern.DOTALL);
    //private static final Pattern SEQUENCE_REPEAT = Pattern.compile("(.+)\\1{4,}", Pattern.DOTALL);

    // Repeated sequences separated by whitespace or common commas,
    // e.g. let's go, let's go, let's go...
    private static final Pattern SEPARATED_SEQUENCE_REPEAT = Pattern.compile("(.{2,}?)(?:[,，、\\s]+\\1){4,}", Pattern.DOTALL);

    @Nullable
    public static String compressRepeatsRegex(@Nullable String s) {
        if (!ReaderSettings.getIsReduceRepeatText() || s == null || s.length() < 5)
            return s;

        // 1. Compress repeated sequences separated by delimiters.
        s = replaceSeparatedSequences(s);

        // 2. Compress repeated adjacent sequences.
        s = replaceAdjacentSequences(s);

        return s;
    }

    @NonNull
    private static String replaceSeparatedSequences(@NonNull String s) {
        Matcher matcher = SEPARATED_SEQUENCE_REPEAT.matcher(s);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            String unit = matcher.group(1);
            if (unit == null || unit.isEmpty()) {
                continue;
            }

            String matched = matcher.group();

            // Count non-overlapping occurrences of the repeated unit.
            int count = 0;
            int pos = 0;

            while ((pos = matched.indexOf(unit, pos)) != -1) {
                count++;
                pos += unit.length();
            }

            if (count < 5) {
                continue;
            }

            result.append(s, lastEnd, matcher.start());
            result.append(unit).append(" x ").append(count);
            lastEnd = matcher.end();
        }

        result.append(s, lastEnd, s.length());
        return result.toString();
    }

    @NonNull
    private static String replaceAdjacentSequences(@NonNull String s) {
        Matcher matcher = SEQUENCE_REPEAT.matcher(s);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            String unit = matcher.group(1);
            if (unit == null || unit.isEmpty()) {
                continue;
            }

            String matched = matcher.group();

            int unitLength = unit.codePointCount(0, unit.length());
            int matchedLength =
                    matched.codePointCount(0, matched.length());

            if (unitLength == 0
                    || matchedLength % unitLength != 0) {
                continue;
            }

            int count = matchedLength / unitLength;

            if (count < 5) {
                continue;
            }

            result.append(s, lastEnd, matcher.start());
            result.append(unit).append(" x ").append(count);
            lastEnd = matcher.end();
        }

        result.append(s, lastEnd, s.length());
        return result.toString();
    }
}
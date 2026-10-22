package com.blueberryjoy.reader.main.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.settings.ReaderSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class _ContentText {

    //private static final Pattern HAN_REPEAT = Pattern.compile("(\\p{IsHan})\\1{4,}");
    private static final Pattern HAN_REPEAT = Pattern.compile("([\\u4E00-\\u9FFF])\\1{4,}");

    private static final Pattern SYMBOL_REPEAT = Pattern.compile("([^\\p{L}\\p{N}\\s])\\1{4,}");

    private static final Pattern ENGLISH_WORD_REPEAT = Pattern.compile("([a-zA-Z]+)(?:\\s+\\1){4,}");

    @Nullable
    public static String compressRepeatsRegex(@Nullable String s) {
        if (!ReaderSettings.getIsReduceRepeatText() || s == null || s.length() < 5) return s;

        // 1. Compress repeated Chinese characters.
        s = replaceWithCount(s, HAN_REPEAT, 1);

        // 2. Compress repeated symbols.
        s = replaceWithCount(s, SYMBOL_REPEAT, 1);

        // 3. Compress repeated English words separated by whitespace.
        s = replaceEnglishWords(s);

        return s;
    }

    @NonNull
    private static String replaceWithCount(@NonNull String s, @NonNull Pattern pattern, int group) {

        Matcher matcher = pattern.matcher(s);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(s, lastEnd, matcher.start());

            String unit = matcher.group(group);
            assert unit != null;
            int count = matcher.group().codePointCount(0, matcher.group().length()) / unit.codePointCount(0, unit.length());

            result.append(unit).append(" x ").append(count);
            lastEnd = matcher.end();
        }

        result.append(s, lastEnd, s.length());
        return result.toString();
    }

    @NonNull
    private static String replaceEnglishWords(@NonNull String s) {
        Matcher matcher = ENGLISH_WORD_REPEAT.matcher(s);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(s, lastEnd, matcher.start());

            String match = matcher.group();
            String word = matcher.group(1);

            // Count the occurrences of the word.
            assert word != null;
            Matcher wordMatcher = Pattern.compile("(?<!\\S)" + Pattern.quote(word) + "(?!\\S)").matcher(match);

            int count = 0;
            while (wordMatcher.find()) {
                count++;
            }

            result.append(word).append(" x ").append(count);
            lastEnd = matcher.end();
        }

        result.append(s, lastEnd, s.length());
        return result.toString();
    }

}

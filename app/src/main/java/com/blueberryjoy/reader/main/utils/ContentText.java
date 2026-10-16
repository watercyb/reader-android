package com.blueberryjoy.reader.main.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.settings.ReaderSettings;
import com.blueberryjoy.reader.utils.RunTime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentText {

    //private static final Pattern HAN_REPEAT = Pattern.compile("(\\p{IsHan})\\1{4,}");
    private static final Pattern HAN_REPEAT = Pattern.compile("([\\u4E00-\\u9FFF])\\1{4,}");

    private static final Pattern SYMBOL_REPEAT = Pattern.compile("([^\\p{L}\\p{N}\\s])\\1{4,}");

    private static final Pattern ENGLISH_WORD_REPEAT = Pattern.compile("([a-zA-Z]+)(?:\\s+\\1){4,}");

    /*
    public static String compressRepeats(String s) {
        if (!ReaderSettings.getIsReduceRepeatText() || s == null || s.isEmpty()) return s;

        StringBuilder result = new StringBuilder(s.length());
        int n = s.length();
        int i = 0;

        while (i < n) {
            int start = i;
            String unit;

            // Read one unit: either a word or a single non-word character.
            if (Character.isLetterOrDigit(s.codePointAt(i))) {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);

                while (i < n) {
                    cp = s.codePointAt(i);
                    if (!Character.isLetterOrDigit(cp)) {
                        break;
                    }
                    i += Character.charCount(cp);
                }
            } else {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);
            }

            unit = s.substring(start, i);

            // Count consecutive identical units, including a separator
            // between words (e.g. "No No No").
            int count = 1;
            int end = i;

            while (end < n) {
                int nextStart = end;

                // For words, require one or more spaces between repetitions.
                if (Character.isLetterOrDigit(s.codePointAt(start))) {
                    while (end < n && Character.isWhitespace(s.codePointAt(end))) {
                        end += Character.charCount(s.codePointAt(end));
                    }

                    if (end == nextStart || end >= n) {
                        end = nextStart;
                        break;
                    }
                }

                int nextUnitStart = end;
                int cp = s.codePointAt(end);

                if (Character.isLetterOrDigit(s.codePointAt(start))) {
                    if (!Character.isLetterOrDigit(cp)) {
                        end = nextStart;
                        break;
                    }

                    end += Character.charCount(cp);
                    while (end < n) {
                        cp = s.codePointAt(end);
                        if (!Character.isLetterOrDigit(cp)) {
                            break;
                        }
                        end += Character.charCount(cp);
                    }
                } else {
                    end += Character.charCount(cp);
                }

                if (!s.regionMatches(start, s, nextUnitStart, unit.length()) || end - nextUnitStart != unit.length()) {
                    end = nextStart;
                    break;
                }

                count++;
                i = end;
            }

            if (count > 5) {
                result.append(unit).append(" x ").append(count);
            } else {
                result.append(s, start, i);
            }
        }

        return result.toString();
    }
    */

    @Nullable
    public static String compressRepeatsRegex(@Nullable String s) {
        if (!ReaderSettings.getIsReduceRepeatText() || s == null || s.length() < 5) return s;

        RunTime.start(1);

        // 1. Compress repeated Chinese characters.
        s = replaceWithCount(s, HAN_REPEAT, 1);

        // 2. Compress repeated symbols.
        s = replaceWithCount(s, SYMBOL_REPEAT, 1);

        // 3. Compress repeated English words separated by whitespace.
        s = replaceEnglishWords(s);

        RunTime.endAndShow(1);

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

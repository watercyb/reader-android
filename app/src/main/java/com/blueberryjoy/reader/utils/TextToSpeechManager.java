package com.blueberryjoy.reader.utils;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import androidx.annotation.Nullable;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.main.message.MessageReader;
import com.blueberryjoy.reader.main.message.MessageSender;
import com.blueberryjoy.reader.main.utils.ContentText;
import com.blueberryjoy.reader.settings.ReaderSettings;

import java.util.ArrayList;
import java.util.List;

public class TextToSpeechManager {
    @Nullable
    private static TextToSpeech tts;
    private static final List<String> engineList = new ArrayList<>();
    private static final List<Voice> voiceList = new ArrayList<>();
    private static UtteranceProgressListener utteranceProgressListener;
    public static final String UTTERANCE_SYSTEM = "utterance_system";
    public static final String UTTERANCE_TEXT = "utterance_text";

    //Initialized from ReaderForeground.
    public static void initial(UtteranceProgressListener utteranceProgressListener) {
        TextToSpeechManager.utteranceProgressListener = utteranceProgressListener;
        initializeTTSEngine();
        initializeTTS();
    }

    public static void initializeTTSEngine() {
        engineList.clear();
        TextToSpeech tts = new TextToSpeech(App.Companion.getAppContext(), null);
        for (TextToSpeech.EngineInfo engines : tts.getEngines()) {
            if (engines.name.equals("com.google.android.tts")) {
                engineList.add(0, engines.name);
            } else {
                engineList.add(engines.name);
            }
        }
        tts.shutdown();
    }

    public static void setSpeed(float voiceSpeed) {
        assert tts != null;
        tts.setSpeechRate(voiceSpeed);
        ReaderSettings.saveSpeed(voiceSpeed);
    }

    public static void setPitch(float voicePitch) {
        assert tts != null;
        tts.setPitch(voicePitch);
        ReaderSettings.savePitch(voicePitch);
    }

    public static void setVoiceList() {
        if (tts != null) {
            tts.setSpeechRate(ReaderSettings.getSpeed());
            tts.setPitch(ReaderSettings.getPitch());
            voiceList.clear();
            voiceList.addAll(tts.getVoices());
            String voiceName = ReaderSettings.getVoiceName();
            for (Voice voice : voiceList) {
                if (voice.getName().equals(voiceName)) tts.setVoice(voice);
            }
        }
    }

    public static String setVoice(int idx) {
        if (tts == null || idx < 0 || idx >= voiceList.size()) return "";
        Voice voice = voiceList.get(idx);
        tts.setVoice(voice);
        ReaderSettings.saveVoice(tts.getVoice().getName());
        return tts.getVoice().getName();
    }

    public static List<Voice> getVoiceList() {
        return voiceList;
    }

    public static int getTTSEngineIdx() {
        for (int i = 0; i < engineList.size(); i++) {
            if (engineList.get(i).equals(ReaderSettings.getTTSEngine())) return i;
        }
        return -1;
    }

    public static boolean speak(String str) {
        if (tts == null) return false;
        str = ContentText.compressRepeatsRegex(str);
        if (tts.isSpeaking()) {
            tts.speak(str, TextToSpeech.QUEUE_ADD, null, UTTERANCE_TEXT);
        } else {
            tts.speak(str, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_TEXT);
        }
        return true;
    }

    public static void speakSystem(String str) {
        if (tts == null) return;
        tts.speak(str, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_SYSTEM);
    }

    public static void stop() {
        if (tts != null) tts.stop();
    }

    public static void setTTSListener() {
        assert tts != null;
        tts.setOnUtteranceProgressListener(utteranceProgressListener);
    }

    public static boolean isTTSNull() {
        return tts == null;
    }

    public static List<String> getEngineList() {
        initializeTTSEngine();
        return engineList;
    }

    public static boolean isEngineListEmpty() {
        return engineList.isEmpty();
    }

    public static void setEngine(OnTTSInitializedListener onTTSInitializedListener, int idx) {
        String engine = engineList.get(idx);
        if (!engine.equals(ReaderSettings.TTSEngine) && !engine.isEmpty()) {
            if (tts != null)
                tts.shutdown();
            tts = new TextToSpeech(App.Companion.getAppContext(), state -> {
                if (state == TextToSpeech.SUCCESS) {
                    TextToSpeechManager.setVoiceList();
                    ReaderSettings.saveEngine(engine);
                    setVoice(0);
                    setTTSListener();
                    onTTSInitializedListener.onTTSInitialized();
                }
            }, engine);
        }
    }

    public static void initializeTTS() {
        if (TextToSpeechManager.isEngineListEmpty())
            MessageSender.sendMessage(new MessageReader(MessageReader.ACTION_ERROR, MessageReader.ACTION_ERROR_TTS));
        if (tts != null) tts.shutdown();
        tts = new TextToSpeech(App.Companion.getAppContext(), state -> {
            if (state == TextToSpeech.SUCCESS) {
                if (TextToSpeechManager.isTTSNull()) {
                    MessageSender.sendMessage(new MessageReader(MessageReader.ACTION_ERROR, MessageReader.ACTION_ERROR_TTS));
                    return;
                }
                TextToSpeechManager.setVoiceList();
                setTTSListener();
            }
        }, ReaderSettings.getTTSEngine());
    }

    public static void shutdown() {
        if (tts != null) tts.shutdown();
    }

    public interface OnTTSInitializedListener {
        void onTTSInitialized();
    }
}

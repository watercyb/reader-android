package com.blueberryjoy.reader.main;

import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_CAN_PLAY;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_CLEAR;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_CLEAR_NOTIFICATION;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_IDX;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_INITIAL;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_INITIAL_SAME_PAGE;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_NEXT;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_NOTIFICATION_SETTING_CHANGED;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_PAUSE;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_PREV;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_REFRESH_WITHOUT_STOP;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_RESUME;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_STOP_READING_AND_CLEAR_NOTIFICATION;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_UNFOCUSED;
import static com.blueberryjoy.reader.main.utils.ReaderController.ACTION_UPDATE_MEDIA_TITLE;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.main.message.MessageNode;
import com.blueberryjoy.reader.main.message.MessageReader;
import com.blueberryjoy.reader.main.message.MessageSender;
import com.blueberryjoy.reader.main.utils.ReaderNotification;
import com.blueberryjoy.reader.utils.Message;
import com.blueberryjoy.reader.utils.TextToSpeechManager;
import com.blueberryjoy.reader.settings.ReaderSettings;
import com.blueberryjoy.reader.main.window.WindowList;
import com.blueberryjoy.reader.utils.Favicon;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReaderForeground extends Service {
    private boolean isPause = true;
    private boolean isWaiting = false;
    private int pauseButtonCount = 0;
    private boolean isNextOrPrevCoolingDown = false;

    private static AudioManager audioManager;
    @Nullable
    private static AudioFocusRequest focusRequest;
    private static int audioFocusRequest = 0;
    private MediaSession mediaSession;
    private MediaPlayer mediaPlayer;
    private CountDownTimer foregroundTimeoutTimer;
    @NonNull
    private final ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
    private ReaderNotification readerNotification;
    private CountDownTimer resumeFocusTimer;
    private CountDownTimer nextOrPrevCooldownTimer;
    private PlaybackState.Builder stateBuilder;
    private MediaMetadata.Builder metadataBuilder;
    private boolean canPlay = true;
    private AudioManager.AudioPlaybackCallback audioPlaybackCallback;
    private boolean isHoldFocus = false;
    private CountDownTimer holdFocusTimer;
    private UtteranceProgressListener utteranceProgressListener;

    @Override
    protected void attachBaseContext(@NonNull Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        utteranceProgressListener = new UtteranceProgressListener() {
            @Override
            public void onDone(String utteranceId) {
                if (!isPause && utteranceId.equals(TextToSpeechManager.UTTERANCE_TEXT))
                    new Handler(Looper.getMainLooper()).post(() -> read(0));
            }

            @Override
            public void onError(String utteranceId) {
            }

            @Override
            public void onStart(String utteranceId) {
            }
        };

        TextToSpeechManager.initial(utteranceProgressListener);

        readerNotification = new ReaderNotification(getBaseContext());
        stateBuilder = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        metadataBuilder = new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "reader").putLong(MediaMetadata.METADATA_KEY_DURATION, 1);
        startForeground(1, readerNotification.getNotification(getBaseContext()));

        setAudioFocus();

        foregroundTimeoutTimer = new CountDownTimer(30 * 60 * 1000, 3000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        };

        nextOrPrevCooldownTimer = new CountDownTimer(100, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                isNextOrPrevCoolingDown = false;
            }
        }.start();
    }

    @Override
    public int onStartCommand(@androidx.annotation.Nullable Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;
        switch (intent.getAction()) {
            case ACTION_INITIAL:
                if (!isPause) {
                    stop();
                    int newIdx = intent.getIntExtra("idx", -1);
                    if (newIdx >= 0) WindowList.setIdxWithoutSave(newIdx);
                    readerNotification.updateNotificationTitle(WindowList.getTitle());
                    updateMedia(true);
                    play();
                } else {
                    int newIdx = intent.getIntExtra("idx", -1);
                    if (newIdx >= 0) WindowList.setIdxWithoutSave(newIdx);
                    readerNotification.updateNotificationTitle(WindowList.getTitle());
                    updateMedia(true);
                    updateContext(intent.getBooleanExtra("move", true) ? 1 : 0);
                    WindowList.addPageToCache(null, -1, null);
                }
                break;
            case ACTION_INITIAL_SAME_PAGE:
                int newIdx = intent.getIntExtra("idx", -1);
                if (newIdx >= 0) WindowList.setIdxWithoutSave(newIdx);
                readerNotification.updateNotificationTitle(WindowList.getTitle());
                updateMedia(true);
                break;
            case ACTION_PAUSE:
                if (isPause) {
                    play();
                } else {
                    pause();
                }
                break;
            case ACTION_PREV:
                prev();
                break;
            case ACTION_NEXT:
                next();
                break;
            case ACTION_IDX:
                stop();
                WindowList.setIdxWithoutSave(intent.getIntExtra("idx", 0));
                play();
                break;
            case ACTION_RESUME:
                //After change language ReaderSettings.tts becomes to null, cause ReaderSetting is reloaded, but Reader is not.
                if (TextToSpeechManager.isTTSNull())
                    TextToSpeechManager.initial(utteranceProgressListener);

                restartMediaPlayerForMediaSession();

                //Run everytime when switch back, and before "initial" when open the app.
                if (WindowList.isValidReadingStatus()) {
                    //When ForegroundService stopped.
                    updateMedia(true);
                    readerNotification.updateNotificationTitle(WindowList.getTitle());
                    readerNotification.updateNotification(WindowList.getCurrentStr());
                } else {
                    updateMedia(false);
                }
                foregroundTimeoutTimer.cancel();
                break;
            case ACTION_UNFOCUSED:
                if (isPause) {
                    foregroundTimeoutTimer.cancel();
                    foregroundTimeoutTimer.start();
                }
                break;
            case ACTION_CLEAR:
                isWaiting = false;
                break;
            case ACTION_NOTIFICATION_SETTING_CHANGED:
                if (!ReaderSettings.getIsShowContentInNotification()) {
                    readerNotification.clearNotification();
                } else {
                    readerNotification.updateNotification(WindowList.getCurrentStr());
                }
                break;
            case ACTION_CLEAR_NOTIFICATION:
                readerNotification.clearNotification();
                readerNotification.clearNotificationTitle();
                updateMedia(false);
                break;
            case ACTION_REFRESH_WITHOUT_STOP:
                readerNotification.updateNotificationTitle(WindowList.getTitle());
                updateMedia(true);
                updateContext(intent.getBooleanExtra("move", true) ? 1 : 0);
                break;
            case ACTION_STOP_READING_AND_CLEAR_NOTIFICATION:
                if (!isPause) pause();
                readerNotification.clearNotification();
                readerNotification.clearNotificationTitle();
                updateMedia(false);
                break;
            case ACTION_UPDATE_MEDIA_TITLE:
                updateMedia(true);
                break;
            case ACTION_CAN_PLAY:
                canPlay = intent.getBooleanExtra("enable", true);
                break;
            default:
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent intent = new Intent(this, ReaderForeground.class);
        stopService(intent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (resumeFocusTimer != null) resumeFocusTimer.cancel();
        if (foregroundTimeoutTimer != null) foregroundTimeoutTimer.cancel();
        if (nextOrPrevCooldownTimer != null) nextOrPrevCooldownTimer.cancel();
        stop();
        TextToSpeechManager.shutdown();
        if (mediaSession != null) mediaSession.release();
        if (mediaPlayer != null) mediaPlayer.release();
        if (readerNotification != null) readerNotification.close();
        if (audioPlaybackCallback != null)
            audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
        if (focusRequest != null)
            audioManager.abandonAudioFocusRequest(focusRequest);
        audioFocusRequest = AudioManager.AUDIOFOCUS_NONE;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void read(int direction) {
        if (!WindowList.hasNext()) {
            pause();
            updateContext(direction);
        } else {
            String regex = "https?://\\S+|www\\.\\S+";
            WindowList.moveToNext();
            String str = ReaderSettings.getIsSkipURL() ? WindowList.getCurrentStr().replaceAll(regex, "") : WindowList.getCurrentStr();
            if (!TextToSpeechManager.speak(str)) {
                stop();
                return;
            }
            updateActionStart();
            updateContext(direction);
        }
    }

    private void setAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        resumeFocusTimer = new CountDownTimer(1500, 1500) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                assert focusRequest != null;
                if (audioFocusRequest != AudioManager.AUDIOFOCUS_REQUEST_GRANTED && ReaderSettings.getIsRequireAudioFocusWhenPlay()) {
                    audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
                    isHoldFocus = true;
                    if (holdFocusTimer != null) holdFocusTimer.cancel();
                    holdFocusTimer = new CountDownTimer(1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            isHoldFocus = false;
                            holdFocusTimer = null;
                        }
                    }.start();
                }
                if (isWaiting && isPause) {
                    play();
                    isWaiting = false;
                }
            }
        };

        audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(@NonNull List<AudioPlaybackConfiguration> configs) {
                super.onPlaybackConfigChanged(configs);
                //Called when audioPlaybackConfigurations list is changed
                if (!ReaderSettings.getIsStopWhenAudioFocusChanged()) return;
                resumeFocusTimer.cancel();
                boolean isNavigation = false;
                for (AudioPlaybackConfiguration audioPlaybackConfiguration : configs) {
                    if (audioPlaybackConfiguration.getAudioAttributes().getUsage() == 12) {
                        isNavigation = true;
                        break;
                    }
                }
                if (audioManager.isMusicActive() && !isPause && (audioFocusRequest != AudioManager.AUDIOFOCUS_GAIN || isNavigation)) {
                    pause();
                    isWaiting = true;
                } else if ((!audioManager.isMusicActive() || configs.isEmpty()) && isPause && isWaiting) {
                    resumeFocusTimer.start();
                }
            }
        };
        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);

        //Called when lost audio focus.
        AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
            if (!isHoldFocus) {
                audioFocusRequest = focusChange;
            } else {
                assert focusRequest != null;
                audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
            }
        };
        AudioAttributes playbackAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focusRequest = null;
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(playbackAttributes).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(audioFocusChangeListener).build();
    }

    private void broadCast(@NonNull MessageNode messageNode) {
        MessageSender.sendMessage(messageNode);
    }

    private void updateContext(int direction) {
        if (WindowList.isValidReadingStatus()) {
            broadCast(new MessageReader(MessageReader.ACTION_CONTEXT, direction));
            readerNotification.updateNotification(WindowList.getCurrentStr());
        }
    }

    private void updateActionPause() {
        broadCast(new MessageReader(MessageReader.ACTION_PAUSE, pauseButtonCount++));
        readerNotification.pause();
    }

    private void updateActionStart() {
        broadCast(new MessageReader(MessageReader.ACTION_START, pauseButtonCount++));
        readerNotification.play();
    }

    private void play() {
        if (!WindowList.isValidReadingStatus() || !canPlay) return;
        if (isPause) {
            if (ReaderSettings.getIsRequireAudioFocusWhenPlay()) {
                if (audioFocusRequest != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    assert focusRequest != null;
                    audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
                    isHoldFocus = true;
                    if (holdFocusTimer != null) holdFocusTimer.cancel();
                    holdFocusTimer = new CountDownTimer(1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            isHoldFocus = false;
                            holdFocusTimer = null;
                        }
                    }.start();
                } else {
                    assert focusRequest != null;
                    audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
                }
            }

            isWaiting = false;
            isPause = false;

            read(1);
        }
        foregroundTimeoutTimer.cancel();
        setPlaybackStatePlay();
    }

    private void stop() {
        isPause = true;
        TextToSpeechManager.stop();
        WindowList.resetNextIdx();
        updateActionPause();
    }

    private void pause() {
        if (!isPause) {
            isPause = true;
            TextToSpeechManager.stop();
            WindowList.resetNextIdx();
            updateActionPause();
            ActivityManager.getMyMemoryState(process);
            if (process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                foregroundTimeoutTimer.cancel();
                foregroundTimeoutTimer.start();
            }
            setPlaybackStatePause();
        }
    }

    private void next() {
        if (WindowList.getLoadingState() != 0) return;
        if (!isPause) {
            if (isNextOrPrevCoolingDown) return;
            TextToSpeechManager.stop();
            isNextOrPrevCoolingDown = true;
            nextOrPrevCooldownTimer.start();
            read(0);
        } else {
            WindowList.idxInc();
            updateContext(1);
        }
    }

    private void prev() {
        if (WindowList.getLoadingState() != 0) return;
        if (!isPause) {
            if (isNextOrPrevCoolingDown) return;
            TextToSpeechManager.stop();
            isNextOrPrevCoolingDown = true;
            nextOrPrevCooldownTimer.start();
            WindowList.idxDecForPrv();
            read(0);
        } else {
            WindowList.idxDec();
            updateContext(-1);
        }
    }

    private void mediaControl() {
        if (mediaSession != null) return;
        restartMediaPlayerForMediaSession();
        mediaSession = new MediaSession(getBaseContext(), "1");
        mediaSession.setMetadata(metadataBuilder.build());
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                WindowList.pause();
            }

            @Override
            public void onPause() {
                super.onPause();
                WindowList.pause();
            }

            @Override
            public void onSkipToPrevious() {
                super.onStop();
                if (WindowList.isChangingIdxEnabled())
                    prev();
            }

            @Override
            public void onSkipToNext() {
                super.onStop();
                if (WindowList.isChangingIdxEnabled())
                    next();
            }

        });

        mediaSession.setActive(true);
    }

    private void restartMediaPlayerForMediaSession() {
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, R.raw.second);
        mediaPlayer.setOnCompletionListener(mp -> {
        });
        mediaPlayer.setVolume(0.0f, 0.0f);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        mediaPlayer.pause();
    }

    private void setPlaybackStatePlay() {
        mediaControl();
        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f).build());
        //Recreate the MediaPlayer to restore Bluetooth media-button routing.
        restartMediaPlayerForMediaSession();
    }

    private void setPlaybackStatePause() {
        mediaControl();
        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f).build());
        mediaPlayer.pause();
    }

    private void updateMedia(boolean enable) {
        if (enable) {
            if (mediaSession == null) mediaControl();
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, WindowList.getTitle());
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, Favicon.getIconFromUrl(WindowList.getUrl()));
            mediaSession.setMetadata(metadataBuilder.build());
            mediaSession.setActive(true);
        } else {
            if (mediaSession == null) mediaControl();
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, "reader");
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, Favicon.getIconFromUrl(""));
            mediaSession.setMetadata(metadataBuilder.build());
            mediaSession.setActive(false);
        }
    }
}
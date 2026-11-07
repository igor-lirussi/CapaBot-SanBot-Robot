package com.sanbot.capaBot;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local Google/Android TTS for Nano — MainService cloud TTS often times out,
 * and STREAM_MUSIC sticks on a phantom wired headset with no jack plugged in.
 * Java 7–compatible (CapaBot targets sourceCompatibility 1.7).
 */
public final class AndroidTtsSpeaker implements TextToSpeech.OnInitListener {
    private static final String TAG = "AndroidTtsSpeaker";
    private static final String UTTERANCE_ID = "capabot_local_speak";

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean speaking = new AtomicBoolean(false);
    private TextToSpeech tts;
    private volatile String pendingText;
    private volatile int pendingSpeed = 50;
    private volatile int pendingVolume = 90;
    private volatile String pendingLang = "en";
    private volatile Runnable onDone;

    public AndroidTtsSpeaker(Context context) {
        this.appContext = context.getApplicationContext();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    tts = new TextToSpeech(appContext, AndroidTtsSpeaker.this, "com.google.android.tts");
                } catch (Exception e) {
                    Log.w(TAG, "Google TTS engine ctor failed, trying default", e);
                    try {
                        tts = new TextToSpeech(appContext, AndroidTtsSpeaker.this);
                    } catch (Exception e2) {
                        Log.e(TAG, "TextToSpeech init failed", e2);
                    }
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) {
            ready.set(false);
            Log.w(TAG, "TTS init status=" + status);
            return;
        }
        try {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    speaking.set(true);
                }

                @Override
                public void onDone(String utteranceId) {
                    speaking.set(false);
                    Runnable cb = onDone;
                    onDone = null;
                    if (cb != null) {
                        mainHandler.post(cb);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    speaking.set(false);
                    Runnable cb = onDone;
                    onDone = null;
                    if (cb != null) {
                        mainHandler.post(cb);
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "UtteranceProgressListener", e);
        }
        ready.set(true);
        Log.i(TAG, "Android TTS ready");
        flushPending();
    }

    public boolean isReady() {
        return ready.get() && tts != null;
    }

    public boolean isSpeaking() {
        return speaking.get();
    }

    /**
     * Speak on the device speaker via Google TTS.
     * @return true if queued / spoken locally
     */
    public boolean speak(String text, int speed0to100, int volume0to100, String lang, Runnable done) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        pendingText = text.trim();
        pendingSpeed = speed0to100;
        pendingVolume = volume0to100;
        pendingLang = lang;
        onDone = done;
        speaking.set(true);
        if (!isReady()) {
            Log.w(TAG, "TTS not ready yet — queued");
            return true;
        }
        // If already on the main looper, speak inline. Posting + waiting on main
        // (concludeSpeak) never runs flushPending and deadlocks the UI.
        if (Looper.myLooper() == mainHandler.getLooper()) {
            flushPending();
        } else {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    flushPending();
                }
            });
        }
        return true;
    }

    public void stop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tts != null) {
                        tts.stop();
                    }
                } catch (Exception ignored) {
                }
                speaking.set(false);
            }
        });
    }

    private void flushPending() {
        String text = pendingText;
        pendingText = null;
        if (text == null || tts == null || !ready.get()) {
            return;
        }
        try {
            forceSpeakerRoute(appContext);
            ensureAudibleMusicVolume();
            Locale locale = resolveLocale(pendingLang);
            int langResult = tts.setLanguage(locale);
            if (langResult == TextToSpeech.LANG_MISSING_DATA
                    || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
            float rate = 0.5f + (Math.max(0, Math.min(100, pendingSpeed)) / 100f);
            tts.setSpeechRate(rate);
            float gain = Math.max(0f, Math.min(1f, pendingVolume / 100f));
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, gain);
            // STREAM_TTS (9) routes to speaker on Nano; MUSIC often sticks on phantom headset.
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamForSpeak());
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID);
            Log.i(TAG, "speak result=" + result + " lang=" + locale + " vol=" + pendingVolume
                    + " text=" + text);
            if (result != TextToSpeech.SUCCESS) {
                speaking.set(false);
                Runnable cb = onDone;
                onDone = null;
                if (cb != null) {
                    cb.run();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "speak failed", e);
            speaking.set(false);
            Runnable cb = onDone;
            onDone = null;
            if (cb != null) {
                cb.run();
            }
        }
    }

    static void forceSpeakerRoute(Context ctx) {
        disconnectPhantomHeadset();
        try {
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            int forMedia = audioSystem.getField("FOR_MEDIA").getInt(null);
            int forceSpeaker = audioSystem.getField("FORCE_SPEAKER").getInt(null);
            audioSystem.getMethod("setForceUse", int.class, int.class)
                    .invoke(null, forMedia, forceSpeaker);
        } catch (Throwable t) {
            Log.w(TAG, "FORCE_SPEAKER failed", t);
        }
        try {
            if (ctx == null) {
                return;
            }
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.setMode(AudioManager.MODE_NORMAL);
                am.setSpeakerphoneOn(true);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Nano often keeps DEVICE_OUT_WIRED_HEADSET "connected" with no jack — silence on speaker. */
    static void disconnectPhantomHeadset() {
        try {
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            int unavailable = audioSystem.getField("DEVICE_STATE_UNAVAILABLE").getInt(null);
            int[] devices = new int[] {
                    fieldOr(audioSystem, "DEVICE_OUT_WIRED_HEADSET", 0x4),
                    fieldOr(audioSystem, "DEVICE_OUT_WIRED_HEADPHONE", 0x8),
                    fieldOr(audioSystem, "DEVICE_OUT_USB_HEADSET", 0x4000000),
            };
            try {
                java.lang.reflect.Method m = audioSystem.getMethod(
                        "setDeviceConnectionState", int.class, int.class, String.class, String.class);
                for (int i = 0; i < devices.length; i++) {
                    m.invoke(null, devices[i], unavailable, "", "");
                }
                Log.i(TAG, "phantom headset disconnected");
                return;
            } catch (NoSuchMethodException ignored) {
            }
            java.lang.reflect.Method m3 = audioSystem.getMethod(
                    "setDeviceConnectionState", int.class, int.class, String.class);
            for (int i = 0; i < devices.length; i++) {
                m3.invoke(null, devices[i], unavailable, "");
            }
            Log.i(TAG, "phantom headset disconnected (3-arg)");
        } catch (Throwable t) {
            Log.w(TAG, "disconnectPhantomHeadset failed", t);
        }
    }

    private static int fieldOr(Class<?> audioSystem, String name, int fallback) {
        try {
            return audioSystem.getField(name).getInt(null);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int streamForSpeak() {
        try {
            return AudioManager.class.getField("STREAM_TTS").getInt(null);
        } catch (Throwable ignored) {
            return 9;
        }
    }

    private void ensureAudibleMusicVolume() {
        try {
            AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) {
                return;
            }
            bumpStream(am, AudioManager.STREAM_MUSIC);
            bumpStream(am, streamForSpeak());
            bumpStream(am, AudioManager.STREAM_SYSTEM);
        } catch (Exception ignored) {
        }
    }

    private static void bumpStream(AudioManager am, int stream) {
        try {
            int max = Math.max(1, am.getStreamMaxVolume(stream));
            int cur = am.getStreamVolume(stream);
            if (cur < Math.max(1, max / 2)) {
                am.setStreamVolume(stream, Math.max(1, (max * 2) / 3), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private static Locale resolveLocale(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return Locale.US;
        }
        String key = lang.trim().toLowerCase(Locale.US);
        if ("zh".equals(key) || "cn".equals(key) || "chinese".equals(key) || "zh-cn".equals(key)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if ("it".equals(key) || "italian".equals(key)) {
            return Locale.ITALY;
        }
        if ("es".equals(key) || "spanish".equals(key)) {
            return new Locale("es", "ES");
        }
        if ("fr".equals(key) || "french".equals(key)) {
            return Locale.FRANCE;
        }
        if ("de".equals(key) || "german".equals(key)) {
            return Locale.GERMANY;
        }
        return Locale.US;
    }
}

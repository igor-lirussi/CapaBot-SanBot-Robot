package com.sanbot.capaBot;

import android.util.Log;

import com.sanbot.opensdk.function.beans.SpeakOption;
import com.sanbot.opensdk.function.unit.SpeechManager;

/**
 * Speak helper: prefers local Google TTS (Settings → Google TTS speaker)
 * so Nano is audible when Qihan cloud TTS / phantom headset fails.
 */
public final class MySpeak {
    private static final String TAG = "MySpeak";

    private MySpeak() {
    }

    public static void speak(SpeechManager speechManager, String text, SpeakOption option) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (MySettings.isGoogleTtsSpeakerEnabled()) {
            AndroidTtsSpeaker local = MyApp.getAndroidTts();
            if (local != null) {
                int speed = 50;
                int volume = 90;
                if (option != null) {
                    try {
                        speed = option.getSpeed();
                    } catch (Throwable ignored) {
                    }
                }
                boolean queued = local.speak(text, speed, volume, "en", null);
                if (queued) {
                    Log.i(TAG, "local Google TTS: " + text);
                    return;
                }
            }
            Log.w(TAG, "Google TTS unavailable — falling back to OpenSDK cloud TTS");
        }
        if (speechManager != null) {
            speechManager.startSpeak(text, option != null ? option : MySettings.getSpeakDefaultOption());
        }
    }

    /** True while local Google TTS is speaking (cloud path still uses SpeechManager). */
    public static boolean isLocalSpeaking() {
        AndroidTtsSpeaker local = MyApp.getAndroidTts();
        return local != null && local.isSpeaking();
    }
}

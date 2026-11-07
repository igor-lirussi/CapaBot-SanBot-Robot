package com.sanbot.capaBot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Optional Google / Android {@link SpeechRecognizer} path — bypasses Qihan MainService ASR.
 * Requires a RecognitionService provider (e.g. Google Search) and RECORD_AUDIO.
 */
public final class AndroidSpeechRecognizer {
    private static final String TAG = "AndroidSpeechRecognizer";

    public interface Listener {
        void onFinalResult(String text);

        void onListeningStarted();

        void onListeningEnded();

        void onError(int errorCode, String message);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private Listener listener;
    private boolean listening;
    private boolean destroyed;

    public AndroidSpeechRecognizer(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static boolean isRecognitionAvailable(Context context) {
        if (context == null) {
            return false;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return false;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        List<ResolveInfo> list = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list != null && !list.isEmpty();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isListening() {
        return listening;
    }

    public void startListening() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                startListeningOnMain();
            }
        });
    }

    public void stopListening() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopListeningOnMain();
            }
        });
    }

    public void destroy() {
        destroyed = true;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopListeningOnMain();
                if (recognizer != null) {
                    try {
                        recognizer.destroy();
                    } catch (Exception ignored) {
                    }
                    recognizer = null;
                }
            }
        });
    }

    private void startListeningOnMain() {
        if (destroyed) {
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            notifyError(-1, "SpeechRecognizer not available (install Google Search)");
            return;
        }
        ensureRecognizer();
        if (recognizer == null) {
            notifyError(-1, "Failed to create SpeechRecognizer");
            return;
        }
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.getPackageName());
            listening = true;
            recognizer.startListening(intent);
            Log.i(TAG, "startListening");
        } catch (Exception e) {
            listening = false;
            Log.e(TAG, "startListening failed", e);
            notifyError(-1, e.getMessage());
        }
    }

    private void stopListeningOnMain() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.stopListening();
            } catch (Exception ignored) {
            }
            try {
                recognizer.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    private void ensureRecognizer() {
        if (recognizer != null) {
            return;
        }
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (listener != null) {
                        listener.onListeningStarted();
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    listening = false;
                    if (listener != null) {
                        listener.onListeningEnded();
                    }
                }

                @Override
                public void onError(int error) {
                    listening = false;
                    notifyError(error, errorToString(error));
                }

                @Override
                public void onResults(Bundle results) {
                    listening = false;
                    String text = firstResult(results);
                    if (text != null && listener != null) {
                        Log.i(TAG, "result: " + text);
                        listener.onFinalResult(text);
                    } else if (listener != null) {
                        notifyError(SpeechRecognizer.ERROR_NO_MATCH, "no match");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createSpeechRecognizer failed", e);
            recognizer = null;
        }
    }

    private static String firstResult(Bundle results) {
        if (results == null) {
            return null;
        }
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private void notifyError(int code, String message) {
        Log.w(TAG, "error " + code + ": " + message);
        if (listener != null) {
            listener.onError(code, message != null ? message : "error");
        }
    }

    private static String errorToString(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "audio";
            case SpeechRecognizer.ERROR_CLIENT:
                return "client";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "permission";
            case SpeechRecognizer.ERROR_NETWORK:
                return "network";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "network_timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "no_match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "server";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "speech_timeout";
            default:
                return "unknown_" + error;
        }
    }
}

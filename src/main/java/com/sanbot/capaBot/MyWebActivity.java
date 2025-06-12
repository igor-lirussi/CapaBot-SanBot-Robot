package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;

public class MyWebActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-WEB";

    boolean infiniteWakeup = true;

    private SpeechManager speechManager;    //speech
    private SystemManager systemManager;    //emotions

    WebView myWebView;
    Button exitButton;

    String lastRecognizedSentence;
    String place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyWebActivity.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_web);

        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        myWebView = findViewById(R.id.webViewBrowser);
        exitButton = findViewById(R.id.exit);


        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });

        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        Log.i(TAG, "Loading URL");
        //laod url
        Intent intent = getIntent();
        String url = intent.getExtras().getString("url");
        myWebView.loadUrl(url);

        initListeners();
    }


    void initListeners() {
        //Set wakeup, sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {

            }

            @Override
            public void onWakeUp() {
                Log.i(TAG, "WAKE UP callback");
            }

            @Override
            public void onSleep() {
                Log.i(TAG, "SLEEP callback");
                if (infiniteWakeup) {
                    //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                    speechManager.doWakeUp();
                }
            }
        });
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(RecognizeTextBean recognizeTextBean) {
            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                lastRecognizedSentence = Objects.requireNonNull(grammar.getText()).toLowerCase();
                Log.i(TAG, "Recognized Result: " + lastRecognizedSentence);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (lastRecognizedSentence.contains("ok") ||lastRecognizedSentence.contains("yes") ||lastRecognizedSentence.contains("i am")||lastRecognizedSentence.contains("we are")
                                ||lastRecognizedSentence.contains("sure") ||lastRecognizedSentence.contains("of course") ||lastRecognizedSentence.contains("thank")) {
                            if (lastRecognizedSentence.contains("thank")) {
                                //thanks
                                speechManager.startSpeak("thank you", MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                            }
                            //happy
                            systemManager.showEmotion(EmotionsType.KISS);
                            speechManager.startSpeak("I'm happy to hear about it", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //finish
                            finishThisActivity();
                            return;
                        }
                        if (lastRecognizedSentence.equals("no") ||lastRecognizedSentence.contains("so and so") ) {
                            //sad
                            speechManager.startSpeak("I'm sad to hear this", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.GOODBYE);
                            concludeSpeak(speechManager);
                            //finish
                            finishThisActivity();
                            return;
                        }
                        if (lastRecognizedSentence.contains("exit")) {
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.SMILE);
                            concludeSpeak(speechManager);
                            //finish
                            finishThisActivity();
                            return;
                        }

                        boolean newPlace = false;
                        String[] separators = {"show me ", " of ", " in ", " on ", " to ", };
                        for (String separator : separators) {
                            if (lastRecognizedSentence.contains(separator)) {
                                place = StringUtils.substringAfter(lastRecognizedSentence, separator);
                                newPlace = true;
                                Log.i(TAG, "New Place detected: " + place);
                            }
                        }
                        if(newPlace) {
                            speechManager.startSpeak("OK, let's go to " + place, MySettings.getSpeakDefaultOption());
                            //Computing URL
                            place = place.replace(" ", "+");
                            String url = "https://www.google.com/maps/search/" + place;
                            Log.i(TAG, url);
                            //loading URL in the web-view
                            myWebView.loadUrl(url);
                        }
                    }
                });
                return true;
            }

            @Override
            public void onRecognizeVolume(int i) {
            }

            @Override
            public void onStartRecognize() {

            }

            @Override
            public void onStopRecognize() {

            }

            @Override
            public void onError(int i, int i1) {

            }
        });


    }


    @Override
    protected void onMainServiceConnected() {}

    private void finishThisActivity() {
        //force sleep
        infiniteWakeup = false;
        speechManager.doSleep();
        //starts dialog activity
        Intent myIntent = new Intent(MyWebActivity.this, MyDialogActivity.class);
        MyWebActivity.this.startActivity(myIntent);
        //finish
        finish();
        return;
    }
}

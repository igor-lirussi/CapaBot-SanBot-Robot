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

    boolean infiniteWakeup = true;

    private SpeechManager speechManager;    //speech
    private SystemManager systemManager;    //emotions

    WebView mywebview;
    Button exitButton;

    String lastRecognizedSentence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyWebActivity.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_web);

        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        mywebview = findViewById(R.id.webViewBrowser);
        exitButton = findViewById(R.id.exit);


        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });

        mywebview.getSettings().setJavaScriptEnabled(true);
        mywebview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //laod url
        Intent intent = getIntent();
        String url = intent.getExtras().getString("url");
        mywebview.loadUrl(url);

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
                Log.i("IGOR-WEB", "WAKE UP callback");
            }

            @Override
            public void onSleep() {
                Log.i("IGOR-WEB", "SLEEP callback");
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
                        }
                        if (lastRecognizedSentence.equals("no") ||lastRecognizedSentence.contains("so and so") ) {
                            //sad
                            speechManager.startSpeak("I'm sad to hear this", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.GOODBYE);
                            concludeSpeak(speechManager);
                            //finish
                            finishThisActivity();
                        }
                        if (lastRecognizedSentence.contains("exit")) {
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.SMILE);
                            concludeSpeak(speechManager);
                            //finish
                            finishThisActivity();
                        }

                        String[] separators = {"show me ", " of ", " in ", " on ", " to ", };
                        for (String separator : separators) {
                            if (lastRecognizedSentence.contains(separator)) {
                                String place = StringUtils.substringAfter(lastRecognizedSentence, separator);
                                speechManager.startSpeak("OK, let's go to " + place, MySettings.getSpeakDefaultOption());
                                place = place.replace(" ", "+");
                                String url = "https://www.google.com/maps/search/" + place;
                                Log.i("IGOR-WEB", url);
                                mywebview.loadUrl(url);
                            }
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
    }
}

package com.sanbot.capaBot;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.headmotion.AbsoluteAngleHeadMotion;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.beans.wheelmotion.RelativeAngleWheelMotion;
import com.sanbot.opensdk.function.beans.wing.AbsoluteAngleWingMotion;
import com.sanbot.opensdk.function.beans.wing.NoAngleWingMotion;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.TouchSensorListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyUtils.sleepy;

public class MyPresentActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-PRS";

    @BindView(R.id.exit)
    Button exitButton;

    @BindView(R.id.imagePresentation)
    ImageView imagePresentation;

    private SpeechManager speechManager; //voice, speechRec
    private HeadMotionManager headMotionManager;    //head movements
    private WingMotionManager wingMotionManager;    //hands movements
    private SystemManager systemManager; //emotions
    private HardWareManager hardWareManager; //leds //touch sensors //voice locate //gyroscope
    private WheelMotionManager wheelMotionManager;


    boolean infiniteWakeup = true;
    boolean finishedPresentation = false;

    MediaPlayer mp1;
    String lastRecognizedSentence= "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyPresentActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_present);

        ButterKnife.bind(this);
        //initialize managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        wingMotionManager = (WingMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToDialogAndExit(true);
            }
        });

        initListeners();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startPresentation();
            }
        }, 1000);

        //released sound at the end
        mp1 = MediaPlayer.create(MyPresentActivity.this,R.raw.lalala);
        mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
             @Override
             public void onCompletion(MediaPlayer mediaPlayer) {
                 mp1.release();
             }
         }    );
    }

    @Override
    protected void onMainServiceConnected() {}


    void initListeners() {
        //touch sensors
        hardWareManager.setOnHareWareListener(
                new TouchSensorListener() {
                    @Override
                    public void onTouch(int i, boolean b) {

                    }

                    @Override
                    public void onTouch(int part) {
                        try {
                            if (speechManager.isSpeaking().getResult().equals("1") && finishedPresentation) {
                                Log.i(TAG, "Touching occurred");
                                switch (part) {
                                    case 1:
                                        speechManager.startSpeak("You are touching the right part of my jaw", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 2:
                                        speechManager.startSpeak("You are touching the left part of my jaw", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 3:
                                        speechManager.startSpeak("You are touching the left part of my chest", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 4:
                                        speechManager.startSpeak("You are touching the right part of my chest", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 5:
                                        speechManager.startSpeak("You are touching the left part of my back-head", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 6:
                                        speechManager.startSpeak("You are touching the right part of my back-head", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 7:
                                        speechManager.startSpeak("You are touching the left part of my back", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 8:
                                        speechManager.startSpeak("You are touching the right part of my back", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 9:
                                        speechManager.startSpeak("You are touching my left hand", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 10:
                                        speechManager.startSpeak("You are touching my right hand", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 11:
                                        speechManager.startSpeak("You are touching the middle of my head", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 12:
                                        speechManager.startSpeak("You are touching the left part of my head", MySettings.getSpeakDefaultOption());
                                        break;
                                    case 13:
                                        speechManager.startSpeak("You are touching the right part of my head", MySettings.getSpeakDefaultOption());
                                        break;
                                }
                            }

                        } catch (NullPointerException e) {
                            //no speech manager
                            e.printStackTrace();
                        }
                    }
                }
        );
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
        //voice listener
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(@NonNull RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                lastRecognizedSentence = Objects.requireNonNull(grammar.getText()).toLowerCase();
                Log.i(TAG, "Recognized: "+ lastRecognizedSentence);
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
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
                        }
                        if (lastRecognizedSentence.equals("no") ||lastRecognizedSentence.contains("so and so") ) {
                            //sad
                            speechManager.startSpeak("I'm sad to hear this", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.GOODBYE);
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
                        }
                        if (lastRecognizedSentence.contains("exit")) {
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.SMILE);
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
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


    private void startPresentation() {
        new Thread(new Runnable() {
            public void run(){
                Log.i(TAG, "Presentation Started");
                //intro
                speechManager.startSpeak("I'm SanBot, a robot of the ISR. My main purpose is to help people ", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);

                //DISPLAY
                speechManager.startSpeak("Even if i'm small, I am fully equipped:", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //show emotion
                systemManager.showEmotion(EmotionsType.KISS);
                speechManager.startSpeak("I can change my face expression to show emotions", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);

                speechManager.startSpeak(" my body color, ", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //flicker led
                hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_RANDOM));

                speechManager.startSpeak("display things on my tablet and even project on a wall! ", MySettings.getSpeakDefaultOption());
                changeImage("project");
                concludeSpeak(speechManager);

                //ACTUATORS
                speechManager.startSpeak("I can move my head", MySettings.getSpeakDefaultOption());
                //head movement
                AbsoluteAngleHeadMotion absoluteAngleHeadMotion = new AbsoluteAngleHeadMotion(
                        AbsoluteAngleHeadMotion.ACTION_HORIZONTAL,130
                );

                headMotionManager.doAbsoluteAngleMotion(absoluteAngleHeadMotion);
                sleepy(2);
                //head movement
                absoluteAngleHeadMotion = new AbsoluteAngleHeadMotion(
                        AbsoluteAngleHeadMotion.ACTION_HORIZONTAL,90
                );
                headMotionManager.doAbsoluteAngleMotion(absoluteAngleHeadMotion);
                sleepy(2);

                speechManager.startSpeak("my arms...", MySettings.getSpeakDefaultOption());
                //hand up
                AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_LEFT, 5, 70);
                wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
                sleepy(2);
                //hands down (reset position)
                wingMotionManager.doNoAngleMotion(new NoAngleWingMotion(NoAngleWingMotion.PART_BOTH, 5,NoAngleWingMotion.ACTION_RESET));

                speechManager.startSpeak("and navigate in any direction thanks to my omnidirectional wheels... I can even dance!", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //play music
                int maxVolume = 100;
                int currVolume = 50;
                float log1=(float)(Math.log(maxVolume-currVolume)/Math.log(maxVolume));
                mp1.setVolume(log1,log1);
                mp1.start();

                systemManager.showEmotion(EmotionsType.SMILE);
                hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_RANDOM_THREE_GROUP));
                absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 5, 0);
                wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
                RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(
                        RelativeAngleWheelMotion.TURN_LEFT, 5,360
                );
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
                sleepy(2);
                //hands down (reset position)
                wingMotionManager.doNoAngleMotion(new NoAngleWingMotion(NoAngleWingMotion.PART_BOTH, 5,NoAngleWingMotion.ACTION_RESET));
                sleepy(4);

                //SENSORS
                changeImage("overview");
                speechManager.startSpeak("I have a microphone and powerful subwoofers to communicate with people, infrared sensors to feel distances, 2 PIR to detect humans, touch sensors all over my body and a wide HD camera and a depth camera to understand the environment. ", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //image sensors

                speechManager.startSpeak( "With these capabilities I can assist humans in Education", MySettings.getSpeakDefaultOption());
                //image education
                changeImage("education");
                concludeSpeak(speechManager);

                speechManager.startSpeak(", Public Service ", MySettings.getSpeakDefaultOption());
                //image public service
                changeImage("public");
                concludeSpeak(speechManager);

                speechManager.startSpeak("and Health-care.", MySettings.getSpeakDefaultOption());
                //image health care
                changeImage("health");
                concludeSpeak(speechManager);

                sleepy(2);
                speechManager.startSpeak("Are you satisfied? ", MySettings.getSpeakDefaultOption());
                changeImage("presentsanbot");
                concludeSpeak(speechManager);
                //wake up and listen
                infiniteWakeup = true;
                finishedPresentation = true;
                speechManager.doWakeUp();
            }
        }).start();

    }


    private void changeImage(final String image) {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (image) {
                    case "project":
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.project));
                        break;
                    case "overview":
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.overview));
                        break;
                    case "education":
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.presentedu));
                        break;
                    case "public":
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.presentpub));
                        break;
                    case "health":
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.presenthealtcare));
                        break;
                    default:
                        imagePresentation.setImageDrawable(getDrawable(R.drawable.presentsanbot));
                }
            }
        });
    }

    private void goToDialogAndExit(boolean res) {
        Log.i(TAG, "Finished presentation");
        //force sleep
        infiniteWakeup = false;
        speechManager.doSleep();
        //starts dialog activity
        Intent myIntent = new Intent(MyPresentActivity.this, MyDialogActivity.class);
        MyPresentActivity.this.startActivity(myIntent);
        //finish
        finish();
    }
}

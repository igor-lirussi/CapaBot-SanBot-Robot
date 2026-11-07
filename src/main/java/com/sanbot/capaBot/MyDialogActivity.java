package com.sanbot.capaBot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.sanbot.capaBot.AIML.ab.AIMLProcessor;
import com.sanbot.capaBot.AIML.ab.Bot;
import com.sanbot.capaBot.AIML.ab.Chat;
import com.sanbot.capaBot.AIML.ab.Graphmaster;
import com.sanbot.capaBot.AIML.ab.MagicBooleans;
import com.sanbot.capaBot.AIML.ab.MagicStrings;
import com.sanbot.capaBot.AIML.ab.PCAIMLProcessorExtension;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.headmotion.LocateAbsoluteAngleHeadMotion;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.beans.wing.AbsoluteAngleWingMotion;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.GyroscopeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.compensationSanbotAngle;
import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyUtils.rotateAtCardinalAngle;
import static com.sanbot.capaBot.MyUtils.rotateAtRelativeAngle;
import static com.sanbot.capaBot.MyUtils.sleepy;


/**
 * Handles the Dialog with a person
 * Gets the pronounced sentence with speech recognition and answers with Text to Speech.
 * if specific intentions are recognized starts another activity to handle them
 * if the dialog is open (no tasks asked) the Conversational Engine handles an answer.
 *
 * NOTE: "wake up" in this context (according to the SDK) means start listening, "sleep" means stop listening
 */
public class MyDialogActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-DIAL";

    //view
    @BindView(R.id.tv_speech_recognize_result)
    TextView tvSpeechRecognizeResult;
    @BindView(R.id.imageListen)
    TextView imageListen;
    @BindView(R.id.wake)
    Button wakeButton;
    @BindView(R.id.grid_view_examples)
    GridView gridExamples;
    @BindView(R.id.exit)
    Button exitButton;

    /** MY VARIABLES */
    private SpeechManager speechManager;    //speech
    private SystemManager systemManager; //emotions
    private HardWareManager hardWareManager;    //leds
    private HeadMotionManager headMotionManager;    //head movements
    private WheelMotionManager wheelMotionManager;    //head movements
    private WingMotionManager wingMotionManager;    //head movements
    private ModularMotionManager modularMotionManager; //follow

    LocateAbsoluteAngleHeadMotion locateAbsoluteAngleHeadMotion = new LocateAbsoluteAngleHeadMotion(
            LocateAbsoluteAngleHeadMotion.ACTION_VERTICAL_LOCK,90,30
    );

    String lastRecognizedSentence = " ";
    Handler noResponseAction = new Handler();
    /** Off-main so concludeSpeak can wait on Google TTS without deadlocking the UI. */
    Handler speechResponseHandler;
    private android.os.HandlerThread speechResponseThread;
    private AndroidSpeechRecognizer googleSpeech;
    private static final int REQ_RECORD_AUDIO = 4401;
    ArrayAdapter<String> adapter;
    private int askOtherTimes = 0;
    private int currentCardinalAngle = 0;

    int noResponse = 0;
    boolean infiniteWakeup = true; //to force the robot always listening (it goes to listening sleep after some seconds)s
    String youCanSay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyDialogActivity.class);
        //The screen is always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //layout
        setContentView(R.layout.activity_dialog);
        ButterKnife.bind(this);

        //Initialize managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        wheelMotionManager = (WheelMotionManager)getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        wingMotionManager = (WingMotionManager)getUnitManager(FuncConstant.WINGMOTION_MANAGER);
        modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);

        speechResponseThread = new android.os.HandlerThread("capa-speech-response");
        speechResponseThread.start();
        speechResponseHandler = new Handler(speechResponseThread.getLooper());

        initGoogleSpeechRecognizer();

        //listeners
        initListener();

        //wake button, useful for people that wait so much to speak that the robot goes to sleep
        wakeButton.setVisibility(View.GONE);
        wakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wakeUpListening();
            }
        });

        //exit button
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //force stop listening
                infiniteWakeup = false;
                speechManager.doSleep();
                //starts base activity
                Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
                MyDialogActivity.this.startActivity(myIntent);
                //terminates
                finish();
            }
        });

        //GridView examples, (this array inflates the view)
        // to light one remember to pass position of this array at the greenFlashButton function (or colorFlashButton)
        final String[] examples = new String[]{
                "shake my hand",
                "where is the meeting-room?",
                "where is the laboratory?",
                "where is the bathroom?",
                "show me a video",
                "save my suggestion",
                "no / nothing / go away",
                "weather",
                "present yourself",
                "time",
                "map",
                "calendar"
                };
        adapter = new ArrayAdapter<String>(this, R.layout.grid_element_layout , examples);
        gridExamples.setAdapter(adapter);
        gridExamples.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos, long id)
            {
                if(!MySettings.isTaskButtonsEnabled()) {
                    youCanSay = getString(R.string.you_can_say) + " " + examples[pos];
                    Toast.makeText(getApplicationContext(), youCanSay, Toast.LENGTH_SHORT).show();
                    MySpeak.speak(speechManager, "Instead of touching, you can be more polite with me and " + youCanSay, MySettings.getSpeakDefaultOption());
                    wakeUpListening();
                }else{
                    Grammar grammarTmp = new Grammar();
                    grammarTmp.setText(examples[pos]);
                    speechManager.onRecognizeResult(grammarTmp);
                }
            }
        });



        Log.i(TAG,"-START SILENT PRESENTATION");
        //grabbing the name from the previous activity
        Intent intent = getIntent();
        String name_received = "";
        try {
            name_received = intent.getExtras().getString("name");
        } catch (NullPointerException e) { Log.e(TAG,"no name received in the intent");}

        //if the name is passed/exists
        if(name_received!= null && !name_received.equals("")) {
            //creating a silent presentation with the name received
            String silent_presentation = "my name is " + name_received;
            //the silent presentation is given to the conversational engine that memorizes the name
            String response = MyApp.chat.multisentenceRespond(silent_presentation);
            Log.i(TAG, "Human: " + silent_presentation);
            Log.i(TAG, "Robot: " + response);
        } else {
            Log.i(TAG,"-NAME NOT PASSED");
        }
        Log.i(TAG,"-FINISH SILENT PRESENTATION");

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Robot head up, ask what to do, and wake up listening (speak wait off-main)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                if (speechResponseHandler == null) {
                    wakeUpListening();
                    return;
                }
                speechResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MySpeak.speak(speechManager, getString(R.string.what_could_do), MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wakeUpListening();
                            }
                        });
                    }
                });
            }
        }, 200);
    }

    /**
     * Initialize the listener
     */
    private void initListener() {
        //Gyro callback
        hardWareManager.setOnHareWareListener(new GyroscopeListener() {
            @Override
            public void gyroscopeCheckResult(boolean b, boolean b1) {

            }

            @Override
            public void gyroscopeData(float v, float v1, float v2) {
                /*
                //gyroscope received
                Log.i("gyro", "GYRO first: " + v + ", second: " + v1 + ", third: " + v2);
                // get the angle corrected
                currentCardinalAngle = compensationSanbotAngle(v);
                Log.i("compass", "compass: " + currentCardinalAngle);
                */
            }
        });
        //Set wakeup, sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {

            }

            @Override
            public void onWakeUp() {
                Log.i(TAG, "WAKE UP callback");
                if (MySettings.isDebug()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyDialogActivity.this, "Listening now", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onSleep() {
                Log.i(TAG, "SLEEP callback");
                //infiniteWakeup is a custom variable to control the duration
                if (infiniteWakeup) {
                    if (useGoogleSpeechRecognizer()) {
                        // MainService sleep is irrelevant; Google STT restarts itself on error/end.
                    } else {
                        //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                        speechManager.doWakeUp();
                    }
                } else {
                    //change button in the view to notify the sleep status
                    imageListen.setVisibility(View.GONE);
                    wakeButton.setVisibility(View.VISIBLE);
                }
            }
        });
        //Speech recognition callback
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(@NonNull RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                // When Google STT is active, ignore MainService ASR to avoid double replies.
                if (useGoogleSpeechRecognizer()) {
                    return true;
                }

                //start timer
                //long startTime = System.nanoTime();
                //cast object received to text string lastRecognizedSentence
                String text;
                try {
                    text = Objects.requireNonNull(grammar.getText());
                } catch (NullPointerException e) {
                    text = "null";
                }
                queueRecognizedUtterance(text);

                //IGOR: must not exceed 200ms — return quickly so MainService does not answer randomly.
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

    private void queueRecognizedUtterance(String text) {
        try {
            lastRecognizedSentence = Objects.requireNonNull(text).toLowerCase();
        } catch (NullPointerException e) {
            lastRecognizedSentence = "null";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvSpeechRecognizeResult.setText(lastRecognizedSentence);
            }
        });
        if (speechResponseHandler == null) {
            return;
        }
        speechResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                        Log.i(TAG, ">>>>Recognized voice: "+ lastRecognizedSentence);
                        boolean recognizedWhatToDo = false; //given a recognized result for now we don't know what to do

                        //deletes "no response action"
                        noResponseAction.removeCallbacksAndMessages(null);


                        //---- INTERACTION PART ----
                        //basic interaction
                        if (lastRecognizedSentence.contains("hello") || lastRecognizedSentence.equals("hi")|| lastRecognizedSentence.contains("your name")) {
                            recognizedWhatToDo = true;
                            systemManager.showEmotion(EmotionsType.SMILE);
                            MySpeak.speak(speechManager, "Hi, I'm SanBot", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("how are you")) {
                            recognizedWhatToDo = true;
                            systemManager.showEmotion(EmotionsType.SMILE);
                            MySpeak.speak(speechManager, "I'm feeling full of energy", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (/*(lastRecognizedSentence.contains("what") || lastRecognizedSentence.contains("tell")) &&*/ lastRecognizedSentence.contains("time")) {
                            recognizedWhatToDo = true;
                            String time_sentence = "It's " + new SimpleDateFormat("HH:mm", Locale.ITALY).format(Calendar.getInstance().getTime());
                            MySpeak.speak(speechManager, time_sentence, MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("hear")) {
                            recognizedWhatToDo = true;
                            MySpeak.speak(speechManager, "I can hear you.", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("what") && lastRecognizedSentence.contains("do")) {
                            recognizedWhatToDo = true;
                            MySpeak.speak(speechManager, "I can give information to public and directions, teach something or just keep company, shake hands, tell the time and weather, project a video and more...", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("hands up")) {
                            recognizedWhatToDo = true;
                            //hands up
                            AbsoluteAngleWingMotion absoluteAngleWingMotion1 = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 0);
                            wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion1);
                            systemManager.showEmotion(EmotionsType.SNICKER);
                            MySpeak.speak(speechManager, "put your hands up in the air!", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            sleepy(2);
                            //hands down
                            AbsoluteAngleWingMotion absoluteAngleWingMotion2 = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 180);
                            wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion2);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("test")) {
                            recognizedWhatToDo = true;
                            systemManager.showEmotion(EmotionsType.NORMAL);
                            MySpeak.speak(speechManager, "Test OK, stop testing and go working", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        //---- PURPOSE PART ----
                        if (lastRecognizedSentence.contains("shake")) {
                            recognizedWhatToDo = true;
                            //increments stats request handshake
                            MySettings.incrementRequestShake();
                            //greenFlashButton(gridExamples.getChildAt(0));
                            MySpeak.speak(speechManager, "OK", MySettings.getSpeakDefaultOption());
                            //starts shake hand activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyShakeActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("map")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(5));
                            MySpeak.speak(speechManager, "OK let's see the map", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //compute the place to pass
                            String place = MySettings.getCityMap();
                            //if any separator is said the place after is passed in the url
                            String[] separators = {" of ", " in ", " on "};
                            for (String separator : separators) {
                                if (lastRecognizedSentence.contains(separator)) {
                                    place = StringUtils.substringAfter(lastRecognizedSentence, separator);
                                }
                            }
                            String url = "https://www.google.com/maps/place/" + place;
                            Log.i("IGOR", url);
                            //starts web activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyWebActivity.class);
                            myIntent.putExtra("url", url);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("weather")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(5));
                            MySpeak.speak(speechManager, "OK let's see the weather", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //compute the place to pass
                            String place = MySettings.getCityWeather();
                            //if any separator is said the place after is passed in the url
                            String[] separators = {" of ", " in ", " on "};
                            for (String separator : separators) {
                                if (lastRecognizedSentence.contains(separator)) {
                                    place = StringUtils.substringAfter(lastRecognizedSentence, separator);
                                }
                            }
                            Log.i("IGOR", place);
                            //starts weather activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyWeatherActivity.class);
                            myIntent.putExtra("place", place);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("calendar")) {
                            recognizedWhatToDo = true;
                            MySpeak.speak(speechManager, "OK let's see the calendar", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts calendar activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyCalendarActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("present yourself")) {
                            recognizedWhatToDo = true;
                            MySpeak.speak(speechManager, "OK", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts present ys activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyPresentActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("charge") || lastRecognizedSentence.contains("battery")) {
                            recognizedWhatToDo = true;
                            MySpeak.speak(speechManager, "OK", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts charge activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyChargeActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("meeting")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(1));
                            //increments stats request location
                            MySettings.incrementRequestLocation();
                            MySpeak.speak(speechManager, getString(R.string.meeting_room_is), MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //indicate
                            indicate(45);
                            askOther();
                        }

                        if (lastRecognizedSentence.contains("bathroom") || lastRecognizedSentence.contains("toilet")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(1));
                            //increments stats request location
                            MySettings.incrementRequestLocation();
                            MySpeak.speak(speechManager, "the bathroom is south, near the stairs", MySettings.getSpeakDefaultOption());
                            //indicate
                            indicate(180);
                            askOther();
                        }

                        if ( ( lastRecognizedSentence.contains("show") || lastRecognizedSentence.contains("project") ) && lastRecognizedSentence.contains("video") ) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(2));
                            //increments stats request video
                            MySettings.incrementRequestVideo();
                            //starts project activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyProjectStoryActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }
                        if (lastRecognizedSentence.contains("lab") || lastRecognizedSentence.contains("laboratory")) {
                            recognizedWhatToDo = true;
                            //increments stats request location
                            MySettings.incrementRequestLocation();
                            //greenFlashButton(gridExamples.getChildAt(1));
                            MySpeak.speak(speechManager, getString(R.string.lab_is), MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //indicate
                            indicate(0);
                            askOther();
                        }

                        if (lastRecognizedSentence.contains("suggestion")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(3));
                            MySpeak.speak(speechManager, "OK", MySettings.getSpeakDefaultOption());
                            //starts leave suggestion activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyXMLSuggestionActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                            return;
                        }

                        //exits if answer is "no time" or "nothing"
                        if (lastRecognizedSentence.equals("no") || lastRecognizedSentence.contains("nothing") ||
                                lastRecognizedSentence.contains("no thank you") || lastRecognizedSentence.contains("no time")||
                                lastRecognizedSentence.contains("go away") || lastRecognizedSentence.contains("exit")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton( gridExamples.getChildAt(4) );
                            MySpeak.speak(speechManager, getString(R.string.see_you), MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            if (lastRecognizedSentence.contains("go away")) {
                                MySpeak.speak(speechManager, "I will go away", MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                                systemManager.showEmotion(EmotionsType.CRY);
                                //rotates of 180
                                rotateAtRelativeAngle(wheelMotionManager, 180);
                            }
                            if (lastRecognizedSentence.contains("thank")){
                                systemManager.showEmotion(EmotionsType.KISS);
                                MySpeak.speak(speechManager, getString(R.string.thanks_you_kind), MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                            }
                            //force listening sleep
                            infiniteWakeup = false;
                            speechManager.doSleep();

                            //starts base activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);

                            finish();
                            return;
                        }


                        /*
                        //not recognized ask to repeat
                        if (!recognizedWhatToDo) {
                            systemManager.showEmotion(EmotionsType.QUESTION);
                            MySpeak.speak(speechManager, getString(R.string.please_repeat) , MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            wakeUpListening();
                        }
                        */
                        //not recognized pass to CHATBOT
                        if (!recognizedWhatToDo) {
                            //CHATBOT PART
                            long startTime = System.nanoTime();
                            String response = MyApp.chat.multisentenceRespond(lastRecognizedSentence);

                            Log.i(TAG, "Human: " + lastRecognizedSentence);
                            Log.i(TAG, "Robot: " + response);

                            Log.i(TAG, "DURATION COMPUTED RESPONSE millisec: " + (System.nanoTime() - startTime) / 1000000);
                            MySpeak.speak(speechManager, response, MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            wakeUpListening();
                        }

                        showRandomFace();
                    }
                });
    }

    private boolean useGoogleSpeechRecognizer() {
        return MySettings.isGoogleSpeechRecognizerEnabled()
                && googleSpeech != null
                && AndroidSpeechRecognizer.isRecognitionAvailable(this);
    }

    private void initGoogleSpeechRecognizer() {
        googleSpeech = new AndroidSpeechRecognizer(this);
        googleSpeech.setListener(new AndroidSpeechRecognizer.Listener() {
            @Override
            public void onFinalResult(String text) {
                queueRecognizedUtterance(text);
            }

            @Override
            public void onListeningStarted() {
                if (MySettings.isDebug()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyDialogActivity.this, "Listening (Google)", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onListeningEnded() {
            }

            @Override
            public void onError(int errorCode, String message) {
                Log.w(TAG, "Google STT error " + errorCode + " " + message);
                if (!infiniteWakeup) {
                    return;
                }
                if (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        || errorCode == SpeechRecognizer.ERROR_NO_MATCH
                        || errorCode == SpeechRecognizer.ERROR_CLIENT
                        || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (infiniteWakeup && useGoogleSpeechRecognizer() && googleSpeech != null) {
                                googleSpeech.startListening();
                            }
                        }
                    }, 400);
                }
            }
        });
    }

    private void startGoogleListeningWithPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                return;
            }
        }
        if (googleSpeech != null) {
            googleSpeech.startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleSpeech != null) {
                    googleSpeech.startListening();
                }
            } else {
                Toast.makeText(this, "Microphone permission needed for Google speech", Toast.LENGTH_LONG).show();
                MySettings.setGoogleSpeechRecognizerEnabled(false);
                if (speechManager != null) {
                    speechManager.doWakeUp();
                }
            }
        }
    }

    @Override
    protected void onMainServiceConnected() {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stop infinite listening
        infiniteWakeup = false;
        if (googleSpeech != null) {
            googleSpeech.destroy();
            googleSpeech = null;
        }
        speechManager.doSleep();
        //if person pushes "back" before the normal termination
        MyBaseActivity.busy = false;
        //deletes "no response action" and all the handlers
        noResponseAction.removeCallbacksAndMessages(null);
        if (speechResponseHandler != null) {
            speechResponseHandler.removeCallbacksAndMessages(null);
        }
        if (speechResponseThread != null) {
            speechResponseThread.quit();
            speechResponseThread = null;
            speechResponseHandler = null;
        }
        Log.i(TAG, "destroy, noResponseHandler deleted");
    }

    /* *****MY FUNCTIONS ***** */

    /**
     * IGOR: designed to flash in the view the right element of the array according to the choice
     * EDIT: you have to write the whole text of the button, for now I search the position in the array manually
     * @param array the array of strings where to search the text
     * @param searched the text to search in the array
     * @return the position of the string searched in the array
     */
    public int findStringPositionInArray(String[] array, String searched) {
        //return Arrays.asList(array).indexOf(searched);
        int z = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(searched)) {
                return i;
            }
        }
        return z;
    }


    public void colorFlashButton( View view_passed, int color_passed) {
        //update ui
        view_passed.setBackgroundColor(ContextCompat.getColor(view_passed.getContext(), color_passed));
    }

    public void greenFlashButton( View view_passed) {
        colorFlashButton(view_passed, R.color.colorGreen);
    }

    /**
     * asking if something else is needed
     * @param seconds seconds after start to ask
     */
    public void askOther(int seconds) {
        //after passed seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                askOtherTimes++;
                if (askOtherTimes == 2) {
                    MySpeak.speak(speechManager, getString(R.string.please_suggestion), MySettings.getSpeakDefaultOption());
                    concludeSpeak(speechManager);
                }
                MySpeak.speak(speechManager, getString(R.string.something_else), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                wakeUpListening();
            }
        }, seconds*1000);
    }

    public void askOther() {
        askOther(0);
    }

    private void wakeUpListening() {
        imageListen.setVisibility(View.VISIBLE);
        wakeButton.setVisibility(View.GONE);
        //head up
        headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
        if (useGoogleSpeechRecognizer()) {
            Log.i(TAG, "wakeup via Google SpeechRecognizer");
            startGoogleListeningWithPermission();
        } else {
            Log.i(TAG, "wakeup via MainService doWakeUp");
            speechManager.doWakeUp();
        }
        //activate no response handler
        Log.i(TAG, "wakeup answer-active, noResponseHandler activated");
        noResponseAction.removeCallbacksAndMessages(null);
        noResponseAction.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (noResponse) {
                    case 1:
                        //first time waiting too much asks for out loud
                        MySpeak.speak(speechManager, "please speak out loud!", MySettings.getSpeakDefaultOption());
                        wakeUpListening();
                        noResponse++;
                        break;
                    case 2:
                        //sends to the bot the "NO RESPONSE" code
                        String response = MyApp.chat.multisentenceRespond("NORESP");

                        Log.i(TAG,"Human @not responded@");
                        Log.i(TAG,"Robot: " + response);
                        MySpeak.speak(speechManager, response , MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                        wakeUpListening();
                        noResponse++;
                        break;
                    default:
                        //force sleep
                        infiniteWakeup = false;
                        if (googleSpeech != null) {
                            googleSpeech.stopListening();
                        }
                        speechManager.doSleep();
                        //angry emotion
                        systemManager.showEmotion(EmotionsType.ANGRY);
                        //flicker leds
                        hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_RED));
                        //speak
                        MySpeak.speak(speechManager, getString(R.string.no_answer), MySettings.getSpeakDefaultOption());
                        //starts base activity
                        Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
                        MyDialogActivity.this.startActivity(myIntent);
                        //terminates
                        finish();
                        return;
                }
            }
        }, 1000 * MySettings.getSeconds_waitingResponse());
    }

    private void indicate(int desiredCardinalAngle) {
        //INDICATION
        //if not initiated the gyroscope
        if (currentCardinalAngle == 0) {
            //looking around to initiate the gyro
            int lookingAroundAngle = 50;
            rotateAtRelativeAngle(wheelMotionManager, lookingAroundAngle);
            sleepy(2);
            rotateAtRelativeAngle(wheelMotionManager, -lookingAroundAngle);
            sleepy(4);
        }
        //rotate at cardinal angle direction desired
        int cardinalrotation = rotateAtCardinalAngle(wheelMotionManager, currentCardinalAngle, desiredCardinalAngle);
        sleepy(4);
        //indicate
        AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_LEFT, 5, 70);
        wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
        //speak direction
        MySpeak.speak(speechManager, "in that direction!", MySettings.getSpeakDefaultOption());
        concludeSpeak(speechManager);
        //rotate back
        rotateAtRelativeAngle(wheelMotionManager, -cardinalrotation);
        //hand down
        absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_LEFT, 5, 180);
        wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
        sleepy(2);
    }

    public void showRandomFace() {
        double random_num = Math.random();
        //probability not to show any face
        if (random_num > 0.5) {
            //showing random face
            int min = 0, max = 20;
            int randomNum = (int) (Math.random() * ((max - min) + 1)) + min;
            switch (randomNum) {
                case 3:
                    systemManager.showEmotion(EmotionsType.ARROGANCE);
                    break;
                case 5:
                    systemManager.showEmotion(EmotionsType.FAINT);
                    break;
                case 6:
                    systemManager.showEmotion(EmotionsType.GOODBYE);
                    break;
                case 7:
                    systemManager.showEmotion(EmotionsType.GRIEVANCE);
                    break;
                case 8:
                    systemManager.showEmotion(EmotionsType.KISS);
                    break;
                case 9:
                    systemManager.showEmotion(EmotionsType.LAUGHTER);
                    break;
                case 10:
                    systemManager.showEmotion(EmotionsType.PICKNOSE);
                    break;
                case 11:
                    systemManager.showEmotion(EmotionsType.PRISE);
                    break;
                case 12:
                    systemManager.showEmotion(EmotionsType.QUESTION);
                    break;
                case 13:
                    systemManager.showEmotion(EmotionsType.SHY);
                    break;
                case 14:
                    systemManager.showEmotion(EmotionsType.SLEEP);
                    break;
                case 15:
                    systemManager.showEmotion(EmotionsType.SMILE);
                    break;
                case 16:
                    systemManager.showEmotion(EmotionsType.SNICKER);
                    break;
                case 17:
                    systemManager.showEmotion(EmotionsType.SURPRISE);
                    break;
                case 18:
                    systemManager.showEmotion(EmotionsType.SWEAT);
                    break;
                case 19:
                    systemManager.showEmotion(EmotionsType.WHISTLE);
                    break;
                default:
                    systemManager.showEmotion(EmotionsType.NORMAL);
            }
        }
    }


    //END
}

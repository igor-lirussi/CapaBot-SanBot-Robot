package com.sanbot.capaBot;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
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
    Handler speechResponseHandler = new Handler();
    ArrayAdapter<String> adapter;
    private int askOtherTimes = 0;
    private int currentCardinalAngle = 0;

    int noResponse = 0;
    boolean infiniteWakeup = true; //to force the robot always listening (it goes to listening sleep after some seconds)s
    String youCanSay;

    public Bot bot;
    public static Chat chat;

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
        wingMotionManager = (WingMotionManager)getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);

        //listeners
        initListener();

        //wake button, useful for people that have to wait so much to speak that the robot goes to sleep
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
                youCanSay = getString(R.string.you_can_say)+" " + examples[pos];
                Toast.makeText(getApplicationContext(), youCanSay, Toast.LENGTH_SHORT).show();
                speechManager.startSpeak("Instead of touching, you can be more polite with me and " + youCanSay, MySettings.getSpeakDefaultOption());
                wakeUpListening();

            }
        });


        //Robot head up, ask what to do, and wake up listening
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //head up
                headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                //ask
                speechManager.startSpeak(getString(R.string.what_could_do), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //wake up
                wakeUpListening();
            }
        }, 200);

        //CHAT BOT SETUP
        long startTime = System.nanoTime();
        //checking SD card availability
        boolean availableSD = isSDCARDAvailable();
        Log.i("IGOR-SPEECH", "SD available: " + availableSD);
        //receiving the assets from the app directory
        AssetManager assets = getResources().getAssets();
        //creating a new directory in the device
        File jayDir = new File(Environment.getExternalStorageDirectory().toString() + "/hari/bots/Hari");
        boolean dir_check = jayDir.mkdirs();
        if (jayDir.exists()) {
            //Reading the file
            try {
                //for every subdirectory
                for (String dir : assets.list("Hari")) {
                    //create the subdirectory in the device
                    File subdir = new File(jayDir.getPath() + "/" + dir);
                    boolean subdir_check = subdir.mkdirs();
                    //for every file in the subdirectory
                    for (String file : assets.list("Hari/" + dir)) {
                        //create file in subdirectory
                        File f = new File(jayDir.getPath() + "/" + dir + "/" + file);
                        if (f.exists()) {
                            continue;
                        }
                        InputStream in = null;
                        OutputStream out = null;
                        in = assets.open("Hari/" + dir + "/" + file);
                        out = new FileOutputStream(jayDir.getPath() + "/" + dir + "/" + file);
                        //copy file from assets to the mobile's SD card or any secondary memory
                        //function is below
                        copyFile(in, out);
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //get the working directory
        MagicStrings.root_path = Environment.getExternalStorageDirectory().toString() + "/hari";
        Log.i("IGOR-SPEECH","Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        //Assign the AIML files to bot for processing
        bot = new Bot("Hari", MagicStrings.root_path, "chat");
        chat = new Chat(bot);
        String[] args = null;
        mainFunction(args);

        Log.i("IGOR-SPEECH", "DURATION BOT millisec: " + (System.nanoTime() - startTime)/1000000);


        Log.i("IGOR-SPEECH","-START SILENT PRESENTATION");
        //grabbing the name from the previous activity
        Intent intent = getIntent();
        String name_received = "";
        try {
            name_received = intent.getExtras().getString("name");
        } catch (NullPointerException e) { Log.e("IGOR-SPEECH","no name received in the intent");}

        //if the name is passed/exists
        if(name_received!= null && !name_received.equals("")) {
            //creating a silent presentation with the name received
            String silent_presentation = "my name is " + name_received;
            //the silent presentation is given to the conversational engine that memorizes the name
            String response = chat.multisentenceRespond(silent_presentation);
            Log.i("IGOR-SPEECH", "Human: " + silent_presentation);
            Log.i("IGOR-SPEECH", "Robot: " + response);
        } else {
            Log.i("IGOR-SPEECH","-NAME NOT PASSED");
        }
        Log.i("IGOR-SPEECH","-FINISH SILENT PRESENTATION");

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
                Log.i("IGOR-DIAL", "WAKE UP callback");
            }

            @Override
            public void onSleep() {
                Log.i("IGOR-DIAL", "SLEEP callback");
                //infiniteWakeup is a custom variable to control the duration
                if (infiniteWakeup) {
                    //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                    speechManager.doWakeUp();
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

                //start timer
                //long startTime = System.nanoTime();
                //cast object received to text string lastRecognizedSentence
                try {
                    lastRecognizedSentence = Objects.requireNonNull(grammar.getText()).toLowerCase();
                } catch (NullPointerException e) {
                    lastRecognizedSentence = "null";
                }
                //recognized part
                //notify update to UI with a separate thread not to freeze the interface
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //update ui with text recognized
                        tvSpeechRecognizeResult.setText(lastRecognizedSentence);
                    }
                });

                //here can start the computation on the text recognized

                //IGOR: must not exceed 200ms (or less?) don't trust the documentation(500ms), I had to create an handler
                //separate handler so the function could return quickly true, otherwise the robot answers random things over your answers.
                speechResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        Log.i("IGOR-DIAL", ">>>>Recognized voice: "+ lastRecognizedSentence);
                        boolean recognizedWhatToDo = false;

                        //deletes "no response action"
                        noResponseAction.removeCallbacksAndMessages(null);


                        //---- INTERACTION PART ----
                        //basic interaction
                        if (lastRecognizedSentence.contains("hello") || lastRecognizedSentence.equals("hi")|| lastRecognizedSentence.contains("your name")) {
                            recognizedWhatToDo = true;
                            systemManager.showEmotion(EmotionsType.SMILE);
                            speechManager.startSpeak("Hi, I'm SanBot", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("how are you")) {
                            recognizedWhatToDo = true;
                            systemManager.showEmotion(EmotionsType.SMILE);
                            speechManager.startSpeak("I'm feeling full of energy", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (/*(lastRecognizedSentence.contains("what") || lastRecognizedSentence.contains("tell")) &&*/ lastRecognizedSentence.contains("time")) {
                            recognizedWhatToDo = true;
                            String time_sentence = "It's " + new SimpleDateFormat("HH:mm", Locale.ITALY).format(Calendar.getInstance().getTime());
                            speechManager.startSpeak(time_sentence, MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("hear")) {
                            recognizedWhatToDo = true;
                            speechManager.startSpeak("I can hear you.", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("what") && lastRecognizedSentence.contains("do")) {
                            recognizedWhatToDo = true;
                            speechManager.startSpeak("I can give directions, shake hands, tell the time, project a video and more...", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        if (lastRecognizedSentence.contains("hands up")) {
                            recognizedWhatToDo = true;
                            //hands up
                            AbsoluteAngleWingMotion absoluteAngleWingMotion1 = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 0);
                            wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion1);
                            systemManager.showEmotion(EmotionsType.SNICKER);
                            speechManager.startSpeak("put your hands up in the air!", MySettings.getSpeakDefaultOption());
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
                            speechManager.startSpeak("Test OK, stop testing and go working", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            askOther();
                        }
                        //---- PURPOSE  PART ----
                        if (lastRecognizedSentence.contains("shake")) {
                            recognizedWhatToDo = true;
                            //increments stats request handshake
                            MySettings.incrementRequestShake();
                            //greenFlashButton(gridExamples.getChildAt(0));
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            //starts shake hand activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyShakeActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("map")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(5));
                            speechManager.startSpeak("OK let's see the map", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //compute the url to pass (default is lisbon)
                            String url = "https://www.google.com/maps/place/Lisbon";
                            //if any separator is said the place after is passed in the url
                            String[] separators = {" of ", " in ", " on "};
                            for (String separator : separators) {
                                if (lastRecognizedSentence.contains(separator)) {
                                    String place = StringUtils.substringAfter(lastRecognizedSentence, separator);
                                    url = "https://www.google.com/maps/place/" + place;
                                    Log.e("IGOR", url);
                                }
                            }
                            //starts weather activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyWebActivity.class);
                            myIntent.putExtra("url", url);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("weather")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(5));
                            speechManager.startSpeak("OK let's see the weather", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts weather activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyWeatherActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("calendar")) {
                            recognizedWhatToDo = true;
                            speechManager.startSpeak("OK let's see the calendar", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts calendar activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyCalendarActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("present yourself")) {
                            recognizedWhatToDo = true;
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts present ys activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyPresentActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("charge") || lastRecognizedSentence.contains("battery")) {
                            recognizedWhatToDo = true;
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //starts charge activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyChargeActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }
                        if (lastRecognizedSentence.contains("meeting")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(1));
                            //increments stats request location
                            MySettings.incrementRequestLocation();
                            speechManager.startSpeak(getString(R.string.meeting_room_is), MySettings.getSpeakDefaultOption());
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
                            speechManager.startSpeak("the bathroom is south, near the stairs", MySettings.getSpeakDefaultOption());
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
                        }
                        if (lastRecognizedSentence.contains("lab") || lastRecognizedSentence.contains("laboratory")) {
                            recognizedWhatToDo = true;
                            //increments stats request location
                            MySettings.incrementRequestLocation();
                            //greenFlashButton(gridExamples.getChildAt(1));
                            speechManager.startSpeak(getString(R.string.lab_is), MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            //indicate
                            indicate(0);
                            askOther();
                        }

                        if (lastRecognizedSentence.contains("suggestion")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton(gridExamples.getChildAt(3));
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            //starts leave suggestion activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyXMLSuggestionActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);
                            //terminates
                            finish();
                        }

                        //exits if answer is "no time" or "nothing"
                        if (lastRecognizedSentence.equals("no") || lastRecognizedSentence.contains("nothing") ||
                                lastRecognizedSentence.contains("no thank you") || lastRecognizedSentence.contains("no time")||
                                lastRecognizedSentence.contains("go away") || lastRecognizedSentence.contains("exit")) {
                            recognizedWhatToDo = true;
                            //greenFlashButton( gridExamples.getChildAt(4) );
                            speechManager.startSpeak(getString(R.string.see_you), MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            if (lastRecognizedSentence.contains("go away")) {
                                speechManager.startSpeak("I will go away", MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                                systemManager.showEmotion(EmotionsType.CRY);
                                //rotates of 180
                                rotateAtRelativeAngle(wheelMotionManager, 180);
                            }
                            if (lastRecognizedSentence.contains("thank")){
                                systemManager.showEmotion(EmotionsType.KISS);
                                speechManager.startSpeak(getString(R.string.thanks_you_kind), MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                            }
                            //force listening sleep
                            infiniteWakeup = false;
                            speechManager.doSleep();

                            //starts base activity
                            Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
                            MyDialogActivity.this.startActivity(myIntent);

                            finish();
                        }


                        /*
                        //not recognized ask to repeat
                        if (!recognizedWhatToDo) {
                            systemManager.showEmotion(EmotionsType.QUESTION);
                            speechManager.startSpeak(getString(R.string.please_repeat) , MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            wakeUpListening();
                        }
                        */
                        showRandomFace();
                        //chatbot
                        long startTime = System.nanoTime();
                        String response = chat.multisentenceRespond(lastRecognizedSentence);

                        Log.i("IGOR-SPEECH","Human: "+lastRecognizedSentence);
                        Log.i("IGOR-SPEECH","Robot: " + response);

                        Log.i("IGOR-SPEECH", "DURATION COMPUTED RESPONSE millisec: " + (System.nanoTime() - startTime)/1000000);
                        speechManager.startSpeak(response , MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                        wakeUpListening();
                    }
                });

                //Log.i("IGOR-DIAL", "DURATION millisec: " + (System.nanoTime() - startTime)/1000000);
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
    protected void onMainServiceConnected() {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stop infinite listening
        infiniteWakeup = false;
        speechManager.doSleep();
        //if person pushes "back" before the normal termination
        MyBaseActivity.busy = false;
        //deletes "no response action" and all the handlers
        noResponseAction.removeCallbacksAndMessages(null);
        Log.i("IGOR-DIAL", "destroy, noResponseHandler deleted");
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
        view_passed.setBackgroundColor(getColor(color_passed));
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
                    speechManager.startSpeak(getString(R.string.please_suggestion), MySettings.getSpeakDefaultOption());
                    concludeSpeak(speechManager);
                }
                speechManager.startSpeak(getString(R.string.something_else), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                wakeUpListening();
            }
        }, seconds*1000);
    }

    public void askOther() {
        askOther(0);
    }

    private void wakeUpListening() {
        speechManager.doWakeUp();
        imageListen.setVisibility(View.VISIBLE);
        wakeButton.setVisibility(View.GONE);
        //head up
        headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
        //normal face
        //systemManager.showEmotion(EmotionsType.NORMAL);
        //activate no response handler
        Log.i("IGOR-DIAL", "wakeup answer-active, noResponseHandler activated");
        noResponseAction.removeCallbacksAndMessages(null);
        noResponseAction.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (noResponse) {
                    case 1:
                        //first time waiting too much asks for out loud
                        speechManager.startSpeak("please speak out loud!", MySettings.getSpeakDefaultOption());
                        wakeUpListening();
                        noResponse++;
                        break;
                    case 2:
                        //sends to the bot the "NO RESPONSE" code
                        String response = chat.multisentenceRespond("NORESP");

                        Log.i("IGOR-SPEECH","Human @not responded@");
                        Log.i("IGOR-SPEECH","Robot: " + response);
                        speechManager.startSpeak(response , MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                        wakeUpListening();
                        noResponse++;
                        break;
                    default:
                        //force sleep
                        infiniteWakeup = false;
                        speechManager.doSleep();
                        //angry emotion
                        systemManager.showEmotion(EmotionsType.ANGRY);
                        //flicker leds
                        hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_RED));
                        //speak
                        speechManager.startSpeak(getString(R.string.no_answer), MySettings.getSpeakDefaultOption());
                        //starts base activity
                        Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
                        MyDialogActivity.this.startActivity(myIntent);
                        //terminates
                        finish();
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
        speechManager.startSpeak("in that direction!", MySettings.getSpeakDefaultOption());
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
        Log.i("IGOR-DIAL", "Random for face = " + random_num);
        //probability not to show any face
        if (random_num > 0.5) {
            //showing random face
            int min = 0, max = 20;
            int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
            switch (randomNum) {
                case 1:
                    systemManager.showEmotion(EmotionsType.ABUSE);
                    break;
                case 2:
                    systemManager.showEmotion(EmotionsType.ANGRY);
                    break;
                case 3:
                    systemManager.showEmotion(EmotionsType.ARROGANCE);
                    break;
                case 4:
                    systemManager.showEmotion(EmotionsType.CRY);
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

    //CHATBOT FUNCTIONS
    //check SD card availability
    public static boolean isSDCARDAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)? true :false;
    }
    //copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    //Request and response of user and the bot
    public static void mainFunction (String[] args) {
        MagicBooleans.trace_mode = false;
        Log.i("IGOR-SPEECH","trace mode = " + MagicBooleans.trace_mode);
        Graphmaster.enableShortCuts = true;
    }


    //END
}

package com.sanbot.capaBot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sanbot.capaBot.video.VisionMediaDecoder;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.beans.OperationResult;
import com.sanbot.opensdk.function.beans.FaceRecognizeBean;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.StreamOption;
import com.sanbot.opensdk.function.beans.headmotion.LocateAbsoluteAngleHeadMotion;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.beans.speech.SpeakStatus;
import com.sanbot.opensdk.function.beans.wing.AbsoluteAngleWingMotion;
import com.sanbot.opensdk.function.beans.wing.NoAngleWingMotion;
import com.sanbot.opensdk.function.beans.wing.RelativeAngleWingMotion;
import com.sanbot.opensdk.function.unit.HDCameraManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.GyroscopeListener;
import com.sanbot.opensdk.function.unit.interfaces.hardware.PIRListener;
import com.sanbot.opensdk.function.unit.interfaces.hardware.TouchSensorListener;
import com.sanbot.opensdk.function.unit.interfaces.hardware.VoiceLocateListener;
import com.sanbot.opensdk.function.unit.interfaces.media.FaceRecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.media.MediaStreamListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.SpeakListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.sanbot.capaBot.MyUtils.compensationSanbotAngle;
import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyUtils.rotateAtRelativeAngle;

/**
 * Starting Activity.
 *  Handles the default state of the robot, looking for someone to help.
 *  The robot (optionally) wanders around.
 *  The process of sound localization redirects the robot towards noise/calls.
 *  Face Localization is active to start dialog (DialogActivity) if somebody is looking at the robot.
 *  if the battery is low the robot goes to the ChargeActivity to go back to charging station.
 */
public class  MyBaseActivity extends TopBaseActivity implements SurfaceHolder.Callback {

    private final static String TAG = "IGOR-BAS";

    //view objects
    @BindView(R.id.sv_media)
    SurfaceView svMedia;
    @BindView(R.id.tv_capture)
    TextView tvCapture;
    @BindView(R.id.iv_capture)
    ImageView ivCapture;
    @BindView(R.id.battery_base)
    TextView batteryTV;
    @BindView(R.id.imageCompass)
    ImageView imageComp;
    @BindView(R.id.textCompass)
    TextView textComp;
    @BindView(R.id.name_face)
    TextView nameFace;
    @BindView(R.id.time_face)
    TextView time_face;
    @BindView(R.id.info_face)
    TextView infoFace;
    @BindView(R.id.handshakes)
    TextView handshakesTextView;
    @BindView(R.id.presentationLock)
    TextView presentationLock;
    @BindView(R.id.start_interaction)
    Button startInteraction;
    @BindView(R.id.loading)
    TextView loadingText;
    @BindView(R.id.exit)
    Button exitButt;
    @BindView(R.id.settings)
    Button settingsButt;
    @BindView(R.id.DEBUG)
    LinearLayout debugLayout;

    //robot managers
    private HDCameraManager hdCameraManager; //video, faceRec
    private SpeechManager speechManager; //voice, speechRec
    private HeadMotionManager headMotionManager;    //head movements
    private WingMotionManager wingMotionManager;    //hands movements
    private SystemManager systemManager; //emotions
    private HardWareManager hardWareManager; //leds //touch sensors //voice locate //gyroscope
    private ModularMotionManager modularMotionManager; //wander
    private WheelMotionManager wheelMotionManager;

    //video stuff
    private VisionMediaDecoder mediaDecoder;
    private List<Integer> handleList = new ArrayList<>();
    private int mWidth, mHeight;


    /*** MY VARIABLES ***/
    //better with getter & setter
    public static boolean busy = false;

    //if it's just greeted with someone it doesn't greet for X seconds (timer starts from beginning of presentation)
    private boolean justGreeted = false;

    Handler wanderHandler = new Handler();
    Handler checkBatteryStatusHandler = new Handler();
    Handler checkBotReadyHandler = new Handler();

    private CountDownTimer countDownPresentationLocked;

    private String lastRecognizedSentence = "";

    boolean paused = false;

    //hand motion
    private byte handAb = AbsoluteAngleWingMotion.PART_LEFT;
    private byte handRe = RelativeAngleWingMotion.PART_LEFT;
    NoAngleWingMotion noAngleWingMotionUP = new NoAngleWingMotion(NoAngleWingMotion.PART_LEFT, 5, NoAngleWingMotion.ACTION_UP);
    NoAngleWingMotion noAngleWingMotionDOWN = new NoAngleWingMotion(NoAngleWingMotion.PART_LEFT, 5, NoAngleWingMotion.ACTION_DOWN);
    NoAngleWingMotion noAngleWingMotionSTOP = new NoAngleWingMotion(NoAngleWingMotion.PART_LEFT, 5, NoAngleWingMotion.ACTION_STOP);

    //head motion
    LocateAbsoluteAngleHeadMotion locateAbsoluteAngleHeadMotion = new LocateAbsoluteAngleHeadMotion(
            LocateAbsoluteAngleHeadMotion.ACTION_VERTICAL_LOCK,90,30
    );
    RelativeAngleHeadMotion relativeHeadMotionDOWN = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, 30);

    // record the compass picture angle turned
    private float currentAnimationDegree = 0f;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyBaseActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //set view
        setContentView(R.layout.activity_base);
        ButterKnife.bind(this);
        //set visibility of loading and button
        loadingText.setVisibility(View.VISIBLE);
        startInteraction.setVisibility(View.GONE);
        if(MySettings.isDebug()){
            debugLayout.setVisibility(View.VISIBLE);
        } else {
            debugLayout.setVisibility(View.GONE);
        }
        //initialize managers
        hdCameraManager = (HDCameraManager) getUnitManager(FuncConstant.HDCAMERA_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        wingMotionManager = (WingMotionManager) getUnitManager(FuncConstant.WINGMOTION_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        //for video view on screen
        svMedia.getHolder().addCallback(this);
        mediaDecoder = new VisionMediaDecoder();
        //float button of the system
        systemManager.switchFloatBar(true, getClass().getName());

        //check app permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE}, 12);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 12);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, 12);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{INTERNET}, 12);
        }
        //LOAD handshakes stats
        MySettings.initializeXML();
        MySettings.loadHandshakes();

        //initialize speak
        MySettings.initializeSpeak();

        //initialize listeners of hardware
        initHardwareListeners();

        //initialize listeners of view (buttons/switch listeners)
        initViewListeners();

        //initialize body
        wanderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //hands down
                AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 180);
                wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
                //head up
                headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                //initially sets the wander to on
                wanderOnNow();
            }
        }, 1000);

        //cyclic check battery
        checkBatteryStatusHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateView();
                //grab battery value
                int battery_value = systemManager.getBatteryValue();
                Log.i("IGOR-BAS-BAT", "Battery: "+ battery_value);
                //if battery connected deactivate autocharge
                if (systemManager.getBatteryStatus() == systemManager.STATUS_CHARGE_LINE || systemManager.getBatteryStatus() == systemManager.STATUS_CHARGE_PILE) {
                    MySettings.setAutoChargeAllowed(false);
                    Log.i("IGOR-BAS-BAT", "Battery charging already with line or charge pile");
                }
                //check if the charge is low
                if (MySettings.isAutoChargeAllowed() && (battery_value <= MySettings.getBatteryLOW())) {
                    //starts charge activity
                    Intent myIntent = new Intent(MyBaseActivity.this, MyChargeActivity.class);
                    MyBaseActivity.this.startActivity(myIntent);
                    //finish
                    finish();
                    return;
                } else {
                    //otherwise re-post the same handler in X seconds
                    checkBatteryStatusHandler.postDelayed(this, 1000 * MySettings.getSeconds_checkingBattery());
                }
            }
        }, 1000*MySettings.getSeconds_checkingBattery());


        //cyclic check bot loaded
        checkBotReadyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("IGOR-BAS-BOT", "BOT: "+ MyApp.botReady);
                if (MyApp.botReady){
                    loadingText.setVisibility(View.GONE);
                    startInteraction.setVisibility(View.VISIBLE);
                    checkBotReadyHandler.removeCallbacksAndMessages(null);
                    updateView();
                } else {
                    //otherwise re-post the same handler in X seconds
                    checkBotReadyHandler.postDelayed(this, 500);
                }
            }
        }, 500);


        //update view (to update handshakes)
        updateView();

        //when everything initialized is not busy
        busy = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //kill the handlers
        checkBatteryStatusHandler.removeCallbacksAndMessages(null);
        wanderHandler.removeCallbacksAndMessages(null);
        //stop moving
        wanderOffNow();
    }

    /**
     * initialize listeners
     */
    private void initHardwareListeners() {
        //face recognition
        hdCameraManager.setMediaListener(new FaceRecognizeListener() {
            @Override
            public void recognizeResult(@NonNull List<FaceRecognizeBean> list) {
                //taking face info
                StringBuilder sb = new StringBuilder();
                for (FaceRecognizeBean bean : list) {
                    sb.append(new Gson().toJson(bean));
                    sb.append("\n");
                }
                infoFace.setText(sb.toString());
                //taking face name
                String user_name = list.get(0).getUser();
                if (user_name != null){
                    nameFace.setText(user_name);
                } else {
                    nameFace.setText(R.string.unknown);
                }
                //time of detection
                time_face.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ITALY).format(Calendar.getInstance().getTime()));

                Log.i(TAG,">>>>FACE DETECTED " + user_name );

                //selects function
                if (!busy) {
                    if (!speechManager.isSpeaking().equals("1") ) {
                        if (!justGreeted) {
                            //responses
                            //stop wander
                            wanderOffNow();
                            //increment stats of face interaction
                            MySettings.incrementInteractionFace();
                            if (user_name != null) {
                                //PERSON IS KNOWN
                                //passing the name
                                startInteraction(user_name);
                            } else {
                                //PERSON UNKNOWN
                                //deactivated riseHand presentation, the face recognition is too weak and the robot keeps presenting itself
                                //if(MySettings.isPresentationBeforeDialog()) {riseHand();}
                                startInteraction("");
                            }
                        } else {
                            Log.i(TAG, "justGreeted = true, detection aborted");
                        }
                    } else {
                        Log.i(TAG, "is speaking, detection aborted");
                    }
                } else {
                    Log.i(TAG, "is busy, detection aborted");
                }

            }
        });
        //media
        hdCameraManager.setMediaListener(new MediaStreamListener() {
            @Override
            public void getVideoStream(int handle, byte[] bytes, int width, int height) {
                if (mediaDecoder != null) {
                    if (width != mWidth || height != mHeight) {
                        mediaDecoder.onCreateCodec(width, height);
                        mWidth = width;
                        mHeight = height;
                    }
                    mediaDecoder.drawVideoSample(ByteBuffer.wrap(bytes));
                    // Log.i(TAG, "getVideoStream: Video data:" + bytes.length);
                }
            }

            @Override
            public void getAudioStream(int i, @NonNull byte[] bytes) {

            }
        });
        //voice
        //Set wakeup/sleep sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {

            }

            @Override
            public void onWakeUp() {
                Log.i(TAG, "wake up !");
            }
            @Override
            public void onSleep() {
                Log.i(TAG, "sleep !");
            }
        });

        //Speech recognition callback
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText( @NonNull RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                //IGOR: not exceed 300ms
                //Blocked only if RECOGNIZE_MODE is set to 1 in Manifest and this function returns true
                lastRecognizedSentence = grammar.getText().toLowerCase();
                Log.i(TAG, ">>>>Recognized voice: "+ lastRecognizedSentence + "/"+ grammar.getTopic());
                return true;
            }

            @Override
            public void onRecognizeVolume(int i) {
                //value range at 0~30
                Log.i("speechmanager", "volume detected to "+ String.valueOf(i));
            }

            @Override
            public void onStartRecognize() {
                Log.i("speechmanager", "onStartRecognize ");
            }

            @Override
            public void onStopRecognize() {
                Log.i("speechmanager", "onStopRecognize ");
            }

            @Override
            public void onError(int i, int i1) {
                Log.i("speechmanager", "onError: i="+i+" i1="+i1);
            }
        });
        //Speech synthesis state callback
        speechManager.setOnSpeechListener(new SpeakListener() {
            @Override
            public void onSpeakStatus(SpeakStatus speakStatus) {
                if (speakStatus != null) {
                    Log.e("speechmanager", "" + speakStatus.getProgress());
                }
            }
        });
        //pir localization
        hardWareManager.setOnHareWareListener(new PIRListener() {
            @Override
            public void onPIRCheckResult(boolean isCheck, int part) {
                if(part != 1) {
                    //if it's the back PIR
                    Log.i(TAG, "PIR back triggered -> rotating");
                    if (!busy && MySettings.isSoundRotationAllowed()) {
                        //flicker led
                        hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_PINK));
                        //rotate at angle
                        rotateAtRelativeAngle(wheelMotionManager, 180);
                    }
                } else {
                    Log.i(TAG, "PIR frontal triggered");
                }
            }
        });
        //voice angle localization
        hardWareManager.setOnHareWareListener(new VoiceLocateListener() {
            @Override
            public void voiceLocateResult(int angle) {
                Log.i(TAG,"voice located at : " + angle);
                //if it is idle
                if (!busy && MySettings.isSoundRotationAllowed()) {
                    //stop wander
                    wanderOffNow();
                    //head up
                    headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                    //flicker led
                    hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_BLUE));
                    //rotate at angle
                    rotateAtRelativeAngle(wheelMotionManager, angle);

                    //wanderOn after a while
                    //handler to avoid motor overlapping
                    wanderHandler.removeCallbacksAndMessages(null);
                    wanderHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            wanderOnNow();
                            wanderOffNow();
                            wanderOnNow();
                        }
                    }, 1000* MySettings.getSeconds_waitingToWanderAfterSoundLocalization());
                }
            }
        });
        //Gyro callback
        hardWareManager.setOnHareWareListener(new GyroscopeListener() {
            @Override
            public void gyroscopeCheckResult(boolean b, boolean b1) {

            }

            @Override
            public void gyroscopeData(float v, float v1, float v2) {
                Log.i("gyro", "GYRO first: " + v + ", second: " + v1 + ", third: " + v2);

                // get the angle corrected
                float degreeCorrected = compensationSanbotAngle(v);
                Log.i("gyro", "compass corrected: " + degreeCorrected);
                //get the facing direction
                String facing;
                if (degreeCorrected > 45 && degreeCorrected <= 135) {
                    facing = "EAST";
                } else if (degreeCorrected > 135 && degreeCorrected <= 225) {
                    facing = "SOUTH";
                } else if (degreeCorrected > 225 && degreeCorrected <= 315) {
                    facing = "WEST";
                } else  {
                    facing = "NORTH";
                }
                String correctedStr ="Corrected: " + (int)degreeCorrected + " degrees (" +(int)v+"), facing: " + facing;
                textComp.setText(correctedStr);
                //animation
                // create a rotation animation (reverse turn degree degrees)
                RotateAnimation ra = new RotateAnimation(
                        currentAnimationDegree,
                        -degreeCorrected,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);
                // how long the animation will take place
                ra.setDuration(210);
                // set the animation after the end of the reservation status
                ra.setFillAfter(true);
                // Start the animation
                imageComp.startAnimation(ra);
                currentAnimationDegree = -degreeCorrected;
            }
        });
        //hardware touch
        hardWareManager.setOnHareWareListener(
                new TouchSensorListener() {
                    @Override
                    public void onTouch(int i, boolean b) {

                    }

                    @Override
                    public void onTouch(int part) {
                        switch (part) {
                            case 9:
                                Log.i("hwmanager", "touching hand left");
                                break;
                            case 10:
                                Log.i("hwmanager", "touching hand right" );
                                break;
                            case 1 : case 2:
                                speechManager.startSpeak("ehy, don't touch", MySettings.getSpeakDefaultOption());

                        }
                    }
                }
        );


    }

    private void initViewListeners() {
        //start interaction button
        startInteraction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!busy) {
                    wanderOffNow();
                    //increment stats of button interaction
                    MySettings.incrementInteractionButton();
                    startInteraction("");
                }
            }
        });
        //exit button
        exitButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wanderHandler.removeCallbacksAndMessages(null);
                wanderOffNow();
                finish();
                return;
            }
        });
        //settings button
        settingsButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!busy) {
                    wanderOffNow();
                    //starts settings activity
                    Intent myIntent = new Intent(MyBaseActivity.this, MySettings.class);
                    MyBaseActivity.this.startActivity(myIntent);
                    //finish
                    finish();
                    return;
                }
            }
        });
    }


    @Override
    protected void onMainServiceConnected() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //resume after another activity occurred (typically Dialog)
        paused = false;
        updateView();
        busy = false;
        //new Handler to start walking again
        wanderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //hands down
                AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 180);
                wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
                //head up
                headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                //initially sets the wander to on
                wanderOnNow();
                wanderOffNow();
                wanderOnNow();
            }
        }, 1000);
        //stops the robot to start another recognition for X seconds
        justGreeted = true;
        if(MySettings.isDebug()){
            presentationLock.setVisibility(View.VISIBLE);
        }
        //if there is a previous timer this is cancelled
        if(countDownPresentationLocked != null) {
            countDownPresentationLocked.cancel();
        }
        //new Timer to unlock the interaction after secondsJustGreeted
        countDownPresentationLocked = new CountDownTimer(MySettings.getSeconds_justGreeted()*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                String presentation_locked = getString(R.string.presentation_locked) + " " + millisUntilFinished / 1000;
                presentationLock.setText(presentation_locked);
            }

            public void onFinish() {
                justGreeted = false;
                presentationLock.setVisibility(View.GONE);
            }
        }.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Set parameters and open the media stream for video
        StreamOption streamOption = new StreamOption();
        streamOption.setChannel(StreamOption.MAIN_STREAM);
        streamOption.setDecodType(StreamOption.HARDWARE_DECODE);
        streamOption.setJustIframe(false);
        OperationResult operationResult = hdCameraManager.openStream(streamOption);
        Log.i(TAG, "surfaceCreated: operationResult=" + operationResult.getResult());
        int result = Integer.valueOf(operationResult.getResult());
        if (result != -1) {
            handleList.add(result);
        }
        mediaDecoder.setSurface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        //Close media stream
        if (handleList.size() > 0) {
            for (int handle : handleList) {
                Log.i(TAG, "surfaceDestroyed: " + hdCameraManager.closeStream(handle));
            }
        }
        handleList = null;
        mediaDecoder.stopDecoding();
    }

    //debug buttons
    @OnClick({R.id.tv_capture, R.id.knowYouMeeting, R.id.firstMeeting})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_capture:
                storeImage(hdCameraManager.getVideoImage());
                ivCapture.setImageBitmap(hdCameraManager.getVideoImage());
                break;

            case R.id.knowYouMeeting:
                wanderOffNow();
                startInteraction("debugger");
                break;

            case R.id.firstMeeting:
                wanderOffNow();
                startInteraction("");
                break;
        }
    }

    public void storeImage(Bitmap bitmap){
        String dir = Environment.getExternalStorageDirectory().getPath() +"/CAPABOT/";
        final File f = new File(dir);
        if (!f.exists()) {
            final boolean mkdirs = f.mkdirs();
            if (!mkdirs) Log.e("storeImage", "Error in mkdirs");
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(f, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startInteraction(String person_name) {
        if (!busy && MyApp.botReady) {
            Log.i(TAG, "START INTERACTION! now busy");
            //starts greeting with this person passing
            busy = true;
            //say hi
            speechManager.startSpeak(getString(R.string.hi) + person_name, MySettings.getSpeakDefaultOption());
            concludeSpeak(speechManager);

            // 50% say Good morning/afternoon/ecc...
            double random_num = Math.random();
            if (random_num < 0.5) {
                int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hours < 6) {
                    speechManager.startSpeak(getString(R.string.early_morning), MySettings.getSpeakDefaultOption());
                } else if (hours < 12) {
                    speechManager.startSpeak(getString(R.string.morning), MySettings.getSpeakDefaultOption());
                } else if (hours < 18) {
                    speechManager.startSpeak(getString(R.string.afternoon), MySettings.getSpeakDefaultOption());
                } else if (hours <= 24) {
                    speechManager.startSpeak(getString(R.string.night), MySettings.getSpeakDefaultOption());
                }
                concludeSpeak(speechManager);
            }

            //start the dialog activity.
            Intent myIntent = new Intent(MyBaseActivity.this, MyDialogActivity.class);
            //insert the name of the person in the annex
            myIntent.putExtra("name", person_name);
            MyBaseActivity.this.startActivity(myIntent);
            //terminate this activity
            finish();
        }
    }



    public void wanderOnNow() {
        if (!busy) {
            if (MySettings.isDebug()){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyBaseActivity.this, "Wander " + MySettings.isWanderAllowed()+" now", Toast.LENGTH_SHORT).show();
                    }
                });}
            modularMotionManager.switchWander(MySettings.isWanderAllowed());
            Log.i(TAG, "Wander " + MySettings.isWanderAllowed() + " now");
        }
    }

    public void wanderOffNow() {
        if (MySettings.isDebug()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyBaseActivity.this, "Wander off now", Toast.LENGTH_SHORT).show();
                }
            });}
        modularMotionManager.switchWander(false);
        Log.i(TAG, "Wander forced off now");
    }

    public void updateView() {
        //update handshakesTextView
        String handshakesStr = getString(R.string.handshakes) + " " + MySettings.getHandshakes();
        handshakesTextView.setText(handshakesStr);
        //battery
        int battery_value = systemManager.getBatteryValue();
        batteryTV.setText("Battery: " + battery_value + "%");
    }

    //END
}

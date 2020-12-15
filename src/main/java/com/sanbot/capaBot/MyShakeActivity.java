package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.handmotion.AbsoluteAngleHandMotion;
import com.sanbot.opensdk.function.beans.handmotion.NoAngleHandMotion;
import com.sanbot.opensdk.function.beans.handmotion.RelativeAngleHandMotion;
import com.sanbot.opensdk.function.beans.headmotion.LocateAbsoluteAngleHeadMotion;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.unit.HandMotionManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.TouchSensorListener;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.sleepy;
import static com.sanbot.capaBot.MyUtils.temporaryEmotion;

public class MyShakeActivity extends TopBaseActivity {


    @BindView(R.id.handshakes_sa)
    TextView handshakes;

    @BindView(R.id.shakeLayout)
    LinearLayout shakeLayout;


    //robot managers
    private SpeechManager speechManager; //voice, speechRec
    private HeadMotionManager headMotionManager;    //head movements
    private HandMotionManager handMotionManager;    //hands movements
    private SystemManager systemManager; //emotions
    private HardWareManager hardWareManager; //leds //touch sensors //voice locate //gyroscope


    //to understand if it is in the position waiting the touch of the hand
    private boolean waitingTouchPosition = false;
    Handler waitingTouchHandler = new Handler();
    Handler incitement = new Handler();

    //hand motion
    private byte handAb = AbsoluteAngleHandMotion.PART_LEFT;
    private byte handRe = RelativeAngleHandMotion.PART_LEFT;
    NoAngleHandMotion noAngleHandMotionUP = new NoAngleHandMotion(NoAngleHandMotion.PART_LEFT, 5, NoAngleHandMotion.ACTION_UP);
    NoAngleHandMotion noAngleHandMotionDOWN = new NoAngleHandMotion(NoAngleHandMotion.PART_LEFT, 5, NoAngleHandMotion.ACTION_DOWN);
    NoAngleHandMotion noAngleHandMotionSTOP = new NoAngleHandMotion(NoAngleHandMotion.PART_LEFT, 5, NoAngleHandMotion.ACTION_STOP);

    //head motion
    LocateAbsoluteAngleHeadMotion locateAbsoluteAngleHeadMotion = new LocateAbsoluteAngleHeadMotion(
            LocateAbsoluteAngleHeadMotion.ACTION_VERTICAL_LOCK,90,30
    );
    RelativeAngleHeadMotion relativeHeadMotionDOWN = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, 30);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyShakeActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //set view
        setContentView(R.layout.activity_shake);
        ButterKnife.bind(this);
        //init managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        handMotionManager = (HandMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        //initialize listeners
        initListener();

        //initialize speak
        MySettings.initializeSpeak();

        //update handshakesTextView
        updateHandshakes();

        //initialize body after 1 sec
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //hands down
                AbsoluteAngleHandMotion absoluteAngleHandMotion = new AbsoluteAngleHandMotion(AbsoluteAngleHandMotion.PART_BOTH, 8, 180);
                handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
                //head up
                headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                //calls first meeting presentation
                firstMeeting();

            }
        }, 1000);

    }


    /**
     * initialize listeners
     */
    private void initListener() {
        //hardware touch
        hardWareManager.setOnHareWareListener(
                new TouchSensorListener() {
                    @Override
                    public void onTouch(int part) {
                        switch (part) {
                            case 9:
                                Log.i("hwmanager", "touching hand left");
                                if (waitingTouchPosition) {
                                    Log.i("IGOR", "shake hand called");
                                    shakeHand();
                                }
                                break;
                            case 10:
                                Log.i("hwmanager", "touching hand right" );
                                //if is waiting in the position
                                if (waitingTouchPosition) {
                                    Log.i("IGOR", "shake hand called");
                                    shakeHand();
                                }
                                break;
                        }
                    }
                }
        );
    }

    @Override
    protected void onMainServiceConnected() {

    }


    /****** my functions *******/
    public void firstMeeting () {

        //todo align with person?

        //up the head
        headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);


        //hand up
        AbsoluteAngleHandMotion absoluteAngleHandMotion = new AbsoluteAngleHandMotion(handAb, 5, 70);
        handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
        //self presentation
        speechManager.startSpeak(getString(R.string.i_am_sanbot), MySettings.getSpeakDefaultOption());

        //waiting touch
        waitingTouchPosition = true;
        Log.i("IGOR", "waitingTouchPosition = true");

        //waiting touch too much event
        waitingTouchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("IGOR", "no touched hand in time");
                //waiting touch false
                waitingTouchPosition = false;
                Log.i("IGOR", "waitingTouchPosition = false ");
                //hand down
                AbsoluteAngleHandMotion absoluteAngleHandMotion = new AbsoluteAngleHandMotion(handAb, 5, 180);
                handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
                //cry face
                temporaryEmotion(systemManager, EmotionsType.CRY);
                //down the head
                headMotionManager.doRelativeAngleMotion(relativeHeadMotionDOWN);
                //up head after 10 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotion);
                    }
                }, 10000);

                //sad sentence
                speechManager.startSpeak(getString(R.string.sad_no_shake), MySettings.getSpeakDefaultOption());
                sleepy(8);

                //starts dialog activity
                Intent myIntent = new Intent(MyShakeActivity.this, MyDialogActivity.class);
                MyShakeActivity.this.startActivity(myIntent);

                //finish
                finish();
            }
        }, 1000 * MySettings.getSeconds_waitingTouch());
        //incitement in middle
        incitement.postDelayed(new Runnable() {
            @Override
            public void run() {
                speechManager.startSpeak(getString(R.string.come_on), MySettings.getSpeakDefaultOption());
                //change image
                shakeLayout.setBackgroundResource(R.drawable.handred);
            }
        }, 500 * MySettings.getSeconds_waitingTouch());
    }

    public void shakeHand() {
        //SHAKING HAND MOMENT
        //cancel "waiting touch too much" response
        waitingTouchHandler.removeCallbacksAndMessages(null);
        incitement.removeCallbacksAndMessages(null);
        Log.i("IGOR", "handler waitingTouchHandler deleted!");

        //waiting touch false, no more waiting
        waitingTouchPosition = false;
        Log.i("IGOR", "waitingTouchPosition = false ");

        MySettings.incrementHandshakes();
        updateHandshakes();

        //happy face
        temporaryEmotion(systemManager, EmotionsType.SMILE, 5);

        //flicker leds
        hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_PURPLE));
        //led off
        //hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_CLOSE, (byte) 1, (byte) 1));

        //Shake hands
        /*
        //absolute shaking
        sleepy(1);
        absoluteAngleHandMotion = new AbsoluteAngleHandMotion( handAb, 8, 50);
        handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
        sleepy(1);
        absoluteAngleHandMotion = new AbsoluteAngleHandMotion( handAb, 8, 70);
        handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
        sleepy(1);
        absoluteAngleHandMotion = new AbsoluteAngleHandMotion( handAb, 8, 50);
        handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
        sleepy(1);
        absoluteAngleHandMotion = new AbsoluteAngleHandMotion( handAb, 8, 70);
        handMotionManager.doAbsoluteAngleMotion(absoluteAngleHandMotion);
        */

        /*
        //relative shaking
        RelativeAngleHandMotion relativeAngleHandMotionUP = new RelativeAngleHandMotion(handRe, 10, RelativeAngleHandMotion.ACTION_UP, 10);
        RelativeAngleHandMotion relativeAngleHandMotionDOWN = new RelativeAngleHandMotion(handRe, 10, RelativeAngleHandMotion.ACTION_DOWN, 10);
        handMotionManager.doRelativeAngleMotion(relativeAngleHandMotionDOWN);
        sleepy(1);
        handMotionManager.doRelativeAngleMotion(relativeAngleHandMotionUP);
        sleepy(1);
        handMotionManager.doRelativeAngleMotion(relativeAngleHandMotionDOWN);
        sleepy(1);
        handMotionManager.doRelativeAngleMotion(relativeAngleHandMotionUP);
        sleepy(1);
        */


        //motion without angle
        handMotionManager.doNoAngleMotion(noAngleHandMotionUP);
        sleepy(0.5);
        handMotionManager.doNoAngleMotion(noAngleHandMotionDOWN);
        sleepy(0.5);
        handMotionManager.doNoAngleMotion(noAngleHandMotionUP);
        sleepy(0.5);
        handMotionManager.doNoAngleMotion(noAngleHandMotionDOWN);
        sleepy(0.5);
        handMotionManager.doNoAngleMotion(noAngleHandMotionUP);
        sleepy(0.5);
        handMotionManager.doNoAngleMotion(noAngleHandMotionDOWN);




        //hands down (reset position)
        handMotionManager.doNoAngleMotion(new NoAngleHandMotion(NoAngleHandMotion.PART_BOTH, 5,NoAngleHandMotion.ACTION_RESET));


        //after shaking sentence
        speechManager.startSpeak(getString(R.string.glad_meet_you), MySettings.getSpeakDefaultOption());
        sleepy(3);

        //starts dialog activity
        Intent myIntent = new Intent(MyShakeActivity.this, MyDialogActivity.class);
        MyShakeActivity.this.startActivity(myIntent);

        finish();
    }



    public void updateHandshakes() {
        handshakes.setText(getString(R.string.handshakes) + " " + MySettings.getHandshakes());
    }
}

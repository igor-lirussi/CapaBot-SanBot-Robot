package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyUtils.sleepy;

/**
 * When the robot is low on battery sends the robot to charge
 * if the robot is moved/unplugged/poked sends back the robot to charge
 * once finished charging starts back the BaseActivity
 */
public class MyChargeActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-CHARGE";

    @BindView(R.id.exit)
    Button exitButton;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.battery_percent)
    TextView batteryPercent;

    private SpeechManager speechManager; //voice, speechRec
    private SystemManager systemManager; //emotion, battery level
    private WheelMotionManager wheelMotionManager; //forward run
    private ModularMotionManager modularMotionManager; //battery autocharge

    Handler checkBatteryStatus = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyChargeActivity.class);
        //todo check if works better with no "screen always on"
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_charge);

        ButterKnife.bind(this);

        //initialize managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCharging();
            }
        });

        //progress bar layout
        progressBar.setProgressDrawable(getDrawable(R.drawable.progressbar));
        //set bar progress
        progressBar.setProgress(systemManager.getBatteryValue());
        batteryPercent.setText(systemManager.getBatteryValue()+" %");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speechManager.startSpeak("Time to charge a little bit!", MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                //activate auto charge
                modularMotionManager.switchCharge(true);
            }
        }, 1000);


        //cyclic check battery
        checkBatteryStatus.postDelayed(new Runnable() {
            @Override
            public void run() {
                //show emotion
                systemManager.showEmotion(EmotionsType.SLEEP);
                //to avoid bounce let's check every time if it's charging
                modularMotionManager.switchCharge(true);
                //if still not charging (glitch of SDK) switch charge off and then on
                if (!modularMotionManager.getAutoChargeStatus().getResult().equals("1")) {
                    Log.i(TAG, "battery still not charging");
                    modularMotionManager.switchCharge(false);
                    modularMotionManager.switchCharge(true);
                }
                //grab battery value
                int battery_value = systemManager.getBatteryValue();
                Log.i(TAG, "battery: "+ battery_value);
                //update UI
                progressBar.setProgress(battery_value);
                batteryPercent.setText(battery_value+" %");
                //check if the charge is enough
                if (battery_value >= MySettings.getBatteryOK()) {
                    finishCharging();
                } else {
                    //re-post the same handler in X seconds
                    checkBatteryStatus.postDelayed(this, 1000 * MySettings.getSeconds_checkingBattery());
                }
            }
        }, 1000*MySettings.getSeconds_checkingBattery());
    }

    @Override
    protected void onMainServiceConnected() {}



    private void finishCharging() {
        //kill loop check handler
        checkBatteryStatus.removeCallbacksAndMessages(null);
        //sentence
        speechManager.startSpeak("I'm charged "+systemManager.getBatteryValue()+"%, I think it's ok", MySettings.getSpeakDefaultOption());
        concludeSpeak(speechManager);
        //deactivate auto charge
        modularMotionManager.switchCharge(false);
        //go ahead 20 cm
        DistanceWheelMotion distanceWheelMotion = new DistanceWheelMotion(
                DistanceWheelMotion.ACTION_FORWARD_RUN,  5,20
        );
        wheelMotionManager.doDistanceMotion(distanceWheelMotion);
        sleepy(5);
        //starts base activity
        Intent myIntent = new Intent(MyChargeActivity.this, MyBaseActivity.class);
        MyChargeActivity.this.startActivity(myIntent);
        //finish
        finish();
    }


}

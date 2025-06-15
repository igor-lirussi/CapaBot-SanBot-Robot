package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.function.beans.SpeakOption;
import com.sanbot.opensdk.function.unit.ProjectorManager;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtilsXML.createFileInXMLDirectory;
import static com.sanbot.capaBot.MyUtilsXML.xmlAddToNodeNow;
import static com.sanbot.capaBot.MyUtilsXML.xmlReadStatsHandshakesNumber;

/**
 * MySettings class
 * used to save all the settings about the robot
 */
public class MySettings extends TopBaseActivity {

    private final static String TAG = "IGOR-SETTINGS";

    @BindView(R.id.switchWander)
    Switch switchButtonWander;
    @BindView(R.id.switchSoundRot)
    Switch switchSoundRotation;
    @BindView(R.id.switchAutocharge)
    Switch switchAutocharge;
    @BindView(R.id.cityWeather)
    EditText cityWeatherInpText;
    @BindView(R.id.cityMap)
    EditText cityMapInpText;
    @BindView(R.id.seconds_justGreeted)
    EditText seconds_justGreetedInpText;
    @BindView(R.id.seconds_waitingTouch)
    EditText seconds_waitingTouchInpText;
    @BindView(R.id.seconds_waitingResponse)
    EditText seconds_waitingResponseInpText;
    @BindView(R.id.seconds_checkingBattery)
    EditText seconds_checkingBatteryInpText;
    @BindView(R.id.seconds_waitingToWanderAfterSoundLocalization)
    EditText seconds_waitingToWanderAfterSoundLocalizationInpText;
    @BindView(R.id.batteryLOW)
    EditText batteryLowInpText;
    @BindView(R.id.batteryOK)
    EditText batteryOKInpText;


    @BindView(R.id.debugMode)
    Switch switchDebugMode;
    @BindView(R.id.projectCeiling)
    Switch switchProjectCeiling;
    @BindView(R.id.testConvEng)
    Button testConvEngButt;


    @BindView(R.id.exit)
    Button exitButton;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MySettings.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //set view
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        //UPDATE VIEW
        //update the switches
        switchDebugMode.setChecked(isDebug());
        switchButtonWander.setChecked(isWanderAllowed());
        switchSoundRotation.setChecked(isSoundRotationAllowed());
        switchAutocharge.setChecked(isAutoChargeAllowed());
        switchProjectCeiling.setChecked(isProject_on_ceiling());
        //update city settings
        cityWeatherInpText.setText(getCityWeather());
        cityMapInpText.setText(getCityMap());
        //update seconds settings
        seconds_justGreetedInpText.setText(String.valueOf(getSeconds_justGreeted()));
        seconds_waitingTouchInpText.setText(String.valueOf(getSeconds_waitingTouch()));
        seconds_waitingResponseInpText.setText(String.valueOf(getSeconds_waitingResponse()));
        seconds_checkingBatteryInpText.setText(String.valueOf(getSeconds_checkingBattery()));
        seconds_waitingToWanderAfterSoundLocalizationInpText.setText(String.valueOf(getSeconds_waitingToWanderAfterSoundLocalization()));
        //update battery settings
        batteryLowInpText.setText(String.valueOf(getBatteryLOW()));
        batteryOKInpText.setText(String.valueOf(getBatteryOK()));
        
        //LISTENERS
        //debug switch
        switchDebugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDebug(isChecked);
            }
        });
        //wander switch
        switchButtonWander.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWanderAllowed(isChecked);
            }
        });
        //rotation movement switch
        switchSoundRotation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSoundRotationAllowed(isChecked);
            }
        });
        //autocharge switch
        switchAutocharge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAutoChargeAllowed(isChecked);
            }
        });
        //project on ceiling
        switchProjectCeiling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setProject_on_ceiling(isChecked);
            }
        });
        //city listeners
        cityWeatherInpText.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { setCityWeather(s.toString());  }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        cityMapInpText.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { setCityMap(s.toString());  }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        //update seconds listeners
        seconds_justGreetedInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setSeconds_justGreeted( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        seconds_waitingTouchInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setSeconds_waitingTouch( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        seconds_waitingResponseInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setSeconds_waitingResponse( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        seconds_checkingBatteryInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setSeconds_checkingBattery( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        seconds_waitingToWanderAfterSoundLocalizationInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setSeconds_waitingToWanderAfterSoundLocalization( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        //battery listeners
        batteryLowInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setBatteryLOW( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
        batteryOKInpText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    setBatteryOK( Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MySettings.this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });

        testConvEngButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //starts test conv engine activity
                Intent myIntent = new Intent(MySettings.this, TestConversationalEngineActivity.class);
                MySettings.this.startActivity(myIntent);
                //finish
                finish();
                return;
            }
        });
        //exit button
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //starts base activity
                Intent myIntent = new Intent(MySettings.this, MyBaseActivity.class);
                MySettings.this.startActivity(myIntent);
                //terminates
                finish();
            }
        });


    }

    @Override
    protected void onMainServiceConnected() {

    }

    //if the app is in debug will show more data in the screen
    private static boolean debug = false;

    //initial wanderAllowed state, defines if the robot is can sometimes wander or it is stationary
    private static boolean wanderAllowed = false;

    //initial soundRotationAllowed state, defines if rotation when detecting sound is allowed
    private static boolean soundRotationAllowed = false;

    //initial autoChargeAllowed state, defines if robot goes to charge when percentage below battery low
    private static boolean autoChargeAllowed = false;

    private static String cityWeather = "Udine,IT";

    private static String cityMap = "Udine";


    //seconds the robot ignores others after greetings
    private static int seconds_justGreeted = 10;

    //seconds the robots waits the handshake
    private static int seconds_waitingTouch = 15;

    //seconds the robot waits the answer after a question
    private static int seconds_waitingResponse = 20;

    //seconds every time the robot checks the battery
    private static int seconds_checkingBattery = 10;

    //seconds the robot waits to start wander after rotating for a sound
    private static int seconds_waitingToWanderAfterSoundLocalization = 20;

    //level of the battery the robot goes to charge
    private static int batteryLOW = 20;

    //level of the battery the robot finishes to charge
    private static int batteryOK = 90;

    //at first meeting, (if face not recognized) presentation of the robot with hand before dialog
    private static boolean presentationBeforeDialog = true;

    //handshakes counter
    private static int countHandshakes = -1;

    //projector mode : WALL/CEILING
    private static boolean project_on_ceiling = false;
    private static int projectorMode = ProjectorManager.MODE_WALL;


    //SPEAK settings  language/speed/intonation
    private static SpeakOption speakDefaultOption = new SpeakOption();
    private static SpeakOption speakSlowOption = new SpeakOption();

    public static boolean initializeSpeak() {
        speakDefaultOption.setLanguageType(SpeakOption.LAG_ENGLISH_US);
        speakDefaultOption.setSpeed(50); //from 0 to 100 default: 40
        speakDefaultOption.setIntonation(40); //from 0 to 100 default: 30
        speakSlowOption.setLanguageType(SpeakOption.LAG_ENGLISH_US);
        speakSlowOption.setSpeed(35); //from 0 to 100 default: 40
        speakSlowOption.setIntonation(40); //from 0 to 100 default: 30
        return true;
    }


    //SAVE AND LOAD HANDSHAKES
    private static String xmlFileName = "xml_stats.xml";
    private static File fileXML = createFileInXMLDirectory(xmlFileName);

    public static void initializeXML() {
        xmlFileName = "xml_stats.xml";
        fileXML = createFileInXMLDirectory(xmlFileName);
    }

    public static void loadHandshakes() {
        //sets the counter to the number of hs inside the XML
        countHandshakes = xmlReadStatsHandshakesNumber(fileXML);
    }

    public static void incrementHandshakes() {
        countHandshakes++;
        xmlAddToNodeNow(fileXML, "handshakes");
    }

    public static int getHandshakes(){
        return countHandshakes;
    }

    public static void incrementRequestShake() {
        xmlAddToNodeNow(fileXML, "request_shake");
    }

    public static void incrementRequestLocation() {
        xmlAddToNodeNow(fileXML, "request_location");
    }

    public static void incrementRequestVideo() {
        xmlAddToNodeNow(fileXML, "request_video");
    }

    public static void incrementInteractionButton() {
        xmlAddToNodeNow(fileXML, "interaction_button"); 
    }

    public static void incrementInteractionFace() {
        xmlAddToNodeNow(fileXML, "interaction_face");
    }


    // GETTERS-SETTERS
    public static boolean isDebug() { return debug; }

    public static void setDebug(boolean debug) { MySettings.debug = debug; }

    public static boolean isWanderAllowed(){ return wanderAllowed;  }

    public static void setWanderAllowed(boolean set) {  wanderAllowed = set;   }

    public static boolean isSoundRotationAllowed() {   return soundRotationAllowed;  }

    public static void setSoundRotationAllowed(boolean set) { soundRotationAllowed = set; }

    public static boolean isAutoChargeAllowed() { return autoChargeAllowed;  }

    public static void setAutoChargeAllowed(boolean autoChargeAllowed) {    MySettings.autoChargeAllowed = autoChargeAllowed; }

    public static String getCityWeather() { return cityWeather; }

    public static void setCityWeather(String cityWeather) { MySettings.cityWeather = cityWeather;}

    public static String getCityMap() {return cityMap; }

    public static void setCityMap(String cityMap) { MySettings.cityMap = cityMap;  }

    public static int getSeconds_waitingTouch() { return seconds_waitingTouch; }
    
    public static void setSeconds_justGreeted(int seconds_justGreeted) {  MySettings.seconds_justGreeted = seconds_justGreeted; }

    public static int getSeconds_justGreeted() { return seconds_justGreeted;  }
    
    public static void setSeconds_waitingTouch(int seconds_waitingTouch) { MySettings.seconds_waitingTouch = seconds_waitingTouch;}

    public static int getSeconds_waitingResponse() { return seconds_waitingResponse; }
    
    public static void setSeconds_waitingResponse(int seconds_waitingResponse) {  MySettings.seconds_waitingResponse = seconds_waitingResponse; }
    
    public static int getSeconds_checkingBattery() { return seconds_checkingBattery; }

    public static void setSeconds_checkingBattery(int seconds_checkingBattery) {  MySettings.seconds_checkingBattery = seconds_checkingBattery; }

    public static int getSeconds_waitingToWanderAfterSoundLocalization() { return seconds_waitingToWanderAfterSoundLocalization;}
    
    public static void setSeconds_waitingToWanderAfterSoundLocalization(int seconds_waitingToWanderAfterSoundLocalization) { MySettings.seconds_waitingToWanderAfterSoundLocalization = seconds_waitingToWanderAfterSoundLocalization; }

    public static int getBatteryLOW() { return batteryLOW; }

    public static void setBatteryLOW(int batteryLOW) { MySettings.batteryLOW = batteryLOW; }

    public static int getBatteryOK() { return batteryOK; }
    
    public static void setBatteryOK(int batteryOK) { MySettings.batteryOK = batteryOK;  }

    public static boolean isPresentationBeforeDialog() {
        return presentationBeforeDialog;
    }

    public static boolean isProject_on_ceiling() {
        return project_on_ceiling;
    }

    public static void setProject_on_ceiling(boolean project_on_ceiling) {
        MySettings.project_on_ceiling = project_on_ceiling;
        if (project_on_ceiling) {
            projectorMode = ProjectorManager.MODE_CEILING;
        } else {
            projectorMode = ProjectorManager.MODE_WALL;
        }
    }

    public static int getProjectorMode() {
        return projectorMode;
    }

    public static SpeakOption getSpeakDefaultOption() {
        return speakDefaultOption;
    }

    public static SpeakOption getSpeakSlowOption() {
        return speakSlowOption;
    }
}

package com.sanbot.capaBot;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import java.util.Objects;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;

public class MyWeatherActivity extends TopBaseActivity implements MyWeatherDownloadAsyncTask.AsyncTaskListener {

    private final static String TAG = "IGOR-WEATHER";

    private SpeechManager speechManager;    //speech
    Handler speechResponseHandler = new Handler();
    String lastRecognizedSentence = "";

    static ProgressBar loader;
    static TextView cityField, summaryField, updatedField;
    static LinearLayout forecastContainerLL;
    Button exitButton;
    String summaryToSay = "";

    boolean infiniteWakeup = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyWeatherActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dark_weather);

        //get place passed
        Intent intent = getIntent();
        String placePassed = intent.getExtras().getString("place");
        Log.i(TAG, "Passed Place: " + placePassed);


        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        loader = findViewById(R.id.loader);
        cityField = findViewById(R.id.city_field);
        updatedField = findViewById(R.id.updated_field);
        summaryField = findViewById(R.id.summary_field);
        forecastContainerLL = findViewById(R.id.forecasts_container);
        exitButton = findViewById(R.id.exit);

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });

        //CREATE THE ASYNC TASK
        taskLoadUp(placePassed);

        /*
        cityField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyWeatherActivity.this);
                alertDialog.setTitle("Change City");
                final EditText input = new EditText(MyWeatherActivity.this);
                input.setText(city);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Change",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                city = input.getText().toString();
                                taskLoadUp(city);
                            }
                        });
                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }

        });
        */
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

                //IGOR: must not exceed 200ms (or less?) don't trust the documentation(500ms), I had to create an handler
                //separate handler so the function could return quickly true, otherwise the robot answers random things over your answers.
                speechResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Speech recognized: " + lastRecognizedSentence);
                        //ANYTHING SAID doesn't matter
                        speechManager.startSpeak("ok", MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                        finishThisActivity();
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

    public void taskLoadUp(String query) {
        if (isNetworkAvailable(getApplicationContext())) {
            Log.i(TAG, "network ok, launching Async task");
            MyWeatherDownloadAsyncTask task = new MyWeatherDownloadAsyncTask(this, forecastContainerLL);
            task.execute(query);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyWeatherActivity.this, "No Internet Connection", Toast.LENGTH_LONG).show();
                }
            });
            speechManager.startSpeak("No Internet Connection", MySettings.getSpeakDefaultOption());
            concludeSpeak(speechManager);
            finishThisActivity();
        }
    }


    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) Objects.requireNonNull(context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo() != null;
    }

    @Override
    protected void onMainServiceConnected() {
    }

    @Override
    public void giveProgress(String progress, String summary) {
        summaryToSay = summary;
        //finished task
        Log.i(TAG, "Async Task finished: "+ progress);
        if (progress.equals("OK")) {
            //new thread not to lock the UI with the sleep
            new Thread(new Runnable() {
                public void run() {
                    speechManager.startSpeak(summaryToSay, MySettings.getSpeakSlowOption());
                    concludeSpeak(speechManager);
                    speechManager.startSpeak("Are you satisfied?", MySettings.getSpeakDefaultOption());
                    concludeSpeak(speechManager);
                    speechManager.doWakeUp();
                }
            }).start();
        }
    }

    private void finishThisActivity() {
        Log.i(TAG, "finishing weather activity");
        //force sleep
        infiniteWakeup = false;
        speechManager.doSleep();
        //starts dialog activity
        Intent myIntent = new Intent(MyWeatherActivity.this, MyDialogActivity.class);
        MyWeatherActivity.this.startActivity(myIntent);
        //finish
        finish();
        return;
    }
}


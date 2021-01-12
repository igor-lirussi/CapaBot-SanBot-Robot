package com.sanbot.capaBot;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sanbot.capaBot.AIML.ab.AIMLProcessor;
import com.sanbot.capaBot.AIML.ab.Bot;
import com.sanbot.capaBot.AIML.ab.Chat;
import com.sanbot.capaBot.AIML.ab.Graphmaster;
import com.sanbot.capaBot.AIML.ab.MagicBooleans;
import com.sanbot.capaBot.AIML.ab.MagicStrings;
import com.sanbot.capaBot.AIML.ab.PCAIMLProcessorExtension;
import com.sanbot.opensdk.base.TopBaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TestConversationalEngineActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-TEST-CONV";


    @BindView(R.id.exit)
    Button exitButton;

    @BindView(R.id.sendCE)
    Button sendCE;

    @BindView(R.id.inputCE)
    EditText inputCE;

    @BindView(R.id.outputCE)
    TextView outputCE;


    //fields
    public Bot bot;
    public static Chat chat;

    String inputSentence = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(TestConversationalEngineActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //set view
        setContentView(R.layout.activity_test_conversational_engine);
        ButterKnife.bind(this);

        //CONVERSATIONAL ENGINE SETUP
        long startTime = System.nanoTime();
        //checking SD card availability
        boolean availableSD = isSDCARDAvailable();
        Log.i(TAG, "SD available: " + availableSD);
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
        Log.i(TAG,"Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        //Assign the AIML files to bot for processing
        bot = new Bot("Hari", MagicStrings.root_path, "chat");
        chat = new Chat(bot);
        String[] args = null;
        mainFunction(args);

        Log.i(TAG, "DURATION BOT millisec: " + (System.nanoTime() - startTime)/1000000);



        //Button listeners
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //starts dialog activity
                Intent myIntent = new Intent(TestConversationalEngineActivity.this, MyDialogActivity.class);
                TestConversationalEngineActivity.this.startActivity(myIntent);
                //terminates activity
                finish();
            }
        });

        sendCE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the input
                inputSentence = inputCE.getText().toString();

                //chatbot
                long startTime = System.nanoTime();
                String response = chat.multisentenceRespond(inputSentence);

                //set output
                outputCE.setText(response);

                Log.i(TAG,"Human: "+ inputSentence);
                Log.i(TAG,"Robot: " + response);

                Log.i(TAG, "DURATION COMPUTED RESPONSE millisec: " + (System.nanoTime() - startTime)/1000000);
            }
        });
    }

    @Override
    protected void onMainServiceConnected() {
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
        Log.i(TAG,"trace mode = " + MagicBooleans.trace_mode);
        Graphmaster.enableShortCuts = true;
    }

}

package com.sanbot.capaBot;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.sanbot.capaBot.AIML.ab.AIMLProcessor;
import com.sanbot.capaBot.AIML.ab.Bot;
import com.sanbot.capaBot.AIML.ab.Chat;
import com.sanbot.capaBot.AIML.ab.Graphmaster;
import com.sanbot.capaBot.AIML.ab.MagicBooleans;
import com.sanbot.capaBot.AIML.ab.MagicStrings;
import com.sanbot.capaBot.AIML.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MyApp extends Application {

    private final static String TAG = "IGOR-MYAPP";

    public static Bot bot;
    public static Chat chat;

    public static boolean botReady = false;  // flag to check when ready

    @Override
    public void onCreate() {
        super.onCreate();

        //new thread in background to load chatbot
        new Thread(new Runnable() {
            @Override
            public void run() {
                setupChatBot();
                botReady = true;

            }
        }).start();

    }

    private void setupChatBot() {
        //CHAT BOT SETUP
        long startTime = System.nanoTime();
        //checking SD card availability
        boolean availableSD = isSDCARDAvailable();
        Log.i(TAG, "SD available: " + availableSD);
        //creating a new directory in the device
        File botBaseDir = new File(Environment.getExternalStorageDirectory().getPath() + "/CAPABOT" + "/hari/bots/Hari");
        if (!botBaseDir.exists()) {
            if (botBaseDir.mkdirs()) {
                Log.i(TAG, "Directory created: " + botBaseDir.getAbsolutePath());
                // COPY ASSETS
                //Reading the file
                try {
                    //receiving the assets from the app directory
                    AssetManager assets = getResources().getAssets();
                    //for every subdirectory
                    for (String dir : assets.list("Hari")) {
                        //create the subdirectory in the device
                        File subdir = new File(botBaseDir.getPath() + "/" + dir);
                        if (!subdir.exists()) {
                            subdir.mkdirs();
                        }
                        //for every file in the subdirectory
                        for (String file : assets.list("Hari/" + dir)) {
                            //create file in subdirectory
                            File f = new File(botBaseDir.getPath() + "/" + dir + "/" + file);
                            if (f.exists()) {
                                continue;
                            }
                            InputStream in = null;
                            OutputStream out = null;
                            in = assets.open("Hari/" + dir + "/" + file);
                            out = new FileOutputStream(botBaseDir.getPath() + "/" + dir + "/" + file);
                            //copy file from assets to the mobile's SD card or any secondary memory
                            //function is below
                            copyFile(in, out);
                            in.close();
                            in = null;
                            out.flush();
                            out.close();
                            out = null;
                            Log.i(TAG, "Files copied! in: " + botBaseDir.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Failed to create bot directory at " + botBaseDir.getAbsolutePath());
                return;
            }
        } else {
            Log.i(TAG, "Bot directory already exists. Skipping asset copy.");
        }

        //get the working directory
        MagicStrings.root_path = Environment.getExternalStorageDirectory().getPath() + "/CAPABOT" + "/hari";
        Log.i(TAG,"Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        //Assign the AIML files to bot for processing
        bot = new Bot("Hari", MagicStrings.root_path, "chat");
        chat = new Chat(bot);
        String[] args = null;
        mainFunction(args);

        Log.i(TAG, "TIME TO LOAD BOT millisec: " + (System.nanoTime() - startTime)/1000000);

    }



    //CHATBOT FUNCTIONS
    //check SD card availability
    public static boolean isSDCARDAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    //copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
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

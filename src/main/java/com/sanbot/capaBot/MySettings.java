package com.sanbot.capaBot;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.sanbot.opensdk.function.beans.SpeakOption;
import com.sanbot.opensdk.function.unit.ProjectorManager;

import java.io.File;

import static com.sanbot.capaBot.MyUtilsXML.createFileInXMLDirectory;
import static com.sanbot.capaBot.MyUtilsXML.xmlAddHandshakeNow;
import static com.sanbot.capaBot.MyUtilsXML.xmlAddToNodeNow;
import static com.sanbot.capaBot.MyUtilsXML.xmlReadStatsHandshakesNumber;

/**
 * MySettings class
 */

public class MySettings {

    //initial wanderAllowed state, defines if the robot is can sometimes wander or it is stationary
    private static boolean wanderAllowed = false;

    //initial soundRotationAllowed state, defines if rotation when detecting sound is allowed
    private static boolean soundRotationAllowed = true;

    //after first meeting presentation robot asks if can help
    private static boolean dialogAfterPresentation = true;

    //handshakesTextView countHandshakes
    private static int countHandshakes = -1;

    //seconds the robot ignores others greetings
    private static int seconds_justGreeted = 30;

    //seconds the robots waits the handshake
    private static int seconds_waitingTouch = 15;

    //seconds the robot waits the answer after a question
    private static int seconds_waitingResponse = 20;

    //seconds the robot waits to start wander after rotating for a sound
    private static int seconds_waitingToWanderAfterSoundLocalization = 20;

    //projector mode : WALL/CEILING
    private static int projectorMode = ProjectorManager.MODE_WALL;

    //speak seetings  language/speed/intonation
    private static SpeakOption speakDefaultOption = new SpeakOption();

    public static boolean initializeSpeak() {
        speakDefaultOption.setLanguageType(SpeakOption.LAG_ENGLISH_US);
        speakDefaultOption.setSpeed(30); //from 0 to 100 default: 30
        speakDefaultOption.setIntonation(40); //from 0 to 100 default: 30
        return true;
    }

    public static SpeakOption getSpeakDefaultOption() {
        return speakDefaultOption;
    }


    public static boolean isWanderAllowed(){
        return wanderAllowed;
    }

    public static void setWanderAllowed(boolean set) {
        wanderAllowed = set;
    }


    public static boolean isDialogAfterPresentation() {
        return dialogAfterPresentation;
    }


    @Deprecated
    public static boolean save(Context context) {
        SharedPreferences settings = context.getSharedPreferences("UserInfo", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("Handshakes", countHandshakes) ;
        editor.commit();
        Log.i("IGOR", "Saved Settings");
        return true;
    }

    @Deprecated
    public static boolean loadSaved(Context context) {
        SharedPreferences settings = context.getSharedPreferences("UserInfo", 0);
        settings.getInt("Handshakes", countHandshakes);
        Log.i("IGOR", "Loaded Settings");
        return true;
    }


    //SAVE AND LOAD HANDSHAKES
    private static String xmlFileName = "xml_stats.xml";
    private static File fileXML = createFileInXMLDirectory(xmlFileName);

    public static void loadHandshakes() {
        //sets the counter to the number of hs inside the XML
        countHandshakes = xmlReadStatsHandshakesNumber(fileXML);
    }



    public static void incrementHandshakes() {
        countHandshakes++;
        xmlAddHandshakeNow(fileXML);
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




    public static int getSeconds_waitingTouch() {
        return seconds_waitingTouch;
    }


    public static int getSeconds_justGreeted() {
        return seconds_justGreeted;
    }


    public static int getSeconds_waitingResponse() {
        return seconds_waitingResponse;
    }

    public static boolean isSoundRotationAllowed() {
        return soundRotationAllowed;
    }

    public static void setSoundRotationAllowed(boolean rotationAllowed) {
        soundRotationAllowed = rotationAllowed;
    }

    public static int getProjectorMode() {
        return projectorMode;
    }

    public static int getSeconds_waitingToWanderAfterSoundLocalization() {
        return seconds_waitingToWanderAfterSoundLocalization;
    }

    public static void setSeconds_waitingToWanderAfterSoundLocalization(int seconds_waiting) {
        seconds_waitingToWanderAfterSoundLocalization = seconds_waiting;
    }
}

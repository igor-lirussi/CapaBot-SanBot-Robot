package com.sanbot.capaBot;

import com.sanbot.opensdk.function.beans.SpeakOption;
import com.sanbot.opensdk.function.unit.ProjectorManager;

import java.io.File;

import static com.sanbot.capaBot.MyUtilsXML.createFileInXMLDirectory;
import static com.sanbot.capaBot.MyUtilsXML.xmlAddToNodeNow;
import static com.sanbot.capaBot.MyUtilsXML.xmlReadStatsHandshakesNumber;

/**
 * MySettings class
 * used to save all the settings about the robot
 */
public class MySettings {

    //initial wanderAllowed state, defines if the robot is can sometimes wander or it is stationary
    private static boolean wanderAllowed = false;

    //initial soundRotationAllowed state, defines if rotation when detecting sound is allowed
    private static boolean soundRotationAllowed = false;

    //after first meeting presentation robot asks if can help
    private static boolean dialogAfterPresentation = true;

    //handshakesTextView countHandshakes
    private static int countHandshakes = -1;

    //seconds the robot ignores others greetings
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

    //projector mode : WALL/CEILING
    private static int projectorMode = ProjectorManager.MODE_CEILING;

    //speak settings  language/speed/intonation
    private static SpeakOption speakDefaultOption = new SpeakOption();

    public static boolean initializeSpeak() {
        speakDefaultOption.setLanguageType(SpeakOption.LAG_ENGLISH_US);
        //speakDefaultOption.setSpeed(50); //from 0 to 100 default: 40
        //speakDefaultOption.setIntonation(40); //from 0 to 100 default: 30
        return true;
    }

    public static SpeakOption getSpeakDefaultOption() {
        return speakDefaultOption;
    }

    // GETTERS-SETTERS

    public static boolean isWanderAllowed(){
        return wanderAllowed;
    }

    public static void setWanderAllowed(boolean set) {
        wanderAllowed = set;
    }

    public static boolean isSoundRotationAllowed() {
        return soundRotationAllowed;
    }

    public static void setSoundRotationAllowed(boolean set) { soundRotationAllowed = set; }

    public static boolean isDialogAfterPresentation() {
        return dialogAfterPresentation;
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

    public static void incrementInteractionButton() { xmlAddToNodeNow(fileXML, "interaction_button"); }

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


    public static int getSeconds_waitingToWanderAfterSoundLocalization() {
        return seconds_waitingToWanderAfterSoundLocalization;
    }

    public static int getSeconds_checkingBattery() {
        return seconds_checkingBattery;
    }

    public static int getBatteryLOW() {
        return batteryLOW;
    }

    public static int getBatteryOK() {
        return batteryOK;
    }

    public static int getProjectorMode() {
        return projectorMode;
    }
}

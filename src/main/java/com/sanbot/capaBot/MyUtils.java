package com.sanbot.capaBot;

import android.os.Handler;
import android.util.Log;

import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.wheelmotion.RelativeAngleWheelMotion;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;

/**
 * a class for utils of SanBot
 */
public final class MyUtils {


    /**
     * Igor: makes the thread sleep fot the seconds passed,
     * useful to avoid speech over speech.
     * @param seconds seconds to block the thread
     */
    public static void sleepy(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * waits until the speech is finished
     * @param speechManager the speech manager to check
     */
    public static boolean concludeSpeak(SpeechManager speechManager) {
        while (speechManager.isSpeaking().getResult().equals("1")) {
            sleepy(0.2);
        }
        return true;
    }

    /**
     * Igor: compensates the error of the Sanbot compass, trial and error to find the magic values
     * @param passed the angle of the gyro
     * @return the angle corrected of the gyro
     */
    public static int compensationSanbotAngle(float passed) {
        float corrected = 180;
        //passed is between 0 and 60
        if (passed <=60) {
            corrected = passed/2-30;
        }
        //passed is between 60 and 180
        if (passed > 60 && passed <=180) {
            //v-60 : 120 = x : 180
            corrected=(passed-60)*180/120;
        }
        //passed is between 180 and 340
        if (passed > 180 && passed <= 340) {
            //v-180 : 160 = x-180 : 180
            corrected=((passed-180)*180/160)+180;
        }
        //passed is between 340 and 360
        if (passed > 340) {
            corrected = passed/2 + 170;
        }
        //final corrections to avoid angle overflow
        while (corrected<0)
            corrected+=360;
        while (corrected>=360)
            corrected-=360;
        return (int) corrected;
    }

    /**
     * rotation at a relative angle in the shortest way
     * @param wheelMotionManager the motion manager to rotate the robot
     * @param angle the relative angle clockwise desired to turn, can be negative to define counter clockwise
     * @return 1 if the rotation is clockwise, -1 otherwise
     */
    public static int rotateAtRelativeAngle(WheelMotionManager wheelMotionManager, int angle) {
        //correction negative angles
        while (angle < 0 ) angle = angle + 360;
        //calculation best direction
        if (angle < 180) {
            RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_RIGHT, 5, angle);
            wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
            Log.i("IGOR-rotation","turning right " + angle);
            return 1;
        } else {
            RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_LEFT, 5, (360-angle));
            wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
            Log.i("IGOR-rotation","turning left " + (360-angle));
            return -1;
        }
    }

    /**
     * rotates the robot to face a cardinal direction angle, the angle is in degrees clockwise from 0 corresponding to NORTH
     * @param wheelMotionManager the manager to let the robot rotate
     * @param currentCardinalAngle the cardinal direction is facing the robot, 0 is NORTH, 90 is EAST, 180 SOUTH, 270 WEST
     *                             /!\ IT HAS TO BE ALREADY COMPENSATED /!\ (use the compensationSanbotAngle() function on the gyro angle)
     * @param desiredCardinalAngle the cardinal direction desired the robot to face, 0 is NORTH, 90 is EAST, 180 SOUTH, 270 WEST
     * @return the angle calculated to rotate clockwise
     */
    public static int rotateAtCardinalAngle(WheelMotionManager  wheelMotionManager, int currentCardinalAngle, int desiredCardinalAngle) {
        int clockwiseRotationAngle = 0;
        //can be negative if the rotation in counter clockwise
        clockwiseRotationAngle = desiredCardinalAngle - currentCardinalAngle;
        //clockwise rotation passed to rotate
        rotateAtRelativeAngle( wheelMotionManager, clockwiseRotationAngle);
        //return the angle clockwise calculated to go back
        return clockwiseRotationAngle;
    }


    /**
     * Igor: sets an emotion that expires after x seconds, then becomes normal
     * @param emotionPassed the emotion for the eyes
     */
    public static void temporaryEmotion(final SystemManager systemManager, EmotionsType emotionPassed, int seconds_passed) {
        systemManager.showEmotion(emotionPassed);
        //reset face after passed seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                systemManager.showEmotion(EmotionsType.NORMAL);
            }
        }, seconds_passed * 1000);
    }
    public static void temporaryEmotion(final SystemManager systemManager, EmotionsType emotionPassed) {
        //if no 2nd argument 10 seconds
        temporaryEmotion(systemManager, emotionPassed, 10);
    }



}

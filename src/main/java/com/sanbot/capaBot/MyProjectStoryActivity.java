package com.sanbot.capaBot;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.ProjectorManager;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;

/**
 * function: projection the story of vislab
 */

public class MyProjectStoryActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-PROJ";

    @BindView(R.id.exit)
    Button exitButton;

    //managers
    private ProjectorManager projectorManager;
    private SpeechManager speechManager; //voice, speechRec

    //video view for fullscreen
    VideoView videoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyProjectStoryActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projector_story);
        ButterKnife.bind(this);
        //init manager
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        //other settings
        /*
        projectorManager.setTrapezoidH(0);
        projectorManager.setTrapezoidV(0);
        projectorManager.setAcuity(0);
        projectorManager.setSaturation(0);
        projectorManager.setColor(0);
        projectorManager.setBright(0);
        projectorManager.setContrast(0);
        projectorManager.setMirror(ProjectorManager.MIRROR_CLOSE);*/

        //handler to open projector
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "handler called to open projector");
                //mode from settings
                projectorManager.setMode(MySettings.getProjectorMode());
                //OPEN PROJECTOR
                projectorManager.switchProjector(true);
                //voice introduction
                speechManager.startSpeak(getString(R.string.show_video), MySettings.getSpeakDefaultOption());
            }
        }, 500);


        //videoview play video
        videoView = findViewById(R.id.myvideoview);
        //copy to storage if doesn't exist
        String externalPath = Environment.getExternalStorageDirectory().getPath() + "/CAPABOT/video-projected.mp4";
        File videoFile = new File(externalPath);
        if (!videoFile.exists()) {
            copyRawResourceToStorage(R.raw.video_projected, videoFile);
        }

        //videoView.setVideoURI(Uri.parse("https://www.youtube.com/watch?v=HO2pyUKodq0"));
        videoView.setVideoURI(Uri.parse(externalPath));
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
        videoView.pause();

        Log.i(TAG, "Video Ready, waiting the projector to be ON");

        //handler to start video when the projector is effectively started (needs 10 seconds)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start video called");
                //start video
                videoView.start();
            }
        }, 10000);

        initListeners();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });
    }

    public void initListeners(){
        //close projector when video ends
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(TAG, "Video Finished");
                //close projector
                projectorManager.switchProjector(false);
                //end sentence
                speechManager.startSpeak(getString(R.string.good_video), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                speechManager.startSpeak(getString(R.string.cooling_projector), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                finishThisActivity();
            }
        });
    }


    @Override
    protected void onMainServiceConnected() {

    }

    private void finishThisActivity() {
        //starts dialog activity
        Intent myIntent = new Intent(MyProjectStoryActivity.this, MyDialogActivity.class);
        MyProjectStoryActivity.this.startActivity(myIntent);

        //calls finish activity
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Android documentation says this:
        //"do not count on this method being called"
        //so I can't use this for operation: "ending activity" in the code,
        //better call finishThisActivity()
    }

    private void copyRawResourceToStorage(int resId, File outFile) {
        try {
            File dir = outFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            InputStream in = getResources().openRawResource(resId);
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

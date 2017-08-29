package com.xangnun.facealarm;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Jack Wang on 10/18/2016.
 */

/**
 * Sources:
 * https://www.youtube.com/playlist?list=PL4uut9QecF3DLAacEoTctzeqTyvgzqYwA
 */
public class RingTonePlayingService extends Service {

    private static final String TAG = "RingTonePlayingService";

    //set up some variables
    private MediaPlayer mediaPlayer;

    private AudioManager mAudioManager;

    private int mOrigVolume;

    private int mMaxValume;

    private boolean isRunning = false;

    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;

    private SharedPreferences sharedPreferences;
    private Set<String> defvalues;
    private Set<String> values;
    private String KEY = "Intents";
    private Context context;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onCreate();
        Log.i("LocalService", "Receive start id " +  startId + ": " + intent);

        defvalues = new HashSet<>();

        //create a file descriptor for the ring tone

        //initialize the audiomanager
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        //backup the original volume settings
        mOrigVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        mMaxValume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);


        //create songs
        //mediaPlayer = MediaPlayer.create(this, R.raw.hillary_laughing);
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            //set the mediaplayer to the alarm stream
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

        }

        String state = intent.getExtras().getString("extra");
        switch (state) {
            case "Alarm ON": startId = 1; break;
            case "Alarm OFF": startId = 0; break;
            default:startId = 0;
        }

        if(!this.isRunning && (startId == 1)) {
            restoreIntents();
            //create wakelocks
            createWakeLocks();

            //wake up the device;
            wakeDevice();
            //release full_wake_up;
            if(fullWakeLock != null && fullWakeLock.isHeld())
                fullWakeLock.release();

            //partialWakeLock.acquire();
            partialWakeLock.acquire();
            //destroy wake up locks
            //destroyWakeLocks();

            mediaPlayer.reset();

            try {
                String FilePath = intent.getExtras().getString("RingTone");
                Uri musFile = Uri.parse(FilePath);
                Log.d(TAG, FilePath);
                //mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mediaPlayer.setLooping(true);
                mediaPlayer.setDataSource(this, musFile);
                mediaPlayer.prepare();
            } catch(IOException e) {
                Log.e(TAG, "Failed to prepare the alarm player", e);
            }

            mediaPlayer.start();

            //set infinite looping
            mediaPlayer.setScreenOnWhilePlaying(true);

            //set the status to true
            this.isRunning = true;
            // This is how you start a new activity.
            //Intent wakeUpIntent = new Intent(this, FaceActivity.class);
            Intent wakeUpIntent = new Intent(this, FaceActivity.class);

            //necessary for calling a new activity from a service
            wakeUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent targetIntent = null;
            try { targetIntent = Intent.getIntent(intent.getExtras().getString("URI"));}
            catch ( URISyntaxException e) { e.printStackTrace();}
            wakeUpIntent.putExtra("intent", targetIntent.getExtras().getString("Uid"));

            removeIntent(targetIntent);
            startActivity(wakeUpIntent);
        }

        else if(this.isRunning && (startId == 0)) {
            //stop and reset the music
            mediaPlayer.stop();
            mediaPlayer.reset();

            //release wake locks
            destroyWakeLocks();
            //set the status to false
            this.isRunning = false;
            this.stopSelf();
        }

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        //recover the original volume setting.
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mOrigVolume, 0);
        mediaPlayer.release();
        //release wake_lock
        destroyWakeLocks();

        super.onDestroy();
    }

    private void createWakeLocks() {
        PowerManager powerManager = (PowerManager) getSystemService(this.POWER_SERVICE);
        if (fullWakeLock == null) {
            fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "Loneworker - FULL WAKE LOCK");
        }
        if (partialWakeLock == null) {
            partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loneworker - PARTIAL WAKE LOCK");
        }
    }

    // wake the device up
    private void wakeDevice() {
        //fullWakeLock.acquire();
        fullWakeLock.acquire();

        //copied from stack_overflow
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(this.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        //keyguardLock.disableKeyguard();
    }

    private void destroyWakeLocks() {
        if(fullWakeLock != null && fullWakeLock.isHeld())
            fullWakeLock.release();
        if(partialWakeLock != null && partialWakeLock.isHeld())
            partialWakeLock.release();
    }

    private boolean removeIntent(Intent intent){
        Iterator<String> iterator = values.iterator();
        Log.d(TAG, "Finding: " + intent.getExtras().getString("Uid"));
        while(iterator.hasNext()){
            Intent temp = new Intent();
            try{ temp = Intent.getIntent(iterator.next());}
            catch (URISyntaxException e){ e.printStackTrace();}
            Log.d(TAG, "Intent: " + temp.getExtras().getString("Uid"));
            if(temp.getExtras().getString("Uid").equals(intent.getExtras().getString("Uid"))) {
                iterator.remove();
                saveIntents();
                return true;
            }
        }
        return false;
    }

    public void saveIntents(){
        if(defvalues != sharedPreferences.getStringSet(KEY, defvalues)) {
            sharedPreferences.edit().remove(KEY).commit();
        }
        sharedPreferences.edit().putStringSet(KEY, values).commit();
    }

    public void restoreIntents(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(defvalues == sharedPreferences.getStringSet(KEY, defvalues)){
            values = new HashSet<>();
            Log.d(TAG, "EMPTY");
        } else {
            values = sharedPreferences.getStringSet(KEY, defvalues);
            sharedPreferences.edit().remove(KEY).commit();
            Log.d(TAG, "NONEMPTY");
        }
    }
}

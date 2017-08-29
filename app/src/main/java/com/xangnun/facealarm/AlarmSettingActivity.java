package com.xangnun.facealarm;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by Jack Wang on 10/17/2016.
 */

/**
 * Sources:
 *
 * Alarm
 * https://www.youtube.com/playlist?list=PL4uut9QecF3DLAacEoTctzeqTyvgzqYwA
 */
public class AlarmSettingActivity extends AppCompatActivity{

    private final String TAG = "AlarmSettingActivity";
    //The Time Picker
    private TimePicker time_picker;
    //The buttons
    private Button set_alarm;
    private Button set_mus;
    private Button cancelButton;
    private Button deleteButton;
    //Alarm Manager
    private AlarmManager alarm_manager;
    //Calendar
    private Calendar calendar;
    private Calendar currentTime;
    //Context
    private Context context;
    //Pending Intent
    private PendingIntent pending_intent;
    private Intent my_intent;
    private Uri audioFileUri;
    static final int PICKFILE_RESULT_CODE = 1;
    static final int resultCode = -1;


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_setting);

        //initialize
        time_picker = (TimePicker) findViewById(R.id.timePicker);


        //TODO set default uri
        audioFileUri = Uri.parse("android.resource://com.xangnun.facealarm/" + R.raw.ppap);

        //create Calender
        calendar = Calendar.getInstance();
        //set context
        this.context = this;
        //initialize buttons
        set_alarm = (Button) findViewById(R.id.set_on);
        set_mus = (Button) findViewById(R.id.set_mus);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        //initialize a intent
        my_intent = new Intent();
        //initialize a intent
        my_intent.putExtra("Calendar", Long.toString(System.currentTimeMillis()));
        my_intent.putExtra("Status", "1");
        my_intent.putExtra("Music", audioFileUri.toString());


        setResult(resultCode, my_intent);
        set_alarm.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //cancel the previous alarm request if any
                set_alarm.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.general_blue,null));
                //setting up calendar
                currentTime = Calendar.getInstance();

                //reset the calendar to current time
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.SECOND, 0);

                //set up the alarm time
                calendar.set(Calendar.HOUR_OF_DAY, time_picker.getCurrentHour());
                calendar.set(Calendar.MINUTE, time_picker.getCurrentMinute());


                //check if the set time is already passed
                if(calendar.compareTo(currentTime) < 0) {
                    //set the date to tommorow
                    calendar.add(Calendar.DATE, 1);
                }


                //set up extra string for intent
                my_intent.putExtra("Calendar", Long.toString(calendar.getTimeInMillis()));
                my_intent.putExtra("Status", "0");
                my_intent.putExtra("Music", audioFileUri.toString());

                setResult(resultCode, my_intent);
                finish();
            }
        });

        set_mus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                fileintent = new Intent(Intent.ACTION_GET_CONTENT);
                fileintent.setType("audio/mpeg");
                try {
                    startActivityForResult(Intent.createChooser(fileintent, "music_select"), PICKFILE_RESULT_CODE);
                } catch(ActivityNotFoundException e) {
                    Log.e("yeah", "yeah");
                }
                */
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, PICKFILE_RESULT_CODE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                cancelButton.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.general_blue,null));
                my_intent.putExtra("Status", "1");
                setResult(resultCode, my_intent);
                finish();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                deleteButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.general_blue, null));
                my_intent.putExtra("Status", "2");
                setResult(resultCode, my_intent);
                finish();
            }

        });
        //initialize alarm_manager
        alarm_manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //TODO if back button is pushed then finish.
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.getData() != null)) {
                audioFileUri = data.getData();
                Log.d(TAG, "Audio set to: " + audioFileUri.toString());
            }
        }
        */
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == RESULT_OK) {
            audioFileUri = data.getData();
            Log.d(TAG, "Audio set to: " + audioFileUri.toString());
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

package com.xangnun.facealarm;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Vincent on 11/5/16.
 */

/**
 * Sources:
 *
 * Alarm
 * https://www.youtube.com/playlist?list=PL4uut9QecF3DLAacEoTctzeqTyvgzqYwA
 */
public class AlarmControl extends AppCompatActivity {

    //
    // Constants
    //
    private static final String TAG = "AlarmSettingActivity";

    private static final String KEY = "Intents";

    private static final int EDIT_ALARM_CODE = 2;

    private static final int ADD_ALARM_CODE = 1;
    // BUTTON PRESSED
    private static final int SET_ALARM = 0;

    private static final int CANCEL = 1;

    private static final int DELETE = 2;

    //
    // Instance Variables
    //

    // Context
    private Context context;
    private SharedPreferences sharedPreferences;
    private Set<String> values;
    private Set<String> defvalues;
    // ListView
    private ListView alarmListView;
    private ArrayList<Intent> alarmArrayList;
    private ArrayAdapter<Intent> alarmListViewAdapter;
    // Alarm Manager
    private AlarmManager alarmManager;
    // Pending Intent
    private Intent intentClicked;
    // Button
    private Button addButton;



    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_control);

        context = this;
        defvalues = new HashSet<>();

        //initialize the intentOnclicked
        intentClicked = new Intent(this.context, Alarm_Receiver.class);
        //initialize adapter and arraylist
        alarmArrayList = new ArrayList<>();

        //initialize alarmManager
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //Button to add
        addButton = (Button) findViewById(R.id.addButton);
        //initialize listview
        alarmListView = (ListView) findViewById(R.id.list);

        //ADD Button implemented
        addButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent alarmSettingActivityIntent = new Intent(AlarmControl.this, AlarmSettingActivity.class);
                startActivityForResult(alarmSettingActivityIntent, ADD_ALARM_CODE);
            }
        });

        alarmListViewAdapter = new IntentArrayAdapter(context, 1, alarmArrayList);

        alarmListView.setAdapter(alarmListViewAdapter);
        //Click to edit implemented
        alarmListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                intentClicked = (Intent) parent.getAdapter().getItem(position);
                Intent alarmSettingActivityIntent = new Intent(AlarmControl.this, AlarmSettingActivity.class);
                startActivityForResult(alarmSettingActivityIntent, EDIT_ALARM_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        restoreIntents();

        int status = Integer.parseInt(data.getExtras().getString("Status"));

        //if adding a new alarm
        if ((requestCode == ADD_ALARM_CODE) && (resultCode == Activity.RESULT_OK)) {
            if(status == SET_ALARM) createAlarm(data);
        } else if((requestCode == EDIT_ALARM_CODE) && (resultCode == Activity.RESULT_OK)){
            //if user clicked cancel then nothing needs to be changed
            switch (status) {
                case CANCEL: break;
                case DELETE: removeAlarm(); break;
                case SET_ALARM: removeAlarm(); createAlarm(data); break;
                default: break;
            }
        }

        //save the intents back to the sharedpreference
        saveIntents();

    }

    private boolean removeAlarm() {

        int uid = Integer.parseInt(intentClicked.getExtras().getString("Uid"));

        //recreate the original alarmIntent
        Intent alarmIntent = new Intent(this.context, Alarm_Receiver.class);
        alarmIntent.putExtra("Uid", intentClicked.getExtras().getString("Uid"));
        alarmIntent.putExtra("Calendar", intentClicked.getExtras().getString("Calendar"));
        alarmIntent.putExtra("extra", intentClicked.getExtras().getString("extra"));
        alarmIntent.putExtra("RingTone", intentClicked.getExtras().getString("RingTone"));
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(AlarmControl.this,
                uid, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmPendingIntent.cancel();
        alarmManager.cancel(alarmPendingIntent);

        //iterate to remove the alarm from the local record
        Iterator<String> iterator = values.iterator();
        while(iterator.hasNext()){
            Intent temp = new Intent();
            try{ temp = Intent.getIntent(iterator.next());}
            catch (URISyntaxException e){ e.printStackTrace();}
            if(temp.getExtras().getString("Uid").equals(intentClicked.getExtras().getString("Uid"))) {
                iterator.remove();
                return true;
            }
        }
        alarmArrayList.remove(intentClicked);
        return false;
    }

    private void createAlarm(Intent data) {

        //get the calendar for the alarm
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(data.getExtras().getString("Calendar")));

        //Use the current time as the unique id for pendingIntent
        int uid = (int)System.currentTimeMillis();

        // Create alarm intent
        Intent alarmIntent = new Intent(this.context, Alarm_Receiver.class);
        alarmIntent.putExtra("Uid", Integer.toString(uid));
        alarmIntent.putExtra("Calendar", data.getExtras().getString("Calendar"));
        alarmIntent.putExtra("extra", "Alarm ON");
        alarmIntent.putExtra("RingTone", data.getExtras().getString("Music"));

        //add it to the local record
        values.add(alarmIntent.toURI());

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(AlarmControl.this,
                uid, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //set up the alarm using alarm manager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                alarmPendingIntent);

        alarmArrayList.add(alarmIntent);
    }


    @Override
    public void onPause() {
        super.onPause();
        saveIntents();
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreIntents();
        String[] tempArray = values.toArray(new String[values.size()]);
        //initialize a intent
        alarmArrayList.clear();
        for(int i = 0; i < tempArray.length; i++){
            try{ alarmArrayList.add(Intent.getIntent(tempArray[i]));}
            catch(URISyntaxException e){ e.printStackTrace();}
        }
        alarmListViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        saveIntents();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
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
        } else {
            values = sharedPreferences.getStringSet(KEY, defvalues);
            sharedPreferences.edit().remove(KEY).commit();
        }
    }



}
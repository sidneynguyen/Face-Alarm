package com.xangnun.facealarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Vincent on 11/5/16.
 */

public class IntentArrayAdapter extends ArrayAdapter<Intent> {

    private Context context;
    private List<Intent> intentList;

    //constructor
    public IntentArrayAdapter(Context context, int resource, ArrayList<Intent> intents){
        super(context, resource, intents);

        this.context = context;
        this.intentList = intents;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        //get the alarm intent we are displaying
        Intent intent = intentList.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_intent, null);

        TextView hour = (TextView) view.findViewById(R.id.hour);
        TextView minute = (TextView) view.findViewById(R.id.minute);
        TextView ampm = (TextView) view.findViewById(R.id.ampm);

        //set hour and minute
        long alarmTime = Long.parseLong(intent.getExtras().getString("Calendar"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarmTime);

        String hourString = Integer.toString(calendar.get(Calendar.HOUR));
        String minuteString = Integer.toString(calendar.get(Calendar.MINUTE));
        String ampmString = "AM";

        //find if it is am or pm
        int hourofday = calendar.get(Calendar.HOUR_OF_DAY);
        if(hourofday >= 12){
            ampmString = "PM";
        }

        //set price and rental attributes
        hour.setText(toHour(hourString));
        minute.setText(toMinute(minuteString));
        ampm.setText(ampmString);

        return view;
    }

    public static String toHour(String hour) {
        int hr = Integer.parseInt(hour);
        if (hr == 0) {
            return "12";
        } else if (hr < 10) {
            return "0" + hr;
        } else {
            return hour;
        }
    }

    public static String toMinute(String min) {
        if (Integer.parseInt(min) < 10) {
            return "0" + min;
        } else {
            return min;
        }
    }
}

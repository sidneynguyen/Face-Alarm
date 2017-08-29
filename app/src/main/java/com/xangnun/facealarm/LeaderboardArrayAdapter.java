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
import java.util.List;

/**
 * Created by normansun on 11/13/16.
 */

public class LeaderboardArrayAdapter extends ArrayAdapter<FacebookUser> {

    private Context context;
    private List<FacebookUser> myList;

    //constructor, call on creation
    public LeaderboardArrayAdapter(Context context, int resource, ArrayList<FacebookUser> objects) {
        super(context, resource, objects);

        this.context = context;
        this.myList = objects;
    }

    //called when rendering the list
    public View getView(int position, View convertView, ViewGroup parent) {

        //get the property we are displaying
        FacebookUser user = myList.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.leaderboard_listview, null);

        TextView name = (TextView) view.findViewById(R.id.name);
        TextView score = (TextView) view.findViewById(R.id.score);


        name.setText(String.valueOf(user.getName()));
        score.setText(String.valueOf(user.getCount()));


        return view;
    }

}

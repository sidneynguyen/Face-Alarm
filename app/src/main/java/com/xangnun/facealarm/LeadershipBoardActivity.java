package com.xangnun.facealarm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Sources:
 * https://firebase.google.com/docs/database/android/start/
 */
public class LeadershipBoardActivity extends AppCompatActivity {

    private static final String TAG = "LeadershipBoardActivity";

    private ArrayList<FacebookUser> myList = new ArrayList<FacebookUser>();
    private boolean filledTable[];

    // Initialize GUI variables
    private TextView mMyScore;
    private ListView mLeadershipBoard;
    private ArrayAdapter<FacebookUser> arrayAdapter;


    /* LIFETIME FUNCTIONS */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leadership_board);

        mMyScore = (TextView) findViewById(R.id.textview_my_score);
        mLeadershipBoard = (ListView) findViewById(R.id.list_leaderboard);
        arrayAdapter = new LeaderboardArrayAdapter(this, 0, myList);
    }


    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "UID: " + AccessToken.getCurrentAccessToken().getUserId());

        fillFriendsList();

        String uid = AccessToken.getCurrentAccessToken().getUserId();
        DatabaseReference myScoreRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("count");
        myScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int score = 0;
                if (dataSnapshot.exists()) {
                    score = dataSnapshot.getValue(Integer.class);
                }
                mMyScore.setText("" + score);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mLeadershipBoard.setAdapter(arrayAdapter);
    }


    @Override
    public void onStop() {
        super.onStop();
        myList.clear();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /* HELPER FUNCTIONS */

    /*
     * Used to populate the ArrayList of FacebookUser objects
     */
    private void fillFriendsList(){
        // Access friends list
        AccessToken token = AccessToken.getCurrentAccessToken();
        GraphRequest.newMyFriendsRequest(token, new GraphRequest.GraphJSONArrayCallback() {
                @Override
                public void onCompleted(JSONArray objects, GraphResponse response) {
                    if (objects != null) {
                        try {
                            //Create the array of friends
                            // Loop through list of friends and insert array of FacebookUser
                            Log.d(TAG, objects.length() + " friends");
                            for (int index = 0; index < objects.length(); index++) {
                                Log.d(TAG, "Friend: " + objects.getJSONObject(index).getString("id"));
                                // Store the friends in a tree as a FacebookUser object
                                myList.add(new FacebookUser(objects.getJSONObject(index).getString("id"),
                                                          objects.getJSONObject(index).getString("name")));
                            }
                            arrayAdapter.notifyDataSetChanged();
                            populateCount();
                        } catch (JSONException e) {
                            System.err.println("Error in fillFriendsList() " + e.getMessage());
                        }
                    }
                }
        }).executeAsync();
    }


    /*
     * Used to populate count variable in all FacebookUser objects in ArrayList from Firebase
     */
    private void populateCount() {
        String id = AccessToken.getCurrentAccessToken().getUserId();
        DatabaseReference userCountRef = FirebaseDatabase.getInstance().getReference("users").child(id).child("count");

        filledTable = new boolean[myList.size()];

        // Loop through the ArrayList to fill the FacebookUsers with their count from Firebase
        for (int i = 0; i < myList.size(); i++) {
            DatabaseReference friendCountRef = FirebaseDatabase.getInstance().getReference("users").child(myList.get(i).getID()).child("count");
            final int index = i;
            friendCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (myList.isEmpty() || filledTable.length == 0) {
                        return;
                    }

                    filledTable[index] = true;

                    if (dataSnapshot.exists()) {
                        myList.get(index).setCount(dataSnapshot.getValue(Integer.class));
                    }

                    // Sort once it's done populating
                    /*if (index == (myList.size() - 1)) {
                        // Sort ArrayList
                        Collections.sort(myList, new CustomComparator());
                    }*/

                    for (int i = 0; i < filledTable.length; i++) {
                        if (!filledTable[i])
                            break;

                        Collections.sort(myList, new CustomComparator());
                    }
                    arrayAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error: onCancelled in populateCount()");
                }
            });
        }
    }


    // Custom Compare Method
    public static class CustomComparator implements Comparator<FacebookUser> {
        @Override
        public int compare(FacebookUser o1, FacebookUser o2) {
            if (o1.getCount() < o2.getCount())
                return 1;
            else if (o1.getCount() > o2.getCount())
                return -1;
            else
                return 0;
        }
    }

}
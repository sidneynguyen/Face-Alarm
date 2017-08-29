package com.xangnun.facealarm;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by Jack Wang on 10/22/2016.
 */

/**
 * Sources:
 *
 * Alarm
 * https://www.youtube.com/playlist?list=PL4uut9QecF3DLAacEoTctzeqTyvgzqYwA
 */
public class WakeUpActivity extends AppCompatActivity {

    //
    // CONSTANT
    //

    //create a Button
    private Button emergency_turn_off;
    //create a Intent;
    private Intent my_intent;

    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set up the content view
        setContentView(R.layout.activity_wake_up);

        //set up the context
        context = this;

        //initialize the turn off button
        emergency_turn_off = (Button) findViewById(R.id.emergency_turn_off);
        emergency_turn_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                //need to set the text back to off
                my_intent = new Intent(context, Alarm_Receiver.class);
                //stop the alarm
                my_intent.putExtra("extra", "Alarm OFF");
                sendBroadcast(my_intent);
                finish();
            }
        });
    }
}

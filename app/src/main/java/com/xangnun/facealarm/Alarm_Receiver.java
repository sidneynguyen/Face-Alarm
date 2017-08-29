package com.xangnun.facealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Jack Wang on 10/18/2016.
 */

/**
 * Sources:
 *
 * Alarm
 * https://www.youtube.com/playlist?list=PL4uut9QecF3DLAacEoTctzeqTyvgzqYwA
 */
public class Alarm_Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("In the Receiver", "Good");

        //create a service intent
        Intent service_intent = new Intent(context, RingTonePlayingService.class);
        service_intent.putExtra("extra", intent.getExtras().getString("extra"));
        service_intent.putExtra("RingTone", intent.getExtras().getString("RingTone"));
        service_intent.putExtra("Uid", intent.getExtras().getString("Uid"));
        service_intent.putExtra("URI", intent.toURI());
        context.startService(service_intent);
    }

}

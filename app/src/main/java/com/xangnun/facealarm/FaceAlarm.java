package com.xangnun.facealarm;

import com.facebook.FacebookSdk;

/**
 * Created by sidney on 11/5/16.
 */

public class FaceAlarm extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}

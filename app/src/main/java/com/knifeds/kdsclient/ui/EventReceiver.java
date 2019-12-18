package com.knifeds.kdsclient.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.knifeds.kdsclient.utils.StayForTrigger;

public class EventReceiver extends BroadcastReceiver {
    private static final String TAG = "EventReceiver";
    public static final String ACTION = "com.knifeds.event.triggered";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            String gender = intent.getStringExtra("gender");
//            Log.d(TAG, "onReceive: event triggered. gender=" + gender);
            StayForTrigger.getInstance().addEvent(gender);
        }
    }

}


package com.knifeds.kdsclient.utils;

import android.util.Log;

import com.knifeds.kdsclient.data.StateChanged;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;

public class StayForTrigger {
    private static final String TAG = "StayForTrigger";
    public static StayForTrigger getInstance() { return mInstance; }

    public long stayForSeconds = 5;

    private String currentEvent = "";
    private Date lastEventTime = null;
    private Date firstEventTime = null;
    private volatile boolean fired = false;

    public void addEvent(String event) {
        Date currentTime = Calendar.getInstance().getTime();

        if (currentEvent.length() == 0 ||firstEventTime == null || lastEventTime == null) {
            currentEvent = event;
            firstEventTime = currentTime;
            lastEventTime = currentTime;
            return;
        }

//        Log.d(TAG, "addEvent: Checking stay period...");
        if (event.equals(currentEvent)) {
            long diffLast = currentTime.getTime() - lastEventTime.getTime();
            if (diffLast > 1000) {  // More than 1 second since the last event, reset counting
                firstEventTime = currentTime;
                lastEventTime = currentTime;
                return;
            } else {
                lastEventTime = currentTime;
            }

            long diffFirst = currentTime.getTime() - firstEventTime.getTime();

            if (diffFirst > 1000 * stayForSeconds) {
                firstEventTime = currentTime;
                lastEventTime = currentTime;

                Log.d(TAG, "fireEvent: " + currentEvent);
                if (!fired) {
                    fired = true;
                    EventBus.getDefault().post(new StateChanged(StateChanged.State.ConditionChanged, currentEvent));
                }
            }
        } else {
            // This is a new event
            currentEvent = event;
            firstEventTime = currentTime;
            lastEventTime = currentTime;
            return;
        }
    }

    public void reset() {
        fired = false;
    }

    public boolean getFired() { return fired; }

    public void setPeriod(long period) {
        stayForSeconds = period;
    }

    private static final StayForTrigger mInstance = new StayForTrigger();
}


package com.knifeds.kdsclient.utils;

import android.os.Handler;
import android.os.Looper;

import com.knifeds.kdsclient.schedule.TimerMessage;

import org.greenrobot.eventbus.EventBus;

public class MessageUtil {
    public static void postMessageDelayed(final TimerMessage msg, final long delay) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(msg);
            }
        }, delay);
    }
}


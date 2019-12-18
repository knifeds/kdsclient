package com.knifeds.kdsclient.schedule;

public class TimerMessage {

    public int what;

    public Object obj;

    public TimerMessage() {}

    public TimerMessage(int what, Object obj) {
        this.what = what;
        this.obj = obj;
    }
}


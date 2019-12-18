package com.knifeds.kdsclient.data;

public class StateChanged {
    public enum State {
        None,
        PrerequisiteReady,
        MqttServiceReady,
        MqttConnectionLost,
        ContentReady,
        LicenseOk,
        LicenseError,
        ConditionChanged,
    }

    public State state = State.None;

    public Playlist playlist = null;

    public String condition;

    public StateChanged(State state) {
        this.state = state;
    }

    public StateChanged(State state, Playlist playlist) {
        this.state = state;
        this.playlist = playlist;
    }

    public StateChanged(State state, String condition) {
        this.state = state;
        this.condition = condition;
    }
}

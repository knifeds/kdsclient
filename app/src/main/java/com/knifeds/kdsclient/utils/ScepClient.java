package com.knifeds.kdsclient.utils;

public class ScepClient extends Commandline {
    public ScepClient(final ScepClientConfig config) {
        super();
        command = config.getScepClientCommand();
    }
}
package com.knifeds.kdsclient.utils;

import java.util.HashMap;
import java.util.Map;

public class ScepClientConfig {
    private static final Map<String, String> hostedScepClientFilenames = new HashMap<String, String>() {{
        put("i686", "scepclient-linux-386");
        put("x86_64", "scepclient-linux-amd64");
        put("armv7l", "scepclient-linux-arm");
        put("armv8l", "scepclient-linux-arm64");
        put("arm64", "scepclient-linux-arm64");
        put("aarch64", "scepclient-linux-arm64");
        put("freebsd", "scepclient-freebsd-amd64");
        put("macos", "scepclient-darwin-amd64");
    }};

    private String scepServerUrl;
    private String scepClientFilePath;
    private String clientCertName;
    private String clientCertFilePath;
    private String clientKeyFilePath;

    public ScepClientConfig(final String scepServerUrl, final String scepClientFilePath, final String clientCertName, final String clientCertFilePath, final String clientKeyFilePath) {
        this.scepServerUrl = scepServerUrl;
        this.scepClientFilePath = scepClientFilePath;
        this.clientCertName = clientCertName;
        this.clientCertFilePath = clientCertFilePath;
        this.clientKeyFilePath = clientKeyFilePath;
    }

    public static String getHostedScepClientFilename(final String arch) {
        return hostedScepClientFilenames.get(arch);
    }

    public String getScepClientCommand() {
        return scepClientFilePath +
                " -certificate" + " " + clientCertFilePath +
                " -cn" + " " + clientCertName +
                " -country" + " CN" +
                " -province" + " Beijing" +
                " -locality" + " Beijing" +
                " -organization" + " LBD" +
                " -ou" + " BD" +
                " -server-url" + " " + scepServerUrl +
                " -private-key" + " " + clientKeyFilePath +
                " -challenge=secret";
    }
}

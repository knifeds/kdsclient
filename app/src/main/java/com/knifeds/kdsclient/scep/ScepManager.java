package com.knifeds.kdsclient.scep;

import android.util.Log;

import com.knifeds.kdsclient.data.Config;
import com.knifeds.kdsclient.data.Consts;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.data.StateChanged;
import com.knifeds.kdsclient.utils.Commandline;
import com.knifeds.kdsclient.utils.HostedFiles;
import com.knifeds.kdsclient.utils.ScepClient;
import com.knifeds.kdsclient.utils.ScepClientConfig;

import org.greenrobot.eventbus.EventBus;

import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScepManager implements Observer {
    private static final String TAG = "ScepManager";

    @Inject
    public ScepManager() { }

    @Inject
    DataContext dataContext;

    public void doScep() {
        String arch = System.getProperty("os.arch");
        Log.d(TAG, "doScep: arch = " + arch);

        HostedFiles hostedFiles = new HostedFiles(dataContext);

        hostedFiles.addObserver(this);
        hostedFiles.add(dataContext.scepClientFilePath, Config.hostedFileBaseUrl + ScepClientConfig.getHostedScepClientFilename(arch));
        hostedFiles.add(dataContext.getMqttCaFilePath(), Config.hostedFileBaseUrl + Config.mqttCaFilename);

        hostedFiles.checkAndFetchAll(Consts.O_GET_REQUIRED_FILES, Consts.O_GET_REQUIRED_FILES_FAILED,false);
    }

    // Handle Observer of the downloader
    @Override
    public void update(Observable observable, Object o) {
        int tag = (int)o;
        switch(tag) {
            case Consts.O_GET_REQUIRED_FILES:
                Log.d(TAG, "update: Files checked. Starting MQTT client");
                this.makeScepClientRunnable();
                this.execScepClient();

                // Notify MainActivity
                EventBus.getDefault().post(new StateChanged(StateChanged.State.PrerequisiteReady));
                break;
        }
    }

    private void makeScepClientRunnable() {
        try{
            Commandline commandline = new Commandline();
            commandline.command = "chmod a+x " + dataContext.scepClientFilePath;
            commandline.run();
        }catch(Exception e){
            Log.e(TAG, "makeScepClientRunnable: " + e.getMessage());
        }
    }

    private void execScepClient() {
        try{
            ScepClientConfig scepClientConfig = new ScepClientConfig(dataContext.getScepServerUrl(), dataContext.scepClientFilePath, dataContext.getClientCertName(), dataContext.getClientCertFilePath(), dataContext.getClientKeyFilePath());
            ScepClient client = new ScepClient(scepClientConfig);
            client.run();
        }catch(Exception e){
            Log.e(TAG, "execScepClient: " + e.getMessage());
        }
    }
}


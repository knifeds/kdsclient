package com.knifeds.kdsclient.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Commandline {
    private static final String TAG = "Commandline";
    public String command;
    public void run() {
        try{
            Log.d(TAG, "run: " + command);

            Process cmd = Runtime.getRuntime().exec(new String[]{
                    "sh", "-c", command
            });

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(cmd.getInputStream()));

            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            cmd.waitFor();

            Log.d(TAG, "run: Exec command done. \n" + output.toString());

        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public boolean run2() {
        boolean bSuccess = false;
        try {
            ConsoleOutput output = null;

            output = SystemProcess.execCommand(command);
            if(output.isSuccess) {
                bSuccess = true;
            }else{
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.i(TAG, "Exception: "+e.toString());
            e.printStackTrace();
        }

        return bSuccess;
    }

}


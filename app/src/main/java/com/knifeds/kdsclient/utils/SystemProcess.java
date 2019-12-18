package com.knifeds.kdsclient.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SystemProcess {
    private static final String TAG = "SystemProcess";

    private static Boolean installPermission = null;
    private static Boolean uninstallPermission = null;

    public static boolean hasInstallPermission(Context context) {
        if(installPermission == null) {
            installPermission = context.checkCallingOrSelfPermission(Manifest.permission.INSTALL_PACKAGES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return installPermission;
    }

    public static boolean hasUninstallPermission(Context context) {
        if(uninstallPermission == null) {
            uninstallPermission = context.checkCallingOrSelfPermission(Manifest.permission.DELETE_PACKAGES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return uninstallPermission;
    }

    public static ConsoleOutput execCommand(String command) {
        Log.d(TAG, "SystemProcess: execCommand() called: " + command);
        ConsoleOutput result = new ConsoleOutput();
        String[] args = command.split(" ");
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replaceAll("\"", "");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        BufferedReader in = null;
        BufferedReader er = null;
        try {
            Process process = processBuilder.start();
            process.waitFor();
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            er = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = er.readLine()) != null) {
                result.error += line + "\n";
            }
            while ((line = in.readLine()) != null) {
                result.contents.add(line);
            }
            process.destroy();
        } catch (InterruptedException ie) {
            Log.e(TAG, TAG + " system " + ie.getMessage());
        } catch (IOException ioe) {
            Log.e(TAG, TAG + " system " + ioe.getMessage());
        } catch (RuntimeException e) {
            Log.e(TAG, TAG + " system " + e.getMessage());
        } finally {
            Tools.close(in);
            Tools.close(er);
        }

        result.isSuccess = Tools.isSuccess(result);

        Log.d(TAG, TAG + " system error:" + result.error);
        return result;
    }
}


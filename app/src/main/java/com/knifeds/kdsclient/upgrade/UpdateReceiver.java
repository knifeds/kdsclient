package com.knifeds.kdsclient.upgrade;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.knifeds.kdsclient.MyApplication;
import com.knifeds.kdsclient.data.DataContext;

import java.io.File;

import javax.inject.Inject;

public class UpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "UpdateReceiver";

    public static final String UPDATE_ACTION = "kdsclient";
    private static final String UPDATE_FILENAME = "kdsclientupdate.apk";
    private boolean isShowDialog;
    MyApplication app;

    @Inject
    DataContext dataContext;

    public UpdateReceiver(boolean isShowDialog) {
        super();
        this.isShowDialog = isShowDialog;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (app == null) {
            app = (MyApplication) ((Activity)context).getApplication();
        }

        UpdateResponse updateResponse = dataContext.getUpdateResponse(UpgradeManager.UPDATE_DOWNLOAD_KEY);
        if (updateResponse == null) {
            updateResponse = dataContext.getUpdateResponse(UpgradeManager.UPDATE_UPGRADE_KEY);
            if (updateResponse == null) {
                Log.e(TAG, "No upgrade command found!");
                return;
            } else {
                Log.d(TAG, "Got Client Upgrade request.");
            }
        } else {
            Log.d(TAG, "Got Client Download request.");
        }

        try {
            UpdateInfo.localVersion = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode;
            UpdateInfo.localVersionName = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionName;

            // UpdateInfo.appname = app.getResources().getString(R.string.app_name);
            if (!TextUtils.isEmpty(updateResponse.getVersion())) {
                UpdateInfo.serverVersion = Integer.parseInt(updateResponse.getVersion());
            }

            if (!TextUtils.isEmpty(updateResponse.getVersionName())) {
                UpdateInfo.serverVersionName = updateResponse.getVersionName();
            }
            UpdateInfo.url = updateResponse.getUrl();
            UpdateInfo.description = updateResponse.getDescription();

            checkVersion(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkVersion(Context context) {
        if (UpdateInfo.serverVersion > UpdateInfo.localVersion) {
            clearUpdateFile(context);

            update(context);
        } else {
            if (isShowDialog) {
                noNewVersion(context);
            }
        }
    }

    private int getJekinsBuildNumber(final String versionName) {
        // localVersionName is x.y.zzzz, zzzz is the Jenkins build #
        int li = versionName.lastIndexOf('.');
        return Integer.parseInt(versionName.substring(li+1));
    }

    private void update(Context context) {
        normalUpdate(context);
    }

    private void noNewVersion(final Context context) {

    }

    private void normalUpdate(final Context context) {
        Intent i = new Intent(context, UpdateService.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Pass data to intent
        i.putExtra("apkname", UPDATE_FILENAME); //UpdateInfo.appname);
        i.putExtra("apkurl", UpdateInfo.url);
        context.startService(i);
    }

    private void clearUpdateFile(final Context context) {
        try {
            File updateDir;
            File updateFile;
            if (Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                updateDir = new File(Environment.getExternalStorageDirectory(),
                        UpdateInfo.downloadDir);
            } else {
                updateDir = context.getFilesDir();
            }
//            updateFile = new File(updateDir.getPath(), context.getResources()
//                    .getString(R.string.app_name) + ".apk");
            updateFile = new File(updateDir.getPath(), UPDATE_FILENAME);
            if (updateFile.exists()) {
                updateFile.delete();
            }
        } catch (Exception e) {
        }
    }
}


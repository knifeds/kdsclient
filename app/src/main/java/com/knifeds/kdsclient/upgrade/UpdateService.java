package com.knifeds.kdsclient.upgrade;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.knifeds.kdsclient.R;
import com.knifeds.kdsclient.data.Consts;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.utils.StatusMessage;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

import javax.inject.Inject;

import static com.knifeds.kdsclient.upgrade.UpgradeManager.CLIENT_DOWNLOAD_COMMANDID;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.CLIENT_UPGRADE_COMMANDID;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.UPDATE_DOWNLOAD_KEY;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.UPDATE_UPGRADE_KEY;

/**
 * Need to be registered in mainfest file
 */
public class UpdateService extends Service {
    // Size in Byte
    private static final float SIZE_BT = 1024L;
    // Size in KB
    private static final float SIZE_KB = SIZE_BT * 1024.0f;
    // Size in MB
    private static final float SIZE_MB = SIZE_KB * 1024.0f;

    private String apkName = null;  // Apk name
    private String apkUrl = null;   // Apk download url
    private File updateDir = null;  // update file dir
    private File updateFile = null; // update file
    String mDownloadCommandid;
    UpdateResponse updateResponse;

    @Inject
    DataContext dataContext;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        apkName = intent.getStringExtra("apkname");
        apkUrl = intent.getStringExtra("apkurl");
        new UpdateThread().execute();
    }

    /**
     * Download using AsyncTask
     */
    class UpdateThread extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            return downloadUpdateFile(apkUrl);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            try {
                if (result == Consts.DOWNLOAD_COMPLETE) {
                    EventBus.getDefault().post(new StatusMessage("New client downloaded, continue with the upgrade..."));

                    Log.d("update", getResources().getString(R.string.update_apk_download_success));
                    String cmd = "chmod 777 " + updateFile.getPath();
                    try {
                        Runtime.getRuntime().exec(cmd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    dataContext.setUpdateFile(updateFile);
                    if (dataContext.hasPrivateConfig(CLIENT_DOWNLOAD_COMMANDID)) {
                        mDownloadCommandid = dataContext.getPrivateConfig(CLIENT_DOWNLOAD_COMMANDID, "");
                        updateResponse = dataContext.getUpdateResponse(UPDATE_DOWNLOAD_KEY);
                    } else if (dataContext.hasPrivateConfig(CLIENT_UPGRADE_COMMANDID)) {
                        updateResponse = dataContext.getUpdateResponse(UPDATE_UPGRADE_KEY);
                    }
                    stopSelf();
                } else if (result == Consts.DOWNLOAD_NOMEMORY) {
                    stopSelf();
                    EventBus.getDefault().post(new StatusMessage("Client download failed due to insufficient space!"));
                } else if (result == Consts.DOWNLOAD_FAIL) {
                    stopSelf();
                    EventBus.getDefault().post(new StatusMessage("Client download failed!"));
                }

                UpdateMessage updateMessage = new UpdateMessage();
                updateMessage.setUpdateResult(result);
                if (TextUtils.isEmpty(mDownloadCommandid)) {
                    updateMessage.setDownloadOnly(false);
                    updateMessage.setUpgradeTime(updateResponse.getUpgradeTime());
                } else {
                    updateMessage.setDownloadOnly(true);
                }
                EventBus.getDefault().post(updateMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Download Update File
     * @param downloadUrl
     * @return
     */
    private int downloadUpdateFile(String downloadUrl) {
        int count = 0;
        long totalSize = 0;
        long downloadSize = 0;
        URL uri = null;

        EventBus.getDefault().post(new StatusMessage("Downloading client package from " + downloadUrl + "..."));

        HttpURLConnection httpURLConnection = null;
        try {
            uri = new URL(downloadUrl);
            httpURLConnection = (HttpURLConnection) uri.openConnection();
        } catch (Exception e) {
//            String encodedUrl = downloadUrl.replace(' ', '+');
//            httpGet = new HttpGet(encodedUrl);
            e.printStackTrace();
        }
//        HttpClient httpClient = new DefaultHttpClient();
//        HttpResponse httpResponse = null;
        FileOutputStream fos = null;
        InputStream is = null;
        try {
//            httpResponse = httpClient.execute(httpGet);
//            if (httpResponse != null) {
//                int stateCode = httpResponse.getStatusLine().getStatusCode();
            if (httpURLConnection.getResponseCode() == 200) {
                if (httpURLConnection != null) {
                    totalSize = httpURLConnection.getContentLength();
                    //如果内存可用
                    if (MemoryAvailable(totalSize)) {
                        is = httpURLConnection.getInputStream();
                        if (is != null) {
                            fos = new FileOutputStream(updateFile, false);
                            byte buffer[] = new byte[4096];
                            int readsize = 0;
                            while ((readsize = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, readsize);
                                downloadSize += readsize;
                                if ((count == 0)
                                        || (int) (downloadSize * 100 / totalSize) >= count) {
                                    count += 5;
//                                        updateNotification.contentView
//                                                .setTextViewText(
//                                                        R.id.download_notice_speed_tv,
//                                                        getMsgSpeed(downloadSize,totalSize));
//                                        updateNotificationManager.notify(0,
//                                                updateNotification);
                                    EventBus.getDefault().post(new StatusMessage("Downloading (" + getMsgSpeed(downloadSize,totalSize) + "): " + downloadUrl + "..."));
                                }
                            }
                            fos.flush();
                            if (totalSize >= downloadSize) {
                                return Consts.DOWNLOAD_COMPLETE;
                            } else {
                                return Consts.DOWNLOAD_FAIL;
                            }
                        }
                    } else {
                        return Consts.DOWNLOAD_NOMEMORY;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Consts.DOWNLOAD_FAIL;
    }

    private boolean MemoryAvailable(long fileSize) {
        fileSize += (1024 << 10);
        if (MemoryStatus.externalMemoryAvailable()) {
            if ((MemoryStatus.getAvailableExternalMemorySize() <= fileSize)) {
                if ((MemoryStatus.getAvailableInternalMemorySize() > fileSize)) {
                    createFile(false);
                    return true;
                } else {
                    return false;
                }
            } else {
                createFile(true);
                return true;
            }
        } else {
            if (MemoryStatus.getAvailableInternalMemorySize() <= fileSize) {
                return false;
            } else {
                createFile(false);
                return true;
            }
        }
    }

    public static String getMsgSpeed(long downSize, long allSize) {
        StringBuffer sBuf = new StringBuffer();
        sBuf.append(getSize(downSize));
        sBuf.append("/");
        sBuf.append(getSize(allSize));
        sBuf.append(" ");
        sBuf.append(getPercentSize(downSize, allSize));
        return sBuf.toString();
    }

    public static String getSize(long size) {
        if (size >= 0 && size < SIZE_BT) {
            return (double) (Math.round(size * 10) / 10.0) + "B";
        } else if (size >= SIZE_BT && size < SIZE_KB) {
            return (double) (Math.round((size / SIZE_BT) * 10) / 10.0) + "KB";
        } else if (size >= SIZE_KB && size < SIZE_MB) {
            return (double) (Math.round((size / SIZE_KB) * 10) / 10.0) + "MB";
        }
        return "";
    }

    public static String getPercentSize(long downSize, long allSize) {
        String percent = (allSize == 0 ? "0.0" : new DecimalFormat("0.0")
                .format((double) downSize / (double) allSize * 100));
        return "(" + percent + "%)";
    }

    private void createFile(boolean sd_available) {
        if (sd_available) {
            updateDir = new File(Environment.getExternalStorageDirectory(),
                    UpdateInfo.downloadDir);
        } else {
            updateDir = getFilesDir();
        }
        updateFile = new File(updateDir.getPath(), apkName);
        if (!updateDir.exists()) {
            updateDir.mkdirs();
        }
        if (!updateFile.exists()) {
            try {
                updateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            updateFile.delete();
            try {
                updateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

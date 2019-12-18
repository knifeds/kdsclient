package com.knifeds.kdsclient.data;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.knifeds.kdsclient.utils.HostedFiles;
import com.knifeds.kdsclient.utils.QRCodeUtil;
import com.knifeds.kdsclient.utils.StatusMessage;
import com.knifeds.kdsclient.utils.UnZipper;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class Playlist extends Observable implements Observer {
    @Expose
    public String Url;
    @Expose
    public String StartTime;
    @Expose
    public String EndTime;
    @Expose
    public String PlayListId;
    @Expose
    public String playListMd5;
    @Expose
    public int PlayListType;
    @Expose
    public List<QRImage> QRImages;
    @Expose
    public String Condition;

    public static DataContext dataContext;

    private static final String TAG = "Playlist";

    public Playlist() { PlayListType = 0; }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String Url) {
        this.Url = Url;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public Date getFormatEndtime() {
        return stringToDate(EndTime, "HH:mm");
    }

    public int getFormatStarttime() {
        SimpleDateFormat df1 = new SimpleDateFormat("HHmm");
        int mFormatStartTime = Integer.parseInt(df1.format(stringToDate(StartTime, "HH:mm")));
        return mFormatStartTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public boolean isIdlePlaylist() {
        return (PlayListType == 0);
    }

    public static Playlist parse(final String input) {
        Gson gson = new Gson();
        Playlist playlist = gson.fromJson(input, Playlist.class);
        return playlist;
    }

    // strTime要转换的string类型的时间，formatType要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日
    // HH时mm分ss秒，
    // strTime的时间格式必须要与formatType的时间格式相同
    public Date stringToDate(String strTime, String formatType) {
        Date date = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(formatType);
            date = formatter.parse(strTime);
        } catch (Exception e) {
        }
        return date;
    }

    public boolean equalsTo(Playlist another) {
        return (Url.equals(another.Url) && StartTime.equals(another.StartTime) && EndTime.equals(another.EndTime));
    }

    public void prepareContent() {
        String contentBaseFolder = dataContext.filesDir + "/" + PlayListId;
        File contentBaseDir = new File(contentBaseFolder);
        if (!contentBaseDir.exists()) {
            contentBaseDir.mkdirs();
        }

        contentZipFilePath = contentBaseFolder + "/web.zip";
        contentFolder = contentBaseFolder + "/content";
        indexHtmlPath = "file://" + contentBaseFolder + "/content/html/index.html";

        if (!isContentReady()) {
            if (!downloadingContent) {
                downloadingContent = true;
                Log.d(TAG, "prepareContent: Start downloading Content for PlayListId=" + PlayListId);
                downloadContent();
            } else {
                Log.w(TAG, "prepareContent: Another download is in progress for PlayListId=" + PlayListId);
            }
        } else {
            emitContentReady();
        }
    }

    public void downloadContent() {
        String downloadUrl = Config.changeWwwToStaging ? Url.replace("https://www.", "https://staging.") : Url;
        EventBus.getDefault().post(new StatusMessage("Downloading and displaying content:" + downloadUrl));
        HostedFiles hostedFiles = new HostedFiles(dataContext);
        hostedFiles.addObserver(this);
        hostedFiles.add(contentZipFilePath, downloadUrl);
        hostedFiles.checkAndFetchAll(Consts.O_GET_PLAYLIST_CONTENT, Consts.O_GET_PLAYLIST_CONTENT_FAILED, true);
    }

    public String getIndexHtmlPath() {
        return indexHtmlPath;
    }

    @Override
    public void update(Observable o, Object arg) {
        int tag = (int)arg;
        switch(tag) {
            case Consts.O_GET_PLAYLIST_CONTENT:
//                Logger.d("Got zip file, unzipping...");
                unzipContentFile();
                break;
            case Consts.O_GET_PLAYLIST_CONTENT_FAILED:
                Log.e(TAG, "update: Downloading playlist content failed.");
                dataContext.resetDeviceStateHash();   // FIXME: Quick way to tell the server to push the playlist again
                break;
            case Consts.O_UNZIPPED:
//                Logger.d("Unzip done, notifying MainActivity...");
                generateQRImages();
                downloadingContent = false;
                contentReady = true;
                // FIXME: Set something other than empty so that contentReady can use this to decide whether to download.
                if (playListMd5 == null || playListMd5.length() == 0) {
                    playListMd5 = "0";
                }
                dataContext.setPlaylistMd5(PlayListId, playListMd5);
                emitContentReady();
                break;
            case Consts.O_UNZIPPED_FAILED:
                downloadingContent = false;
                break;
        }
    }

    private void emitContentReady() {
        // Notify ScheduleManager to start next download
        setChanged();
        notifyObservers(Consts.O_PREPARE_PLAYLIST);

        // Notify MainActivity to play
        EventBus.getDefault().post(new StateChanged(StateChanged.State.ContentReady, this));
    }

    // Implementation
    private void unzipContentFile() {
        UnZipper unzipper = new UnZipper(contentZipFilePath, contentFolder);
        unzipper.addObserver(this);
        unzipper.unzip();
    }

    private void generateQRImages() {
        if (QRImages == null) {
            return;
        }

        String qrImagesLocation = contentFolder + "/apps/qr-code/";

        for (int i = 0; i < QRImages.size(); i++) {
            QRImage qrImage = QRImages.get(i);
            if (qrImage.QRPicName == null || qrImage.QRUrl == null) {
                continue;
            }
            String realUrl = qrImage.QRUrl.replace("{DEVICE_ID}", dataContext.getDeviceUuid());
            new QRCodeUtil().generateAndSaveImage(realUrl,qrImagesLocation + qrImage.QRPicName);
        }
    }

    private boolean isContentReady() {
        if (contentReady) return true;

        final String existingMd5 = dataContext.getPlaylistMd5(PlayListId);

        // FIXME: When provided playListMd5 is NULL or empty, if our Md5 is not empty, then we win
        if (playListMd5 == null || playListMd5.length() == 0) {
            return false;
        }

        if (existingMd5.length() > 0 && existingMd5.equals(playListMd5)) {
            contentReady = true;
            return true;
        }

        return false;
    }

    private volatile boolean downloadingContent = false;
    private boolean contentReady = false;
    private String contentZipFilePath;
    private String contentFolder;
    private String indexHtmlPath;
}

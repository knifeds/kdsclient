package com.knifeds.kdsclient.utils;

import com.knifeds.kdsclient.data.DataContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class HostedFiles extends Observable implements OnFileDownloaded {
    int successTag = 0;
    int failureTag = 1;
    boolean overwrite = false;
    @Override
    public void onFileDownloaded(String url) {
        if (url.length() == 0) {
            setChanged();
            notifyObservers(failureTag);
            return; // Do nothing?
        }

        // Determine the nextStep
        int nextStep = requiredLocalFiles.size();
        for (int i = 0; i < requiredLocalFiles.size(); i++) {
            RequiredFile requiredFile = requiredLocalFiles.get(i);
            if (url.compareToIgnoreCase(requiredFile.url) == 0) {
                nextStep = i+1;
                break;
            }
        }

        if (nextStep < requiredLocalFiles.size()) {
            checkAndFetchOne(nextStep);
        } else {
            setChanged();
            notifyObservers(successTag);
        }
    }

    private class RequiredFile {
        public String localFilename;
        public String url;
    }

    private List<RequiredFile> requiredLocalFiles = new ArrayList<>();

    DataContext dataContext = null;
    public HostedFiles(DataContext dataContext){
        this.dataContext = dataContext;
    }

    public void add(final String localFilePath, final String hostedFileUrl){
        requiredLocalFiles.add(new RequiredFile() {{
            localFilename = localFilePath;
            url = hostedFileUrl;
        }});
    }

    public void checkAndFetchAll(int successTag, int failureTag, boolean overwrite){
        this.successTag = successTag;
        this.failureTag = failureTag;
        this.overwrite = overwrite;
        if (requiredLocalFiles.size() > 0) {
            checkAndFetchOne(0);
        } else {
            setChanged();
            notifyObservers(this.successTag);
        }
    }

    private void downloadFile(RequiredFile requiredFile) {
        FileDownloader downloader = new FileDownloader(requiredFile.localFilename, this, dataContext);
        downloader.execute(requiredFile.url);
    }

    private void checkAndFetchOne(int step){
        RequiredFile requiredFile = requiredLocalFiles.get(step);
        File file = new File(requiredFile.localFilename);
        if (overwrite || !file.exists()) {
            this.downloadFile(requiredFile);
        } else {
            this.onFileDownloaded(requiredFile.url);
        }
    }
}

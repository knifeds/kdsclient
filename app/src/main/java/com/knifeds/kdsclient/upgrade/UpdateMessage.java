package com.knifeds.kdsclient.upgrade;

public class UpdateMessage {
    public int updateResult;
    public boolean downloadOnly;
    public String upgradeTime;

    public int getUpdateResult() {
        return updateResult;
    }

    public void setUpdateResult(int updateResult) {
        this.updateResult = updateResult;
    }

    public boolean isDownloadOnly() {
        return downloadOnly;
    }

    public void setDownloadOnly(boolean downloadOnly) {
        this.downloadOnly = downloadOnly;
    }

    public String getUpgradeTime() {
        return upgradeTime;
    }

    public void setUpgradeTime(String upgradeTime) {
        this.upgradeTime = upgradeTime;
    }
}

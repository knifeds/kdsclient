package com.knifeds.kdsclient.upgrade;

import com.google.gson.Gson;

public class UpdateResponse
{
    // Just a number
    public String version;
    // Like 2.0.11
    public String versionName;

    public String description;
    // apk url
    public String url;
    // time to download
    public String  downloadTime;
    // md5: version  url checkCode
    public String checkCode;
    // time to run
    public String upgradeTime;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getUpgradeTime() {
        return upgradeTime;
    }

    public void setUpgradeTime(String upgradeTime) {
        this.upgradeTime = upgradeTime;
    }

    public String getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(String downloadTime) {
        this.downloadTime = downloadTime;
    }

    public static UpdateResponse parse(final String input){
        Gson gson = new Gson();
        UpdateResponse updateResponse = gson.fromJson(input, UpdateResponse.class);
        return updateResponse;
    }
}

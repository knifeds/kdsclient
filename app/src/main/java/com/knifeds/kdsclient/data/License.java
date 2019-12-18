package com.knifeds.kdsclient.data;

import com.google.gson.Gson;

public class License {
    public String licensePath;//https://<>/images/dss/license/358240051111110_license.dat
    public String publicKeyPath;//https://<>/images/dss/license/358240051111110_publickey.key
    public String randomSecret;//352430d26d4484255808b19dc18617c0d817f48d30550463

    public String getLicensePath() {
        return licensePath;
    }

    public void setLicensePath(String licensePath) {
        this.licensePath = licensePath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getRandomSecret() {
        return randomSecret;
    }

    public void setRandomSecret(String randomSecret) {
        this.randomSecret = randomSecret;
    }

    public License() {
    }

    public License(String licensePath, String publicKeyPath, String randomSecret) {
        this.licensePath = licensePath;
        this.publicKeyPath = publicKeyPath;
        this.randomSecret = randomSecret;
    }

    public static License parse(final String input){
        Gson gson = new Gson();
        License license = gson.fromJson(input, License.class);
        return license;
    }
}

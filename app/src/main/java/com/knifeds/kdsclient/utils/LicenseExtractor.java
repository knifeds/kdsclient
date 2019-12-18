package com.knifeds.kdsclient.utils;

import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;

public class LicenseExtractor {
    private static final String TAG = "LicenseExtractor";

    public static boolean generate(String commandUUid,String licensePath,String publicKeyPath,String randomSecrect) throws Exception {
//        byte[] bytes = FileUtils.readFileToByteArray(new File("/optaps/images/dss/license/358240051111110_license.dat"));
//        String publicKey = FileUtils.readFileToString(new File("/optaps/images/dss/license/358240051111110_publicKey.key"));
        File licenseFile = new File(licensePath);
        if(!licenseFile.exists())return false;
        byte[] bytes = FileUtils.readFileToByteArray(licenseFile);
        String publicKey = FileUtils.readFileToString(new File(publicKeyPath));
        byte[] decodedData = RSAUtils.decryptByPublicKey(bytes, publicKey);
        String target = new String(decodedData);

        Log.d(TAG, "generate: Decrypted: \n" + target);

        JSONObject json = new JSONObject(target);

        String expires = json.getString("expires");

        // Device random password，encrypted text are sent by license command, the following secret is plain text
        String secret = json.getString("secret");
        String uuid = json.getString("uuid");

        // verify if uuid is the same as self uuid
        boolean isVaildUUid = false;
        if(commandUUid!=null&&commandUUid.equals(uuid)){
            isVaildUUid = true;
        }
        // verify that the expiration date is greater than now
        boolean isExpires = false;
        if(!TimeUtil.afterDate(expires)){
            isExpires = true;
        }
        // Verify random secret
        boolean result = PasswordUtil.verify(secret, randomSecrect);
        if(isVaildUUid && isExpires && result){
            // Verification is passed only if the above three steps all passed. Exception during decryption are considered failure to
            return true;
        }
        return false;
    }
}

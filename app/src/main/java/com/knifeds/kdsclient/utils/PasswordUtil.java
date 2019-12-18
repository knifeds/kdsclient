package com.knifeds.kdsclient.utils;

import org.spongycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {
    private PasswordUtil() {
        throw new IllegalStateException("PasswordUtil class");
    }

    /**
     * Check if the password is correct
     */
    public static boolean verify(String password, String md5) {
        if (password.isEmpty() || md5.isEmpty()) {
            return false;
        }
        char[] cs1 = new char[32];
        char[] cs2 = new char[16];
        for (int i = 0; i < 48; i += 3) {
            cs1[i / 3 * 2] = md5.charAt(i);
            cs1[i / 3 * 2 + 1] = md5.charAt(i + 2);
            cs2[i / 3] = md5.charAt(i + 1);
        }
        String salt = new String(cs2);
        String passwordFormt = md5Hex(password + salt);

        if (passwordFormt.isEmpty()) {
            return false;
        } else {
            return passwordFormt.equals(new String(cs1));
        }
    }

    /**
     * Get MD5 in Hex format
     */
    public static String md5Hex(String src) {
        byte[] bs = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            bs = md5.digest(src.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new String(Hex.encode(bs));
    }
}

package com.knifeds.kdsclient.data;

public class Consts {
    public static final int O_GET_REQUIRED_FILES=1;
    public static final int O_GET_REQUIRED_FILES_FAILED=1+100;
    public static final int O_GET_PLAYLIST_CONTENT=2;
    public static final int O_GET_PLAYLIST_CONTENT_FAILED=2+100;
    public static final int O_UNZIPPED=3;
    public static final int O_UNZIPPED_FAILED=3+100;
    public static final int O_LICENSE=4;
    public static final int O_LICENSE_FAILED=4+100;
    public static final int O_PUBLICKEY=5;
    public static final int O_PUBLICKEY_FAILED=5+100;
    public static final int O_PREPARE_PLAYLIST=6;
    public static final int O_PREPARE_PLAYLIST_FAILED=6+100;

    public final static int DOWNLOAD_COMPLETE = 1;
    public final static int DOWNLOAD_NOMEMORY = -1;
    public final static int DOWNLOAD_FAIL = -2;

    public static final int TIMER_SCHEDULER = 100;
    public static final int TIMER_DOWNLOAD_APK = 101;
    public static final int TIMER_UPGRADE_APK = 102;
}

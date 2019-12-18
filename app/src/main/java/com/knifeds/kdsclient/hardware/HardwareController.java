package com.knifeds.kdsclient.hardware;

import android.app.Activity;
import android.content.Context;

import java.util.Date;

public interface HardwareController {
    void reboot(Context context);
    void shutdown(Context context);
    void setOnOffDate(Context context, Date onDate, Date offDate);
    String takeScreenshot(Activity activity);
    String getCapabilities();
    String getInstallCommand(String pkg, String path);
    boolean hasHardwareRotation();
    void setOrientation(int orientation);
}

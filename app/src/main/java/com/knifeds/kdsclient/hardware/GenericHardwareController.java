package com.knifeds.kdsclient.hardware;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class GenericHardwareController implements HardwareController {
    @Override
    public void reboot(Context context) {
        try {
            Runtime.getRuntime().exec(new String[]{"su","-c","shutdown"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_REBOOT);
            intent.putExtra("nowait", 1);
            intent.putExtra("interval", 1);
            intent.putExtra("window", 0);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final String takeScreenshot(Activity activity) {
        try {
            // create bitmap screen capture
            View v1 = activity.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File file = new File(Environment.getExternalStorageDirectory() + "/kdsclient");
            if (!file.exists())
                file.mkdir();

            String imageName = "screenshot";
            // Image name and path to include sd card appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/kdsclient/" + imageName + ".jpg";
            File imageFile = new File(mPath);
            if (imageFile.exists())
                imageFile.delete();

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            return mPath;

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public String getCapabilities() {
        return "";
    }

    @Override
    public void setOnOffDate(Context context, Date onDate, Date offDate) { }

    @Override
    public String getInstallCommand(String pkg, String path) {
        return "";
    }

    @Override
    public boolean hasHardwareRotation() { return false; }

    @Override
    public void setOrientation(int orientation) { }

}

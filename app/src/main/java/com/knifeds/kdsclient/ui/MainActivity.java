package com.knifeds.kdsclient.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import com.knifeds.kdsclient.R;
import com.knifeds.kdsclient.data.Config;
import com.knifeds.kdsclient.data.Consts;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.data.Playlist;
import com.knifeds.kdsclient.data.ServerMessage;
import com.knifeds.kdsclient.data.StateChanged;
import com.knifeds.kdsclient.data.UIMessage;
import com.knifeds.kdsclient.hardware.HardwareController;
import com.knifeds.kdsclient.mqtt.MqttManager;
import com.knifeds.kdsclient.mqtt.MqttService;
import com.knifeds.kdsclient.scep.ScepManager;
import com.knifeds.kdsclient.schedule.ScheduleManager;
import com.knifeds.kdsclient.schedule.TimerMessage;
import com.knifeds.kdsclient.upgrade.UpdateInfo;
import com.knifeds.kdsclient.upgrade.UpdateMessage;
import com.knifeds.kdsclient.upgrade.UpdateReceiver;
import com.knifeds.kdsclient.upgrade.UpdateResponse;
import com.knifeds.kdsclient.upgrade.UpgradeManager;
import com.knifeds.kdsclient.utils.Commandline;
import com.knifeds.kdsclient.utils.FileUploader;
import com.knifeds.kdsclient.utils.FileUtil;
import com.knifeds.kdsclient.utils.StatusMessage;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.knifeds.kdsclient.upgrade.UpgradeManager.ACTION_DOWNLOAD;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.ACTION_DOWNLOAD_UPGRADE;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.ACTION_LICENSE_DOWNLOAD;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.ACTION_UPGRADE;
import static com.knifeds.kdsclient.upgrade.UpgradeManager.UPDATE_DOWNLOAD_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_READ_PHONE_STATE = 100;
    private static final int REQUEST_WRITE_EXT_STORAGE = 101;
    private static final int REQUEST_READ_EXT_STORAGE = 102;
    private static final int REQUEST_LOCATION = 103;
    private static final int REQUEST_CAMERA = 104;
    private static final int REQUEST_INSTALL = 105;

    private volatile boolean sendingEnrollMessage = false;
    private volatile boolean shouldSendHeartbeat = false;
    private volatile boolean doUpgrade = false;
    private volatile boolean destroying = false;
    private volatile boolean screenshoting = false;
    private volatile boolean isFirstMqttReadyEvent = true;

    ScheduledExecutorService enrollScheduler = null;
    ScheduledFuture<?> enrollFuture = null;

    WebView webView;
    TextView noLicenseText;
    TextView testModeText;
    String screenShotUploadUrl;

    @Inject
    HardwareController hardwareController;

    @Inject
    ScepManager scepManager;

    @Inject
    DataContext dataContext;

    @Inject
    ScheduleManager scheduleManager;

    @Inject
    MqttManager mqttManager;

    @Inject
    WebViewHelper webViewHelper;

    @Inject
    UpgradeManager upgradeManager;

    private UpdateReceiver updateReceiver = null;
    private BroadcastReceiver configReceiver = null;
    private EventReceiver eventReceiver = null;

    private static final int LOCATION_REFRESH_TIME = 5000;
    private static final int LOCATION_REFRESH_DISTANCE = 10;
    private final LocationListener mLocationListener;

    {
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                dataContext.setLocation(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        Config.loadConfig(this);

        dataContext.init(PreferenceManager.getDefaultSharedPreferences(this), getSharedPreferences("app_config", Context.MODE_PRIVATE), getFilesDir());
        dataContext.capabilities = hardwareController.getCapabilities();

//        dataContext.resetDeviceStateHash(); // Reset device state so that PaaS will always re-push the commands.

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.kadsp.dstest.SetConfig");
//        filter.addAction("com.kadsp.dstest.SendCommand");

        configReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.kadsp.dstest.SetConfig")) {
                    // Logic: 1st time: Config.envText always empty. if incoming text is not empty, switch env.
                    Log.d(TAG, "onReceive: Got config from DsTest.");
                    Config.envText = intent.getStringExtra("env");
                    Config.mqttUrl = intent.getStringExtra("mqttUrl");
                    Config.scepServerUrl = intent.getStringExtra("scepServerUrl");
                    Config.screenShotPrefix = intent.getStringExtra("screenShotPrefix");
                    Config.hostedFileBaseUrl = intent.getStringExtra("hostedFileBaseUrl");
                    Config.changeWwwToStaging = intent.getBooleanExtra("changeWwwToStaging", false);
//                    dataContext.setEnv(Config.envText);

                    if (Config.envText.length() > 0) {
                        testModeText.setText(Config.envText);
                        testModeText.setVisibility(View.VISIBLE);
                    } else {
                        testModeText.setVisibility(View.GONE);
                    }

                    proceedWithConfig();
//                } else if (intent.getAction().equals("com.kadsp.dstest.SendCommand")) {
//                    exitApp();
                }
            }
        };

        registerReceiver(configReceiver, filter);

        try {
            UpdateInfo.localVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
            UpdateInfo.localVersionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            Log.d(TAG, "onCreate: localVersion=" + UpdateInfo.localVersion + "; localVersionName=" + UpdateInfo.localVersionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
        }

        // setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        doCreate(savedInstanceState);

//        if (false && !NetworkUtil.isConnected(this)) {  // Disable network check
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("No Internet Connection");
//            builder.setMessage("You need to connect to the Internet.");
//
//            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//            });
//
//            builder.show();
//        } else {
//            doCreate(savedInstanceState);
//        }
    }

    private void exitApp() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else if (Build.VERSION.SDK_INT >= 16) {
            finishAffinity();
        } else {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    private boolean checkPermissions() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            setStatusText("Waiting for user to grant permission.");
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION);
            return false;
        }

        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            setStatusText("Waiting for user to grant permission.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
            return false;
        }

        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            setStatusText("Waiting for user to grant permission.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXT_STORAGE);
            return false;
        }

        if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            setStatusText("Waiting for user to grant permission.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXT_STORAGE);
            return false;
        }

        if (!hasPermission(Manifest.permission.CAMERA)) {
            setStatusText("Waiting for user to grant permission.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return false;
        }

//        if (!hasPermission(Manifest.permission.INSTALL_PACKAGES)) {
//            setStatusText("Waiting for user to grant permission.");
//            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.INSTALL_PACKAGES }, REQUEST_INSTALL);
//            return false;
//        }


        return true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            int i = 0;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            int i = 0;
        }
    };

    private boolean isDsTestInstalled() {
//        boolean testMode = false;
        boolean isInstalled = false;
        try {
            Intent i = new Intent("com.kadsp.dstest.START_CONFIG_SERVICE");
            i.setPackage("com.kadsp.dstest");
            ComponentName c = startService(i);
            if (c != null) {
//                testMode = true;
                isInstalled = true;
//                Logger.d("In Test Mode.");
            }
        } catch (Exception e) {
            // e.printStackTrace();
        } finally {
//            dataContext.setEnv(testMode ? "test" : "prod");
//            if (testMode) {
//                testModeText.setVisibility(View.VISIBLE);
//            } else {
//                testModeText.setVisibility(View.GONE);
//            }
        }
        return isInstalled;
    }

    private void goFullScreen() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        View contentView = findViewById(R.id.fullscreen_content);
        contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void doCreate(Bundle savedInstanceState) {
        if(savedInstanceState == null){
            // everything else that doesn't update UI
            Log.d(TAG, "1st onCreate.");
        }else{
            Log.d(TAG, "2+ onCreate.");
        }

        // Start UI related creation
        setContentView(R.layout.activity_main);

        changeDeviceAngle(dataContext.getDeviceAngle());

        goFullScreen();

//        if( getIntent().getBooleanExtra("Exit me", false)){
//            finish();
//            return; // add this to prevent from doing unnecessary stuffs
//        }

        noLicenseText = findViewById(R.id.text_no_license);
        testModeText = findViewById(R.id.text_test_mode);

        webView = findViewById(R.id.webView1);
        webViewHelper.init(webView);

        EventBus.getDefault().register(this);

        scheduleManager.executeCurrentSchedule(); // Start playing now.

        showPinText(false); // Disable pin text at this stage

        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        dataContext.setResolution(width, height);

        if (checkPermissions()) {
            proceedWithPermissions();
        }
    }

    /*
    private void loadPluginFragment() {
        if (!Config.loadPlugins)
            return;

        final String PLUGIN_PACKAGE_NAME = "com.knifeds.arcsoftplugin";
        final String PLUGIN_FRAGMENT_NAME = "CameraPreviewFragment";

        try {
//            Context ctx = createPackageContext("com.knifeds.plugina", Context.CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
//            ClassLoader cl = ctx.getClassLoader();
//            Class<?> c = cl.loadClass("com.knifeds.plugina.CameraFragment");
//            Fragment fragObj = (Fragment)c.newInstance();

            Class<?> requiredClass = null;
            final String apkPath = getPackageManager().getApplicationInfo(PLUGIN_PACKAGE_NAME,0).sourceDir;
            final File dexTemp = getDir("temp_folder", 0);
            final String fullName = PLUGIN_PACKAGE_NAME + "." + PLUGIN_FRAGMENT_NAME;
            boolean isLoaded = true;

            // Check if class loaded
            try {
                requiredClass = Class.forName(fullName);
            } catch(ClassNotFoundException e) {
                isLoaded = false;
            }

            if (!isLoaded) {
                final DexClassLoader classLoader = new DexClassLoader(apkPath,
                        dexTemp.getAbsolutePath(),
                        null,
                        getApplicationContext().getClassLoader());

                requiredClass = classLoader.loadClass(fullName);
            }

            if (null != requiredClass) {
                // Try to cast to required interface to ensure that it's can be cast
                final FragmentProvider provider = FragmentProvider.class.cast(requiredClass.newInstance());

                if (null != provider) {
                    final Fragment fragment = provider.getFragment();

                    if (null != fragment) {
                        Log.d(TAG, "onCreate: ");
                        final FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

                        trans.add(R.id.plugin_fragment, fragment, PLUGIN_FRAGMENT_NAME).commit();
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }*/

    private void proceedWithPermissions() {
        if (isDsTestInstalled()) {
            Log.d(TAG, "proceedWithPermissions: Waiting for broadcast of 'com.kadsp.dstest.SetConfig'");
        } else {
            proceedWithConfig();
        }
    }

    private void startExtraActivities() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.knifeds.arcsoftplugin", "com.knifeds.arcsoftplugin.ArcSoftPluginActivity");
            startActivity(intent);
        } catch (Exception e) {

        }
    }

    private void proceedWithConfig() {
        goFullScreen();

        setupLocationManager();

//        loadPluginFragment();

        if (dataContext.hasSavedDeviceUuid()) {
            startClient();
        } else {
            getUuidAndStartClient();
        }

        registerBroadcastReceivers();

        executePendingClientUpgrade();

        startExtraActivities();
    }

    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        public void run() {
            Log.i("", "Long press!");
            EventBus.getDefault().post(new UIMessage());
        }
    };

    // https://stackoverflow.com/questions/32050784/chromium-webview-does-not-seems-to-work-with-android-applyoverrideconfiguration
    // https://bugs.chromium.org/p/chromium/issues/detail?id=521753#c8
    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        long dt = event.getDownTime();
        long et = event.getEventTime();
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            handler.postDelayed(mLongPressed, 5000);
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            long gap = et-dt;
            Log.i("", "Moving...");
            if (gap > 100) {
                // handler.removeCallbacks(mLongPressed);
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.removeCallbacks(mLongPressed);
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed(){
        // Do nothing so that back button won't switch the app to background
    }

    @SuppressLint("MissingPermission")
    private void setupLocationManager() {
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, mLocationListener);
        } catch (Exception e) {
            Log.e("Error getting location", e.getMessage());
        }
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));
    }

    private void getUuidAndStartClient(){
        try{
            TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            String deviceUuid = tManager.getDeviceId();
            if (deviceUuid == null){
                deviceUuid = Build.SERIAL;
                if (deviceUuid.equals("unknown") || deviceUuid.length() < 10) {
                    Log.d(TAG, "getUuidAndStartClient: First time deviceUuid=" + deviceUuid);
                    deviceUuid = UUID.randomUUID().toString();
                }
            }
            dataContext.setDeviceUuid(deviceUuid);
        }catch(SecurityException ex){
            dataContext.setDeviceUuid(UUID.randomUUID().toString());
        }

        startClient();
    }

    private void takeScreenshot() {
        if (screenshoting) {
            Log.d(TAG, "takeScreenshot: Busy... Screenshot Ignored.");
            return;
        }
        screenshoting = true;

        final String screenshotFile = hardwareController.takeScreenshot(this);
        if (screenshotFile.length() > 0) {
            uploadScreenShot(screenShotUploadUrl, screenshotFile);
        } else {
            screenshoting = false;
        }
    }

    private void startClient(){
        setStatusText("Starting client...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!destroying) {
                    initClient();
                }
            }
        }, 1000);
    }

    private void changeDeviceAngle(final String angle) {
        if (angle.equals("0")) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (angle.equals("90")) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (angle.equals("180")) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (angle.equals("270")) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            // FIXME: Doesn't work on some devices.
//            int o = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//            o |= ActivityInfo.SCREEN_ORIENTATION_LOCKED;
//            setRequestedOrientation (o);
        }
    }

    private void setOrientation(int orientation) {
        if (hardwareController.hasHardwareRotation()) {
            hardwareController.setOrientation(orientation);
        } else {
            setRequestedOrientation(orientation);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            boolean isRelatedPermission = false;
            switch (requestCode) {
                case REQUEST_READ_PHONE_STATE:
                    isRelatedPermission = true;
                    break;
                case REQUEST_WRITE_EXT_STORAGE:
                    isRelatedPermission = true;
                    break;
                case REQUEST_READ_EXT_STORAGE:
                    isRelatedPermission = true;
                    break;
                case REQUEST_LOCATION:
                    isRelatedPermission = true;
                    break;
                case REQUEST_CAMERA:
                    isRelatedPermission = true;
                    break;
                case REQUEST_INSTALL:
                    isRelatedPermission = true;
                    break;
                default:
                    break;
            }
            if (isRelatedPermission && checkPermissions()) {
                proceedWithPermissions();
            }
        }
    }

    // Main dispatcher for Mqtt message
    @Subscribe
    public void onEvent(MqttMessage message) {
        ServerMessage serverMessage = new ServerMessage(dataContext).build(message);

        switch (serverMessage.result) {
            case None:
                break;
            case EnrollSucceeded:
                sendingEnrollMessage = false;   // Stop sending enroll message
                Log.d(TAG, "onEvent: Activation succeeded.");
                setStatusText("Connected to server, waiting for command...");

                dataContext.setActivated(true);
                showPinText(false);
                showStatusText(false);

                startSendingHeartbeat();
                break;
            case SchedulePlaylistReceived:
                Log.d(TAG, "onEvent: Parsing Schedule");
                scheduleManager.executeUpdatedSchedule(serverMessage.schedule);
                setStatusText("Playlist received, adding to schedule...");
                break;
            case ScreenShotReceived:
                Log.d(TAG, "onEvent: Device ScreenShot");
                String token = serverMessage.token;
                screenShotUploadUrl = dataContext.getScreenShotPrefix() + token + "/" + dataContext.getDeviceUuid();
                takeScreenshot();
                break;
            case ClientDownloadReceived:
                Log.d(TAG, "onEvent: Client Download");
                setStatusText("Downloading new client...");
                upgradeManager.handleClientDownload(serverMessage);
                break;
            case ClientUpgradeReceived:
                Log.d(TAG, "onEvent: Client Upgrade");
                setStatusText("Upgrading client...");
                upgradeManager.handleClientUpgrade(serverMessage);
                break;
            case LicenseInstallReceived:
                Log.d(TAG, "onEvent: license Verify");
                setStatusText("Verifying license...");
                upgradeManager.setLicenseCommand(serverMessage);
                timerAction(ACTION_LICENSE_DOWNLOAD);
                break;
            case ClientDownloadSucceeded:
                // TODO: post action after client apk downloaded
                Log.d(TAG, "onEvent: Upgrade APK has been downloaded");
                if (doUpgrade) {
                    installApk();
                    doUpgrade = false;
                }
                break;
            case RestartReceived:
                hardwareController.reboot(this);
                break;
            case ShutdownReceived:
                hardwareController.shutdown(this);
                break;
            case UnloadPlaylistReceived:
                // No need to do this now.
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.clearCache(true);
//                        webView.loadUrl("about:blank");
//                    }
//                });
                break;
            case ClearCacheReceived:
                clearCache();
                break;
            case DeviceControlReceived:
                dataContext.setDeviceAngle(serverMessage.angle);
                changeDeviceAngle(serverMessage.angle);
                break;
            case SetOnOffReceived:
                hardwareController.setOnOffDate(this, serverMessage.onDate, serverMessage.offDate);
                break;
            default:
                break;
        }
    }

    // For those event that requires Scheduling, here is where they get dispatched
    @Subscribe
    public void onEvent(TimerMessage message) {
        switch (message.what) {
            case Consts.TIMER_SCHEDULER:
                Playlist currentPlaylist = (Playlist) message.obj;
                dataContext.setCurrentPlaylist(currentPlaylist);
                String indexHtmlPath = currentPlaylist.getIndexHtmlPath();

                Log.d(TAG, "onEvent: Showing content at: " + indexHtmlPath);
                webViewHelper.setWebUrl(indexHtmlPath);

                scheduleManager.provisionNextPlaylist();
                sendOutOfBandHeartbeat();
                break;
            case Consts.TIMER_DOWNLOAD_APK: case Consts.TIMER_UPGRADE_APK:
                String actionName = (String)message.obj;
                timerAction(actionName);
                break;
            default:
                break;
        }
    }

    public void timerAction(final String actionName){
        // Assume we already have all the permissions
        if (actionName.equals(ACTION_UPGRADE)) {
            installApk();
        } else if(actionName.equals(ACTION_DOWNLOAD_UPGRADE)) {
            doUpgrade = true;
            // checkUpdate
            sendBroadcast(new Intent(UpdateReceiver.UPDATE_ACTION));
        } else if(actionName.equals(ACTION_DOWNLOAD)) {
            // checkUpdate
            sendBroadcast(new Intent(UpdateReceiver.UPDATE_ACTION));
        } else if(actionName.equals(ACTION_LICENSE_DOWNLOAD)) {
            upgradeManager.licenseDownload();
        }
    }

    // Handle messages from UpdateService
    @Subscribe
    public void onEvent(UpdateMessage message) {
        switch (message.getUpdateResult()) {
            case Consts.DOWNLOAD_COMPLETE:
                if (message.isDownloadOnly()) {
                    upgradeManager.sendClientDownloadResult();
                } else {
                    upgradeManager.scheduleClientUpgrade(message.getUpgradeTime(), ACTION_UPGRADE);
                }
                break;
            case Consts.DOWNLOAD_NOMEMORY:
            case Consts.DOWNLOAD_FAIL:
                break;
        }
    }

    @Subscribe
    public void onEvent(StatusMessage message) {

        if (message.message.length() == 0) {
            this.showStatusText(false);
        } else {
            this.setStatusText(message.message);
        }
    }

    @Subscribe
    public void onEvent(UIMessage message) {
        final MainActivity self = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(self);
                builder.setTitle("Switch env");
                builder.setMessage("Current env is '" + dataContext.getEnv() + "'. Do you want to switch env?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dataContext.flipEnv();
//                        Intent intent = new Intent(self, MainActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        intent.putExtra("Exit me", true);
//                        startActivity(intent);
//                        //finish();
                        self.finish();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();
            }
        });
    }

    @Subscribe
    public void onEvent(StateChanged stateChanged) {
        Log.d(TAG, "onEvent: State Changed to: " + stateChanged.state);
        switch(stateChanged.state) {
            case None:
                break;
            case PrerequisiteReady:
                if (!destroying) {
                    initMqtt();
                }
                break;
            case MqttServiceReady:
                if (isFirstMqttReadyEvent) {
                    isFirstMqttReadyEvent = false;
                    showStatusText(false);
                    doEnroll();
                } else {
                    Log.d(TAG, "onEvent: Ignoring further MqttServiceReady events...");
                }
                break;
            case MqttConnectionLost:
                Log.d(TAG, "onEvent: Trying to restart MqttService...");
                stopService(new Intent(this, MqttService.class));
                // Restart the Service to trigger the connection loop
                startService(new Intent(this, MqttService.class));
                break;
            case ContentReady:
                showStatusText(false);
                break;
            case LicenseOk:
                noLicenseText.setVisibility(View.GONE);
                break;
            case LicenseError:
                noLicenseText.setVisibility(View.VISIBLE);
                break;
            case ConditionChanged:
                scheduleManager.executeWithCondition(stateChanged.condition);
                Log.i(TAG, "Trigger the [" + stateChanged.condition+ "] playlist");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

//        webViewHelper.pause();
    }

    @Override
    protected void onDestroy() {
        destroying = true;

        if (webView != null) {
            webViewHelper.destroy();
            webView = null;
        }

        if (configReceiver != null) {
            unregisterReceiver(configReceiver);
            configReceiver = null;
        }

        // Disconnect when quit
        new Thread(new Runnable() {
            @Override
            public void run() {
//                try {
                sendingEnrollMessage = false;
                shouldSendHeartbeat = false;
                if (enrollFuture != null) {
                    enrollFuture.cancel(true);
                }
                if (enrollScheduler != null) {
                    enrollScheduler.shutdown();
                }
                //MqttManager.getInstance().disConnect();
                //Logger.d("isConnected: " + false);
//                }
//                catch (MqttException e) {
//                    Logger.d("Unable to disconnect: " + e.getMessage());
//                }
            }
        }).start();

        EventBus.getDefault().unregister(this);
        unRegisterBroadcastReceivers();

        try {
            if (enrollScheduler != null) {
                enrollScheduler.awaitTermination(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Failed to terminate enrollScheduler!");
        }

        super.onDestroy();
    }

    private void initClient() {
        if (Config.useScep) {
            setStatusText("Checking required files...");
            scepManager.doScep();
        } else {
            try {
                FileUtil.getFileFromAssets(this, getFilesDir().toString(), "mqtt_ca.crt");
                FileUtil.getFileFromAssets(this, getFilesDir().toString(), "mqtt_client.crt");
                FileUtil.getFileFromAssets(this, getFilesDir().toString(), "mqtt_client.key");
                if (!destroying) {
                    initMqtt();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void initMqtt() {
        try {
            String mqttUrl = dataContext.getMqttUrl();

            // Logger.d("Connecting...");
            setStatusText("Connecting...");

            Intent i= new Intent(this, MqttService.class);
            i.putExtra("mqttUrl", mqttUrl);
            this.startService(i);
        } catch(Exception ex) {
            Log.d(TAG, "initMqtt: " + ex.getMessage());
            setStatusText("Connection error! Code=" + dataContext.getDeviceUuid());
        }
    }

    private void doEnroll() {
        if (destroying)
            return;

        sendingEnrollMessage = true;

        enrollScheduler = Executors.newSingleThreadScheduledExecutor();
        enrollFuture = enrollScheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!destroying && sendingEnrollMessage) {
                            final int random = new Random().nextInt(9999999);
                            String pin = String.format("%07d", random);
                            setPinText("Pin: " + pin);

                            String enrollMessage = ServerMessage.assembleEnrollMessage(dataContext, pin);
                            boolean b = mqttManager.publish(Config.enrollTopic, 2, enrollMessage.getBytes());
                            if (b) {
                                showPinText(true);
                                Log.d(TAG, "run: publish to enrollTopic: " + enrollMessage);
                            }
                        } else {
                            enrollScheduler.shutdown();
                        }
                    }
                }, 0, 5, TimeUnit.MINUTES);
    }

    private void setStatusText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView statusTextView = (TextView) findViewById(R.id.text_view_status);
                statusTextView.setVisibility(View.VISIBLE);
                statusTextView.setText(text);
            }
        });
    }

    private void setPinText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView pinTextView = (TextView) findViewById(R.id.text_view_pin);
                pinTextView.setText(text);
            }
        });
    }

    private void showPinText(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView pinTextView = (TextView) findViewById(R.id.text_view_pin);
                pinTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showStatusText(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = (TextView) findViewById(R.id.text_view_status);
                textView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void startSendingHeartbeat() {
        shouldSendHeartbeat = true;
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (shouldSendHeartbeat) {
                    sendHeartbeat();
                } else {
                    scheduler.shutdown();
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void sendHeartbeat() {
        String heartbeatMessage = ServerMessage.assembleHeartbeatMessage(dataContext);
        boolean b = mqttManager.publish(Config.requestTopicPrefix + dataContext.getDeviceUuid(), 2, heartbeatMessage.getBytes());
        Log.d(TAG, "sendHeartbeat: sending heartbeat to server: " + b);
        Log.i("dclient", heartbeatMessage);
    }

    private void sendOutOfBandHeartbeat() {
        if (shouldSendHeartbeat) {
            sendHeartbeat();
        }
    }

    public void uploadScreenShot(final String uploadUrl, final String localID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(localID);
                if (file.exists()) {
                    new FileUploader().uploadFile(file, uploadUrl);
                } else {
                    Log.e(TAG, "run: Screenshot not found!");
                }
                screenshoting = false;
                showStatusText(false);
            }}).start();
    }

    private void clearCache() {
        // TODO: delete all cached playlist content from the local storage.
    }

    private void executePendingClientUpgrade(){
        try {
            PackageManager pm = this.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;

            if (dataContext.hasPrivateConfig(UPDATE_DOWNLOAD_KEY)) {
                UpdateResponse updateResponse = dataContext.getUpdateResponse(UPDATE_DOWNLOAD_KEY);
                if(versionName.equals(updateResponse.getVersion())){
                    upgradeManager.sendClientUpgradResult();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void installApk() {
        File file = dataContext.getUpdateFile();
        if (file == null) {
            Log.e(TAG, "installApk: UpdateFile not found!");
            return;
        }

        String silentInstallCommand = hardwareController.getInstallCommand("com.knifeds.kdsclient", file.getPath());
        if (silentInstallCommand.length() > 0) {
            // TODO: Run the command!
            try{
                Commandline commandline = new Commandline();
                commandline.command = silentInstallCommand;
                commandline.run2();
            }catch(Exception e){
                Log.d(TAG, "onEvent: " + e.getMessage());
            }
        } else {

//        File updateDir = new File(Environment.getExternalStorageDirectory()+"/kdsclient","app-debug.apk");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= 24) {
                Uri apkUri = FileProvider.getUriForFile(this, "com.knifeds.kdsclient.fileprovider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }

            this.startActivity(intent);
        }
    }

    private void registerBroadcastReceivers() {
        updateReceiver = new UpdateReceiver(false);
        IntentFilter mIntentFilter = new IntentFilter(UpdateReceiver.UPDATE_ACTION);
        registerReceiver(updateReceiver, mIntentFilter);

        eventReceiver = new EventReceiver();
        IntentFilter eventFilter = new IntentFilter(EventReceiver.ACTION);
        registerReceiver(eventReceiver, eventFilter);
    }

    private void unRegisterBroadcastReceivers() {
        try {
            if (updateReceiver != null) {
                unregisterReceiver(updateReceiver);
                updateReceiver = null;
            }
            if (eventReceiver != null) {
                unregisterReceiver(eventReceiver);
                eventReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

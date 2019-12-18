package com.knifeds.kdsclient.ui;

import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WebViewHelper {
    private WebView webView = null;

    @Inject
    public WebViewHelper() {}

    public void init(WebView webView) {
        this.webView = webView;

        WebSettings webSetting = webView.getSettings();
        webSetting.setBuiltInZoomControls(true);
        webSetting.setJavaScriptEnabled(true);

        // This setting requires (Build.VERSION.SDK_INT >= 17)
        webSetting.setMediaPlaybackRequiresUserGesture(false);

        webSetting.setAllowFileAccess(true);
        webSetting.setAllowContentAccess(true);

        // These 2 settings require (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        webSetting.setAllowFileAccessFromFileURLs(true);
        webSetting.setAllowUniversalAccessFromFileURLs(true);

        // Other settings that might be useful
//        webSetting.setPluginsEnabled(true);
//        webSetting.setPluginState(PluginState.ON);

//        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
//        s.setUseWideViewPort(true);
//        s.setLoadWithOverviewMode(true);
//        s.setSavePassword(true);
//        s.setSaveFormData(true)
//        s.setGeolocationEnabled(true);
//        s.setGeolocationDatabasePath("/data/data/" + getPackageName() +  "/geoloc/");
//        s.setDomStorageEnabled(true);
//        s.setPluginState(WebSettings.PluginState.ON);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Disable touch
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    public void pause() {
        // https://stackoverflow.com/questions/5946698/how-to-stop-youtube-video-playing-in-android-webview
        try {
            Class.forName("android.webkit.WebView")
                    .getMethod("onPause", (Class[]) null)
                    .invoke(webView, (Object[]) null);

        } catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch(NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch(InvocationTargetException ite) {
            ite.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
    }

    public void destroy() {
        // Make sure you remove the WebView from its parent view before doing anything.
//        mWebContainer.removeAllViews();

        webView.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        webView.clearCache(true);

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        webView.loadUrl("about:blank");

        webView.onPause();
        webView.removeAllViews();
        webView.destroyDrawingCache();

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call mWebView.resumeTimers().
        webView.pauseTimers();

        webView.destroy();

//        webView.setWebViewClient(null);
//        webView.setWebChromeClient(null);
        webView = null;
    }

    public void setWebUrl(final String url) {
        webView.clearCache(true);
        webView.resumeTimers();
        webView.loadUrl(url);
    }

    private class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

}


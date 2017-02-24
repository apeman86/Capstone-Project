package com.nalbandian.michael.smartteleprompter;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.nalbandian.michael.smartteleprompter.R;

/**
 * Created by nalbandianm on 2/28/2017.
 */

public class SmartTeleprompterApplication extends Application {
    private Tracker mTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.track_app);
            analytics.enableAutoActivityReports(this);
        }
        return mTracker;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CastConfiguration options = new CastConfiguration.Builder("A9726CFF")
                .enableAutoReconnect()
                .enableDebug()
                .enableLockScreen()
                .enableWifiReconnection()
                .enableNotification()
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE,true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT,true)
                .build();
        DataCastManager.initialize(this, options);
    }
}

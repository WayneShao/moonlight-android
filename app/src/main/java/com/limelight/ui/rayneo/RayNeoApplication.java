package com.limelight.ui.rayneo;

import android.app.Application;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public final class RayNeoApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT < 29 || !RayNeoDevice.enabled(this)) return;
        RayNeoTrace.initialize(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("rayneo_preset_720p30_v1", false)) {
            // Requested one-time preset; subsequent choices remain authoritative.
            prefs.edit().remove("list_resolution_fps")
                    .putString("list_resolution", "1280x720").putString("list_fps", "30")
                    .putInt("seekbar_bitrate_kbps", 5000)
                    .putBoolean("checkbox_stretch_video", false)
                    .putBoolean("checkbox_enable_hdr", false)
                    .putBoolean("checkbox_enable_pip", false)
                    .putBoolean("checkbox_show_onscreen_controls", false)
                    .putBoolean("rayneo_preset_720p30_v1", true).apply();
        }
        registerActivityLifecycleCallbacks(new RayNeoWindows());
    }
}

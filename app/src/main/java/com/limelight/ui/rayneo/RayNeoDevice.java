package com.limelight.ui.rayneo;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.InputDevice;
import android.view.WindowManager;
import java.util.Locale;

public final class RayNeoDevice {
    private RayNeoDevice() {}
    @SuppressWarnings("deprecation")
    public static boolean enabled(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        return RayNeoPolicy.supports(Build.MANUFACTURER, Build.MODEL, Build.DEVICE,
                Build.VERSION.SDK_INT, size.x, size.y);
    }
    public static boolean temple(InputDevice device) {
        if (device == null || device.getName() == null) return false;
        String name = device.getName().toLowerCase(Locale.ROOT);
        return name.contains("cyttsp") || name.contains("capsense");
    }
}

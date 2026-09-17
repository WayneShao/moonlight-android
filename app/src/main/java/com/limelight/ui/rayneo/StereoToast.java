package com.limelight.ui.rayneo;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;

/** Preserves call sites and normal-device Toast behavior; stereo messages use active app UI. */
public final class StereoToast {
    public static final int LENGTH_SHORT=Toast.LENGTH_SHORT, LENGTH_LONG=Toast.LENGTH_LONG;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static WeakReference<TextView> current=new WeakReference<>(null);
    private final Context context;
    private final CharSequence text;
    private final int duration;
    private StereoToast(Context c,CharSequence t,int d) {context=c;text=t;duration=d;}
    public static StereoToast makeText(Context c,CharSequence t,int d) {return new StereoToast(c,t,d);}
    public static StereoToast makeText(Context c,int id,int d) {return makeText(c,c.getText(id),d);}
    public void show() { MAIN.post(()->{
        if(Build.VERSION.SDK_INT>=29 && RayNeoDevice.enabled(context)) {
            for(View root:WindowInspector.getGlobalWindowViews()) if(root.hasWindowFocus()) {
                StereoHost host=RayNeoWindows.findHost(root);
                if(host==null) continue;
                TextView old=current.get();
                if(old!=null && old.getParent() instanceof FrameLayout) ((FrameLayout)old.getParent()).removeView(old);
                TextView message=new TextView(host.getContext());
                message.setText(text); message.setTextColor(0xffffffff); message.setTextSize(14);
                message.setBackgroundColor(0xe6222222); message.setPadding(12,8,12,8);
                message.setGravity(Gravity.CENTER); message.setFocusable(false); message.setClickable(false);
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
                lp.bottomMargin=20; lp.leftMargin=12; lp.rightMargin=12;
                host.content.addView(message,lp); current=new WeakReference<>(message);
                MAIN.postDelayed(()->{if(message.getParent()==host.content) host.content.removeView(message);},duration==LENGTH_LONG?3500:2000);
                return;
            }
        }
        Toast.makeText(context,text,duration).show();
    }); }
}

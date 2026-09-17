package com.limelight.ui.rayneo;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.FrameLayout;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

/** Public API process-window discovery, including framework-created independent dialogs. */
@android.annotation.TargetApi(29)
public final class RayNeoWindows implements Application.ActivityLifecycleCallbacks {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<Activity> resumed=Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<View> installed=Collections.newSetFromMap(new WeakHashMap<>());
    private final WeakHashMap<View,WeakReference<StereoHost>> independent=new WeakHashMap<>();
    private final WeakHashMap<Activity,ViewTreeObserver.OnPreDrawListener> observers=new WeakHashMap<>();
    private boolean scanning;
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if (resumed.isEmpty()) return;
            scan(); handler.postDelayed(this,250);
        }
    };
    public static StereoHost findHost(View root) {
        if (root instanceof StereoHost) return (StereoHost)root;
        if (root instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++) {
                StereoHost host=findHost(group.getChildAt(i));
                if(host!=null) return host;
            }
        }
        return null;
    }
    private void scan() {
        if(scanning || resumed.isEmpty()) return;
        scanning=true;
        try {
            for(View root:WindowInspector.getGlobalWindowViews()) {
                if (!(root instanceof ViewGroup) || !root.isAttachedToWindow()
                        || !(root.getLayoutParams() instanceof WindowManager.LayoutParams)) continue;
                if(installed.contains(root)) {
                    WeakReference<StereoHost> ref=independent.get(root);
                    StereoHost host=ref==null?null:ref.get();
                    if(host!=null) retainGeometry(root,host);
                    continue;
                }
                if(findHost(root)!=null) { installed.add(root); continue; }
                WindowManager.LayoutParams params=(WindowManager.LayoutParams)root.getLayoutParams();
                if(params.type<WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                        || params.type>WindowManager.LayoutParams.LAST_SUB_WINDOW) continue;
                boolean activity=false;
                for(Activity a:resumed) if(a.getWindow().getDecorView()==root) {activity=true;break;}
                if(((ViewGroup)root).getChildCount()==0 || root.getWidth()==0 || root.getHeight()==0) continue;
                install((ViewGroup)root,params,activity);
                installed.add(root);
            }
        } finally { scanning=false; }
    }
    private void retainGeometry(View root,StereoHost host) {
        WindowManager.LayoutParams params=(WindowManager.LayoutParams)root.getLayoutParams();
        if(params.width==1280 && params.height==480 && params.x==0 && params.y==0) return;
        FrameLayout.LayoutParams panel=(FrameLayout.LayoutParams)host.modalPanel.getLayoutParams();
        if(params.width>0 && params.width!=1280) panel.width=Math.min(640,params.width);
        if(params.type>=WindowManager.LayoutParams.FIRST_SUB_WINDOW) {
            panel.leftMargin=Math.max(0,Math.min(Math.floorMod(params.x,640),640-panel.width));
            panel.topMargin=Math.max(0,Math.min(params.y,480-host.modalPanel.getHeight()));
        }
        host.modalPanel.setLayoutParams(panel);
        params.width=1280; params.height=480; params.x=0; params.y=0; params.gravity=Gravity.TOP|Gravity.LEFT;
        ((WindowManager)root.getContext().getSystemService(android.content.Context.WINDOW_SERVICE)).updateViewLayout(root,params);
    }
    private void install(ViewGroup root,WindowManager.LayoutParams params,boolean activity) {
        final StereoHost host=new StereoHost(root.getContext(),false);
        FrameLayout panel=new FrameLayout(root.getContext());
        int originalWidth=root.getWidth(), originalHeight=root.getHeight();
        int[] location=new int[2]; root.getLocationOnScreen(location);
        Drawable background=root.getBackground();
        panel.setBackground(background);
        panel.setPadding(root.getPaddingLeft(),root.getPaddingTop(),root.getPaddingRight(),root.getPaddingBottom());
        root.setPadding(0,0,0,0);
        root.setBackgroundColor(Color.TRANSPARENT);
        while(root.getChildCount()>0) {
            View child=root.getChildAt(0);
            ViewGroup.LayoutParams lp=child.getLayoutParams();
            root.removeView(child);
            panel.addView(child,lp);
        }
        FrameLayout.LayoutParams panelParams;
        if(activity) {
            panelParams=new FrameLayout.LayoutParams(-1,-1);
        } else {
            panelParams=new FrameLayout.LayoutParams(Math.min(640,originalWidth),FrameLayout.LayoutParams.WRAP_CONTENT);
            if(params.type>=WindowManager.LayoutParams.FIRST_SUB_WINDOW) {
                panelParams.gravity=Gravity.TOP|Gravity.LEFT;
                panelParams.leftMargin=Math.min(Math.floorMod(location[0],640),640-panelParams.width);
                panelParams.topMargin=Math.max(0,Math.min(location[1],480-Math.min(480,originalHeight)));
            } else panelParams.gravity=Gravity.CENTER;
        }
        host.content.addView(panel,panelParams);
        if(!activity) {
            host.modalPanel=panel;
            host.outsideAllowed=params.type<WindowManager.LayoutParams.FIRST_SUB_WINDOW
                    || (params.flags&WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)!=0;
        }
        root.addView(host,new ViewGroup.LayoutParams(-1,-1));
        if(!activity) {
            independent.put(root,new WeakReference<>(host));
            params.width=1280; params.height=480; params.x=0; params.y=0;
            params.gravity=Gravity.TOP|Gravity.LEFT;
            WindowManager wm=(WindowManager)root.getContext().getSystemService(android.content.Context.WINDOW_SERVICE);
            wm.updateViewLayout(root,params);
        }
        RayNeoTrace.i("RayNeo","window.install type="+params.type+" root="+root.getClass().getSimpleName()
                +" original="+originalWidth+"x"+originalHeight+" ui=software-single-eye");
    }
    @Override public void onActivityResumed(Activity a) {
        resumed.add(a);
        ViewTreeObserver.OnPreDrawListener observer=()->{ scan(); return true; };
        observers.put(a,observer);
        a.getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(observer);
        handler.removeCallbacks(tick); handler.post(tick);
    }
    @Override public void onActivityPaused(Activity a) {
        resumed.remove(a);
        ViewTreeObserver.OnPreDrawListener observer=observers.remove(a);
        if(observer!=null) a.getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(observer);
        if(resumed.isEmpty()) handler.removeCallbacks(tick);
    }
    @Override public void onActivityCreated(Activity a,Bundle b) {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        RayNeoTrace.i("RayNeo","activity.created "+a.getClass().getSimpleName());
    }
    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) { onActivityPaused(a); }
}

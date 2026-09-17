package com.limelight.ui.rayneo;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Small process-local rotating journal. Sync initialization boundaries, not video frames. */
public final class RayNeoTrace {
    private static volatile File journal;
    private RayNeoTrace() {}
    public static boolean enabled() { return journal!=null; }
    public static void decoder(String message) { if(enabled()) event("RayNeoDecoder",message,true); }
    public static synchronized void initialize(Context context) {
        if(journal!=null) return;
        File directory=new File(context.getFilesDir(),"rayneo-diagnostics");
        if(!directory.isDirectory() && !directory.mkdirs()) {Log.e("RayNeoTrace","journal directory unavailable");return;}
        journal=new File(directory,"events.jsonl");
        event("RayNeo","process.start version="+com.limelight.BuildConfig.VERSION_NAME
                +" model="+Build.MODEL+" device="+Build.DEVICE+" api="+Build.VERSION.SDK_INT
                +" abi="+Arrays.toString(Build.SUPPORTED_ABIS)+" fingerprint="+Build.FINGERPRINT
                +" bootId="+readLine("/proc/sys/kernel/random/boot_id"),true);
        android.app.ActivityManager manager=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager!=null) event("RayNeo","capabilities gl="+manager.getDeviceConfigurationInfo().getGlEsVersion()
                +" lowRam="+manager.isLowRamDevice()+" memoryClassMB="+manager.getMemoryClass()
                +" cpuCores="+Runtime.getRuntime().availableProcessors()+" hardware="+Build.HARDWARE,true);
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            event("RayNeo","uncaught thread="+thread.getName()+" "+Log.getStackTraceString(error),true);
            if(previous!=null) previous.uncaughtException(thread,error);
        });
    }
    private static String readLine(String path) {
        try(BufferedReader reader=new BufferedReader(new FileReader(path))) {return reader.readLine();}
        catch(Exception e) {return "unavailable:"+e.getClass().getSimpleName();}
    }
    public static void i(String tag,String message) {event(tag,message,true);}
    public static void w(String tag,String message) {event(tag,"WARNING "+message,true);}
    public static void e(String tag,String message,Throwable error) {
        event(tag,"ERROR "+message+(error==null?"":" "+Log.getStackTraceString(error)),true);
    }
    public static synchronized void event(String tag,String message,boolean sync) {
        Log.i(tag,message);
        if(journal==null) return;
        try {
            if(journal.length()>2*1024*1024) {
                File previous=new File(journal.getParentFile(),"previous.jsonl");
                if(previous.exists() && !previous.delete()) throw new java.io.IOException("old journal rotation");
                if(!journal.renameTo(previous)) throw new java.io.IOException("journal rotation");
            }
            JSONObject entry=new JSONObject();
            entry.put("wallMs",System.currentTimeMillis()); entry.put("elapsedMs",SystemClock.elapsedRealtime());
            entry.put("pid",android.os.Process.myPid()); entry.put("thread",Thread.currentThread().getName());
            entry.put("tag",tag); entry.put("event",message);
            try(FileOutputStream out=new FileOutputStream(journal,true)) {
                out.write((entry.toString()+"\n").getBytes(StandardCharsets.UTF_8));
                if(sync) out.getFD().sync();
            }
        } catch(Exception e) {Log.e("RayNeoTrace","journal write failed",e);}
    }
}

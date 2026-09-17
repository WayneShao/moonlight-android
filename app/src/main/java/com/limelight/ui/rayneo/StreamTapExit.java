package com.limelight.ui.rayneo;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Three single-finger taps exit; single/double clicks and multi-touch stay intact. */
final class StreamTapExit {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Consumer<MotionEvent> dispatch;
    private final Consumer<MotionEvent> confirmedClick;
    private final Runnable back;
    private final int timeout,slop,longPress;
    private final ArrayList<MotionEvent> taps=new ArrayList<>();
    private MotionEvent down;
    private boolean passing;
    private final Runnable confirm=this::flush;
    private final Runnable hold=this::beginPassing;
    StreamTapExit(int timeout,int slop,int longPress,Consumer<MotionEvent> dispatch,Runnable back) {
        this(timeout,slop,longPress,dispatch,back,null);
    }
    StreamTapExit(int timeout,int slop,int longPress,Consumer<MotionEvent> dispatch,Runnable back,
                  Consumer<MotionEvent> confirmedClick) {
        this.timeout=timeout;this.slop=slop;this.longPress=longPress;this.dispatch=dispatch;this.back=back;
        this.confirmedClick=confirmedClick;
    }
    boolean touch(MotionEvent event) {
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            handler.removeCallbacks(hold);
            MotionEvent last=taps.isEmpty()?null:taps.get(taps.size()-1);
            boolean continuing=last!=null && event.getEventTime()-last.getEventTime()<=timeout
                    && event.getEventTime()>=last.getEventTime()
                    && event.getDeviceId()==last.getDeviceId() && event.getSource()==last.getSource()
                    && Math.abs(event.getX()-last.getX())<=slop*2
                    && Math.abs(event.getY()-last.getY())<=slop*2;
            if(continuing) handler.removeCallbacks(confirm); else flush();
            if(down!=null) down.recycle();
            down=MotionEvent.obtain(event); passing=false;
            if((event.getButtonState()&~MotionEvent.BUTTON_PRIMARY)!=0
                    || event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS
                    || event.getToolType(0)==MotionEvent.TOOL_TYPE_ERASER) beginPassing();
            else handler.postDelayed(hold,longPress);
        } else if(action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_POINTER_DOWN
                || action==MotionEvent.ACTION_POINTER_UP) {
            if(down==null) return true;
            if(event.getPointerCount()>1 || Math.abs(event.getX()-down.getX())>slop
                    || Math.abs(event.getY()-down.getY())>slop) beginPassing();
            if(passing) dispatch.accept(event);
        } else if(action==MotionEvent.ACTION_UP) {
            handler.removeCallbacks(hold);
            if(down==null) return true;
            if(passing) {dispatch.accept(event);down.recycle();down=null;passing=false;}
            else {
                taps.add(down);down=null;taps.add(MotionEvent.obtain(event));
                if(taps.size()==6) {cancel();back.run();}
                else {handler.removeCallbacks(confirm);handler.postDelayed(confirm,timeout);}
            }
        } else if(action==MotionEvent.ACTION_CANCEL) {
            cancel();
        }
        return true;
    }
    private void flush() {
        handler.removeCallbacks(confirm);
        for(int i=0;i<taps.size();i++) {
            MotionEvent event=taps.get(i);
            if(confirmedClick==null) dispatch.accept(event);
            else if(i%2==1) confirmedClick.accept(event);
            event.recycle();
        }
        taps.clear();
    }
    private void beginPassing() {
        handler.removeCallbacks(hold);
        if(passing || down==null) return;
        flush();dispatch.accept(down);passing=true;
    }
    void cancel() {
        handler.removeCallbacks(confirm);handler.removeCallbacks(hold);
        if(passing && down!=null) {
            MotionEvent.PointerProperties properties=new MotionEvent.PointerProperties();
            MotionEvent.PointerCoords coordinates=new MotionEvent.PointerCoords();
            down.getPointerProperties(0,properties);down.getPointerCoords(0,coordinates);
            MotionEvent cancelled=MotionEvent.obtain(down.getDownTime(),android.os.SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL,1,new MotionEvent.PointerProperties[]{properties},
                    new MotionEvent.PointerCoords[]{coordinates},down.getMetaState(),0,
                    down.getXPrecision(),down.getYPrecision(),down.getDeviceId(),down.getEdgeFlags(),
                    down.getSource(),down.getFlags());
            dispatch.accept(cancelled);cancelled.recycle();
        }
        if(down!=null) {down.recycle();down=null;}
        for(MotionEvent event:taps) event.recycle();
        taps.clear();passing=false;
    }
}

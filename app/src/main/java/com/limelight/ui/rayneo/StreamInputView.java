package com.limelight.ui.rayneo;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.limelight.ui.StreamView;

/** Visible, single-eye input target. Does not own a second video surface. */
public final class StreamInputView extends View {
    private final StreamView.InputCallbacks callbacks;
    public interface MouseOutput {
        void position(int x,int y,int width,int height);
        void click(boolean right);
    }
    private MouseOutput mouse;
    private float cursorX=-1,cursorY=-1,lastX,lastY;
    private boolean cursorVisible,moved;
    private int fingers;
    private final android.util.SparseArray<android.graphics.PointF> starts=new android.util.SparseArray<>();
    private final Paint pointer=new Paint(Paint.ANTI_ALIAS_FLAG);
    public void setMouseOutput(MouseOutput mouse) {this.mouse=mouse;}
    public void templeMotion(MotionEvent event) {
        if(mouse==null || getWidth()==0 || getHeight()==0) return;
        if(cursorX<0) {cursorX=getWidth()/2f;cursorY=getHeight()/2f;}
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
            lastX=event.getX();lastY=event.getY();moved=false;cursorVisible=true;fingers=1;
            starts.clear();starts.put(event.getPointerId(0),new android.graphics.PointF(lastX,lastY));
        } else if(event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN) {
            fingers=Math.max(fingers,event.getPointerCount());
            int i=event.getActionIndex();
            starts.put(event.getPointerId(i),new android.graphics.PointF(event.getX(i),event.getY(i)));
        } else if(event.getActionMasked()==MotionEvent.ACTION_MOVE) {
            if(fingers>1) {
                float slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                for(int i=0;i<event.getPointerCount();i++) {
                    android.graphics.PointF start=starts.get(event.getPointerId(i));
                    if(start!=null) moved |= Math.abs(event.getX(i)-start.x)>slop || Math.abs(event.getY(i)-start.y)>slop;
                }
                return;
            }
            cursorX=Math.max(0,Math.min(getWidth()-1,cursorX+event.getX()-lastX));
            cursorY=Math.max(0,Math.min(getHeight()-1,cursorY+event.getY()-lastY));
            lastX=event.getX();lastY=event.getY();moved=true;
            mouse.position((int)cursorX,(int)cursorY,getWidth(),getHeight());
        } else if(event.getActionMasked()==MotionEvent.ACTION_UP && !moved
                && event.getEventTime()-event.getDownTime()<ViewConfiguration.getLongPressTimeout()) {
            if(fingers<=2) {
                mouse.position((int)cursorX,(int)cursorY,getWidth(),getHeight());mouse.click(fingers==2);
                RayNeoTrace.event("RayNeoInput","temple.mouse click="+(fingers==2?"right":"left"),false);
            }
        }
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(cursorVisible) {
            pointer.setColor(0xff000000);canvas.drawCircle(cursorX,cursorY,5,pointer);
            pointer.setColor(0xffffffff);canvas.drawCircle(cursorX,cursorY,3,pointer);
        }
    }
    public StreamInputView(Context context,StreamView.InputCallbacks callbacks) {
        super(context); this.callbacks=callbacks;
        setFocusable(true); setFocusableInTouchMode(true);
        if (android.os.Build.VERSION.SDK_INT >= 26) setDefaultFocusHighlightEnabled(false);
    }
    @Override public boolean onKeyPreIme(int code,KeyEvent event) {
        if(event.getAction()==KeyEvent.ACTION_DOWN && callbacks.handleKeyDown(event)) return true;
        if(event.getAction()==KeyEvent.ACTION_UP && callbacks.handleKeyUp(event)) return true;
        return super.onKeyPreIme(code,event);
    }
}

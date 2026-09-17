package com.limelight.ui.rayneo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/** One 640x480 software UI render, reused in both eyes. Video is a separate Surface. */
public final class StereoHost extends FrameLayout {
    public final FrameLayout content;
    final boolean gameInput;
    private final FocusNavigator navigator;
    private final TempleGesture gesture;
    private Bitmap bitmap;
    private Canvas software;
    private final int doubleMs=ViewConfiguration.getDoubleTapTimeout();
    private final Runnable expire;
    private int touchEyeOffset;
    private boolean loggedDraw;
    View modalPanel;
    boolean outsideAllowed;

    public StereoHost(Context context, boolean gameInput) {
        super(context);
        this.gameInput=gameInput;
        content=new FrameLayout(context);
        content.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        addView(content,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setClipChildren(false);
        navigator=new FocusNavigator(this);
        gesture=new TempleGesture(ViewConfiguration.get(context).getScaledTouchSlop(),
                doubleMs,ViewConfiguration.getLongPressTimeout());
        expire=gesture::expireTap;
        content.getViewTreeObserver().addOnGlobalLayoutListener(navigator::changed);
        content.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus)->navigator.changed());
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec), height=MeasureSpec.getSize(heightSpec);
        content.measure(MeasureSpec.makeMeasureSpec(width/2,MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
        setMeasuredDimension(width,height);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
        content.layout(0,0,(r-l)/2,b-t);
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        int w=getWidth()/2, h=getHeight();
        if (w<=0 || h<=0) return;
        boolean first=!loggedDraw;
        if(first) {
            loggedDraw=true;
            RayNeoTrace.i("RayNeoUI","first-draw begin eye="+w+"x"+h+" compositorHardware="+canvas.isHardwareAccelerated());
        }
        if (bitmap==null || bitmap.getWidth()!=w || bitmap.getHeight()!=h) {
            // Do not recycle the old bitmap while HWUI might still reference it.
            bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            software=new Canvas(bitmap);
        }
        software.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        content.draw(software); // Exactly one software child-tree traversal.
        if(first) RayNeoTrace.i("RayNeoUI","first-draw software-complete");
        canvas.drawBitmap(bitmap,0,0,null);
        canvas.drawBitmap(bitmap,w,0,null);
        if(first) RayNeoTrace.i("RayNeoUI","first-draw composite-recorded");
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_OUTSIDE) return false;
        if (RayNeoDevice.temple(event.getDevice())) return temple(event);
        if (event.getActionMasked()==MotionEvent.ACTION_DOWN) touchEyeOffset=event.getX()>=getWidth()/2f?getWidth()/2:0;
        MotionEvent logical=MotionEvent.obtain(event);
        logical.offsetLocation(-touchEyeOffset,0);
        if(modalPanel!=null && outsideAllowed && event.getActionMasked()==MotionEvent.ACTION_DOWN
                && (logical.getX()<modalPanel.getLeft() || logical.getX()>=modalPanel.getRight()
                || logical.getY()<modalPanel.getTop() || logical.getY()>=modalPanel.getBottom())) {
            // Retain original Dialog/Popup outside-touch cancellation policy after expansion.
            logical.setAction(MotionEvent.ACTION_OUTSIDE);
            boolean outside=getRootView().dispatchTouchEvent(logical);
            logical.recycle(); return outside;
        }
        boolean result=super.dispatchTouchEvent(logical);
        logical.recycle();
        return result;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (RayNeoDevice.temple(event.getDevice())) return temple(event);
        MotionEvent logical=MotionEvent.obtain(event);
        if (event.getX()>=getWidth()/2f) logical.offsetLocation(-getWidth()/2f,0);
        boolean result=super.dispatchGenericMotionEvent(logical);
        logical.recycle(); return result;
    }
    boolean temple(MotionEvent e) {
        if (e.getPointerCount()!=1) { cancelInput(); return true; }
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                navigator.cancelClick(); // A second gesture must never release the first click mid-swipe.
                gesture.down(e.getX(),e.getY(),e.getEventTime()); break;
            case MotionEvent.ACTION_MOVE:
                gesture.move(e.getX(),e.getY()); break;
            case MotionEvent.ACTION_CANCEL: cancelInput(); break;
            case MotionEvent.ACTION_UP:
                TempleGesture.Action action=gesture.up(e.getX(),e.getY(),e.getEventTime());
                if (action!=TempleGesture.Action.NONE && action!=TempleGesture.Action.TAP) {
                    removeCallbacks(expire); navigator.cancelClick();
                }
                switch(action) {
                    case TAP: navigator.tap(doubleMs); removeCallbacks(expire); postDelayed(expire,doubleMs); break;
                    case BACK: navigator.key(KeyEvent.KEYCODE_BACK); break;
                    case LEFT: navigator.move(KeyEvent.KEYCODE_DPAD_LEFT,View.FOCUS_LEFT); break;
                    case RIGHT: navigator.move(KeyEvent.KEYCODE_DPAD_RIGHT,View.FOCUS_RIGHT); break;
                    case UP: navigator.move(KeyEvent.KEYCODE_DPAD_UP,View.FOCUS_UP); break;
                    case DOWN: navigator.move(KeyEvent.KEYCODE_DPAD_DOWN,View.FOCUS_DOWN); break;
                    case LONG_PRESS: navigator.longPress(); break;
                    default: break;
                }
                break;
            default: break; // BUTTON_RELEASE after UP must not become a second tap.
        }
        return true;
    }
    private void cancelInput() { gesture.cancel(); navigator.stop(); removeCallbacks(expire); }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        cancelInput();
        if(focus) navigator.changed();
    }
    @Override protected void onDetachedFromWindow() {
        cancelInput(); bitmap=null; software=null;
        super.onDetachedFromWindow();
    }
}

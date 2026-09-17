package com.limelight.ui.rayneo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;
import java.util.function.Consumer;

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
    private Runnable backAction;
    private Consumer<MotionEvent> streamMouse;
    private Consumer<MotionEvent> streamClick;
    private final StreamTapExit injectedTaps;
    private final StreamTapExit templeTaps;
    private final TripleExitGate exitGate=new TripleExitGate();
    private FrameLayout noticeOverlay;
    private final Runnable hideNotice=()->{
        if(noticeOverlay!=null && noticeOverlay.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup)noticeOverlay.getParent()).removeView(noticeOverlay);
        noticeOverlay=null;
    };

    public StereoHost(Context context, boolean gameInput) {
        super(context);
        this.gameInput=gameInput;
        content=new FrameLayout(context);
        // Cache the complete physical stereo host, not the half-width child layer.
        // Otherwise HWUI damage from dynamic overlays can be clipped to the left eye.
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        addView(content,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setClipChildren(false);
        navigator=new FocusNavigator(this);
        backAction=()->navigator.key(KeyEvent.KEYCODE_BACK);
        int slop=ViewConfiguration.get(context).getScaledTouchSlop();
        injectedTaps=new StreamTapExit(doubleMs,slop,ViewConfiguration.getLongPressTimeout(),
                this::dispatchLogicalTouch,this::back,event->{if(streamClick!=null) streamClick.accept(event);});
        templeTaps=new StreamTapExit(doubleMs,slop,ViewConfiguration.getLongPressTimeout(),
                event->{if(streamMouse!=null) streamMouse.accept(event);},this::back);
        gesture=new TempleGesture(ViewConfiguration.get(context).getScaledTouchSlop(),
                doubleMs,ViewConfiguration.getLongPressTimeout());
        expire=gesture::expireTap;
        content.getViewTreeObserver().addOnGlobalLayoutListener(navigator::changed);
        content.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus)->navigator.changed());
    }
    public void setStreamActions(Consumer<MotionEvent> mouse,Consumer<MotionEvent> click,Runnable back) {
        streamMouse=mouse;streamClick=click;backAction=back;
    }
    private void back() {
        if(gameInput && !exitGate.confirm(android.os.SystemClock.uptimeMillis())) {
            RayNeoTrace.i("RayNeoInput","triple-tap exit-armed timeoutMs=3000");
            showExitNotice();
            return;
        }
        RayNeoTrace.i("RayNeoInput",(gameInput?"triple-tap":"double-tap")+" local-back game="+gameInput);
        backAction.run();
    }
    private void showExitNotice() {
        removeCallbacks(hideNotice);hideNotice.run();
        if(!(getParent() instanceof FrameLayout)) return;
        FrameLayout root=(FrameLayout)getParent();
        noticeOverlay=new FrameLayout(getContext());
        noticeOverlay.setFocusable(false);noticeOverlay.setClickable(false);
        for(int eye=0;eye<2;eye++) {
            android.widget.TextView text=new android.widget.TextView(getContext());
            text.setText("再次三击退出串流");text.setTextSize(16);text.setTextColor(0xffffffff);
            text.setBackgroundColor(0xe6222222);text.setPadding(12,8,12,8);
            text.setGravity(android.view.Gravity.CENTER);
            LayoutParams lp=new LayoutParams(180,40);
            lp.leftMargin=eye*640+230;lp.topMargin=425;
            noticeOverlay.addView(text,lp);
        }
        root.addView(noticeOverlay,new LayoutParams(-1,-1));
        postDelayed(hideNotice,2000);
    }
    private void dispatchLogicalTouch(MotionEvent event) {super.dispatchTouchEvent(event);}
    private boolean injectedPointer(MotionEvent event) {
        return event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)
                || (event.isFromSource(InputDevice.SOURCE_MOUSE)
                && (event.getDevice()==null || event.getDevice().isVirtual()));
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
    @Override public void onDescendantInvalidated(View child,View target) {
        if(android.os.Build.VERSION.SDK_INT>=26) super.onDescendantInvalidated(child,target);
        invalidate(); // Logical child damage must repaint both physical eye rectangles.
    }
    @Override public ViewParent invalidateChildInParent(int[] location,Rect dirty) {
        location[0]=0;location[1]=0;
        dirty.set(0,0,getWidth(),getHeight());
        return super.invalidateChildInParent(location,dirty);
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
        if (RayNeoDevice.temple(event.getDevice())) {
            return gameInput?templeTaps.touch(event):temple(event);
        }
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
        boolean result;
        if(gameInput && injectedPointer(event)) {
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN || event.getActionMasked()==MotionEvent.ACTION_UP)
                RayNeoTrace.event("RayNeoInput","stream.pointer action="+event.getActionMasked()
                        +" source="+event.getSource()+" device="+event.getDeviceId(),false);
            result=injectedTaps.touch(logical);
        } else result=super.dispatchTouchEvent(logical);
        logical.recycle();
        return result;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (RayNeoDevice.temple(event.getDevice())) return gameInput?templeTaps.touch(event):temple(event);
        if(gameInput && injectedPointer(event) && (event.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS
                || event.getActionMasked()==MotionEvent.ACTION_BUTTON_RELEASE)) return true;
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
                    case BACK: back(); break;
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
    private void cancelInput() {
        gesture.cancel(); navigator.stop(); injectedTaps.cancel();templeTaps.cancel();exitGate.reset();removeCallbacks(expire);
    }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        cancelInput();
        if(focus) navigator.changed();
    }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(hideNotice);hideNotice.run();
        cancelInput(); bitmap=null; software=null;
        super.onDetachedFromWindow();
    }
}

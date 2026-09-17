package com.limelight.ui.rayneo;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.view.KeyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class StereoHostTest {
    private StereoHost host(Activity activity) {
        StereoHost host=new StereoHost(activity,false);
        activity.setContentView(host);
        host.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
        host.layout(0,0,1280,480);
        return host;
    }
    @Test public void oneLogicalTreeIsDrawnOnceAtEyeSize() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        StereoHost host=host(activity);
        final int[] calls={0};
        View drawing=new View(activity) {
            @Override protected void onDraw(Canvas canvas) {
                calls[0]++;
                assertFalse("UI subtree must use software canvas",canvas.isHardwareAccelerated());
                assertEquals(640,getWidth()); assertEquals(480,getHeight());
                canvas.drawColor(Color.RED);
            }
        };
        host.content.addView(drawing,new FrameLayout.LayoutParams(-1,-1));
        host.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
        host.layout(0,0,1280,480);
        host.draw(new Canvas(Bitmap.createBitmap(1280,480,Bitmap.Config.ARGB_8888)));
        assertEquals("Stereo must not traverse side-effecting UI twice",1,calls[0]);
    }
    @Test public void detachedTargetNeverReceivesDelayedConfirmation() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button button=new Button(activity);
        final int[] clicks={0}; button.setOnClickListener(v->clicks[0]++);
        host.content.addView(button,new FrameLayout.LayoutParams(200,100));
        layout(host);
        button.requestFocusFromTouch();
        FocusNavigator navigator=new FocusNavigator(host);
        navigator.tap(300);
        host.content.removeView(button);
        ((ShadowLooper)Shadow.extract(Looper.getMainLooper())).idleFor(Duration.ofMillis(350));
        assertEquals(0,clicks[0]);
    }
    private void layout(StereoHost host) {
        host.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
        host.layout(0,0,1280,480);
    }
    private void idle(long millis) {
        ((ShadowLooper)Shadow.extract(Looper.getMainLooper())).idleFor(Duration.ofMillis(millis));
    }
    private void gesture(StereoHost host,int action,long down,long time,float x,float y) {
        MotionEvent event=MotionEvent.obtain(down,time,action,x,y,0);
        host.temple(event); event.recycle();
    }
    @Test public void stableTargetReceivesExactlyOneConfirmation() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button button=new Button(activity);
        final int[] clicks={0}; button.setOnClickListener(v->clicks[0]++);
        host.content.addView(button,new FrameLayout.LayoutParams(200,100));
        layout(host); button.requestFocusFromTouch(); idle(1); layout(host);
        assertTrue(host.hasWindowFocus()); assertEquals(button,host.content.findFocus());
        new FocusNavigator(host).tap(300);
        idle(350);
        assertEquals(1,clicks[0]);
    }
    @Test public void secondContactCancelsFirstClickBeforeLongHoldCompletes() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button button=new Button(activity);
        final int[] clicks={0}; button.setOnClickListener(v->clicks[0]++);
        host.content.addView(button,new FrameLayout.LayoutParams(200,100));
        layout(host); button.requestFocusFromTouch(); idle(1); layout(host);
        gesture(host,MotionEvent.ACTION_DOWN,100,100,20,20);
        gesture(host,MotionEvent.ACTION_UP,100,120,20,20);
        idle(150);
        gesture(host,MotionEvent.ACTION_DOWN,200,200,20,20);
        idle(400);
        assertEquals(0,clicks[0]);
        gesture(host,MotionEvent.ACTION_CANCEL,200,600,20,20);
    }
    @Test public void relabeledButtonDoesNotInheritPendingClick() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button button=new Button(activity); button.setText("Connect");
        final int[] clicks={0}; button.setOnClickListener(v->clicks[0]++);
        host.content.addView(button,new FrameLayout.LayoutParams(200,100));
        layout(host); button.requestFocusFromTouch(); idle(1); layout(host);
        new FocusNavigator(host).tap(300);
        button.setText("Delete"); layout(host); idle(350);
        assertEquals(0,clicks[0]);
    }
    @Test public void navigationBoundaryDoesNotResetFocusOrConfirm() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button button=new Button(activity);
        final int[] clicks={0}; button.setOnClickListener(v->clicks[0]++);
        host.content.addView(button,new FrameLayout.LayoutParams(200,100));
        layout(host); button.requestFocusFromTouch(); idle(1); layout(host);
        new FocusNavigator(host).move(KeyEvent.KEYCODE_DPAD_LEFT,View.FOCUS_LEFT);
        idle(1);
        assertEquals(button,host.content.findFocus()); assertEquals(0,clicks[0]);
    }
    @Test public void removedTargetRecoversToCurrentTreeWithoutClicking() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true).get();
        StereoHost host=host(activity);
        Button first=new Button(activity), second=new Button(activity);
        final int[] clicks={0}; second.setOnClickListener(v->clicks[0]++);
        host.content.addView(first,new FrameLayout.LayoutParams(200,100));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(200,100); lp.leftMargin=220;
        host.content.addView(second,lp);
        layout(host); first.requestFocusFromTouch(); idle(1); layout(host);
        FocusNavigator navigator=new FocusNavigator(host); navigator.recover();
        host.content.removeView(first); layout(host); navigator.recover(); idle(1);
        assertEquals(second,host.content.findFocus()); assertEquals(0,clicks[0]);
    }
}

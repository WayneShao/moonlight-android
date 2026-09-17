package com.limelight.ui.rayneo;

import android.app.Application;
import android.os.Looper;
import android.view.MotionEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class StreamTapExitTest {
    private final List<Integer> sent=new ArrayList<>();
    private final int[] backs={0};
    private final StreamTapExit recognizer=new StreamTapExit(300,12,500,e->sent.add(e.getActionMasked()),()->backs[0]++);
    private void event(int action,long down,long time,float x) {
        MotionEvent event=MotionEvent.obtain(down,time,action,x,30,0);
        assertTrue(recognizer.touch(event));event.recycle();
    }
    private void idle(int ms) {((ShadowLooper)Shadow.extract(Looper.getMainLooper())).idleFor(Duration.ofMillis(ms));}
    @Test public void tripleTapExitsOnceWithoutRemoteClicks() {
        event(0,100,100,20);event(1,100,130,20);
        idle(100);event(0,220,220,22);event(1,220,250,22);
        idle(100);event(0,340,340,22);event(1,340,370,22);idle(400);
        assertEquals(1,backs[0]);assertTrue(sent.isEmpty());
    }
    @Test public void doubleTapRemainsRemoteDoubleClick() {
        event(0,100,100,20);event(1,100,130,20);
        idle(100);event(0,220,220,22);event(1,220,250,22);idle(400);
        assertEquals(0,backs[0]);assertEquals(Arrays.asList(0,1,0,1),sent);
    }
    @Test public void singleTapStillReachesOriginalHandlerOnce() {
        event(0,100,100,20);event(1,100,130,20);
        assertTrue(sent.isEmpty());idle(350);
        assertEquals(Arrays.asList(0,1),sent);assertEquals(0,backs[0]);
    }
    @Test public void dragPreservesDownMoveUp() {
        event(0,100,100,20);event(2,100,150,80);event(1,100,180,100);idle(400);
        assertEquals(Arrays.asList(0,2,1),sent);assertEquals(0,backs[0]);
    }
    @Test public void windowLossCancelsPendingClick() {
        event(0,100,100,20);event(1,100,130,20);recognizer.cancel();idle(400);
        assertTrue(sent.isEmpty());assertEquals(0,backs[0]);
    }
    @Test public void windowLossCancelsForwardedDrag() {
        event(0,100,100,20);event(2,100,150,80);recognizer.cancel();idle(800);
        assertEquals(Arrays.asList(0,2,3),sent);assertEquals(0,backs[0]);
    }
    @Test public void secondaryMouseClicksNeverCountTowardExit() {
        for(int i=0;i<3;i++) {
            MotionEvent.PointerProperties properties=new MotionEvent.PointerProperties();properties.id=0;
            MotionEvent.PointerCoords coordinates=new MotionEvent.PointerCoords();coordinates.x=20;coordinates.y=20;
            MotionEvent d=MotionEvent.obtain(100+i*100,100+i*100,0,1,
                    new MotionEvent.PointerProperties[]{properties},new MotionEvent.PointerCoords[]{coordinates},
                    0,MotionEvent.BUTTON_SECONDARY,1,1,-1,0,0x2002,0);
            recognizer.touch(d);d.recycle();event(1,100+i*100,130+i*100,20);
        }
        idle(500);assertEquals(0,backs[0]);assertEquals(Arrays.asList(0,1,0,1,0,1),sent);
    }
}

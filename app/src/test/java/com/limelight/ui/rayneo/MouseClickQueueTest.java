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
public class MouseClickQueueTest {
    private void idle(long ms) {((ShadowLooper)Shadow.extract(Looper.getMainLooper())).idleFor(Duration.ofMillis(ms));}
    @Test public void confirmedDoubleTapHasReleaseBetweenRemotePresses() {
        List<String> sent=new ArrayList<>();
        MouseClickQueue queue=new MouseClickQueue((right,down)->sent.add((right?"R":"L")+(down?"D":"U")));
        StreamTapExit taps=new StreamTapExit(300,12,500,e->{},()->fail("double click exited"),e->queue.click(false));
        for(int i=0;i<2;i++) {
            MotionEvent d=MotionEvent.obtain(100+i*100,100+i*100,0,20,20,0);
            MotionEvent u=MotionEvent.obtain(100+i*100,130+i*100,1,20,20,0);
            taps.touch(d);taps.touch(u);d.recycle();u.recycle();
        }
        idle(700);assertEquals(Arrays.asList("LD","LU","LD","LU"),sent);
    }
    @Test public void cancellationReleasesPressedButtonAndDropsQueuedClick() {
        List<String> sent=new ArrayList<>();
        MouseClickQueue queue=new MouseClickQueue((right,down)->sent.add((right?"R":"L")+(down?"D":"U")));
        queue.click(true);queue.click(false);queue.cancel();idle(500);
        assertEquals(Arrays.asList("RD","RU"),sent);
    }
}

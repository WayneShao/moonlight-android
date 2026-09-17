package com.limelight.ui.rayneo;

import android.app.Application;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32,application=Application.class)
public class StreamMouseTest {
    private void send(StreamInputView view,int action,int... ids) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++) {
            properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=ids[i]==10?20:80;coords[i].y=30;coords[i].pressure=1;
        }
        MotionEvent event=MotionEvent.obtain(100,200,action,ids.length,properties,coords,0,0,1,1,4,0,0x1002,0);
        view.templeMotion(event);event.recycle();
    }
    @Test public void twoFingerTapWorksForEitherLiftOrder() {
        for(boolean firstLiftsFirst:new boolean[]{true,false}) {
            List<Boolean> clicks=new ArrayList<>();
            StreamInputView view=new StreamInputView(RuntimeEnvironment.getApplication(),null);
            view.layout(0,0,640,360);
            view.setMouseOutput(new StreamInputView.MouseOutput() {
                public void position(int x,int y,int w,int h) {}
                public void click(boolean right) {clicks.add(right);}
            });
            send(view,MotionEvent.ACTION_DOWN,10);
            send(view,MotionEvent.ACTION_POINTER_DOWN|(1<<8),10,20);
            send(view,MotionEvent.ACTION_POINTER_UP|((firstLiftsFirst?0:1)<<8),10,20);
            int remaining=firstLiftsFirst?20:10;
            send(view,MotionEvent.ACTION_MOVE,remaining);
            send(view,MotionEvent.ACTION_UP,remaining);
            assertEquals(Arrays.asList(true),clicks);
        }
    }
}

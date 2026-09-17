package com.limelight.ui.rayneo;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayDeque;

/** Keep the upstream 100 ms observable button pulse, with a release between clicks. */
public final class MouseClickQueue {
    public interface Output { void button(boolean right,boolean down); }
    private final Output output;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayDeque<Boolean> queue=new ArrayDeque<>();
    private Boolean active;
    private boolean gap;
    private final Runnable next=this::start;
    private final Runnable release=this::releaseButton;
    private void releaseButton() {
        if(active!=null) {output.button(active,false);active=null;}
        gap=true;handler.postDelayed(next,40);
    }
    public MouseClickQueue(Output output) {this.output=output;}
    public void click(boolean right) {
        queue.add(right);if(active==null && !gap) start();
    }
    private void start() {
        gap=false;if(queue.isEmpty()) return;
        active=queue.remove();output.button(active,true);handler.postDelayed(release,100);
    }
    public void cancel() {
        handler.removeCallbacks(next);handler.removeCallbacks(release);queue.clear();gap=false;
        if(active!=null) {output.button(active,false);active=null;}
    }
}

package com.limelight.ui.rayneo;

/** One gesture per DOWN/UP sequence, monotonic event time, no button-release duplicate. */
public final class TempleGesture {
    public enum Action { NONE, TAP, BACK, LEFT, RIGHT, UP, DOWN, LONG_PRESS }
    private float x, y;
    private long downTime, lastTap = -1;
    private boolean active, moved;
    private final float slop;
    private final long doubleMs, longMs;
    public TempleGesture(float slop, long doubleMs, long longMs) {
        this.slop = slop; this.doubleMs = doubleMs; this.longMs = longMs;
    }
    public void down(float x, float y, long time) {
        this.x=x; this.y=y; downTime=time; active=true; moved=false;
    }
    public void move(float x, float y) {
        if (active && (Math.abs(x-this.x)>slop || Math.abs(y-this.y)>slop)) moved=true;
    }
    public Action up(float x, float y, long time) {
        if (!active) return Action.NONE;
        move(x,y); active=false;
        float dx=x-this.x, dy=y-this.y;
        if (moved) {
            lastTap=-1;
            if (Math.max(Math.abs(dx),Math.abs(dy))<=slop) return Action.NONE;
            return Math.abs(dx)>=Math.abs(dy) ? (dx>0?Action.RIGHT:Action.LEFT)
                    : (dy>0?Action.DOWN:Action.UP);
        }
        if (time-downTime>=longMs) { lastTap=-1; return Action.LONG_PRESS; }
        if (lastTap>=0 && time-lastTap<=doubleMs && time>=lastTap) {
            lastTap=-1; return Action.BACK;
        }
        lastTap=time; return Action.TAP;
    }
    public void cancel() { active=false; moved=false; lastTap=-1; }
    public void expireTap() { lastTap=-1; }
}

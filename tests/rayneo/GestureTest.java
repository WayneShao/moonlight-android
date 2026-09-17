package com.limelight.ui.rayneo;
import static com.limelight.ui.rayneo.TempleGesture.Action.*;

public final class GestureTest {
    private static void eq(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        TempleGesture g=new TempleGesture(12,300,500);
        g.down(10,10,100); eq(TAP,g.up(10,10,120));
        eq(NONE,g.up(10,10,121)); // release duplicate
        g.down(10,10,200); eq(BACK,g.up(10,10,220));
        g.down(10,10,400); g.move(50,10); eq(RIGHT,g.up(50,10,500));
        g.down(50,10,600); eq(LEFT,g.up(0,10,700));
        g.down(10,50,800); eq(UP,g.up(10,0,900));
        g.down(10,0,1000); eq(DOWN,g.up(10,50,1100));
        g.down(0,0,1200); eq(LONG_PRESS,g.up(0,0,1800));
        g.down(0,0,1900); g.cancel(); eq(NONE,g.up(0,0,1950));
        g.down(0,0,2000); g.move(100,0); eq(NONE,g.up(0,0,2100)); // drag back isn't a tap
        g.down(0,0,2200); eq(TAP,g.up(0,0,2250)); g.expireTap();
        g.down(0,0,2300); eq(TAP,g.up(0,0,2350));
        System.out.println("GestureTest: 12 assertions passed");
    }
}

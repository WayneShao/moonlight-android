package com.limelight.ui.rayneo;

/** A triple tap arms exit; a second triple tap within three seconds confirms it. */
final class TripleExitGate {
    private long armedAt=-1;
    boolean confirm(long now) {
        if(armedAt>=0 && now>=armedAt && now-armedAt<=3000) {reset();return true;}
        armedAt=now;return false;
    }
    void reset() {armedAt=-1;}
}

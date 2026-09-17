package com.limelight.ui.rayneo;

import java.util.Arrays;

public final class PolicyTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(RayNeoPolicy.supports("RayNeo", "ARGF20", "MercuryLiteXR", 32, 1280, 480), "X3 Pro must be recognized");
        check(!RayNeoPolicy.supports("Other", "Tablet", "tablet", 32, 1280, 480), "Resolution alone must not enable stereo");
        check(!RayNeoPolicy.supports("RayNeo", "Other", "air", 32, 1280, 480), "Other RayNeo models must not inherit X3 geometry");
        check(!RayNeoPolicy.supports("RayNeo", "ARGF20", "MercuryLiteXR", 32, 640, 480), "No duplication in logical single-eye display mode");
        check(!RayNeoPolicy.supports("RayNeo", "ARGF20", "MercuryLiteXR", 28, 1280, 480), "Window API needs 29+");
        check(Arrays.equals(RayNeoPolicy.fit(1280, 720, 640, 480), new int[]{0,60,640,360}), "720p must letterbox");
        check(Arrays.equals(RayNeoPolicy.fit(640, 480, 640, 480), new int[]{0,0,640,480}), "4:3 must fill eye");
        check(Arrays.equals(RayNeoPolicy.fit(480, 640, 640, 480), new int[]{140,0,360,480}), "Portrait must pillarbox");
        check(Arrays.equals(RayNeoPolicy.fit(0, 0, 640, 480), new int[]{0,0,640,480}), "Unknown video dimensions must not divide by zero");
        System.out.println("PolicyTest: 9 assertions passed");
    }
}

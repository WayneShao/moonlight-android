package com.limelight.ui.rayneo;

/** Android-independent policy, exercised by tests/rayneo/PolicyTest.java. */
public final class RayNeoPolicy {
    private RayNeoPolicy() {}
    public static boolean supports(String manufacturer, String model, String device,
                                   int api, int width, int height) {
        boolean identity = "ARGF20".equalsIgnoreCase(model)
                || "MercuryLiteXR".equalsIgnoreCase(device);
        return identity && api >= 29 && width == 1280 && height == 480;
    }
    public static int[] fit(int sourceWidth, int sourceHeight, int width, int height) {
        if (sourceWidth <= 0 || sourceHeight <= 0) return new int[]{0, 0, width, height};
        double scale = Math.min((double) width / sourceWidth, (double) height / sourceHeight);
        int w = Math.max(1, (int) Math.round(sourceWidth * scale));
        int h = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new int[]{(width-w)/2, (height-h)/2, w, h};
    }
}

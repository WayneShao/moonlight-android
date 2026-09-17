package com.limelight.ui.rayneo;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import com.limelight.ui.StreamView;

/** Visible, single-eye input target. Does not own a second video surface. */
public final class StreamInputView extends View {
    private final StreamView.InputCallbacks callbacks;
    public StreamInputView(Context context,StreamView.InputCallbacks callbacks) {
        super(context); this.callbacks=callbacks;
        setFocusable(true); setFocusableInTouchMode(true);
        if (android.os.Build.VERSION.SDK_INT >= 26) setDefaultFocusHighlightEnabled(false);
    }
    @Override public boolean onKeyPreIme(int code,KeyEvent event) {
        if(event.getAction()==KeyEvent.ACTION_DOWN && callbacks.handleKeyDown(event)) return true;
        if(event.getAction()==KeyEvent.ACTION_UP && callbacks.handleKeyUp(event)) return true;
        return super.onKeyPreIme(code,event);
    }
}

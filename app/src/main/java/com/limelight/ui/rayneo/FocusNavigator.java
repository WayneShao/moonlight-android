package com.limelight.ui.rayneo;

import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Navigation operates on current tree state, never replays a consumed movement. */
final class FocusNavigator {
    private final StereoHost host;
    private WeakReference<ViewGroup> region = new WeakReference<>(null);
    private Pending pending;
    private final Runnable repair = this::recover;
    FocusNavigator(StereoHost host) { this.host=host; }

    private boolean valid(View view) {
        return view != null && view.isAttachedToWindow() && view.isShown() && view.isEnabled()
                && view.getRootView()==host.getRootView() && view.getGlobalVisibleRect(new Rect());
    }
    private AdapterView<?> list(View view) {
        for (View v=view; v!=null && v!=host; ) {
            if (v instanceof AdapterView) return (AdapterView<?>)v;
            ViewParent p=v.getParent(); v=p instanceof View ? (View)p : null;
        }
        return null;
    }
    private boolean selected(AdapterView<?> list) {
        int p=list.getSelectedItemPosition();
        return list.getAdapter()!=null && p>=0 && p<list.getAdapter().getCount()
                && valid(list.getSelectedView());
    }
    void changed() {
        host.removeCallbacks(repair);
        host.post(repair);
        if (pending!=null && !pending.matches()) cancelClick();
    }
    View recover() {
        if (!host.isAttachedToWindow() || !host.hasWindowFocus() || host.gameInput) return null;
        View focus=host.content.findFocus();
        if (valid(focus)) {
            AdapterView<?> l=list(focus);
            if (l==null || selected(l)) { remember(focus); return focus; }
            establishSelection(l);
            return null; // Wait for layout; never confirm a row while selection is recovering.
        }
        ViewGroup area=region.get();
        if (!valid(area)) area=host.content;
        if (focusIn(area)) return host.content.findFocus();
        if (area!=host.content) focusIn(host.content);
        return host.content.findFocus();
    }
    private void establishSelection(AdapterView<?> list) {
        if (list.getAdapter()==null || list.getAdapter().getCount()==0) return;
        int position=list.getSelectedItemPosition();
        if (position<0 || position>=list.getAdapter().getCount()) position=Math.max(0,list.getFirstVisiblePosition());
        list.requestFocusFromTouch();
        list.setSelection(position);
    }
    private boolean focusIn(ViewGroup area) {
        // Respect the container's default focus ordering before searching visible actions.
        if (area.requestFocusFromTouch()) {
            View f=area.findFocus();
            if (valid(f)) {
                AdapterView<?> l=list(f); if (l!=null && !selected(l)) establishSelection(l);
                remember(f); return true;
            }
        }
        ArrayList<View> candidates=new ArrayList<>();
        area.addFocusables(candidates,View.FOCUS_FORWARD,View.FOCUSABLES_ALL);
        for (View v:candidates) if (valid(v) && v.requestFocusFromTouch()) { remember(v); return true; }
        return false;
    }
    private void remember(View v) {
        ViewParent p=v.getParent();
        if (p instanceof ViewGroup) region=new WeakReference<>((ViewGroup)p);
    }
    boolean key(int code) {
        View root=host.getRootView();
        long time=SystemClock.uptimeMillis();
        boolean down=root.dispatchKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_DOWN,code,0));
        boolean up=root.dispatchKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_UP,code,0));
        return down || up;
    }
    void move(int code, int direction) {
        cancelClick();
        if (host.gameInput) { key(code); return; }
        View before=recover();
        AdapterView<?> l=list(before);
        int position=l==null?-1:l.getSelectedItemPosition();
        boolean handled=key(code);
        View after=host.content.findFocus();
        // Local dispatch may not reach ViewRoot's unhandled focus traversal.
        if (!handled && before!=null && before==after && valid(after)
                && (l==null || position==l.getSelectedItemPosition()) && !host.content.isLayoutRequested()) {
            View next=after.focusSearch(direction);
            if (valid(next) && next!=after && next.hasFocusable()) next.requestFocus(direction);
        }
        changed(); // Inspect after dispatch/layout; boundaries retain the current target.
    }
    void tap(long delay) {
        cancelClick();
        View target=host.gameInput ? host.content.findFocus() : recover();
        if (!valid(target) || host.content.isLayoutRequested()) return;
        AdapterView<?> l=list(target);
        if (l!=null && !selected(l)) return;
        pending=new Pending(target,l);
        host.postDelayed(pending,delay);
    }
    void longPress() {
        cancelClick();
        View target=recover();
        if (valid(target)) {
            AdapterView<?> l=list(target);
            if (l!=null && selected(l)) {
                View row=l.getSelectedView();
                l.showContextMenuForChild(row);
            } else target.performLongClick();
        }
    }
    void cancelClick() {
        if (pending!=null) { host.removeCallbacks(pending); pending.close(); pending=null; }
    }
    void stop() { cancelClick(); host.removeCallbacks(repair); }

    private final class Pending implements Runnable {
        final View target;
        final AdapterView<?> list;
        final Adapter adapter;
        final int position;
        final long id;
        final String label;
        final String description;
        final DataSetObserver observer=new DataSetObserver() {
            @Override public void onChanged() { cancelClick(); }
            @Override public void onInvalidated() { cancelClick(); }
        };
        Pending(View target, AdapterView<?> list) {
            this.target=target; this.list=list;
            adapter=list==null?null:list.getAdapter();
            position=list==null?-1:list.getSelectedItemPosition();
            id=list==null?-1:list.getSelectedItemId();
            label=target instanceof TextView?((TextView)target).getText().toString():null;
            description=String.valueOf(target.getContentDescription());
            if (adapter!=null) adapter.registerDataSetObserver(observer);
        }
        boolean matches() {
            return host.hasWindowFocus() && valid(target) && host.content.findFocus()==target
                    && (label==null || label.equals(((TextView)target).getText().toString()))
                    && description.equals(String.valueOf(target.getContentDescription()))
                    && !host.content.isLayoutRequested()
                    && (list==null || (list.getAdapter()==adapter && selected(list)
                    && list.getSelectedItemPosition()==position && list.getSelectedItemId()==id));
        }
        void close() { if (adapter!=null) adapter.unregisterDataSetObserver(observer); }
        @Override public void run() {
            boolean current=matches(); close(); pending=null;
            if (current) key(KeyEvent.KEYCODE_DPAD_CENTER);
        }
    }
}

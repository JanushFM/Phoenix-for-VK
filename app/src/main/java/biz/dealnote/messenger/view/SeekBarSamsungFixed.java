package biz.dealnote.messenger.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;

public class SeekBarSamsungFixed extends AppCompatSeekBar {

    boolean f50581a = true;

    public SeekBarSamsungFixed(Context context) {
        super(context);
    }

    public SeekBarSamsungFixed(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SeekBarSamsungFixed(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return f50581a && super.onTouchEvent(motionEvent);
    }

    public void setEnabled(boolean z) {
        f50581a = z;
    }
}

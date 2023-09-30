package com.abdelillahbel.winiwalk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

public class ClickableTextView extends AppCompatTextView {

    public ClickableTextView(Context context) {
        super(context);
    }

    public ClickableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        // Handle your click logic here
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
        }

        return result;
    }
}
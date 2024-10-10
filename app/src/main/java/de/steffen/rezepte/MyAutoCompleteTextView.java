package de.steffen.rezepte;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

public class MyAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    public MyAutoCompleteTextView(Context context) {
        super(context);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (!(getValidator() == null)) {
            if (!(getValidator().isValid(text))) {
                getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            } else {
                getBackground().clearColorFilter();
            }
        }
    }
}

package com.blueberryjoy.reader.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdjustMenuWidth {
    public static void adjust(LinearLayout menu) {
        int maxWidth = 0;

        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);

            if (child.getVisibility() != View.VISIBLE
                    || !(child instanceof TextView)) {
                continue;
            }

            child.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(
                            0, View.MeasureSpec.UNSPECIFIED)
            );

            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
        }

        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);

            if (child instanceof TextView) {
                ViewGroup.LayoutParams params = child.getLayoutParams();
                params.width = maxWidth;
                child.setLayoutParams(params);
            } else if (child != null) {
                ViewGroup.LayoutParams params = child.getLayoutParams();
                params.width = maxWidth;
                child.setLayoutParams(params);
            }
        }
    }
}

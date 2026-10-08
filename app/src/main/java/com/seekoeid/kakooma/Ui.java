package com.seekoeid.kakooma;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.seekoeid.kakooma.game.Op;

/** Small helpers for building the UI in code. */
final class Ui {
    static final int BACKGROUND = 0xFFFFF8EE;
    static final int PRIMARY = 0xFF5B4BDB;
    static final int INK = 0xFF2B2540;
    static final int MUTED = 0xFF7A7590;
    static final int CARD = 0xFFFFFFFF;
    static final int WRONG = 0xFFE5484D;

    private Ui() {}

    static int opColor(Op op) {
        switch (op) {
            case ADD: return 0xFF2E9E5B;
            case SUB: return 0xFFE07A1F;
            case MUL: return 0xFF2F7FD8;
            default:  return 0xFF9B4FD1;
        }
    }

    static int dp(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable rounded(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) d.setStroke(dp(c, 2), stroke);
        return d;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView button(Context c, String s, int fill, int textColor) {
        TextView b = text(c, s, 17, textColor, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(c, fill, 0, 16));
        int p = dp(c, 14);
        b.setPadding(p, p, p, p);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Pads {@code root} so content stays clear of the status and navigation bars. */
    @SuppressWarnings("deprecation")
    static void applySystemBarPadding(View root, int basePadding) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(basePadding + insets.getSystemWindowInsetLeft(),
                    basePadding + insets.getSystemWindowInsetTop(),
                    basePadding + insets.getSystemWindowInsetRight(),
                    basePadding + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }

    static String formatTime(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format(java.util.Locale.US, "%02d", s % 60);
    }

}

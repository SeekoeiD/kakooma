package com.seekoeid.kakooma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.seekoeid.kakooma.game.Group;
import com.seekoeid.kakooma.game.Puzzle;

/**
 * Draws the outer "flowers" in a grid with the final flower underneath, and
 * handles taps on their numbers.
 */
public class PuzzleView extends View {

    public interface Listener {
        void onCorrect(boolean puzzleComplete);
        void onWrong();
        void onFinalLocked();
    }

    private static final long SHAKE_MS = 450;
    private static final long POP_MS = 350;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Puzzle puzzle;
    private Listener listener;
    private boolean[] solved;   // one per outer group, plus the final group at the end
    private long[] solvedAt;
    private int wrongGroup = -1;
    private int wrongIndex = -1;
    private long wrongAt;
    private int hintGroup = -1;

    // Layout, recomputed in layout()
    private float[] cx, cy, size;

    public PuzzleView(Context c) {
        super(c);
        stroke.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setPuzzle(Puzzle p) {
        puzzle = p;
        int n = p.outer.size() + 1;
        solved = new boolean[n];
        solvedAt = new long[n];
        wrongGroup = -1;
        hintGroup = -1;
        cx = null;
        invalidate();
    }

    private int finalIndex() {
        return puzzle.outer.size();
    }

    private Group group(int i) {
        return i == finalIndex() ? puzzle.center : puzzle.outer.get(i);
    }

    private boolean outerDone() {
        for (int i = 0; i < finalIndex(); i++) if (!solved[i]) return false;
        return true;
    }

    /** True once every outer flower is solved and the final one is still open. */
    public boolean isFinalOpen() {
        return puzzle != null && outerDone() && !solved[finalIndex()];
    }

    /** Highlights the special number of the first unsolved flower. Returns false if none. */
    public boolean showHint() {
        if (puzzle == null) return false;
        for (int i = 0; i <= finalIndex(); i++) {
            if (!solved[i]) {
                hintGroup = i;
                invalidate();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cx = null;
    }

    private void layout() {
        int n = finalIndex();
        float w = getWidth(), h = getHeight();
        int rows = (n + 1) / 2;
        float cell = Math.min(w / 2f, h / (rows + 1.2f));
        cx = new float[n + 1];
        cy = new float[n + 1];
        size = new float[n + 1];
        for (int i = 0; i < n; i++) {
            int row = i / 2, col = i % 2;
            boolean alone = (i == n - 1) && (n % 2 == 1);
            float left = alone ? (w - cell) / 2f : (w - 2 * cell) / 2f + col * cell;
            cx[i] = left + cell / 2f;
            cy[i] = row * cell + cell * 0.45f;
            size[i] = cell;
        }
        float top = rows * cell;
        float remaining = h - top;
        float f = Math.min(w * 0.75f, remaining);
        cx[n] = w / 2f;
        cy[n] = top + (remaining - f) / 2f + f * 0.45f;
        size[n] = f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (puzzle == null) return;
        if (cx == null) layout();
        boolean animating = false;
        long now = SystemClock.uptimeMillis();
        for (int i = 0; i <= finalIndex(); i++) {
            animating |= drawGroup(canvas, i, now);
        }
        if (animating) postInvalidateOnAnimation();
    }

    private float ringRadius(int i) { return size[i] * 0.28f; }
    private float bubbleRadius(int i) {
        int count = group(i).numbers.length;
        return size[i] * (count <= 4 ? 0.135f : 0.125f);
    }

    /** Draws one flower; returns true while it is still animating. */
    private boolean drawGroup(Canvas c, int gi, long now) {
        Group g = group(gi);
        boolean isFinal = gi == finalIndex();
        boolean locked = isFinal && !outerDone();
        int color = Ui.opColor(g.op);
        float x0 = cx[gi], y0 = cy[gi], s = size[gi];
        float ring = ringRadius(gi), r = bubbleRadius(gi);
        boolean animating = false;

        // Petal background
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Ui.withAlpha(color, solved[gi] ? 60 : locked ? 18 : 30));
        c.drawCircle(x0, y0, ring + r + s * 0.03f, fill);
        if (isFinal) {
            stroke.setColor(Ui.withAlpha(color, locked ? 60 : 160));
            stroke.setStrokeWidth(s * 0.012f);
            c.drawCircle(x0, y0, ring + r + s * 0.03f, stroke);
        }

        // Centre: operation symbol
        fill.setColor(locked ? Ui.withAlpha(color, 90) : color);
        c.drawCircle(x0, y0, s * 0.1f, fill);
        text.setColor(0xFFFFFFFF);
        text.setTextSize(s * 0.13f);
        drawCentered(c, g.op.symbol, x0, y0);

        int count = g.numbers.length;
        for (int k = 0; k < count; k++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * k / count;
            float bx = x0 + (float) (ring * Math.cos(angle));
            float by = y0 + (float) (ring * Math.sin(angle));
            int value = g.numbers[k];
            boolean hidden = isFinal && !solved[k];
            boolean isTarget = value == g.target;
            boolean isOperand = value == g.a || value == g.b;
            float radius = r;

            int bubbleFill = Ui.CARD;
            int bubbleStroke = Ui.withAlpha(color, locked ? 70 : 140);
            int textColor = Ui.INK;

            if (solved[gi]) {
                if (isTarget) {
                    bubbleFill = color;
                    bubbleStroke = color;
                    textColor = 0xFFFFFFFF;
                    long t = now - solvedAt[gi];
                    if (t < POP_MS) {
                        radius *= 1f + 0.22f * (float) Math.sin(Math.PI * t / POP_MS);
                        animating = true;
                    }
                } else if (isOperand) {
                    bubbleFill = Ui.withAlpha(color, 70);
                    bubbleStroke = color;
                } else {
                    bubbleFill = 0xFFF1EEF4;
                    bubbleStroke = 0xFFE0DCE6;
                    textColor = 0xFFB5B0C2;
                }
            } else if (gi == wrongGroup && k == wrongIndex) {
                long t = now - wrongAt;
                if (t < SHAKE_MS) {
                    bx += (float) Math.sin(t / 25.0) * r * 0.18f * (1f - t / (float) SHAKE_MS);
                    bubbleFill = Ui.WRONG;
                    bubbleStroke = Ui.WRONG;
                    textColor = 0xFFFFFFFF;
                    animating = true;
                } else {
                    wrongGroup = -1;
                }
            } else if (gi == hintGroup && isTarget && !hidden) {
                bubbleStroke = 0xFFFFB020;
                float pulse = (float) (0.5 + 0.5 * Math.sin(now / 160.0));
                stroke.setColor(Ui.withAlpha(0xFFFFB020, (int) (90 + 120 * pulse)));
                stroke.setStrokeWidth(r * 0.22f);
                c.drawCircle(bx, by, r * (1.12f + 0.08f * pulse), stroke);
                animating = true;
            }

            fill.setColor(hidden ? 0xFFF4F1F7 : bubbleFill);
            c.drawCircle(bx, by, radius, fill);
            stroke.setColor(bubbleStroke);
            stroke.setStrokeWidth(r * 0.09f);
            c.drawCircle(bx, by, radius, stroke);

            String label = hidden ? "?" : String.valueOf(value);
            float scale = label.length() <= 2 ? 0.95f : 0.72f;
            text.setTextSize(radius * scale);
            text.setColor(hidden ? 0xFFB5B0C2 : textColor);
            drawCentered(c, label, bx, by);
        }

        // Caption under the flower
        String caption = null;
        int captionColor = Ui.MUTED;
        if (solved[gi]) {
            caption = g.equation();
            captionColor = color;
        } else if (isFinal) {
            caption = locked ? "Final flower — solve the others first" : "Final flower — solve it!";
            captionColor = locked ? Ui.MUTED : color;
        }
        if (caption != null) {
            float ts = Math.min(s * 0.075f, Ui.dp(getContext(), isFinal ? 20 : 18));
            text.setTextSize(ts);
            text.setColor(captionColor);
            float maxWidth = s * 0.98f;
            float measured = text.measureText(caption);
            if (measured > maxWidth) text.setTextSize(ts * maxWidth / measured);
            drawCentered(c, caption, x0, y0 + ring + r + s * 0.08f);
        }
        return animating;
    }

    private void drawCentered(Canvas c, String s, float x, float y) {
        Paint.FontMetrics fm = text.getFontMetrics();
        c.drawText(s, x, y - (fm.ascent + fm.descent) / 2f, text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (puzzle == null || e.getAction() != MotionEvent.ACTION_DOWN) {
            return e.getAction() == MotionEvent.ACTION_DOWN || super.onTouchEvent(e);
        }
        if (cx == null) layout();
        float x = e.getX(), y = e.getY();
        for (int gi = 0; gi <= finalIndex(); gi++) {
            Group g = group(gi);
            int count = g.numbers.length;
            float ring = ringRadius(gi), r = bubbleRadius(gi);
            for (int k = 0; k < count; k++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * k / count;
                float bx = cx[gi] + (float) (ring * Math.cos(angle));
                float by = cy[gi] + (float) (ring * Math.sin(angle));
                float dx = x - bx, dy = y - by;
                if (dx * dx + dy * dy <= r * r * 1.3f) {
                    handleTap(gi, k);
                    performClick();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void handleTap(int gi, int k) {
        if (solved[gi]) return;
        if (gi == finalIndex() && !outerDone()) {
            if (listener != null) listener.onFinalLocked();
            return;
        }
        Group g = group(gi);
        if (g.numbers[k] == g.target) {
            solved[gi] = true;
            solvedAt[gi] = SystemClock.uptimeMillis();
            if (hintGroup == gi) hintGroup = -1;
            invalidate();
            if (listener != null) listener.onCorrect(gi == finalIndex());
        } else {
            wrongGroup = gi;
            wrongIndex = k;
            wrongAt = SystemClock.uptimeMillis();
            invalidate();
            if (listener != null) listener.onWrong();
        }
    }
}

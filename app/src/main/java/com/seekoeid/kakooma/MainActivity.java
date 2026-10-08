package com.seekoeid.kakooma;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.seekoeid.kakooma.game.Difficulty;
import com.seekoeid.kakooma.game.Mode;

public class MainActivity extends Activity {

    private Stats stats;
    private Mode mode;
    private Difficulty difficulty;

    private final TextView[] modeChips = new TextView[Mode.values().length];
    private final TextView[] levelChips = new TextView[Difficulty.values().length];
    private TextView record;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        stats = new Stats(this);
        try {
            mode = stats.lastMode();
            difficulty = stats.lastDifficulty();
        } catch (IllegalArgumentException e) {
            mode = Mode.ADD;
            difficulty = Difficulty.EASY;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        scroll.setClipToPadding(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col);
        Ui.applySystemBarPadding(scroll, Ui.dp(this, 20));

        TextView title = Ui.text(this, "Kakooma", 44, Ui.PRIMARY, true);
        title.setGravity(Gravity.CENTER);
        col.addView(title);
        TextView subtitle = Ui.text(this, "Find the special number in every flower", 16, Ui.MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        col.addView(subtitle, margins(0, 0, 0, 20));

        col.addView(Ui.text(this, "Operation", 18, Ui.INK, true), margins(0, 0, 0, 8));
        String[] modeLabels = {"+  Addition", "−  Subtraction", "×  Multiplication", "÷  Division", "✦  Mixed"};
        LinearLayout row = null;
        for (int i = 0; i < Mode.values().length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                col.addView(row, margins(0, 0, 0, 8));
            }
            Mode m = Mode.values()[i];
            TextView chip = chip(modeLabels[i]);
            chip.setOnClickListener(v -> { mode = m; refresh(); });
            modeChips[i] = chip;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i % 2 == 0) lp.rightMargin = Ui.dp(this, 4); else lp.leftMargin = Ui.dp(this, 4);
            row.addView(chip, lp);
            if (i == Mode.values().length - 1 && i % 2 == 0) {
                // keep the last chip half width so the grid lines up
                row.addView(new TextView(this), new LinearLayout.LayoutParams(0, 1, 1));
            }
        }

        col.addView(Ui.text(this, "Level", 18, Ui.INK, true), margins(0, 16, 0, 8));
        for (int i = 0; i < Difficulty.values().length; i++) {
            Difficulty d = Difficulty.values()[i];
            TextView chip = chip("");
            chip.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            chip.setOnClickListener(v -> { difficulty = d; refresh(); });
            levelChips[i] = chip;
            col.addView(chip, margins(0, 0, 0, 8));
        }

        record = Ui.text(this, "", 15, Ui.MUTED, false);
        record.setGravity(Gravity.CENTER);
        col.addView(record, margins(0, 12, 0, 12));

        TextView play = Ui.button(this, "Play", Ui.PRIMARY, 0xFFFFFFFF);
        play.setTextSize(22);
        play.setOnClickListener(v -> {
            stats.saveSelection(mode, difficulty);
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_MODE, mode.name());
            intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name());
            startActivity(intent);
        });
        col.addView(play, margins(0, 0, 0, 12));

        TextView how = Ui.button(this, "How to play", 0x00000000, Ui.PRIMARY);
        how.setOnClickListener(v -> showHowTo());
        col.addView(how);

        setContentView(scroll);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private TextView chip(String label) {
        TextView t = Ui.text(this, label, 16, Ui.INK, true);
        t.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 12);
        t.setPadding(p, p, p, p);
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(this, l), Ui.dp(this, t), Ui.dp(this, r), Ui.dp(this, b));
        return lp;
    }

    private void refresh() {
        if (record == null) return;
        for (int i = 0; i < modeChips.length; i++) {
            boolean on = Mode.values()[i] == mode;
            style(modeChips[i], on);
        }
        for (int i = 0; i < levelChips.length; i++) {
            Difficulty d = Difficulty.values()[i];
            boolean on = d == difficulty;
            levelChips[i].setText(d.label + "\n" + d.describe(mode));
            style(levelChips[i], on);
        }
        long best = stats.best(mode, difficulty);
        int solved = stats.solved(mode, difficulty);
        record.setText(solved == 0
                ? "No puzzles solved yet at this level"
                : "Best time " + Ui.formatTime(best) + "  ·  Solved " + solved);
    }

    private void style(TextView t, boolean on) {
        t.setBackground(Ui.rounded(this, on ? Ui.PRIMARY : Ui.CARD, on ? 0 : 0xFFE3DEEA, 14));
        t.setTextColor(on ? 0xFFFFFFFF : Ui.INK);
    }

    private void showHowTo() {
        new AlertDialog.Builder(this)
                .setTitle("How to play")
                .setMessage("Each flower is a ring of numbers. In every flower exactly one number is special:\n\n"
                        + "+  it is the sum of two other numbers\n"
                        + "−  you can subtract another number from it and get a third\n"
                        + "×  it is the product of two other numbers\n"
                        + "÷  you can divide it by another number and get a third\n\n"
                        + "Tap the special number in each flower. Its answer moves into the final flower "
                        + "at the bottom. When all flowers are solved, find the special number of the final flower "
                        + "to finish the puzzle.\n\n"
                        + "Wrong taps add 5 seconds and hints add 15 seconds. Higher levels use bigger numbers "
                        + "and more numbers per flower. In Mixed mode every flower can use a different operation.\n\n"
                        + "Beginner multiplication and division have no final flower, because the numbers are "
                        + "too small to make one.")
                .setPositiveButton("Got it", null)
                .show();
    }
}

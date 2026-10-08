package com.seekoeid.kakooma;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.seekoeid.kakooma.game.Difficulty;
import com.seekoeid.kakooma.game.Mode;
import com.seekoeid.kakooma.game.Puzzle;
import com.seekoeid.kakooma.game.PuzzleGenerator;

import java.util.Random;

public class GameActivity extends Activity implements PuzzleView.Listener {
    static final String EXTRA_MODE = "mode";
    static final String EXTRA_DIFFICULTY = "difficulty";

    private static final long WRONG_PENALTY_MS = 5_000;
    private static final long HINT_PENALTY_MS = 15_000;

    private final PuzzleGenerator generator = new PuzzleGenerator(new Random());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Stats stats;

    private Mode mode;
    private Difficulty difficulty;
    private PuzzleView puzzleView;
    private TextView timer, info, instruction;

    private long startedAt;     // uptime when the clock was last started
    private long elapsed;       // time accumulated before startedAt
    private long penalty;
    private int mistakes;
    private boolean running;
    private boolean paused;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            if (running) handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        stats = new Stats(this);
        mode = Mode.valueOf(getIntent().getStringExtra(EXTRA_MODE));
        difficulty = Difficulty.valueOf(getIntent().getStringExtra(EXTRA_DIFFICULTY));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.applySystemBarPadding(root, Ui.dp(this, 12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = Ui.text(this, "←", 26, Ui.INK, true);
        back.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back);
        TextView title = Ui.text(this, mode.label + " · " + difficulty.label, 18, Ui.INK, true);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        timer = Ui.text(this, "0:00", 22, Ui.PRIMARY, true);
        top.addView(timer);
        root.addView(top);

        instruction = Ui.text(this, "", 15, Ui.MUTED, false);
        instruction.setGravity(Gravity.CENTER);
        instruction.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        root.addView(instruction);

        puzzleView = new PuzzleView(this);
        puzzleView.setListener(this);
        root.addView(puzzleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = Ui.button(this, "Hint", 0xFFFFE7B3, 0xFF7A5200);
        hint.setOnClickListener(v -> {
            if (running && puzzleView.showHint()) {
                penalty += HINT_PENALTY_MS;
                updateTimer();
            }
        });
        info = Ui.text(this, "", 14, Ui.MUTED, false);
        info.setGravity(Gravity.CENTER);
        TextView fresh = Ui.button(this, "New puzzle", Ui.PRIMARY, 0xFFFFFFFF);
        fresh.setOnClickListener(v -> newPuzzle());
        bottom.addView(hint, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bottom.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bottom.addView(fresh, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f));
        root.addView(bottom);

        setContentView(root);
        newPuzzle();
    }

    private void newPuzzle() {
        Puzzle p = generator.generate(mode, difficulty);
        puzzleView.setPuzzle(p);
        elapsed = 0;
        penalty = 0;
        mistakes = 0;
        startClock();
        instruction.setText(mode.op != null
                ? mode.op.instruction()
                : "Each flower shows its operation in the middle. Find its special number.");
        updateInfo();
        updateTimer();
    }

    private long totalTime() {
        long t = elapsed + penalty;
        if (running) t += SystemClock.uptimeMillis() - startedAt;
        return t;
    }

    private void startClock() {
        startedAt = SystemClock.uptimeMillis();
        running = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void stopClock() {
        if (running) elapsed += SystemClock.uptimeMillis() - startedAt;
        running = false;
        handler.removeCallbacks(tick);
    }

    private void updateTimer() {
        timer.setText(Ui.formatTime(totalTime()));
    }

    private void updateInfo() {
        info.setText(mistakes == 0 ? "" : mistakes + (mistakes == 1 ? " mistake" : " mistakes"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (running) {
            stopClock();
            paused = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
            startClock();
        }
    }

    @Override
    public void onCorrect(boolean puzzleComplete) {
        puzzleView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (!puzzleComplete) {
            if (puzzleView.isFinalOpen()) {
                instruction.setText("Now find the special number in the final flower!");
            }
            return;
        }
        stopClock();
        long total = totalTime();
        boolean best = stats.record(mode, difficulty, total);
        StringBuilder msg = new StringBuilder("Time: ").append(Ui.formatTime(total));
        if (penalty > 0) msg.append("  (includes +").append(penalty / 1000).append("s penalties)");
        msg.append("\nMistakes: ").append(mistakes);
        if (best) msg.append("\n\n⭐ New best time!");
        handler.postDelayed(() -> {
            if (isFinishing()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Solved!")
                    .setMessage(msg.toString())
                    .setCancelable(false)
                    .setPositiveButton("Next puzzle", (d, w) -> newPuzzle())
                    .setNegativeButton("Menu", (d, w) -> finish())
                    .show();
        }, 500);
    }

    @Override
    public void onWrong() {
        mistakes++;
        penalty += WRONG_PENALTY_MS;
        puzzleView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        updateInfo();
        updateTimer();
    }

    @Override
    public void onFinalLocked() {
        Toast.makeText(this, "Solve the other flowers first", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

package com.seekoeid.kakooma;

import android.content.Context;
import android.content.SharedPreferences;

import com.seekoeid.kakooma.game.Difficulty;
import com.seekoeid.kakooma.game.Mode;

/** Best times and solve counts, stored per mode and level. */
final class Stats {
    private final SharedPreferences prefs;

    Stats(Context c) {
        prefs = c.getSharedPreferences("kakooma", Context.MODE_PRIVATE);
    }

    private static String key(String what, Mode m, Difficulty d) {
        return what + "_" + m.name() + "_" + d.name();
    }

    long best(Mode m, Difficulty d) {
        return prefs.getLong(key("best", m, d), 0);
    }

    int solved(Mode m, Difficulty d) {
        return prefs.getInt(key("solved", m, d), 0);
    }

    /** Records a solve; returns true if it is a new best time. */
    boolean record(Mode m, Difficulty d, long ms) {
        long best = best(m, d);
        boolean isBest = best == 0 || ms < best;
        SharedPreferences.Editor e = prefs.edit();
        e.putInt(key("solved", m, d), solved(m, d) + 1);
        if (isBest) e.putLong(key("best", m, d), ms);
        e.apply();
        return isBest;
    }

    Mode lastMode() {
        return Mode.valueOf(prefs.getString("last_mode", Mode.ADD.name()));
    }

    Difficulty lastDifficulty() {
        return Difficulty.valueOf(prefs.getString("last_difficulty", Difficulty.EASY.name()));
    }

    void saveSelection(Mode m, Difficulty d) {
        prefs.edit().putString("last_mode", m.name()).putString("last_difficulty", d.name()).apply();
    }
}

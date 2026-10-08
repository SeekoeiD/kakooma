package com.seekoeid.kakooma.game;

import java.util.List;

/**
 * A full Kakooma puzzle: the special numbers of the outer groups, in order,
 * are the numbers of the final group.
 */
public final class Puzzle {
    public final Mode mode;
    public final Difficulty difficulty;
    public final List<Group> outer;
    public final Group center;

    Puzzle(Mode mode, Difficulty difficulty, List<Group> outer, Group center) {
        this.mode = mode;
        this.difficulty = difficulty;
        this.outer = outer;
        this.center = center;
    }
}

package com.seekoeid.kakooma.game;

public enum Difficulty {
    //        label     groups size sumMax factorMax productMax
    BEGINNER("Beginner", 3,     4,   10,    5,        25),
    EASY   ("Easy",     3,     4,   20,    10,       100),
    MEDIUM ("Medium",   4,     5,   50,    12,       150),
    HARD   ("Hard",     4,     6,   100,   15,       250),
    EXPERT ("Expert",   5,     6,   250,   20,       400);

    public final String label;
    /** Number of outer groups; also the size of the final (centre) group. */
    public final int groups;
    /** Numbers per outer group. */
    public final int groupSize;
    /** Largest number used in addition / subtraction groups. */
    public final int sumMax;
    /** Largest factor used in multiplication / division groups. */
    public final int factorMax;
    /** Largest number used in multiplication / division groups. */
    public final int productMax;

    Difficulty(String label, int groups, int groupSize, int sumMax, int factorMax, int productMax) {
        this.label = label;
        this.groups = groups;
        this.groupSize = groupSize;
        this.sumMax = sumMax;
        this.factorMax = factorMax;
        this.productMax = productMax;
    }

    public String describe(Mode mode) {
        String sums = "numbers up to " + sumMax;
        String products = "factors up to " + factorMax;
        String range;
        if (mode == Mode.MIXED) range = sums + ", " + products;
        else if (mode.op.product) range = products;
        else range = sums;
        String text = groups + " flowers of " + groupSize + " · " + range;
        if (mode.op != null && !PuzzleGenerator.hasFinal(mode.op, this)) text += " \u00b7 no final flower";
        return text;
    }
}

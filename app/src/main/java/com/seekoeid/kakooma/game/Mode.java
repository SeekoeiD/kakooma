package com.seekoeid.kakooma.game;

public enum Mode {
    ADD("Addition", Op.ADD),
    SUB("Subtraction", Op.SUB),
    MUL("Multiplication", Op.MUL),
    DIV("Division", Op.DIV),
    MIXED("Mixed", null);

    public final String label;
    /** The single operation used by this mode, or null for mixed. */
    public final Op op;

    Mode(String label, Op op) {
        this.label = label;
        this.op = op;
    }
}

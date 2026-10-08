package com.seekoeid.kakooma.game;

/**
 * The operation a group is built around.
 *
 * Addition and subtraction groups share the same structure (one number equals
 * the sum of two others, so it is also the number you can subtract another
 * from to get a third). Multiplication and division share the product
 * structure. What differs is how the player is asked to think about it.
 */
public enum Op {
    ADD("+", false),
    SUB("−", false),
    MUL("×", true),
    DIV("÷", true);

    public final String symbol;
    public final boolean product;

    Op(String symbol, boolean product) {
        this.symbol = symbol;
        this.product = product;
    }

    /** Combines two operands the way this group's structure does (sum or product). */
    public int combine(int a, int b) {
        return product ? a * b : a + b;
    }

    /** Human readable equation for a solved group whose special number is {@code target}. */
    public String equation(int target, int a, int b) {
        switch (this) {
            case ADD: return a + " + " + b + " = " + target;
            case SUB: return target + " − " + a + " = " + b;
            case MUL: return a + " × " + b + " = " + target;
            default:  return target + " ÷ " + a + " = " + b;
        }
    }

    public String instruction() {
        switch (this) {
            case ADD: return "Find the number that is the sum of two others.";
            case SUB: return "Find the number you can subtract another from to get a third.";
            case MUL: return "Find the number that is the product of two others.";
            default:  return "Find the number you can divide by another to get a third.";
        }
    }
}

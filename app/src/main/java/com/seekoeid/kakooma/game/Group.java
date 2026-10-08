package com.seekoeid.kakooma.game;

/** One "flower" of numbers. Exactly one number is the special one. */
public final class Group {
    public final Op op;
    public final int[] numbers;
    public final int target;
    /** One pair of operands that produce the target. */
    public final int a;
    public final int b;

    Group(Op op, int[] numbers, int target, int a, int b) {
        this.op = op;
        this.numbers = numbers;
        this.target = target;
        this.a = a;
        this.b = b;
    }

    public String equation() {
        return op.equation(target, a, b);
    }
}

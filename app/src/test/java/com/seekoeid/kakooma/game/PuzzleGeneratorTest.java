package com.seekoeid.kakooma.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class PuzzleGeneratorTest {

    @Test
    public void everyModeAndLevelProducesValidPuzzles() {
        PuzzleGenerator gen = new PuzzleGenerator(new Random(42));
        for (Mode mode : Mode.values()) {
            for (Difficulty d : Difficulty.values()) {
                for (int i = 0; i < 200; i++) {
                    check(gen.generate(mode, d));
                }
            }
        }
    }

    private static void check(Puzzle p) {
        Difficulty d = p.difficulty;
        assertEquals(d.groups, p.outer.size());
        boolean finalExpected = p.mode.op == null || PuzzleGenerator.hasFinal(p.mode.op, d);
        assertEquals(finalExpected, p.hasFinal());
        if (p.hasFinal()) checkGroup(p.center, d.groups, p);
        for (int i = 0; i < p.outer.size(); i++) {
            Group g = p.outer.get(i);
            checkGroup(g, d.groupSize, p);
            if (p.hasFinal()) assertEquals(p.center.numbers[i], g.target);
            if (p.mode.op != null) assertEquals(p.mode.op, g.op);
        }
    }

    @Test
    public void beginnerKeepsNumbersSmall() {
        PuzzleGenerator gen = new PuzzleGenerator(new Random(7));
        for (Mode mode : Mode.values()) {
            for (int i = 0; i < 500; i++) {
                Puzzle p = gen.generate(mode, Difficulty.BEGINNER);
                for (Group g : p.outer) {
                    int limit = g.op.product ? Difficulty.BEGINNER.productMax : Difficulty.BEGINNER.sumMax;
                    for (int n : g.numbers) assertTrue(mode + " " + n, n <= limit);
                }
            }
        }
        assertTrue(PuzzleGenerator.hasFinal(Op.ADD, Difficulty.BEGINNER));
        assertTrue(!PuzzleGenerator.hasFinal(Op.MUL, Difficulty.BEGINNER));
    }

    private static void checkGroup(Group g, int size, Puzzle p) {
        assertEquals(size, g.numbers.length);
        Set<Integer> seen = new HashSet<>();
        for (int n : g.numbers) {
            assertTrue("positive", n > 0);
            assertTrue("distinct", seen.add(n));
        }
        assertTrue(seen.contains(g.a) && seen.contains(g.b));
        assertEquals(g.target, g.op.combine(g.a, g.b));
        assertTrue("unique special number", PuzzleGenerator.isUnique(g.numbers, g.target, g.op.product));
        if (g.op.product) {
            assertTrue(Math.max(g.a, g.b) <= p.difficulty.factorMax || g == p.center);
        }
    }
}

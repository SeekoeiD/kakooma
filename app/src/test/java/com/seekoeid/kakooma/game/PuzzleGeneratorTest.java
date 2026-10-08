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
        checkGroup(p.center, d.groups, p);
        for (int i = 0; i < p.outer.size(); i++) {
            Group g = p.outer.get(i);
            checkGroup(g, d.groupSize, p);
            assertEquals(p.center.numbers[i], g.target);
            if (p.mode.op != null) assertEquals(p.mode.op, g.op);
        }
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

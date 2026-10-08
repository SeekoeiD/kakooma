package com.seekoeid.kakooma.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class PuzzleGenerator {
    private static final int ATTEMPTS = 2000;

    private final Random random;

    public PuzzleGenerator(Random random) {
        this.random = random;
    }

    public Puzzle generate(Mode mode, Difficulty d) {
        while (true) {
            Puzzle p = tryGenerate(mode, d);
            if (p != null) return p;
        }
    }

    private Puzzle tryGenerate(Mode mode, Difficulty d) {
        Op centerOp = mode.op != null ? mode.op : Op.values()[random.nextInt(4)];
        if (mode.op == null && !hasFinal(centerOp, d)) centerOp = random.nextBoolean() ? Op.ADD : Op.SUB;
        if (!hasFinal(centerOp, d)) return withoutFinal(mode, d);

        Group center = makeGroup(centerOp, d.groups, d, null);
        if (center == null) return null;

        List<Group> outer = new ArrayList<>();
        for (int value : center.numbers) {
            Op op = mode.op != null ? mode.op : pickMixedOp(value, d);
            Group g = makeGroup(op, d.groupSize, d, value);
            if (g == null) return null;
            outer.add(g);
        }
        return new Puzzle(mode, d, outer, center);
    }

    /** Single-operation puzzle with independent flowers and no final group. */
    private Puzzle withoutFinal(Mode mode, Difficulty d) {
        List<Integer> pool = mode.op.product ? productPool(d) : sumPool(d);
        List<Integer> targets = new ArrayList<>();
        while (targets.size() < d.groups) {
            int t = pool.get(random.nextInt(pool.size()));
            // Prefer different special numbers in each flower when the pool allows it.
            if (!targets.contains(t) || pool.size() < d.groups) targets.add(t);
        }
        List<Group> outer = new ArrayList<>();
        for (int t : targets) {
            Group g = makeGroup(mode.op, d.groupSize, d, t);
            if (g == null) return null;
            outer.add(g);
        }
        return new Puzzle(mode, d, outer, null);
    }

    /** True if a final group can be built for this operation at this level. */
    public static boolean hasFinal(Op op, Difficulty d) {
        if (!op.product) return d.sumMax >= 7;  // smallest final group: 3 + 4 = 7
        List<Integer> pool = productPool(d);
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                int p = pool.get(i) * pool.get(j);
                if (p <= d.productMax && pool.contains(p)) return true;
            }
        }
        return false;
    }

    private Op pickMixedOp(int value, Difficulty d) {
        boolean productOk = value <= d.productMax && !factorPairs(value, d.factorMax).isEmpty();
        if (productOk && random.nextBoolean()) return random.nextBoolean() ? Op.MUL : Op.DIV;
        return random.nextBoolean() ? Op.ADD : Op.SUB;
    }

    /**
     * Builds a group of {@code size} distinct numbers with exactly one special
     * number. If {@code fixedTarget} is null the target is chosen freely, but
     * every number must itself be a valid target (because the centre group's
     * numbers are the targets of the outer groups).
     */
    Group makeGroup(Op op, int size, Difficulty d, Integer fixedTarget) {
        boolean center = fixedTarget == null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int[] pair = center ? centerTriple(op, d) : operandsFor(op, fixedTarget, d);
            if (pair == null) return null;
            int a = pair[0], b = pair[1];
            int target = op.combine(a, b);

            Set<Integer> used = new HashSet<>();
            used.add(a);
            used.add(b);
            used.add(target);
            List<Integer> nums = new ArrayList<>(used);
            int guard = 0;
            while (nums.size() < size && guard++ < 200) {
                int x = distractor(op, target, d, center);
                if (used.add(x)) nums.add(x);
            }
            if (nums.size() < size) continue;

            int[] arr = new int[size];
            for (int i = 0; i < size; i++) arr[i] = nums.get(i);
            if (!isUnique(arr, target, op.product)) continue;
            shuffle(arr);
            return new Group(op, arr, target, a, b);
        }
        return null;
    }

    /** Operands for a centre group: both operands and their result must be valid targets. */
    private int[] centerTriple(Op op, Difficulty d) {
        if (!op.product) {
            int max = d.sumMax;
            int a = 3 + random.nextInt(Math.max(1, max / 2 - 3));
            int b = 3 + random.nextInt(Math.max(1, max - a - 3));
            if (a == b || a + b > max) return null;
            return new int[]{a, b};
        }
        List<Integer> pool = productPool(d);
        List<int[]> options = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                int p = pool.get(i) * pool.get(j);
                if (p <= d.productMax && pool.contains(p)) {
                    options.add(new int[]{pool.get(i), pool.get(j)});
                }
            }
        }
        if (options.isEmpty()) return null;
        int[] o = options.get(random.nextInt(options.size()));
        return random.nextBoolean() ? o : new int[]{o[1], o[0]};
    }

    private int[] operandsFor(Op op, int target, Difficulty d) {
        if (!op.product) {
            if (target < 3) return null;
            for (int i = 0; i < 50; i++) {
                int a = 1 + random.nextInt(target - 1);
                int b = target - a;
                if (a != b) return new int[]{a, b};
            }
            return null;
        }
        List<int[]> pairs = factorPairs(target, d.factorMax);
        if (pairs.isEmpty()) return null;
        int[] p = pairs.get(random.nextInt(pairs.size()));
        return random.nextBoolean() ? p : new int[]{p[1], p[0]};
    }

    private int distractor(Op op, int target, Difficulty d, boolean center) {
        if (!op.product) {
            if (center) return 3 + random.nextInt(d.sumMax - 2);
            // Keep distractors near the scale of the target so they look plausible.
            int hi = Math.min(d.sumMax, Math.max(target + target / 2, 10));
            return 1 + random.nextInt(hi);
        }
        if (center) {
            List<Integer> pool = productPool(d);
            return pool.get(random.nextInt(pool.size()));
        }
        // Mix small factor-sized numbers with product-sized numbers.
        if (random.nextBoolean()) return 2 + random.nextInt(d.factorMax - 1);
        int hi = Math.min(Math.max(target + target / 2, d.factorMax * 2), d.productMax);
        return 2 + random.nextInt(hi - 1);
    }

    /** Pairs (x, y) with 2 <= x < y <= factorMax and x * y == n. */
    static List<int[]> factorPairs(int n, int factorMax) {
        List<int[]> out = new ArrayList<>();
        for (int x = 2; x * x < n; x++) {
            if (n % x == 0 && n / x <= factorMax) out.add(new int[]{x, n / x});
        }
        return out;
    }

    /** Numbers that can be the special number of a multiplication group. */
    static List<Integer> productPool(Difficulty d) {
        List<Integer> pool = new ArrayList<>();
        for (int n = 4; n <= d.productMax; n++) {
            if (!factorPairs(n, d.factorMax).isEmpty()) pool.add(n);
        }
        return pool;
    }

    /** Numbers that can be the special number of an addition group. */
    static List<Integer> sumPool(Difficulty d) {
        List<Integer> pool = new ArrayList<>();
        for (int n = 3; n <= d.sumMax; n++) pool.add(n);
        return pool;
    }

    /** True if {@code nums[idx]} combines from two other distinct entries. */
    public static boolean isTarget(int[] nums, int idx, boolean product) {
        for (int i = 0; i < nums.length; i++) {
            if (i == idx) continue;
            for (int j = i + 1; j < nums.length; j++) {
                if (j == idx) continue;
                int v = product ? nums[i] * nums[j] : nums[i] + nums[j];
                if (v == nums[idx]) return true;
            }
        }
        return false;
    }

    /** True if {@code target} is the one and only special number in {@code nums}. */
    static boolean isUnique(int[] nums, int target, boolean product) {
        int count = 0;
        boolean found = false;
        for (int i = 0; i < nums.length; i++) {
            if (isTarget(nums, i, product)) {
                count++;
                if (nums[i] == target) found = true;
            }
        }
        return count == 1 && found;
    }

    private void shuffle(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
    }
}

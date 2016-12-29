package com.shuffle.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * When the user provides a seed to the prng, it needs to have enough entropy
 * or else someone else might be able to guess it. The EntropyEstimator uses
 * a simple statistical test to guess the entropy of a string.
 *
 * The EntropyEstimator is conservative. It will tend to underestimate the
 * entropy of a random string. As long as the user relies on some reasonably
 * random process to generate the seed, then the EntropyEstimator is a reliable
 * test to ensure that he has provided enough.
 *
 * On the other hand, if the user types in something that is not as random as
 * it naively looks, such as an English sentence, then the EntropyEstimator may
 * still overestimate the amount of entropy in the string.
 *
 * Created by Daniel Krawisz on 6/14/16.
 */
public class EntropyEstimator {
    private static double log2 = Math.log(2.0);

    private final StringBuilder builder = new StringBuilder();

    Map<Character, Map<Character, Integer>> state = new HashMap<>();
    Map<Character, Integer> totals = new HashMap<>();
    Character last = null;

    public EntropyEstimator() {}

    private void inputChar(Character x) {

        // If this is the first character entered, there is no last.
        if (last == null) {
            last = x;
            return;
        }

        Map<Character, Integer> stateX = state.get(last);
        Integer total = totals.get(last);

        if (stateX == null) {
            stateX = new HashMap<>();
            state.put(last, stateX);
            total = 0;
        }

        Integer count = stateX.get(x);
        if (count == null) {
            count = 1;
        } else {
            count = count + 1;
        }
        stateX.put(x, count);
        total += 1;
        totals.put(last, total);

        last = x;
    }

    private double estimateEntropy() {
        Character last = null;
        String string = builder.toString();
        double entropy = 0;
        for (int i = 0, n = string.length(); i < n; i ++) {
            Character c = string.charAt(i);

            if (last != null) {

                Integer count = state.get(last).get(c);
                Integer total = totals.get(last);

                entropy += Math.log(((double)total)/((double)count));
            }

            last = c;
        }

        return entropy/log2;
    }

    public double put(char x) {
        builder.append(x);
        return estimateEntropy();
    }

    public double put(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            inputChar(s.charAt(i));
        }
        builder.append(s);
        return estimateEntropy();
    }

    public String get() {
        return builder.toString();
    }

    // This method only exists for testing purposes.
    public static void main(String[] args) {
        EntropyEstimator ee = new EntropyEstimator();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Provide a string: ");
            System.out.println("String is likely to have at least " + ee.put(scanner.next()) + " bits of entropy.");
        }
    }
}

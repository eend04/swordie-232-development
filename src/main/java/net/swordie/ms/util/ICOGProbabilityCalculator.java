package net.swordie.ms.util;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.HashMap;
import java.util.Map;

public class ICOGProbabilityCalculator {

    private static final double[] OUTCOMES;
    private static final double[] PROBABILITIES;
    private static final double MEAN;
    private static final double VARIANCE;
    private static final double maxAVG = 12.5; // 10 ATT + 10 Main stat

    static {
        // Step 1: Generate all possible outcomes and count their frequencies
        Map<Double, Integer> frequencyMap = new HashMap<>();
        for (int i = 0; i <= 10; i++) { // ICOG ATT
            for (int j = 0; j <= 10; j++) { // ICOG Main Stat
                double sum = i + (j * 0.25);
                frequencyMap.put(sum, frequencyMap.getOrDefault(sum, 0) + 1);
            }
        }

        // Step 2: Calculate the probabilities and mean
        int totalSums = 11 * 11; // Total combinations
        OUTCOMES = new double[frequencyMap.size()];
        PROBABILITIES = new double[frequencyMap.size()];
        double sumMean = 0;
        int index = 0;
        for (Map.Entry<Double, Integer> entry : frequencyMap.entrySet()) {
            double sum = entry.getKey();
            double probability = entry.getValue() / (double) totalSums;
            OUTCOMES[index] = sum;
            PROBABILITIES[index] = probability;
            sumMean += sum * probability;
            index++;
        }
        MEAN = sumMean;

        // Step 3: Calculate the variance
        double sumVariance = 0;
        for (int i = 0; i < OUTCOMES.length; i++) {
            sumVariance += Math.pow(OUTCOMES[i] - MEAN, 2) * PROBABILITIES[i];
        }
        VARIANCE = sumVariance;
    }

    public static double getProbabilityEqualOrAbove(int trials, double score) {
        double lowerBound = 0;
        double upperBound = trials * maxAVG;

        if (score <= lowerBound) {
            return 1.0;
        } else if (score > upperBound) {
            return 0.0;
        }

        double sumMean = trials * MEAN;
        double sumStd = Math.sqrt(trials * VARIANCE);

        NormalDistribution distribution = new NormalDistribution(sumMean, sumStd);
        return 1.0 - distribution.cumulativeProbability(score);
    }
}
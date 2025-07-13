package src.analysis;

import src.utils.TideDataReader;
import src.optimisation.*;
import java.util.*;
import java.io.IOException;


/**
 * Utility for comparing multiple datasets and validating baseline performance.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class DatasetComparator {
    public static void main(String[] args) throws IOException {
        System.out.println("=== TIDAL DATASET COMPARISON ANALYSIS ===\n");

        // Load both datasets
        System.out.println("Loading datasets...\n");

        List<Double> data_2011 = TideDataReader.readTideHeights("data/b1111463.txt");
        List<Double> data_2010 = loadData2010();

        compareDatasetCharacteristics(data_2011, data_2010);
        compareBaselinePerformance(data_2011, data_2010);

        System.out.println("\n=== ANALYSIS COMPLETE ===");
    }

    /**
     * Loads 2010 tidal data 
     */
    private static List<Double> loadData2010() throws IOException {
        return TideDataReader.readTideHeights("data/b1111451.txt");
    }

    /**
     * Compares baseline characteristics of two datasets.
     */
    private static void compareDatasetCharacteristics(List<Double> data2011, List<Double> data2010) {
        System.out.println("Comparing dataset characteristics...\n");

        // Calculate statistics
        DatasetStats stats2011 = calculateStats(data2011, "2011");
        DatasetStats stats2010 = calculateStats(data2010, "2010");

        // Print statistics
        System.out.printf("%-20s %-12s %-12s %-12s %-12s\n", 
                "Metric", "2011 Value", "2010 Value", "Abs Diff", "Pct Diff");
        System.out.println("-".repeat(68));

        printComparison("Data Points", stats2011.dataPoints, stats2010.dataPoints);
        printComparison("Mean Height (m)", stats2011.mean, stats2010.mean);
        printComparison("Max Height (m)", stats2011.max, stats2010.max);
        printComparison("Min Height (m)", stats2011.min, stats2010.min);
        printComparison("Tidal Range (m)", stats2011.range, stats2010.range);
        printComparison("Std Deviation", stats2011.stdDev, stats2010.stdDev);
        
        System.out.println();

        // Data quality assessment
        System.out.println("=== Data Quality Assessment ===");
        System.out.printf("2011 Data Quality: %.2f%% (missing: %d points)\n", 
                stats2011.getQualityPercentage(), stats2011.getMissingPoints());
        System.out.printf("2010 Data Quality: %.2f%% (missing: %d points)\n", 
                stats2010.getQualityPercentage(), stats2010.getMissingPoints());
        
        // Recommendation
        if (Math.abs(stats2011.mean - stats2010.mean) / stats2011.mean > 0.1) {
            System.out.println("WARNING: Significant difference in tidal characteristics (>10%)");
        } else {
            System.out.println("Datasets show similar tidal characteristics (<10% difference)");
        }
    }

    /**
     * Compares baseline energy performance using simple strategies.
     */
    private static void compareBaselinePerformance(List<Double> data2011, List<Double> data2010) {
        System.out.println("Comparing baseline energy performance...\n");

        // Test simple fixed strategies
        double[] testHeads = {1.5, 2.0, 2.5, 3.0};

        System.out.printf("%-20s %-15s %-15s %-12s\n", 
                "Strategy", "2011 Energy", "2010 Energy", "Difference");
        System.out.println("-".repeat(62));
        
        for (double head : testHeads) {
            double energy2011 = testFixedHeadStrategy(data2011, head);
            double energy2010 = testFixedHeadStrategy(data2010, head);
            double diffPct = ((energy2011 - energy2010) / energy2010) * 100;
            
            System.out.printf("Fixed Head %.1fm     %-15.1f %-15.1f %+.1f%%\n", 
                    head, energy2011/1000, energy2010/1000, diffPct);
        }

         // Test simple grid search
        double gridEnergy2011 = testSimpleGridSearch(data2011);
        double gridEnergy2010 = testSimpleGridSearch(data2010);
        double gridDiffPct = ((gridEnergy2011 - gridEnergy2010) / gridEnergy2010) * 100;
        
        System.out.printf("Simple Grid Search  %-15.1f %-15.1f %+.1f%%\n", 
                gridEnergy2011/1000, gridEnergy2010/1000, gridDiffPct);
        
        System.out.println();
        
        // Validation assessment
        double maxDifference = Math.max(
                Math.abs(gridDiffPct),
                Arrays.stream(testHeads)
                        .map(head -> Math.abs(((testFixedHeadStrategy(data2011, head) - 
                                               testFixedHeadStrategy(data2010, head)) / 
                                               testFixedHeadStrategy(data2010, head)) * 100))
                        .max().orElse(0.0)
        );
        
        if (maxDifference > 15) {
            System.out.println("WARNING: Large energy differences between datasets (>15%)");
            System.out.println("Consider investigating data quality or model issues.");
        } else {
            System.out.println("Baseline energy outputs are consistent between datasets");
            System.out.println("Datasets suitable for robustness testing.");
        }
    }

    /**
     * Tests a fixed head strategy on given data.
     */
    private static double testFixedHeadStrategy(List<Double> tideData, double fixedHead) {
        // Create individual with constant head values
        int halfTides = Math.min(28, tideData.size() / 24); // Weekly simulation max
        Individual individual = new Individual(halfTides);
        
        for (int i = 0; i < halfTides; i++) {
            individual.setStartHead(i, fixedHead);
            individual.setEndHead(i, fixedHead * 0.8); // Slightly lower end head
        }
        
        // Evaluate and return energy
        ObjectiveFunction.evaluate(tideData, individual);
        return individual.getEnergyOutput();
    }
    
    /**
     * Tests a simple 3-point grid search.
     */
    private static double testSimpleGridSearch(List<Double> tideData) {
        double[] testHeads = {1.5, 2.5, 3.5};
        double bestEnergy = 0;
        
        for (double hs : testHeads) {
            for (double he : testHeads) {
                int halfTides = Math.min(28, tideData.size() / 24); // Weekly simulation max
                Individual individual = new Individual(halfTides);

                for (int i = 0; i < halfTides; i++) {
                    individual.setStartHead(i, hs);
                    individual.setEndHead(i, he);
                }
                ObjectiveFunction.evaluate(tideData, individual);
                bestEnergy = Math.max(bestEnergy, individual.getEnergyOutput());
            }
        }
        
        return bestEnergy;
    }
    
    /**
     * Prints a comparison row.
     */
    private static void printComparison(String metric, double value2011, double value2010) {
        double absDiff = Math.abs(value2011 - value2010);
        double pctDiff = ((value2011 - value2010) / value2010) * 100;
        
        System.out.printf("%-20s %-12.2f %-12.2f %-12.2f %+.1f%%\n", 
                metric, value2011, value2010, absDiff, pctDiff);
    }
    
    /**
     * Calculates comprehensive statistics for a dataset.
     */
    private static DatasetStats calculateStats(List<Double> data, String year) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Dataset " + year + " is empty");
        }
        
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (double value : data) {
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        double mean = sum / data.size();
        double variance = 0;
        
        for (double value : data) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= data.size();
        double stdDev = Math.sqrt(variance);
        
        return new DatasetStats(year, data.size(), mean, min, max, stdDev);
    }
    
    /**
     * Data class for dataset statistics.
     */
    private static class DatasetStats {
        final String year;
        final int dataPoints;
        final double mean, min, max, stdDev, range;
        
        DatasetStats(String year, int dataPoints, double mean, double min, double max, double stdDev) {
            this.year = year;
            this.dataPoints = dataPoints;
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.stdDev = stdDev;
            this.range = max - min;
        }
        
        double getQualityPercentage() {
            // Assume expected ~35,000 points per year (15 min intervals)
            int expectedPoints = 35040; // 365 * 24 * 4
            return Math.min(100.0, (dataPoints * 100.0) / expectedPoints);
        }
        
        int getMissingPoints() {
            int expectedPoints = 35040;
            return Math.max(0, expectedPoints - dataPoints);
        }
    }
}
    
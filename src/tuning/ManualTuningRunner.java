package src.tuning;

import src.optimisation.*;
import src.utils.TideDataReader;
import java.util.*;
import java.io.*;

/**
 * Enhanced manual parameter tuning runner with dataset tracking and comprehnsive analysis.
 * 
 * Provides a systematic framework for testing NGSA-II parameter configurations
 * across different datasets and scenarios. Supports three testing phases:
 * 1. TUNING
 * 2. VALIDATION
 * 3. BENCHMARKING
 * 
 * Key features include:
 * - Multi dataset support
 * - Statistical analysis with confidence intervals
 * - Excel-compatible output formatting
 * - Automated result archiving 
 * - Robustness assessment for cross-dataset validation
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class ManualTuningRunner {
    
    // ======================
    // TESTING CONFIGURATION
    // ======================

    /** Current test identifier for tracking results */
    private static final int TEST_NUMBER = 1;

    /** Testing phase: TUNING, VALIDATION, BENCHMARKING */
    private static final String TEST_PHASE = "TUNING";

    /** Dataset selection: 2011, 2012, or BOTH */
    private static final String DATASET_YEAR = "2012";

    /** Descriptive name for current test configuration */
    private static final String TEST_NAME = "Pop400_Pc09_Pm015_HALFTIDE_GAUSSIAN_ANNUAL_TUNING_2D_TOURNAMENT_SIZE_4";

    // ======================
    // ALGORITHM PARAMETERS
    // ======================

    
    /** Number of independent runs for statistical analysis */
    private static final int NUM_RUNS = 5;
    
    // ==========================
    // ROBUSTNESS THRESHOLDS
    // ==========================
    
    /** Excellent robustness threshold (%) */
    private static final double EXCELLENT_ROBUSTNESS = 5.0;
    
    /** Good robustness threshold (%) */
    private static final double GOOD_ROBUSTNESS = 15.0;

    // ==========================
    // MAIN EXECUTION
    // ==========================

    /**
     * Main entry point for parameter tuning experiments.
     * Configures and executes tests based on current settings.
     * 
     * @param args Command line arguments (not used)
     * @throws IOException If file operations fail
     */
    public static void main(String[] args) throws IOException {

        debugObjectiveFunction();
        printTestHeader();

        if (DATASET_YEAR.equals("BOTH")) {
            runBothDatasets();
        } else {
            runSingleDataset();
        }

        System.out.println("\nAll tests completed successfully.");
    }

    // ==========================
    // EXECUTION WORKFLOWS
    // ==========================

    /**
     * Executes test on a single dataset with comprehensive analysis.
     * 
     * @throws IOException If file operations fail
     */
    private static void runSingleDataset() throws IOException {
       List<Double> tideData = loadTideData(DATASET_YEAR);
       NSGA2Config config = createTestConfiguration();

       List<TuningResult> results = runMultipleTrials(config, tideData, DATASET_YEAR);

       printResults(DATASET_YEAR, results);
       printExcelFormat(DATASET_YEAR, results);
       saveResults(DATASET_YEAR, results);
    }

    /**
     * Executes test on both datasets for robustness analysis.
     * Compares performance across different tidal conditions.
     * 
     * @throws IOException if data loading fails
     */
    private static void runBothDatasets() throws IOException {
        System.out.println("Running robustness analysis on BOTH datasets...\n");
        
        // Execute on 2011 dataset
        List<Double> tideData2011 = loadTideData("2011");
        NSGA2Config config = createTestConfiguration();
        List<TuningResult> results2011 = runMultipleTrials(config, tideData2011, "2011");
        
        // Execute on 2012 dataset
        List<Double> tideData2012 = loadTideData("2012");
        List<TuningResult> results2012 = runMultipleTrials(config, tideData2012, "2012");
        
        // Comprehensive comparison analysis
        printCombinedResults(results2011, results2012);
        printCombinedExcelFormat(results2011, results2012);
        
        // Archive both result sets
        saveResults("2011", results2011);
        saveResults("2012", results2012);
    }

    // ==========================
    // DATA MANAGEMENT
    // ==========================
    
    /**
     * Loads tidal data for the specified year.
     * 
     * @param year Dataset year ("2011" or "2012")
     * @return List of tidal heights in meters
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if year is not supported
     */
    private static List<Double> loadTideData(String year) throws IOException {
        String filename;
        switch (year) {
            case "2011":
                filename = "data/2011MUM.txt";
                break;
            case "2012":
                filename = "data/2012MUM.txt";
                break;
            default:
                throw new IllegalArgumentException("Unsupported year: " + year + 
                    ". Supported years: 2011, 2012");
        }
        
        System.out.println("Loading " + year + " data from: " + filename);
        List<Double> data = TideDataReader.readTideHeights(filename);

        if (data.size() > 2640 && year.equals("2011")) {
            System.out.println("Truncating 2011 data from " + data.size() + " to 2640 points for comparison");
            data = data.subList(0, 2640);
        }

        if (data.size() > 2640 && year.equals("2012")) {
            System.out.println("Truncating 2012 data from " + data.size() + " to 2640 points for comparison");
            data = data.subList(0, 2640);
        }

        System.out.printf("Loaded %d tidal height readings\n", data.size());
        return data;
    }

    /**
     * Creates NSGA-II configuration from current test parameters.
     * 
     * @return Configured NSGA2Config instance
     */
    private static NSGA2Config createTestConfiguration() {
        return NSGA2Config.getAnnualConfig();
    }

     // ==========================
    // OPTIMIZATION EXECUTION
    // ==========================
    
    /**
     * Runs multiple independent optimization trials for statistical analysis.
     * 
     * @param config NSGA-II algorithm configuration
     * @param tideData Tidal height data for simulation
     * @param datasetYear Dataset identifier for tracking
     * @return List of results from all trials
     */
    private static List<TuningResult> runMultipleTrials(NSGA2Config config, 
                                                       List<Double> tideData, 
                                                       String datasetYear) {
        List<TuningResult> results = new ArrayList<>();
        
        System.out.printf("Running %d trials on %s data...\n", NUM_RUNS, datasetYear);
        
        for (int run = 1; run <= NUM_RUNS; run++) {
            System.out.printf("  Run %d/%d... ", run, NUM_RUNS);
            
            long startTime = System.currentTimeMillis();
            
            // Execute optimization
            NSGA2Algorithm algorithm = new NSGA2Algorithm(config, tideData);
            NSGA2Algorithm.OptimisationResult result = algorithm.optimise();
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            // Extract performance metrics
            List<Individual> paretoFront = result.getParetoFront();
            double maxEnergy = paretoFront.stream()
                    .mapToDouble(Individual::getEnergyOutput)
                    .max().orElse(0.0);
            double minCost = paretoFront.stream()
                    .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                    .mapToDouble(Individual::getUnitCost)
                    .min().orElse(Double.MAX_VALUE);
            
            // Create result record
            TuningResult tuningResult = new TuningResult(
                    run, datasetYear, maxEnergy, paretoFront.size(), executionTime,
                    result.generationsRun, result.convergenceAchieved, minCost
            );
            results.add(tuningResult);
            
            System.out.printf("Energy=%.1f GWh, PF=%d, Time=%.1fs\n", 
                    maxEnergy/1000, paretoFront.size(), executionTime);
        }
        
        return results;
    }

    // ==========================
    // OUTPUT FORMATTING
    // ==========================
    
    /**
     * Prints comprehensive test header with configuration details.
     */
    private static void printTestHeader() {

        NSGA2Config config = createTestConfiguration();

        System.out.println("=".repeat(70));
        System.out.println("         ENHANCED MANUAL PARAMETER TUNING");
        System.out.println("=".repeat(70));
        System.out.printf("Test #%d: %s\n", TEST_NUMBER, TEST_NAME);
        System.out.printf("Phase: %s\n", TEST_PHASE);
        System.out.printf("Dataset: %s\n", DATASET_YEAR);
        System.out.println("-".repeat(50));
        System.out.println("Configuration:");
        System.out.printf("  Population Size: %d\n", config.getPopulationSize());
        System.out.printf("  Crossover: %.3f (%s)\n", config.getCrossoverProbability(), config.getCrossoverType());
        System.out.printf("  Mutation: %.6f (%s)\n", config.getMutationProbability(), config.getMutationType());
        System.out.printf("  Max Generations: %d\n", config.getMaxGenerations());
        System.out.printf("  Half Tides: %d\n", config.getHalfTides());
        System.out.printf("  Runs: %d\n", NUM_RUNS);
        System.out.println("=".repeat(70));
    }

     /**
     * Prints statistical summary for single dataset results.
     * 
     * @param dataset Dataset identifier
     * @param results List of trial results
     */
    private static void printResults(String dataset, List<TuningResult> results) {
        System.out.printf("\n--- SUMMARY RESULTS (%s Dataset) ---\n", dataset);
        
        double[] energies = results.stream().mapToDouble(r -> r.maxEnergy).toArray();
        double[] pfSizes = results.stream().mapToDouble(r -> r.paretoSize).toArray();
        double[] times = results.stream().mapToDouble(r -> r.executionTime).toArray();
        
        long convergedCount = results.stream().mapToLong(r -> r.converged ? 1 : 0).sum();
        
        System.out.printf("Max Energy:     %.1f ± %.1f GWh\n",
                mean(energies)/1000, std(energies)/1000);
        System.out.printf("Pareto Size:    %.1f ± %.1f solutions\n", 
                mean(pfSizes), std(pfSizes));
        System.out.printf("Execution Time: %.2f ± %.2f seconds\n", 
                mean(times), std(times));
        System.out.printf("Convergence:    %d/%d runs (%.0f%%)\n",
                convergedCount, results.size(), (convergedCount * 100.0) / results.size());
    }

    /**
     * Prints comparative analysis for both datasets.
     * Includes robustness assessment based on energy differences.
     * 
     * @param results2011 Results from 2011 dataset
     * @param results2012 Results from 2012 dataset
     */
    private static void printCombinedResults(List<TuningResult> results2011, 
                                           List<TuningResult> results2012) {
        System.out.println("\n--- ROBUSTNESS ANALYSIS ---");
        
        double mean2011 = mean(results2011.stream().mapToDouble(r -> r.maxEnergy).toArray()) / 1000;
        double mean2012 = mean(results2012.stream().mapToDouble(r -> r.maxEnergy).toArray()) / 1000;
        double difference = ((mean2011 - mean2012) / mean2012) * 100;
        
        System.out.printf("2011 Energy: %.1f ± %.1f GWh\n", mean2011,
                std(results2011.stream().mapToDouble(r -> r.maxEnergy).toArray()) / 1000);
        System.out.printf("2012 Energy: %.1f ± %.1f GWh\n", mean2012,
                std(results2012.stream().mapToDouble(r -> r.maxEnergy).toArray()) / 1000);
        System.out.printf("Difference: %+.1f%% (2011 vs 2012)\n", difference);

        // Robustness assessment
        if (Math.abs(difference) <= EXCELLENT_ROBUSTNESS) {
            System.out.println("Excellent robustness: <5% difference between datasets");
        } else if (Math.abs(difference) <= GOOD_ROBUSTNESS) {
            System.out.println("Good robustness: <15% difference between datasets");
        } else {
            System.out.println("Investigation needed: >15% difference between datasets");
        }
    }

    /**
     * Prints Excel-formatted output for easy copy-paste to tracking spreadsheet.
     * 
     * @param dataset Dataset identifier
     * @param results List of trial results
     */
    private static void printExcelFormat(String dataset, List<TuningResult> results) {

        NSGA2Config config = createTestConfiguration();

        System.out.println("\n--- EXCEL COPY-PASTE FORMAT ---");
        System.out.println("Test\tPhase\tDataset\tPopulation\tCrossover\tMutation\t" +
                          "CrossoverType\tMutationType\tRun\tMaxEnergy\tParetoSize\t" +
                          "ExecutionTime\tGenerations\tConverged\tMinCost");
        
        for (TuningResult result : results) {
            System.out.printf("%d\t%s\t%s\t%d\t%.3f\t%.6f\t%s\t%s\t%d\t%.1f\t%d\t%.2f\t%d\t%s\t%.0f\n",
                    TEST_NUMBER, TEST_PHASE, dataset, config.getPopulationSize(), config.getCrossoverProbability(), config.getMutationProbability(),
                    config.getCrossoverType(), config.getMutationType(), result.runNumber, result.maxEnergy/1000,
                    result.paretoSize, result.executionTime, result.generations,
                    result.converged ? "TRUE" : "FALSE",
                    result.minCost != Double.MAX_VALUE ? result.minCost : 0);
        }
    }

    /**
     * Prints combined Excel format for both datasets.
     * 
     * @param results2011 Results from 2011 dataset
     * @param results2012 Results from 2012 dataset
     */
    private static void printCombinedExcelFormat(List<TuningResult> results2011, 
                                               List<TuningResult> results2012) {
        System.out.println("\n--- COMBINED EXCEL FORMAT ---");
        System.out.println("Test\tPhase\tDataset\tPopulation\tCrossover\tMutation\t" +
                          "CrossoverType\tMutationType\tRun\tMaxEnergy\tParetoSize\t" +
                          "ExecutionTime\tGenerations\tConverged\tMinCost");
        printExcelDataRows("2011", results2011);
        printExcelDataRows("2012", results2012);
    }

    /**
     * Helper method to print Excel data rows for a specific dataset.
     * 
     * @param dataset Dataset identifier
     * @param results List of trial results
     */
    private static void printExcelDataRows(String dataset, List<TuningResult> results) {

        NSGA2Config config = createTestConfiguration();
        for (TuningResult result : results) {
            System.out.printf("%d\t%s\t%s\t%d\t%.3f\t%.6f\t%s\t%s\t%d\t%.1f\t%d\t%.2f\t%d\t%s\t%.0f\n",
                    TEST_NUMBER, TEST_PHASE, dataset, config.getPopulationSize(), config.getCrossoverProbability(), config.getMutationProbability(),
                    config.getCrossoverType(), config.getMutationType(), result.runNumber, result.maxEnergy/1000,
                    result.paretoSize, result.executionTime, result.generations,
                    result.converged ? "TRUE" : "FALSE",
                    result.minCost != Double.MAX_VALUE ? result.minCost : 0);
        }
    }
    
    // ==========================
    // RESULT ARCHIVING
    // ==========================
    
    /**
     * Saves detailed results to CSV file for permanent record keeping.
     * 
     * @param dataset Dataset identifier
     * @param results List of trial results
     */
    private static void saveResults(String dataset, List<TuningResult> results) {

        NSGA2Config config = createTestConfiguration();
        try {
            // Ensure results directory exists
            new File("results").mkdirs();
            
            String filename = String.format("results/test_%d_%s_%s_%s.csv", 
                    TEST_NUMBER, TEST_PHASE.toLowerCase(), dataset, TEST_NAME);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                // CSV header
                writer.println("Test,Phase,Dataset,Population,Crossover,Mutation," +
                              "CrossoverType,MutationType,Run,MaxEnergy,ParetoSize," +
                              "ExecutionTime,Generations,Converged,MinCost");
                
                // Data rows
                for (TuningResult result : results) {
                    writer.printf("%d,%s,%s,%d,%.3f,%.6f,%s,%s,%d,%.1f,%d,%.2f,%d,%s,%.0f\n",
                            TEST_NUMBER, TEST_PHASE, dataset, config.getPopulationSize(), config.getCrossoverProbability(),
                            config.getMutationProbability(), config.getCrossoverType(), config.getMutationType(), result.runNumber,
                            result.maxEnergy, result.paretoSize, result.executionTime,
                            result.generations, result.converged ? "TRUE" : "FALSE",
                            result.minCost != Double.MAX_VALUE ? result.minCost : 0);
                }
            }
            
            System.out.println("\nResults saved to: " + filename);
            
        } catch (IOException e) {
            System.err.println("Error saving results: " + e.getMessage());
        }
    }

    public static void debugSingleRun() throws IOException {
        List<Double> tideData = TideDataReader.readTideHeights("data/b1111463.txt");
        
        NSGA2Config config = NSGA2Config.getAnnualConfig(); // Use your working config
        
        System.out.println("=== DEBUG SINGLE RUN ===");
        System.out.println("Config: " + config.toString());
        
        NSGA2Algorithm algorithm = new NSGA2Algorithm(config, tideData);
        NSGA2Algorithm.OptimisationResult result = algorithm.optimise();
        
        List<Individual> paretoFront = result.getParetoFront();
        
        System.out.println("=== RESULTS ANALYSIS ===");
        System.out.printf("Pareto Front Size: %d\n", paretoFront.size());
        System.out.printf("Population Size: %d\n", config.getPopulationSize());
        System.out.printf("Generations Run: %d\n", result.generationsRun);
        System.out.printf("Converged: %s\n", result.convergenceAchieved);
        
        if (!paretoFront.isEmpty()) {
            System.out.println("\n=== PARETO FRONT SAMPLE ===");
            for (int i = 0; i < Math.min(5, paretoFront.size()); i++) {
                Individual ind = paretoFront.get(i);
                System.out.printf("Solution %d: Energy=%.1f GWh, Cost=£%.0f/MWh\n",
                        i+1, ind.getEnergyOutput()/1000, ind.getUnitCost());
            }
        }
        
        // Check if all solutions are non-dominated
        if (paretoFront.size() == config.getPopulationSize()) {
            System.out.println("\nWARNING: All solutions are non-dominated!");
            System.out.println("This indicates an issue with objective evaluation.");
        }
    }

    public static void debugObjectiveFunction() throws IOException {
        System.out.println("=== OBJECTIVE FUNCTION DEBUG ===");
    
        List<Double> tideData = loadTideData("2011");
        
        // Test with annual config
        NSGA2Config config = NSGA2Config.getAnnualConfig();
        Individual testInd = new Individual(config.getHalfTides());
        
        // Set simple strategy
        for (int i = 0; i < config.getHalfTides(); i++) {
            testInd.setStartHead(i, 2.0);
            testInd.setEndHead(i, 1.5);
        }
        
        System.out.printf("Half-tides: %d\n", config.getHalfTides());
        System.out.printf("Simulation description: %s\n", config.getSimulationDescription());
        
        // Step 1: Get raw simulation output
        double rawEnergy = TidalSimulator.simulate(tideData, testInd);
        System.out.printf("Raw simulation: %.1f MWh\n", rawEnergy);
        
        // Step 2: Call ObjectiveFunction.evaluate
        ObjectiveFunction.evaluate(tideData, testInd);
        System.out.printf("After ObjectiveFunction: %.1f MWh (%.1f GWh)\n", 
                        testInd.getEnergyOutput(), testInd.getEnergyOutput()/1000);
        
        // Step 3: Expected calculation
        int simHours = (int)(config.getHalfTides() * 6.12);
        double expectedAnnual = rawEnergy * (8760.0 / simHours);
        System.out.printf("Expected annualized: %.1f MWh (%.1f GWh)\n", 
                        expectedAnnual, expectedAnnual/1000);
        
        System.out.printf("Simulation hours: %d\n", simHours);
        System.out.printf("Annualization factor: %.2fx\n", 8760.0 / simHours);
        
        if (Math.abs(testInd.getEnergyOutput() - expectedAnnual) > 1000) {
            System.out.println("MISMATCH: ObjectiveFunction not annualizing correctly!");
        } else {
            System.out.println("Annualization working correctly");
        }
    }
    
    // ==========================
    // STATISTICAL UTILITIES
    // ==========================
    
    /**
     * Calculates arithmetic mean of array values.
     * 
     * @param values Array of values
     * @return Mean value
     */
    private static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }
    
    /**
     * Calculates sample standard deviation of array values.
     * 
     * @param values Array of values
     * @return Standard deviation
     */
    private static double std(double[] values) {
        double m = mean(values);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - m, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    // ==========================
    // INNER CLASSES
    // ==========================
    
    /**
     * Data class for storing tuning trial results with comprehensive metrics.
     * Captures all relevant performance indicators for statistical analysis.
     */
    private static class TuningResult {
        
        /** Trial run number (1-based) */
        final int runNumber;
        
        /** Dataset identifier (e.g., "2011", "2012") */
        final String dataset;
        
        /** Maximum energy output achieved (MWh) */
        final double maxEnergy;
        
        /** Number of solutions in final Pareto front */
        final int paretoSize;
        
        /** Total execution time (seconds) */
        final double executionTime;
        
        /** Number of generations completed */
        final int generations;
        
        /** Whether convergence was achieved */
        final boolean converged;
        
        /** Minimum unit cost achieved (£/MWh) */
        final double minCost;
        
        /**
         * Constructs a comprehensive tuning result record.
         * 
         * @param runNumber Trial identifier
         * @param dataset Dataset used
         * @param maxEnergy Peak energy output
         * @param paretoSize Final Pareto front size
         * @param executionTime Total runtime
         * @param generations Generations completed
         * @param converged Convergence status
         * @param minCost Minimum unit cost
         */
        TuningResult(int runNumber, String dataset, double maxEnergy, int paretoSize, 
                    double executionTime, int generations, boolean converged, double minCost) {
            this.runNumber = runNumber;
            this.dataset = dataset;
            this.maxEnergy = maxEnergy;
            this.paretoSize = paretoSize;
            this.executionTime = executionTime;
            this.generations = generations;
            this.converged = converged;
            this.minCost = minCost;
        }
        
        /**
         * Returns formatted string representation of the result.
         * 
         * @return Formatted result summary
         */
        @Override
        public String toString() {
            return String.format("TuningResult[run=%d, dataset=%s, energy=%.1f, " +
                               "paretoSize=%d, time=%.2fs, converged=%s]",
                               runNumber, dataset, maxEnergy/1000, paretoSize, 
                               executionTime, converged);
        }
    }
}


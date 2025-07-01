package src;

import src.optimisation.NSGA2Algorithm;
import src.optimisation.NSGA2Config;
import src.optimisation.Individual;
import src.optimisation.Population;

import src.utils.TideDataReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * Main execution class for NSGA-II optimisation of Swansea Bay Tidal Lagoon.
 * 
 * This class demonstrates the complete optimisation workflow:
 * 1. Load tidal data
 * 2. Configure NSGA-II parameters
 * 3. Run the NSGA-II algorithm
 * 4. Analyse and display results
 * 5. Export Pareto front solutions
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class NSGA2Main {

    private static Map<String, Double> optimisationResults = new HashMap<>();
    private static final String TIDE_DATA_FILE = "data/b1111463.txt";

    public static void main(String[] args) {
        System.out.println("\nStarting NSGA-II optimisation for Swansea Bay Tidal Lagoon...");

        try {
            // Parse command Line arguments for configuration selection
            String configType = (args.length > 0) ? args[0].toLowerCase() : "test";

            // Execute optimisation based on selected configuration
            switch (configType) {
                case "daily":
                    runDailyOptimisation();
                    break;
                case "weekly":
                    runWeeklyOptimisation();
                    break;
                case "annual":
                    runAnnualOptimisation();
                    break;
                case "research":
                    runResearchOptimisation();
                    break;
                case "comparison":
                    runOperatorComparison();
                    break;
            }

        } catch (IOException e) {
            System.err.println("Optimisation failed: " + e.getMessage());
            e.printStackTrace();
        } 

    }

    

    /*
     * Runs daily optimisation for tidal lagoon operation.
     */
    private static void runDailyOptimisation() throws IOException {
        System.out.println("Running daily optimisation...");

        // Load data and configure
        List<Double> tideData = loadTideData();
        NSGA2Config config = NSGA2Config.getDailyConfig();
        List<Double> simulationData = prepareSimulationData(tideData, config);

        // Run optimisation
        NSGA2Algorithm.OptimisationResult result = runOptimisation(config, simulationData);

        double maxEnergy = result.getParetoFront().stream()
            .mapToDouble(Individual::getEnergyOutput).max().orElse(0);
        optimisationResults.put("daily", maxEnergy / 1000); // Store in GWh

        // Analyse results
        analyseResults(result);
        displayParetoFront(result.getParetoFront(), "Daily Optimisation");
        exportResults(result, "results/daily_optimisation_results.txt");
    }

    /*
     * Runs weekly optimisation for tidal lagoon operation.
     */
    private static void runWeeklyOptimisation() throws IOException {
        System.out.println("Running weekly optimisation...");

        // Load data and configure
        List<Double> tideData = loadTideData();
        NSGA2Config config = NSGA2Config.getWeeklyConfig();
        List<Double> simulationData = prepareSimulationData(tideData, config);

        // Run optimisation
        NSGA2Algorithm.OptimisationResult result = runOptimisation(config, simulationData);

        // Analyse results
        analyseResults(result);
        displayParetoFront(result.getParetoFront(), "Weekly Optimisation");
        exportResults(result, "results/weekly_optimisation_results.txt");
    }

    /*
     * Runs annual optimisation for tidal lagoon operation.
     */
    private static void runAnnualOptimisation() throws IOException {
        System.out.println("\nRunning annual optimisation...");

        // Load data and configure
        List<Double> tideData = loadTideData();
        NSGA2Config config = NSGA2Config.getAnnualConfig();
        List<Double> simulationData = prepareSimulationData(tideData, config);

        validateDataRequirements(simulationData.size(), config.getHalfTides());

        // Run optimisation
        NSGA2Algorithm.OptimisationResult result = runOptimisation(config, simulationData);

        // Analyse results
        analyseResults(result);
        displayParetoFront(result.getParetoFront(), "Annual Optimisation");
        exportResults(result, "results/annual_optimisation_results.txt");
    }

    /**
 * Validates data requirements and warns about potential issues
 */
private static void validateDataRequirements(int availableReadings, int halfTides) {
    int requiredReadings = halfTides * 24;
    
    if (availableReadings < requiredReadings) {
        System.out.printf("Warning: Using data cycling. Required: %d, Available: %d%n", 
                         requiredReadings, availableReadings);
        }
    
        System.out.printf("Using %,d readings for %d half-tides%n", availableReadings, halfTides);
    }

    /**
     * Validates energy scaling between different timeframes
     */
    private static void validateEnergyScaling(Map<String, Double> energyResults) {
        if (!energyResults.containsKey("daily") || !energyResults.containsKey("weekly") || 
            !energyResults.containsKey("annual")) {
            return;
        }
        
        double daily = energyResults.get("daily");
        double weekly = energyResults.get("weekly");
        double annual = energyResults.get("annual");
        
        double expectedWeekly = daily * 7;
        double expectedAnnual = daily * 365;
        
        System.out.println("\n=== ENERGY SCALING VALIDATION ===");
        System.out.printf("Daily: %.1f GWh%n", daily);
        System.out.printf("Weekly: %.1f GWh (expected: %.1f)%n", weekly, expectedWeekly);
        System.out.printf("Annual: %.1f GWh (expected: %.1f)%n", annual, expectedAnnual);
        
        double weeklyError = Math.abs(weekly - expectedWeekly) / expectedWeekly;
        double annualError = Math.abs(annual - expectedAnnual) / expectedAnnual;
        
        if (weeklyError > 0.2) {
            System.out.printf("  WARNING: Weekly scaling error: %.1f%%%n", weeklyError * 100);
        }
        if (annualError > 0.2) {
            System.out.printf("  WARNING: Annual scaling error: %.1f%%%n", annualError * 100);
        }
    }

    /*
     * Runs research optimisation for advanced tidal lagoon operation.
     */
    private static void runResearchOptimisation() throws IOException {
        System.out.println("Running research optimisation...");

        // Load data and configure
        List<Double> tideData = loadTideData();
        NSGA2Config config = NSGA2Config.getResearchConfig();
        List<Double> simulationData = prepareSimulationData(tideData, config);

        // Run optimisation
        NSGA2Algorithm.OptimisationResult result = runOptimisation(config, simulationData);

        // Analyse results
        analyseResults(result);
        displayParetoFront(result.getParetoFront(), "Research Optimisation");
        exportResults(result, "results/research_optimisation_results.txt");
    }

    /*
     * Runs operator comparison optimisation for evaluating different NSGA-II operators.
     */
    private static void runOperatorComparison() throws IOException {
        System.out.println("Running genetic operator comparison optimisation...");

        List<Double> tideData = loadTideData();
        String[] crossoverTypes = {"SBX", "UNIFORM", "HALFTIDE"};
        String[] mutationTypes = {"POLYNOMIAL", "GAUSSIAN", "OPERATIONAL"};

        System.out.println("Comparing " + (crossoverTypes.length * mutationTypes.length) + " operator combinations...");

        for (String crossover : crossoverTypes) {
            for (String mutation : mutationTypes) {
                System.out.printf("\n--- Testing %s Crossover + %s Mutation ---\n", crossover, mutation);
                
                NSGA2Config config = NSGA2Config.compareConfigs(crossover, mutation); 
                List<Double> simulationData = prepareSimulationData(tideData, config);
                
                NSGA2Algorithm.OptimisationResult result = runOptimisation(config, simulationData);
                
                // Brief analysis for comparison
                List<Individual> paretoFront = result.getParetoFront();
                double avgEnergy = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).average().orElse(0);
                double avgCost = paretoFront.stream()
                    .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                    .mapToDouble(Individual::getUnitCost).average().orElse(0);
                
                System.out.printf("Result: %d solutions, Avg Energy: %.1f MWh, Avg Cost: \u00A3%.0f/MWh%n",
                                 paretoFront.size(), avgEnergy, avgCost);
            }
        }
    }

    /*
     * Loads tide data from the specified file.
     */
    private static List<Double> loadTideData() throws IOException {
        System.out.println("\nLoading tide data...");
        
        List<Double> tideData = TideDataReader.readTideHeights(TIDE_DATA_FILE);

        if (tideData.isEmpty()) {
            throw new IOException("No tide data found in file: " + TIDE_DATA_FILE);
        }

        double minTide = Collections.min(tideData);
        double maxTide = Collections.max(tideData);

        System.out.printf("Loaded %d tide data points from %s%n", tideData.size(), TIDE_DATA_FILE);
        System.out.printf("Tidal range: %.2f m to %.2f m%n", minTide, maxTide);
        
        return tideData;
    }

    /**
     * Prepares simulation data based on configuration requirements.
     */
    private static List<Double> prepareSimulationData(List<Double> tideData, NSGA2Config config) {
        int requiredReadings = config.getHalfTides() * 24; // 24 readings per half-tide

        if (requiredReadings <= tideData.size()) {
            return tideData.subList(0, requiredReadings);
        } else {
            System.out.printf("Warning: Need %d readings, only %d available%n",
                             requiredReadings, tideData.size());
            System.out.printf("Cycling data to meet requirements%n");

            List<Double> extendedData = new ArrayList<>();
            int cycles = (requiredReadings / tideData.size()) + 1; // Calculate how many times to repeat data

            for (int i = 0; i < cycles; i++) {
                extendedData.addAll(tideData);
            }

            return extendedData.subList(0, requiredReadings);
        }
        
        // List<Double> simulationData = tideData.subList(0, readingsNeeded);
        // System.out.printf("Using %,d readings for %d half-tides%n", 
        //                  simulationData.size(), config.getHalfTides());
        
        // return simulationData;
    }
    
    /**
     * Runs NSGA-II optimisation with given configuration.
     */
    private static NSGA2Algorithm.OptimisationResult runOptimisation(NSGA2Config config, List<Double> simulationData) {
        System.out.println("\nStarting NSGA-II Optimisation...");
        System.out.println(config.getDetailedSummary());
        
        // Validate configuration
        config.validate();
        
        // Create and run algorithm
        NSGA2Algorithm algorithm = new NSGA2Algorithm(config, simulationData);
        NSGA2Algorithm.OptimisationResult result = algorithm.optimise(); 
        
        System.out.println("Optimisation completed!");
        return result;
    }
    
    /**
     * Analyses and displays optimisation results.
     */
    private static void analyseResults(NSGA2Algorithm.OptimisationResult result) {
        System.out.println("\n===Optimisation Results Analysis===");
        
        System.out.println(result);
        
        // Final population statistics
        Population.PopulationStats finalStats = result.finalPopulation.getStatistics();
        System.out.printf("Final Population: %s%n", finalStats);
        
        // Pareto front analysis
        List<Individual> paretoFront = result.getParetoFront();
        System.out.printf("Pareto Front Size: %d solutions%n", paretoFront.size());
        
        if (!paretoFront.isEmpty()) {
            double minEnergy = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).min().orElse(0);
            double maxEnergy = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).max().orElse(0);

            double minEnergyGWh = minEnergy / 1000; // Convert MWh to GWh
            double maxEnergyGWh = maxEnergy / 1000; // Convert MWh to GWh
            
            double minCost = paretoFront.stream()
                .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                .mapToDouble(Individual::getUnitCost).min().orElse(Double.MAX_VALUE);
            double maxCost = paretoFront.stream()
                .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                .mapToDouble(Individual::getUnitCost).max().orElse(0);
            
            System.out.printf("Energy Range: %.1f - %.1f GWh%n", minEnergyGWh, maxEnergyGWh);
            System.out.printf("Cost Range: \u00A3%.0f - \u00A3%.0f per MWh%n", minCost, maxCost);
        }
        
        // Convergence analysis
        if (result.evolutionHistory.size() > 1) {
            NSGA2Algorithm.AlgorithmStats firstGen = result.evolutionHistory.get(0);
            NSGA2Algorithm.AlgorithmStats lastGen = result.evolutionHistory.get(result.evolutionHistory.size() - 1);
            
            System.out.printf("Pareto Front Growth: %d -> %d solutions%n",
                             firstGen.paretoFrontSize, lastGen.paretoFrontSize);
            System.out.printf("Hypervolume Improvement: %.2e -> %.2e%n",
                             firstGen.hypervolume, lastGen.hypervolume);
        }
    }
    
    /**
     * Displays Pareto front solutions in a formatted table.
     */
    private static void displayParetoFront(List<Individual> paretoFront, String title) {
        System.out.printf("\n %s - Pareto Front Solutions%n", title);
        System.out.println("==================================================");
        
        if (paretoFront.isEmpty()) {
            System.out.println("No Pareto front solutions found.");
            return;
        }
        
        System.out.printf("%-8s %-12s %-15s %-10s%n", "Rank", "Energy (GWh)", "Cost (\u00A3/MWh)", "Strategy");
        System.out.println("--------------------------------------------------");
        
        // Sort by energy output for display
        paretoFront.sort((a, b) -> Double.compare(b.getEnergyOutput(), a.getEnergyOutput()));
        
        for (int i = 0; i < Math.min(10, paretoFront.size()); i++) {
            Individual ind = paretoFront.get(i);
            String costStr = (ind.getUnitCost() == Double.MAX_VALUE) ? "Invalid" : 
                           String.format("\u00A3%.0f", ind.getUnitCost());
            
            // Show first few control parameters as strategy indicator
            double avgHs = 0, avgHe = 0;
            int halfTides = ind.getDecisionVariables().length / 2;
            for (int j = 0; j < halfTides; j++) {
                avgHs += ind.getStartHead(j);
                avgHe += ind.getEndHead(j);
            }
            avgHs /= halfTides;
            avgHe /= halfTides;
            
            String strategy = String.format("Hs=%.1f,He=%.1f", avgHs, avgHe);
            double energyGWh = ind.getEnergyOutput() / 1000.0;
            System.out.printf("%-8d %-12.1f %-15s %-10s%n", 
                             i + 1, energyGWh, costStr, strategy);
        }
        
        if (paretoFront.size() > 10) {
            System.out.printf("... and %d more solutions%n", paretoFront.size() - 10);
        }
    }
    
    /**
     * Exports optimisation results to file.
     */
    private static void exportResults(NSGA2Algorithm.OptimisationResult result, String filename) {
        System.out.printf("\n Exporting results to %s...%n", filename);
        
        try {
            // In a real implementation, you would write results to file
            // For now, just indicate what would be exported
            List<Individual> paretoFront = result.getParetoFront();
            
            System.out.printf("Would export:%n");
            System.out.printf("- %d Pareto front solutions%n", paretoFront.size());
            System.out.printf("- %d generations of evolution history%n", result.evolutionHistory.size());
            System.out.printf("- Algorithm configuration and parameters%n");
            System.out.printf("- Performance metrics and convergence data%n");
            
            // TODO: Implement actual file export
            // CSVExporter.exportParetoFront(paretoFront, filename);
            // JSONExporter.exportFullResults(result, filename.replace(".txt", ".json"));
            
            System.out.println("Export completed (simulated)");
            
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
}
        





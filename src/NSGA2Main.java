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
    private static final String TIDE_DATA_FILE = "data/2012MUM.txt";

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
        System.out.println("=== DAILY OPTIMISATION ===");

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
        System.out.println("=== WEEKLY OPTIMISATION ===");

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
        System.out.println("\n=== ANNUAL OPTIMISATION ===\n");

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
   
    /*
     * Loads tide data from the specified file.
     */
    private static List<Double> loadTideData() throws IOException {
        System.out.println("Loading tidal data...\n");
        
        List<Double> tideData = TideDataReader.readTideHeights(TIDE_DATA_FILE);

        if (tideData.isEmpty()) {
            throw new IOException("No tide data found in file: " + TIDE_DATA_FILE);
        }

        double minTide = Collections.min(tideData);
        double maxTide = Collections.max(tideData);

        System.out.printf("Loaded %,d tide data points%n", tideData.size());
        System.out.printf("Tidal range: %.2f m to %.2f m%n", minTide, maxTide);
        
        return tideData;
    }

    /**
     * Prepares simulation data based on configuration requirements.
     */
    private static List<Double> prepareSimulationData(List<Double> tideData, NSGA2Config config) {
        int requiredReadings = config.getHalfTides() * 24; // 24 readings per half-tide

        if (requiredReadings <= tideData.size()) {
            System.out.printf("Using %,d readings for %d half-tides%n", 
                             requiredReadings, config.getHalfTides());
            return tideData.subList(0, requiredReadings);
        } else {
            System.out.printf("Warning: Need %,d readings, only %,d available%n",
                             requiredReadings, tideData.size());
            System.out.printf("Using all available data (%,d readings)%n", tideData.size());
            return new ArrayList<>(tideData);
        }
    }
    
    /**
     * Runs NSGA-II optimisation with given configuration.
     */
    private static NSGA2Algorithm.OptimisationResult runOptimisation(NSGA2Config config, List<Double> simulationData) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("              STARTING NSGA-II OPTIMISATION");
        System.out.println("=".repeat(60));
        System.out.println(config.getDetailedSummary());
        
        // Validate configuration
        config.validate();
        
        // Create and run algorithm
        NSGA2Algorithm algorithm = new NSGA2Algorithm(config, simulationData);
        NSGA2Algorithm.OptimisationResult result = algorithm.optimise(); 
        
        System.out.println("Optimisation completed successfully!");
        return result;
    }
    
    /**
     * Analyses and displays optimisation results.
     */
    private static void analyseResults(NSGA2Algorithm.OptimisationResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("OPTIMISATION RESULTS ANALYSIS");
        System.out.println("=".repeat(60));
        
        // Pareto front analysis
        List<Individual> paretoFront = result.getParetoFront();
        
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
            
            System.out.printf("Execution Time:     %.2f seconds%n", result.executionTimeSeconds);
            System.out.printf("Generations Run:    %d%n", result.generationsRun);
            System.out.printf("Convergence:        %s%n", result.convergenceAchieved ? "Achieved" : "Not achieved");
            System.out.printf("Pareto Front Size:  %d solutions%n", paretoFront.size());
            System.out.printf("Energy Range:       %.1f - %.1f GWh%n", minEnergyGWh, maxEnergyGWh);
            System.out.printf("Cost Range:         \u00A3%.0f - \u00A3%.0f per MWh%n", minCost, maxCost);
        }
        
        // Convergence analysis
        if (result.evolutionHistory.size() > 1) {
            NSGA2Algorithm.AlgorithmStats firstGen = result.evolutionHistory.get(0);
            NSGA2Algorithm.AlgorithmStats lastGen = result.evolutionHistory.get(result.evolutionHistory.size() - 1);
            
            System.out.printf("Improvement:        %d -> %d solutions (Pareto front growth)%n",
                             firstGen.paretoFrontSize, lastGen.paretoFrontSize);
            System.out.printf("Quality Metric:     %.2e -> %.2e (hypervolume)%n",
                             firstGen.hypervolume, lastGen.hypervolume);
        }
    }
    
   /**
     * Displays Pareto front solutions in a formatted table.
     */
    private static void displayParetoFront(List<Individual> paretoFront, String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("PARETO FRONT SOLUTIONS - %s%n", title.toUpperCase());
        System.out.println("=".repeat(60));
        
        if (paretoFront.isEmpty()) {
            System.out.println("No Pareto front solutions found.");
            return;
        }
        
        System.out.printf("%-6s %-14s %-16s %-12s%n", "Rank", "Energy (GWh)", "Cost (\u00A3/MWh)", "Strategy");
        System.out.println("-".repeat(60));
        
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
            
            String strategy = String.format("Hs=%.1f, He=%.1f", avgHs, avgHe);
            double energyGWh = ind.getEnergyOutput() / 1000.0;
            System.out.printf("%-6d %-14.1f %-16s %-12s%n", 
                             i + 1, energyGWh, costStr, strategy);
        }
        
        if (paretoFront.size() > 10) {
            System.out.printf("\n... and %d additional solutions not shown%n", paretoFront.size() - 10);
        }
    }
    
   /**
     * Exports optimisation results to file.
     */
    private static void exportResults(NSGA2Algorithm.OptimisationResult result, String filename) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("              EXPORTING RESULTS");
        System.out.println("=".repeat(60));
        
        try {
            List<Individual> paretoFront = result.getParetoFront();
            
            System.out.printf("Target file:        %s%n", filename);
            System.out.printf("Pareto solutions:   %d%n", paretoFront.size());
            System.out.printf("Evolution history:  %d generations%n", result.evolutionHistory.size());
            System.out.printf("Algorithm config:   Included%n");
            System.out.printf("Performance data:   Included%n");
            
            // TODO: Implement actual file export
            // CSVExporter.exportParetoFront(paretoFront, filename);
            // JSONExporter.exportFullResults(result, filename.replace(".txt", ".json"));
            
            System.out.println("Export completed (simulated)");
            
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
}
        





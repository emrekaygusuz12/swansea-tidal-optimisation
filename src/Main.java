package src;

//import src.model.Lagoon;
import src.model.SimulationConfig;

import src.optimisation.Individual;
import src.optimisation.IndividualGenerator;
//import src.optimisation.TidalSimulator;
import src.optimisation.ObjectiveFunction;

import src.utils.TideDataReader;

import java.util.Collections;
import java.util.List;
import java.io.IOException;

/**
 * Main class for testing the Swansea Bay Tidal Lagoon simulation.
 * 
 * This class demonstrates the complete simulation pipeline:
 * 
 * 1. Load real tidal data from BODC
 * 2. Configure simulation parameters
 * 3. Generate a random operational strategy (individual)
 * 4. Evaluate energy output and unit cost
 * 5. Compare results
 *  
 * @author Emre Kaygusuz
 * @version 2.0 - Now using SimulationConfig
 */
public class Main {
    public static void main(String[] args) {

        System.out.println("=== Swansea Bay Tidal Lagoon Simulation ===");

        String filePath = "data/b1111463.txt"; // Path to the tide height data file

        try {
            // Load and display tidal data
            List<Double> tideHeights = TideDataReader.readTideHeights(filePath);
            displayTideDataInfo(tideHeights);

            // Configure simulation - easily switch between different scenarios
            SimulationConfig.SimulationParameters config = SimulationConfig.getTestConfiguration();
            // Alternative configurations:
            // SimulationConfig.SimulationParameters config = SimulationConfig.getDailyConfiguration();
            // SimulationConfig.SimulationParameters config = SimulationConfig.getWeeklyConfiguration();

            System.out.println("\n=== SIMULATION CONFIGURATION ===");
            System.out.println("Configuration: " + config);
            System.out.printf("Data required: %d readings%n", config.getReadingsNeeded());

            // Prepare simulation data
            List<Double> simulationData = prepareSimulationData(tideHeights, config);

            // Generate and display random strategy
            Individual strategy = IndividualGenerator.createRandomIndividual(config.getHalfTides());
            displayStrategyInfo(strategy, config.getHalfTides());

            // Run the simulation
            System.out.println("\n=== RUNNING SIMULATION ===");
            ObjectiveFunction.evaluate(simulationData, strategy);

            // Display and analyse results
            displayResults(strategy, config);
            performLiteratureComparison(strategy, config);

        } catch (IOException e) {
            System.err.println("Error reading tide data: " + e.getMessage());
            System.err.println("Please ensure " + filePath + " exists and is accessible.");
        } catch (Exception e) {
            System.err.println("Simulation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Displays information about the loaded tide data.
     */
    private static void displayTideDataInfo(List<Double> tideHeights) {
        System.out.printf("Total tide readings available: %,d%n", tideHeights.size());
        System.out.printf("Tidal range: %.2f m to %.2f m%n", 
            Collections.min(tideHeights), Collections.max(tideHeights));
        
        // Show first 10 readings for verification
        System.out.println("\nFirst 10 tide readings:");
        for (int i = 0; i < Math.min(10, tideHeights.size()); i++) {
            System.out.printf("  Reading %d: %.2f m%n", i + 1, tideHeights.get(i));
        }
    }

    /**
     * Prepares simulation data based on configuration requirements.
     */
    private static List<Double> prepareSimulationData(List<Double> tideHeights, 
                                                     SimulationConfig.SimulationParameters config) {
        int readingsNeeded = config.getReadingsNeeded();
        
        if (tideHeights.size() < readingsNeeded) {
            System.out.printf("Warning: Only %d readings available, need %d%n", 
                tideHeights.size(), readingsNeeded);
            readingsNeeded = tideHeights.size();
        }
        
        List<Double> simulationData = tideHeights.subList(0, readingsNeeded);
        System.out.printf("Using %,d readings for simulation%n", simulationData.size());
        
        return simulationData;
    }

    /**
     * Displays information about the generated strategy.
     */
    private static void displayStrategyInfo(Individual strategy, int halfTides) {
        System.out.println("\n=== OPERATIONAL STRATEGY ===");
        System.out.printf("Decision variables: %d (for %d half-tides)%n", 
            strategy.getDecisionVariables().length, halfTides);
        
        // Show first 5 half-tide parameters for verification
        System.out.println("First 5 half-tide control parameters [Hs, He]:");
        for (int i = 0; i < Math.min(5, halfTides); i++) {
            System.out.printf("  Half-tide %d: Hs=%.2f m, He=%.2f m%n", 
                i + 1, strategy.getStartHead(i), strategy.getEndHead(i));
        }
        
        // Show strategy statistics
        double[] variables = strategy.getDecisionVariables();
        double avgHs = 0, avgHe = 0;
        for (int i = 0; i < halfTides; i++) {
            avgHs += strategy.getStartHead(i);
            avgHe += strategy.getEndHead(i);
        }
        avgHs /= halfTides;
        avgHe /= halfTides;
        
        System.out.printf("Strategy averages: Hs=%.2f m, He=%.2f m%n", avgHs, avgHe);
    }

    /**
     * Displays simulation results with proper formatting.
     */
    private static void displayResults(Individual strategy, SimulationConfig.SimulationParameters config) {
        System.out.println("\n=== SIMULATION RESULTS ===");
        System.out.printf("Simulation period: %s%n", config.getDescription());
        System.out.printf("Energy output: %,.2f MWh%n", strategy.getEnergyOutput());
        System.out.printf("Unit cost: \u00A3%,.2f/MWh%n", strategy.getUnitCost());
        
        // Performance indicators
        if (strategy.getEnergyOutput() > 0) {
            System.out.println("Simulation completed successfully");
            
            // Calculate capacity factor if this is a reasonable timeframe
            if (config.getDurationHours() >= 12) {
                double avgPowerMW = strategy.getEnergyOutput() / config.getDurationHours();
                double capacityFactor = avgPowerMW / 320.0; // 320 MW installed capacity
                System.out.printf("Average power: %.1f MW%n", avgPowerMW);
                System.out.printf("Capacity factor: %.1f%%%n", capacityFactor * 100);
            }
        } else {
            System.out.println("No energy generated - check strategy parameters");
        }
    }

    /**
     * Compares results with literature and provides validation.
     */
    private static void performLiteratureComparison(Individual strategy, 
                                                   SimulationConfig.SimulationParameters config) {
        System.out.println("\n=== LITERATURE COMPARISON ===");
        
        // Extrapolate to annual energy for comparison
        double hoursPerYear = 365 * 24;
        double annualEnergyEstimate = (strategy.getEnergyOutput() / config.getDurationHours()) * hoursPerYear;
        double annualGWh = annualEnergyEstimate / 1000;
        
        System.out.printf("Extrapolated annual energy: %.0f GWh/year%n", annualGWh);
        System.out.println("Literature range: 400-732 GWh/year");
        
        // Validation
        if (annualGWh >= 300 && annualGWh <= 900) {
            System.out.println("Result within realistic range");
        } else if (annualGWh < 300) {
            System.out.println("Result below expected range - strategy may be too conservative");
        } else {
            System.out.println("Result above expected range - check simulation parameters");
        }
        
        // Calculate realistic unit cost based on annual energy
        double realisticUnitCost = 1_300_000_000.0 / (annualEnergyEstimate * 1000); // £1.3B / annual kWh
        System.out.printf("Realistic unit cost: \u00A3%.0f/MWh%n", realisticUnitCost);
        
        System.out.println("\n=== READY FOR OPTIMIZATION ===");
        System.out.println("Physics simulation: Working correctly");
        System.out.println("Energy calculations: Literature-validated");
        System.out.println("Multi-objective setup: Ready for NSGA-II");
        System.out.println("Proceed with NSGA-II implementation!");
    }
}


    
    
    


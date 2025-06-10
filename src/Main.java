package src;

import src.optimisation.Individual;
import src.optimisation.IndividualGenerator;
import src.optimisation.TidalSimulator;
import src.utils.TideDataReader;
import src.optimisation.ObjectiveFunction;

import java.util.Collections;
import java.util.List;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
    
        String filePath = "data/b1111463.txt"; // Path to the tide height data file

        try {
            List<Double> tideHeights = TideDataReader.readTideHeights(filePath);
            System.out.printf("Total readings: %d%n", tideHeights.size());
            System.out.printf("Min: %.2f m, Max: %.2f m%n",
            Collections.min(tideHeights), Collections.max(tideHeights));

            int numberOfHalfTides = 48;
            int readingsPerHalfTide = 24;
            int totalReadingsNeeded = numberOfHalfTides * readingsPerHalfTide;

            List<Double> testTideData = tideHeights.subList(0, Math.min(totalReadingsNeeded, tideHeights.size()));
            System.out.printf("Using %d readings for %d half-tides (12 hours simulation)%n", testTideData.size(), numberOfHalfTides);

            System.out.println("\nFirst 20 tide readings:");
            for (int i =0; i< Math.min(20, tideHeights.size()); i++) {
                System.out.printf("Tide %d: %.2f m%n", i + 1, tideHeights.get(i));
            }

            // Create a individual for testing
            Individual ind = IndividualGenerator.createRandomIndividual(numberOfHalfTides);
            
            System.out.println("\nRandom Individual Strategy:");
            System.out.printf("Number of decision variables: %d%n", ind.getDecisionVariables().length);
            System.out.printf("Strategy covers %d half-tides%n", numberOfHalfTides);
            
            // Show first few control parameters for verification
            System.out.println("First 5 half-tide control parameters [Hs, He]:");
            for (int i = 0; i < Math.min(5, numberOfHalfTides); i++) {
                System.out.printf("Half-tide %d: Hs=%.2f m, He=%.2f m%n", 
                    i+1, ind.getStartHead(i), ind.getEndHead(i));
            }

            // Run simulation with corrected data
            ObjectiveFunction.evaluate(testTideData, ind);
            
            System.out.println("\n=== SIMULATION RESULTS ===");
            System.out.printf("Simulation period: 12 hours (%d half-tides)%n", numberOfHalfTides);
            System.out.printf("Energy Output: %.2f MWh%n", ind.getEnergyOutput());
            System.out.printf("Unit Cost of Energy: %.2f GBP/MWh%n", ind.getUnitCost());
            
            // Extrapolate to annual estimate for comparison
            double dailyEnergy = ind.getEnergyOutput() * 2; // 12 hours * 2 = 24 hours
            double annualEnergy = dailyEnergy * 365; // Annual estimate
            System.out.printf("Extrapolated annual energy: %.0f GWh%n", annualEnergy / 1000);
            System.out.printf("Literature range: 400-732 GWh/year%n");
            
            // Performance verification
            if (ind.getEnergyOutput() > 0 && ind.getEnergyOutput() < 1000) {
                System.out.println(" Energy output looks realistic for 12-hour period");
            } else {
                System.out.println(" Energy output may need verification");
            }

        } catch (IOException e) {
            System.err.println("Error reading tide heights: " + e.getMessage());
        }
    }
}

    
    
    


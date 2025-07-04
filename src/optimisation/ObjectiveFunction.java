package src.optimisation;

import src.model.Lagoon;
import java.util.List;

/**
 * Represents the objective function for the optimisation of the Swansea Bay Tidal Lagoon.
 * 
 * VERSION 2.0 - CRITICAL OBJECTIVE FUNCTION FIXES
 * 
 * Evaluates two conflicting objectives:
 * 1. Maximise annual energy output (GWh/year)
 * 2. Minimise unit cost of energy (GBP/MWh)
 * 
 * MAJOR FIX in v2.0:
 * Issue 1: Perfect objective correlation - Fixed with operational penalties
 * Issue 2: Incorrect time scaling - Fixed to 6.12 hours per half-tide
 * Issue 3: Cost component imbalance -Fixed with balanced penalties
 * 
 * @author Emre Kaygusuz
 * @version 1.1
 */
public class ObjectiveFunction {

    private static final int HOURS_IN_YEAR = 365 * 24; 
    private static final double HOURS_PER_HALF_TIDE = 6.12; 
    private static final double EXPECTED_MIN_OUTPUT = 400.0 * 1000.0; // Expected minimum annual output in MWh (400 GWh)
    //private static final double EXPECTED_MAX_OUTPUT = 800.0 * 1000.0; // Expected maximum annual output in MWh (800 GWh)

    public static void evaluate(List<Double>tideHeights, Individual individual) {
        // Objective 1: Simulate energy output for the given period and annualise it
        double periodEnergyOutput =TidalSimulator.simulate(tideHeights, individual);

        // Calculate simluation period in hours
        int simulationHours = calculateSimulationHours(individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours); 

        // CRITICAL DEBUG - Track the relationship
        System.out.printf("PERIOD ANALYSIS: HalfTides=%d, Hours=%d, Days=%.1f, PeriodEnergy=%.1f MWh, AnnualEnergy=%.1f MWh%n",
                     individual.getDecisionVariables().length / 2, 
                     simulationHours, 
                     simulationHours/24.0,
                     periodEnergyOutput, 
                     annualEnergyOutput);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput); 

        // Objective 2: Calculate unit cost of energy
        double operationalComplexityCost = calculateOperationalComplexityCost(individual, annualEnergyOutput);
        individual.setUnitCost(operationalComplexityCost);
    }

    /**
     * Calculates the simulation period in hours based on the number of half tides.
     * Each half tide represents approximately 12 hours of tidal flow.
     * 
     * @param periodEnergyOutput Energy output for the simulation period in MWh
     * @param simulationHours Total hours of the simulation period
     * @return Annualised energy output in MWh
     */
    private static int calculateSimulationHours(Individual individual) {
        int halfTides = individual.getDecisionVariables().length / 2; // Each decision variable represents a half tide
        int currentHours = (int) (halfTides * HOURS_PER_HALF_TIDE); // 6.12 hours per half tide
        return currentHours;
    }


    /**
     * Extrapolates period energy output to annual energy output.
     * 
     * 
     * @param periodEnergyOutput Energy output for the simulation period in MWh
     * @param simulationHours Duration of simulation period in hours
     * @return Annualised energy output in MWh/year
     */
    private static double extrapolate(double periodEnergyOutput, int simulationHours) {
        // Annualise the energy output based on the simulation period
        if (simulationHours <= 0) {
            return 0.0; // Avoid division by zero
        }


        double annualisationFactor = (double) HOURS_IN_YEAR / simulationHours;
        double annualEnergyOutput = periodEnergyOutput * annualisationFactor;

        if (simulationHours >= HOURS_IN_YEAR) {
        System.out.printf("LONG SIMULATION: %d hours (%.1f days) scaled DOWN by %.3fx to annual: %.1f MWh%n", 
                         simulationHours, simulationHours/24.0, annualisationFactor, annualEnergyOutput);
        } else {
            System.out.printf("SHORT SIMULATION: %d hours (%.1f days) scaled UP by %.3fx to annual: %.1f MWh%n",
                             simulationHours, simulationHours/24.0, annualisationFactor, annualEnergyOutput);
        }


        return annualEnergyOutput; // Annualised energy output in MWh
    }

    private static double calculateOperationalComplexityCost(Individual individual, double annualEnergyOutput) {
        if (annualEnergyOutput <= 0) {
            return Double.MAX_VALUE;
        }

        // Drastically reduce capital cost component
        double baseCapitalCost = Lagoon.getTotalCapitalCost() / (annualEnergyOutput * 500.0);

        // Calculate average head across all half-tides
        double avgHead = calculateAverageHead(individual);
        int halfTides = individual.getDecisionVariables().length / 2;
        for (int i = 0; i < halfTides; i++) {
            avgHead += individual.getStartHead(i) + individual.getEndHead(i);
        }
        avgHead /= (halfTides * 2);

        // AGGRESSIVE operational penalty that increases dramatically with energy
        double operationalPenalty = Math.pow(avgHead, 2.0) * 200.0;
        
        // Energy-dependent penalty that creates strong trade-off
        double energyPenalty = Math.pow(annualEnergyOutput / EXPECTED_MIN_OUTPUT, 1.5) * 1000.0;

        double operationalComplexityCost = baseCapitalCost + operationalPenalty + energyPenalty;

        // Add capacity utilization penalty to bias toward higher energy solutions
        double avgPowerMW = annualEnergyOutput / 8760.0; // Annual hours
        double capacityUtilization = avgPowerMW / 320.0; // 320 MW capacity
        if (capacityUtilization < 0.4) { // Below 40% utilization
            operationalComplexityCost *= (1.0 + (0.4 - capacityUtilization) * 2.0); // Penalty
        }

        return operationalComplexityCost;
    }

    private static double calculateAverageHead(Individual individual) {
        double avgHead = 0;
        int halfTides = individual.getDecisionVariables().length / 2;
        for (int i = 0; i < halfTides; i++) {
            avgHead += individual.getStartHead(i) + individual.getEndHead(i);
        }
        double averageHead = avgHead / (halfTides * 2);
        return averageHead;
    }

    public static void evaluate(List<Double> tideHeights, Individual individual, int simulationHours) {
        // Objective 1: Simulate energy output for the given period and annualise it
        double periodEnergyOutput = TidalSimulator.simulate(tideHeights, individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput);

        // Objective 2: Calculate unit cost of energy
        double operationalComplexityCost = calculateOperationalComplexityCost(individual, annualEnergyOutput);
        individual.setUnitCost(operationalComplexityCost);
    }

}

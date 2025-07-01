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

        // CRITICAL DEBUG OUTPUT - ADD THIS
        System.out.printf("ENERGY DEBUG: Period=%.1f MWh, Annual=%.1f MWh, " +
                     "SimHours=%d, Factor=%.1fx%n", 
                     periodEnergyOutput, annualEnergyOutput, 
                     simulationHours, (double)HOURS_IN_YEAR / simulationHours);

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
        return (int) (halfTides * HOURS_PER_HALF_TIDE); // 6.12 hours per half tide
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
        return periodEnergyOutput * annualisationFactor; // Annualised energy output in MWh
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

        return baseCapitalCost + operationalPenalty + energyPenalty;
    }

    private static double calculateAverageHead(Individual individual) {
        double avgHead = 0;
        int halfTides = individual.getDecisionVariables().length / 2;
        for (int i = 0; i < halfTides; i++) {
            avgHead += individual.getStartHead(i) + individual.getEndHead(i);
        }
        return avgHead / (halfTides * 2);
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

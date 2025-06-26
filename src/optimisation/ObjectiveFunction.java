package src.optimisation;

import src.model.Lagoon;

import java.util.List;


/**
 * Represents the objective function for the optimisation of the Swansea Bay Tidal Lagoon.
 * 
 * VERSION 1.1 - CRITICAL UNIT COST FIX
 * 
 * Evaluates two conflicting objectives:
 * 1. Maximise annual energy output (GWh/year)
 * 2. Minimise unit cost of energy (GBP/MWh)
 * 
 * MAJOR FIX in v1.1: The unit cost is not calculated using properly annualised energy output.
 * Previous version inccorrectly used simulation period energy which resulted in unrealistic
 * unit costs.
 * 
 * The unit cost calculation creates a natural trade-off where strategies that generate
 * more energy have lower unit costs, but may require complex operational strategies.
 * 
 * Based on Swansea Bay Tidal Lagoon parameters from published literature and project proposal.
 * 
 * @author Emre Kaygusuz
 * @version 1.1
 */
public class ObjectiveFunction {

    public static void evaluate(List<Double>tideHeights, Individual individual) {
        // Objective 1: Simulate energy output for the given period and annualise it
        double periodEnergyOutput =TidalSimulator.simulate(tideHeights, individual);

        // Calculate simluation period in hours
        int simulationHours = calculateSimulationHours(individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours);

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
        return (int) (halfTides * 6.12); // 12 hours per half tide
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
        
        final int HOURS_IN_YEAR = 365 * 24; // Total hours in a year
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
        double energyPenalty = Math.pow(annualEnergyOutput / 400000.0, 1.5) * 1000.0;

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

    private static void validateEnergyOutput(double annualEnergyOutput) {
        double expectedMinOutput = 400.0 * 1000;
        double expectedMaxOutput = 800.0 * 1000;
        
        if (annualEnergyOutput < expectedMinOutput) {
        System.err.printf("WARNING: Energy output %.1f MWh is below expected minimum %.1f MWh%n", 
                         annualEnergyOutput, expectedMinOutput);
        }
    
        if (annualEnergyOutput > expectedMaxOutput) {
        System.err.printf("WARNING: Energy output %.1f MWh exceeds expected maximum %.1f MWh%n", 
                         annualEnergyOutput, expectedMaxOutput);
    }
    }

    /**
     * Calculates operational complexity based on control strategy characteristics.
     */
    private static double calculateOperationalComplexity(Individual individual) {
        double complexity = 0.0;
        int halfTides = individual.getDecisionVariables().length / 2;
        
        for (int i = 0; i < halfTides; i++) {
            double hs = individual.getStartHead(i);
            double he = individual.getEndHead(i);
            
            // Higher head values increase operational complexity
            complexity += (hs * hs + he * he) * 0.1;
            
            // Large head differences increase complexity (more aggressive operation)
            complexity += Math.abs(hs - he) * 0.5;
            
            // Frequent operational changes increase complexity
            if (i > 0) {
                double prevHs = individual.getStartHead(i-1);
                double prevHe = individual.getEndHead(i-1);
                complexity += (Math.abs(hs - prevHs) + Math.abs(he - prevHe)) * 0.3;
            }
            
            // Very high head values create exponential complexity
            if (hs > 3.0 || he > 3.0) {
                complexity += Math.pow(Math.max(hs, he) - 3.0, 2) * 2.0;
            }
        }
        
        return complexity / halfTides; // Normalize by number of half-tides
    }

    /**
     * Calculates equipment wear cost based on operational intensity.
     */
    private static double calculateEquipmentWearCost(Individual individual) {
        double wearCost = 0.0;
        int halfTides = individual.getDecisionVariables().length / 2;
        
        for (int i = 0; i < halfTides; i++) {
            double hs = individual.getStartHead(i);
            double he = individual.getEndHead(i);
            
            // Higher heads create more wear (exponential relationship)
            wearCost += Math.pow(hs, 1.5) * 0.8 + Math.pow(he, 1.5) * 0.8;
            
            // Rapid operational changes increase wear
            if (i > 0) {
                double prevHe = individual.getEndHead(i-1);
                wearCost += Math.abs(hs - prevHe) * 1.2; // Cost of switching
            }
        }
        
        return wearCost * 0.5; // Scale to reasonable cost range
    }

     /**
     * Calculates environmental impact cost.
     */
    private static double calculateEnvironmentalImpactCost(Individual individual) {
        double impactCost = 0.0;
        int halfTides = individual.getDecisionVariables().length / 2;
        
        for (int i = 0; i < halfTides; i++) {
            double hs = individual.getStartHead(i);
            double he = individual.getEndHead(i);
            
            // Higher heads have greater environmental impact
            impactCost += (hs + he) * 0.8;
            
            // Large head differences disrupt ecosystem more
            impactCost += Math.abs(hs - he) * 1.0;
        }
        
        return impactCost * 0.3; // Scale to reasonable cost range
    }

    /**
     * Calculates the unit cost of energy based on capital cost efficiency.
     * 
     * This approach uses total capital cost divided by annual energy output to
     * create a cost metric in GBP per MWh. Strategies that generate more energy
     * will naturally have lower unit costs, creating the desired trade-off for
     * optimisation.
     * 
     * Uses Swansea Bay Capital Cost of £1.3 billion from published literature.
     * No lifetime assumptions are made to avoid unsubstantiated parameters
     * as the focus is on capital cost efficiency.
     * 
     * @param annualEnergyMWh Annual energy output in MWh
     * @return Unit cost in GBP per MWh, or Double.MAX_VALUE if annual energy is zero or negative
     */
    private static double calculateUnitCost(double annualEnergyMWh) {
        if (annualEnergyMWh <= 0) {
            return Double.MAX_VALUE; 
        }

        double TOTAL_CAPITAL_COST = Lagoon.getTotalCapitalCost();
        return TOTAL_CAPITAL_COST / annualEnergyMWh; // GBP per MWh
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

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
        double annualEnergyOutput = Extrapolate(periodEnergyOutput, simulationHours);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput);

        // Objective 2: Calculate unit cost of energy
        double unitCost = calculateUnitCost(annualEnergyOutput);
        individual.setUnitCost(unitCost);
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
        return halfTides * 12; // 12 hours per half tide
    }


    /**
     * Extrapolates period energy output to annual energy output.
     * 
     * 
     * @param periodEnergyOutput Energy output for the simulation period in MWh
     * @param simulationHours Duration of simulation period in hours
     * @return Annualised energy output in MWh/year
     */
    private static double Extrapolate(double periodEnergyOutput, int simulationHours) {
        // Annualise the energy output based on the simulation period
        if (simulationHours <= 0) {
            return 0.0; // Avoid division by zero
        }
        
        final int HOURS_IN_YEAR = 365 * 24; // Total hours in a year
        double annualisationFactor = (double) HOURS_IN_YEAR / simulationHours;
        return periodEnergyOutput * annualisationFactor; // Annualised energy output in MWh
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
        double annualEnergyOutput = Extrapolate(periodEnergyOutput, simulationHours);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput);

        // Objective 2: Calculate unit cost of energy
        double unitCost = calculateUnitCost(annualEnergyOutput);
        individual.setUnitCost(unitCost);
    }

}

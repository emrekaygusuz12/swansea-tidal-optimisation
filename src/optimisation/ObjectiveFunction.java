package src.optimisation;

import src.model.Lagoon;

import java.util.List;


/**
 * Represents the objective function for the optimisation of the Swansea Bay Tidal Lagoon.
 * 
 * Evaluates two conflicting objectives:
 * 1. Maximise annual energy output (MWh/year)
 * 2. Minimise unit cost of energy (GBP/MWh)
 * 
 * The unit cost is calculated as total capital cost divided by the annual energy output,
 * creating a natural trade-off where strategies that generate more energy have lower unit costs,
 * but may require complex operational strategies.
 * 
 * Based on Swansea Bay Tidal Lagoon parameters from published literature and project proposal.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class ObjectiveFunction {

    public static void evaluate(List<Double>tideHeights, Individual individual) {
        // Objective 1: Simulate annual energy output
        double annualEnergyOutput =TidalSimulator.simulate(tideHeights, individual);
        individual.setEnergyOutput(annualEnergyOutput);

        // Objective 2: Calculate unit cost of energy
        double unitCost = calculateUnitCost(annualEnergyOutput);
        individual.setUnitCost(unitCost);
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

}

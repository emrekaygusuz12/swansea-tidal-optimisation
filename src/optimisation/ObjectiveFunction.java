package src.optimisation;

import src.model.Lagoon;

import java.util.List;

public class ObjectiveFunction {


    /**
     * Evaluates an individual based on:
     * 
     * * 1. Simulating energy output (MWh).
     * * 2. Calculating the unit cost of energy (£/MWh).
     * 
     * Updates the individual's fields directly.
     * 
     * @param tideHeights
     * @param individual
     */
    public static void evaluate(List<Double> tideHeights, Individual individual){
        // Step 1: Simulate energy output
        double energyOutput = TidalSimulator.simulate(tideHeights, individual);
        individual.setEnergyOutput(energyOutput);

        // Step 2: Calculate unit cost of energy
        double capitalCost = Lagoon.getTotalCapitalCost();
        double unitCost = energyOutput > 0 ? capitalCost / energyOutput : Double.MAX_VALUE;
        individual.setUnitCost(unitCost);


    }

    
}

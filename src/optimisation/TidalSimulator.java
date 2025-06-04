package src.optimisation;

import src.model.Lagoon;
import java.util.List;

public class TidalSimulator {

    private static final double WATTS_TO_MW = 1e-6; // Conversion factor from Watts to MW (1,000,000 W = 1 MW)
    private static final double GRAVITY = 9.81; // Acceleration due to gravity in m/s^2


    public static double simulate(List<Double> tideHeights, Individual individual) {

        final double timeStepHours = 0.25; // Time step in hours (15 minutes)
        
        double totalEnergyOutput = 0.0; // Total energy output in MWh
        double lagoonLevel = tideHeights.get(0); // starting with sea level
        int halfTideCount = individual.getDecisionVariables().length / 2; // Each half-tide has two control parameters (Hs and He)

        int stepPerHalfTide = tideHeights.size() / halfTideCount; // Number of steps per half-tide

        for (int i = 0; i < halfTideCount; i++) {
            double Hs = individual.getStartHead(i); // Starting head for half-tide i
            double He = individual.getEndHead(i); // Ending head for half-tide i

            for (int j = 0; j < stepPerHalfTide; j++) {
                int tideIndex = i * stepPerHalfTide + j;
                if (tideIndex >= tideHeights.size()) {
                    break; // Prevent out-of-bounds access
                }
                
               double seaLevel = tideHeights.get(tideIndex);
               double headDifference = Math.abs(seaLevel - lagoonLevel);
                
               boolean isTurbineActive = headDifference >= Hs;
               if (isTurbineActive && headDifference >= He) {

                    double flow = Lagoon.getSluiceAreaM2() * Math.sqrt(2 * GRAVITY * headDifference); // Q = A * sqrt(2gh)
                    double powerMW = flow * headDifference * WATTS_TO_MW;  // p * g * Q * h * η
                    powerMW *= Lagoon.getTurbineDischargeCoefficient(); // apply efficiency once


                    double energy = powerMW * timeStepHours; // Energy in MWh
                    totalEnergyOutput += energy; // Accumulate energy output

                    lagoonLevel += (seaLevel > lagoonLevel) ? 0.01 : -0.01; // Update lagoon level based on flow
               }

            }
        }

        return totalEnergyOutput; // Return the total energy output in MWh
    }
    
}

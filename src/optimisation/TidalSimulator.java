package src.optimisation;

import src.model.Lagoon;
import java.util.List;

public class TidalSimulator {

    private static final double WATTS_TO_MW = 1e-6; // Conversion factor from Watts to MW (1,000,000 W = 1 MW)
    private static final double GRAVITY = 9.81; // Acceleration due to gravity in m/s^2
    private static final double WATER_DENSITY = 1025.0; // Density of water in kg/m^3
    private static final double TIME_STEP_HOURS = 0.25; // Time step in hours (15 minutes)
    private static final double TIME_STEP_SECONDS = TIME_STEP_HOURS * 3600; // Convert hours to seconds

    private static final double MIN_LAGOON_LEVEL = -5.0; // Minimum lagoon level in meters
    private static final double MAX_LAGOON_LEVEL = 10.0; // Maximum lagoon level in meters




    public static double simulate(List<Double> tideHeights, Individual individual) {

        double lagoonSurfaceArea = Lagoon.getLagoonSurfaceAreaM2(); // Lagoon surface area in m^2


        double turbineRadius = Lagoon.getTurbineDiameterM() / 2.0; // Radius of a single turbine in meters
        double turbineArea = Math.PI * Math.pow(turbineRadius, 2); // Area of a single turbine in m^2
        double totalTurbineArea = turbineArea * Lagoon.getNumberOfTurbines(); // Total area of all turbines in m^2

        
        double totalEnergyOutput = 0.0; // Total energy output in MWh
        double lagoonLevel = tideHeights.get(0); // starting with sea level
        int halfTideCount = individual.getDecisionVariables().length / 2; // Each half-tide has two control parameters (Hs and He)

        int stepPerHalfTide = tideHeights.size() / halfTideCount; // Number of steps per half-tide

        for (int i = 0; i < halfTideCount; i++) {
            double Hs = individual.getStartHead(i); // Starting head for half-tide i
            double He = individual.getEndHead(i); // Ending head for half-tide i

            boolean turbineOn = false; // Flag to check if turbine is active

            for (int j = 0; j < stepPerHalfTide; j++) {
                int tideIndex = i * stepPerHalfTide + j;
                if (tideIndex >= tideHeights.size()) {
                    break; // Prevent out-of-bounds access
                }
                
               double seaLevel = tideHeights.get(tideIndex);
               double headDifference = Math.abs(seaLevel - lagoonLevel);

                
               //boolean isTurbineActive = headDifference >= Hs;
               if (!turbineOn && headDifference >= Hs) {
                    turbineOn = true; // Activate the turbine
               }

               if (turbineOn && headDifference < He) {
                    turbineOn = false; // Deactivate the turbine if head difference exceeds He
               }

               if (turbineOn) {

                    double flowDirection = (seaLevel > lagoonLevel) ? 1.0 : -1.0; // Determine flow direction based on sea level


                    double flow = totalTurbineArea * Math.sqrt(2 * GRAVITY * headDifference); // Q = A * sqrt(2gh)
                    flow *= flowDirection; // Adjust flow direction based on sea level

                    double efficiency = Lagoon.getTurbineDischargeCoefficient(); // Efficiency of the turbine
                    double powerMW = Math.abs(flow) * WATER_DENSITY * GRAVITY * headDifference * efficiency; // Power (W) = rho * g * Q * h * efficiency

                    powerMW *= WATTS_TO_MW; // Convert Watts to MW

                    double maxPowerMW = Lagoon.getInstalledCapacityMW();
                    powerMW = Math.min(powerMW, maxPowerMW); // Limit power to installed capacity

                    double energy = powerMW * TIME_STEP_HOURS; // Energy in MWh
                    totalEnergyOutput += energy; // Accumulate energy output

                    double volumeChange = Math.abs(flow) * TIME_STEP_SECONDS; // Volume change in m^3
                    double levelChange = volumeChange / lagoonSurfaceArea; // Change in lagoon level in meters
                    lagoonLevel += levelChange * flowDirection; // Update lagoon level
                    lagoonLevel = Math.max(MIN_LAGOON_LEVEL, Math.min(lagoonLevel, MAX_LAGOON_LEVEL)); // Clamp lagoon level within bounds
               }

            }
        }

        return totalEnergyOutput; // Return the total energy output in MWh
    }
    
}

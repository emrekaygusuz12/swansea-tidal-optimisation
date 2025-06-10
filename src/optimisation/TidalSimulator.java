package src.optimisation;

import src.model.Lagoon;
import java.util.List;


/**
 * Simulates the operation of a tidal lagoon using a 0D model approach.
 * 
 * This class implements the physics of tidal lagoon operation including:
 * - Orifice theory for flow calculations
 * - Mass conservation for lagoon level updates
 * - Power generation calculations
 * - Two-way (bidirectional) turbine operation
 * 
 * Uses backward-difference method for temporal discretisation.
 * 
 * @author Emre Kaygusuz
 * @version 1.1
 */
public class TidalSimulator {

    private static final double WATTS_TO_MW = 1e-6; // Conversion factor from Watts to MW (1,000,000 W = 1 MW)
    private static final double GRAVITY = 9.81; // Acceleration due to gravity in m/s^2
    private static final double WATER_DENSITY = 1025.0; // Density of water in kg/m^3
    private static final double TIME_STEP_HOURS = 0.25 / 24; // Time step in hours (15 minutes)
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

               // Ensure head difference is positive for sqrt calculation
                if (headDifference < 0) {
                    System.err.println("Warning: Negative head difference detected. Setting to 0.");
                    headDifference = 0.0; 
                } 


               //boolean isTurbineActive = headDifference >= Hs;
               if (!turbineOn && headDifference >= Hs) {
                    turbineOn = true; // Activate the turbine
               }

               if (turbineOn && headDifference < He) {
                    turbineOn = false; // Deactivate the turbine if head difference exceeds He
               }

               if (turbineOn && headDifference > 0) { 

                    // Calculate theoretical flow rate using orifice equation: Q = A * sqrt(2gh)
                    // where A is the area of the turbine, g is gravity, and h is the head difference
                    double theoreticalFlow = totalTurbineArea * Math.sqrt(2 * GRAVITY * headDifference); // Flow rate in m^3/s

                    // Apply discharge coefficient for turbine efficiency
                    double dischargeCoefficient = Lagoon.getTurbineDischargeCoefficient(); // Efficiency of the turbine
                    double actualFlow = theoreticalFlow * dischargeCoefficient; // Adjust flow rate for efficiency

                    // Calculate power using: P = water density * g * Q * h * efficiency
                    double powerMW = actualFlow * WATER_DENSITY * GRAVITY * headDifference * WATTS_TO_MW; 
                    
                    // Limit power to installed capacity
                    double maxPowerMW = Lagoon.getInstalledCapacityMW();
                    powerMW = Math.min(powerMW, maxPowerMW); // Limit power to installed capacity

                    // Calculate energy for this time step
                    double energy = powerMW * TIME_STEP_HOURS; // Energy in MWh
                    totalEnergyOutput += energy; // Accumulate energy output

                    // Corrected flow direction logic
                    // Calculate volume change and update lagoon lvel based on flow direction

                    double volumeChange = actualFlow * TIME_STEP_SECONDS; // Volume change in m^3
                    double levelChange = volumeChange / lagoonSurfaceArea; // Change in lagoon level in meters

                    // Flow direction determines level change:
                    if (seaLevel > lagoonLevel) {
                        // Water flows into the lagoon (level rises)
                        lagoonLevel += levelChange; // Ensure positive change
                    } else {
                        // Water flows out of the lagoon (level falls)
                       lagoonLevel -= levelChange; // Ensure negative change
                    }

                    // Clamp lagoon level within bounds
                    lagoonLevel = Math.max(MIN_LAGOON_LEVEL, Math.min(lagoonLevel, MAX_LAGOON_LEVEL)); // Ensure lagoon level stays within bounds
               }

            }
        }

        return totalEnergyOutput; // Return the total energy output in MWh
    }
    
}

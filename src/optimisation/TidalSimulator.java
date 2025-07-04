package src.optimisation;

import src.model.Lagoon;
import src.model.SimulationConfig;

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
    private static final double TIME_STEP_HOURS = SimulationConfig.getTimeStepHours(); // Time step in hours (15 minutes)
    private static final double TIME_STEP_SECONDS = TIME_STEP_HOURS * 3600; // Convert hours to seconds

    private static final double MIN_LAGOON_LEVEL = -5.0; // Minimum lagoon level in meters
    private static final double MAX_LAGOON_LEVEL = 10.0; // Maximum lagoon level in meters

    private static final boolean ENABLE_DETAILED_DEBUG = true;
    private static final boolean ENABLE_SUMMARY_DEBUG = true;




    public static double simulate(List<Double> tideHeights, Individual individual) {

        // Lagoon and turbine parameters
        double lagoonSurfaceArea = Lagoon.getLagoonSurfaceAreaM2(); // Lagoon surface area in m^2
        double turbineRadius = Lagoon.getTurbineDiameterM() / 2.0; // Radius of a single turbine in meters
        double turbineArea = Math.PI * Math.pow(turbineRadius, 2); // Area of a single turbine in m^2
        double totalTurbineArea = turbineArea * Lagoon.getNumberOfTurbines(); // Total area of all turbines in m^2
        double maxPowerMW = Lagoon.getInstalledCapacityMW(); // Maximum power output in MW
        double dischargeCoefficient = Lagoon.getTurbineDischargeCoefficient(); // Discharge coefficient for turbine efficiency

        // Simulation state
        double totalEnergyOutput = 0.0; // Total energy output in MWh
        double lagoonLevel = tideHeights.get(0); // starting with sea level
        int halfTideCount = individual.getDecisionVariables().length / 2; // Each half-tide has two control parameters (Hs and He)
        int stepPerHalfTide = tideHeights.size() / halfTideCount; // Number of steps per half-tide

        // Debug tracking variables
        int totalTimeSteps = 0;
        int turbineActiveSteps = 0;
        double totalPowerGenerated = 0.0;
        double maxPowerSeen = 0.0;
        double totalHeadDifferenceWhenActive = 0.0;
        int activeStepsWithPower = 0;
        double totalFlowVolume = 0.0; // Total volume of water moved in m^3
        double maxHeadSeen = 0.0; // Maximum head difference seen during simulation

        // Energy tracking per half-tide
        double[] halfTideEnergies = new double[halfTideCount];

        if (ENABLE_DETAILED_DEBUG) {
            System.out.printf("=== SIMULATION START DEBUG ===\n");
            System.out.printf("Time step: %.3f hours (%.1f minutes)\n", TIME_STEP_HOURS, TIME_STEP_HOURS * 60);
            System.out.printf("Half-tides: %d, Steps per half-tide: %d\n", halfTideCount, stepPerHalfTide);
            System.out.printf("Total turbine area: %.1f m², Max power: %.1f MW\n", totalTurbineArea, maxPowerMW);
            System.out.printf("Discharge coefficient: %.2f\n", dischargeCoefficient);
            System.out.println("===============================");
        }

        for (int i = 0; i < halfTideCount; i++) {
            double Hs = individual.getStartHead(i); // Starting head for half-tide i
            double He = individual.getEndHead(i); // Ending head for half-tide i
            boolean turbineOn = false; // Flag to check if turbine is active
            double halfTideEnergy = 0.0; // Energy for this half-tide

            for (int j = 0; j < stepPerHalfTide; j++) {
                int tideIndex = i * stepPerHalfTide + j;
                if (tideIndex >= tideHeights.size()) {
                    break; // Prevent out-of-bounds access
                }

                totalTimeSteps++; // Increment total time steps (ADDED FIX)
                double seaLevel = tideHeights.get(tideIndex);
                double headDifference = Math.abs(seaLevel - lagoonLevel);

                maxHeadSeen = Math.max(maxHeadSeen, headDifference); // Track maximum head difference
            

               //boolean isTurbineActive = headDifference >= Hs;
               if (!turbineOn && headDifference >= Hs) {
                    turbineOn = true; // Activate the turbine
                    if (ENABLE_DETAILED_DEBUG) {
                        System.out.printf("Half-tide %d, Step %d: Turbine ON (Head: %.2f >= Hs: %.2f)\n", 
                            i, j, headDifference, Hs);
                    }
               }

               if (turbineOn && headDifference < He) {
                    turbineOn = false; // Deactivate the turbine if head difference exceeds He
                    if (ENABLE_DETAILED_DEBUG) {
                        System.out.printf("Half-tide %d, Step %d: Turbine OFF (Head: %.2f < He: %.2f)\n", 
                            i, j, headDifference, He);
                    }
               }

               if (turbineOn && headDifference > 0) {
                    turbineActiveSteps++; 

                    // Calculate theoretical flow rate using orifice equation: Q = A * sqrt(2gh)
                    // where A is the area of the turbine, g is gravity, and h is the head difference
                    double theoreticalFlow = totalTurbineArea * Math.sqrt(2 * GRAVITY * headDifference); // Flow rate in m^3/s
                    // Apply discharge coefficient for turbine efficiency
                    double actualFlow = theoreticalFlow * dischargeCoefficient; // Adjust flow rate for efficiency

                    // Calculate power using: P = water density * g * Q * h * efficiency
                    double powerWatts = actualFlow * WATER_DENSITY * GRAVITY * headDifference; // Power in Watts
                    double powerMW = Math.min(powerWatts * WATTS_TO_MW, maxPowerMW); // Convert to MW and limit to max power
                    
                    // Energy calculation for this time step
                    double stepEnergy = powerMW * TIME_STEP_HOURS; // Energy in MWh for this time step
                    totalEnergyOutput += stepEnergy; // Accumulate total energy output
                    halfTideEnergy += stepEnergy; // Accumulate energy for this half-tide

                    // Debug tracking 
                    if (powerMW > 0){
                        totalPowerGenerated += powerMW; // Accumulate total power generated
                        maxPowerSeen = Math.max(maxPowerSeen, powerMW); // Track maximum power seen
                        totalHeadDifferenceWhenActive += headDifference; // Accumulate head difference when turbine is active
                        activeStepsWithPower++; // Count steps where turbine was active and generated power
                        totalFlowVolume += actualFlow * TIME_STEP_SECONDS; // Accumulate total flow volume in m^3
                    }

                    // Lagoon level update
                    double volumeChange = actualFlow * TIME_STEP_SECONDS; // Volume change in m^3
                    double levelChange = volumeChange / lagoonSurfaceArea; // Change in lagoon level in meters
                    
                    if (seaLevel > lagoonLevel) {
                        // Water flows into the lagoon (level rises)
                        lagoonLevel += levelChange; // Ensure positive change
                    } else {
                        // Water flows out of the lagoon (level falls)
                        lagoonLevel -= levelChange; // Ensure negative change
                    }

                    // Clamp lagoon level within bounds
                    lagoonLevel = Math.max(MIN_LAGOON_LEVEL, Math.min(lagoonLevel, MAX_LAGOON_LEVEL)); // Ensure lagoon level stays within bounds

                    if (ENABLE_DETAILED_DEBUG && powerMW > 0) {
                        System.out.printf("Step %d: Sea=%.2f, Lagoon=%.2f, Head=%.2f, Power=%.1f MW, Energy=%.3f MWh\n",
                            totalTimeSteps, seaLevel, lagoonLevel, headDifference, powerMW, stepEnergy);
                    }
               }
            }

            halfTideEnergies[i] = halfTideEnergy; // Store energy for this half-tide

            if (ENABLE_DETAILED_DEBUG) {
                System.out.printf("Half-tide %d complete: Energy=%.1f MWh, Hs=%.2f, He=%.2f\n", 
                    i, halfTideEnergy, Hs, He);
            }
        }

        if (ENABLE_SUMMARY_DEBUG) {
            double turbineActivePercent = (totalTimeSteps > 0) ? 
                (100.0 * turbineActiveSteps) / totalTimeSteps : 0.0;
            
            double avgPowerWhenActive = (activeStepsWithPower > 0) ? 
                totalPowerGenerated / activeStepsWithPower : 0.0;
            
            double avgHeadWhenActive = (activeStepsWithPower > 0) ? 
                totalHeadDifferenceWhenActive / activeStepsWithPower : 0.0;
            
            double capacityFactor = (maxPowerMW > 0) ? (avgPowerWhenActive / maxPowerMW) * 100.0 : 0.0;
            
            System.out.printf("=== SIMULATION SUMMARY ===\n");
            System.out.printf("Time: %d half-tides, %.1f hours total\n", 
                halfTideCount, totalTimeSteps * TIME_STEP_HOURS);
            System.out.printf("Energy: %.1f MWh total (%.1f MWh per cycle)\n", 
                totalEnergyOutput, totalEnergyOutput / (halfTideCount / 2.0));
            System.out.printf("Turbine: %.1f%% active (%d/%d steps)\n", 
                turbineActivePercent, turbineActiveSteps, totalTimeSteps);
            System.out.printf("Power: Avg=%.1f MW, Max=%.1f MW, Capacity=%.1f MW\n",
                avgPowerWhenActive, maxPowerSeen, maxPowerMW);
            System.out.printf("Head: Avg=%.2f m, Max=%.2f m\n", avgHeadWhenActive, maxHeadSeen);
            System.out.printf("Capacity Factor: %.1f%%\n", capacityFactor);
            System.out.printf("Flow Volume: %.0f million m³\n", totalFlowVolume / 1_000_000);
            System.out.println("==========================");
        }

        return totalEnergyOutput; // Return the total energy output in MWh
    }
}
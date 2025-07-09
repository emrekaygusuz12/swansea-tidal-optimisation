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

    // ==========================
    // PHYSICAL CONSTANTS
    // ==========================

    /** Conversion factor from Watts to MW (1,000,000 W = 1 MW) */
    private static final double WATTS_TO_MW = 1e-6;

    /** Acceleration due to gravity in m/s^2 */
    private static final double GRAVITY = 9.81;

    /** Density of water in kg/m^3 */
    private static final double WATER_DENSITY = 1025.0;

    // ==========================
    // TEMPORAL PARAMETERS
    // ==========================

    /** Time step in hours (15 minutes) */
    private static final double TIME_STEP_HOURS = SimulationConfig.getTimeStepHours();

    /** Time step converted to seconds */
    private static final double TIME_STEP_SECONDS = TIME_STEP_HOURS * 3600;

    // ==========================
    // CONSTRAINT LIMITS
    // ==========================

    /** Minimum lagoon level in meters */
    private static final double MIN_LAGOON_LEVEL = -5.0; 

    /** Maximum lagoon level in meters */
    private static final double MAX_LAGOON_LEVEL = 10.0;

    /**
     * Simulates tidal lagoon operation for a given tide sequence and control parameters.
     *
     * @param tideHeights List of sea level heights at each time step (in meters)
     * @param individual  Individual containing control parameters (Hs and He) for each half-tide
     * @return Total energy output in MWh
     */
    public static double simulate(List<Double> tideHeights, Individual individual) {

        // ==========================
        // LAGOON CONFIGURATION
        // ==========================

        double lagoonSurfaceArea = Lagoon.getLagoonSurfaceAreaM2(); 
        double turbineRadius = Lagoon.getTurbineDiameterM() / 2.0; 
        double turbineArea = Math.PI * Math.pow(turbineRadius, 2); 
        double totalTurbineArea = turbineArea * Lagoon.getNumberOfTurbines(); 
        double maxPowerMW = Lagoon.getInstalledCapacityMW(); 
        double dischargeCoefficient = Lagoon.getTurbineDischargeCoefficient();

        // ==========================
        // SIMULATION STATE
        // ==========================

        double totalEnergyOutput = 0.0;
        double lagoonLevel = tideHeights.get(0);
        int halfTideCount = individual.getDecisionVariables().length / 2;
        int stepPerHalfTide = tideHeights.size() / halfTideCount;

        // ==========================
        // MAIN SIMULATION LOOP
        // ==========================
        
        for (int i = 0; i < halfTideCount; i++) {
            double Hs = individual.getStartHead(i);
            double He = individual.getEndHead(i);
            boolean turbineOn = false;

            for (int j = 0; j < stepPerHalfTide; j++) {
                int tideIndex = i * stepPerHalfTide + j;
                if (tideIndex >= tideHeights.size()) {
                    break;
                }

                double seaLevel = tideHeights.get(tideIndex);
                double headDifference = Math.abs(seaLevel - lagoonLevel);

               // ==========================
               // TURBINE CONTROL LOGIC
               // ==========================

               if (!turbineOn && headDifference >= Hs) {
                    turbineOn = true; 
               }

               if (turbineOn && headDifference < He) {
                    turbineOn = false; 
               }

               // ==========================
               // POWER GENERATION
               // ==========================
               
               if (turbineOn && headDifference > 0) {
                    // Calculate flow rate using orifice theory
                    double theoreticalFlow = totalTurbineArea * Math.sqrt(2 * GRAVITY * headDifference); 
                    double actualFlow = theoreticalFlow * dischargeCoefficient;

                    // Calculate power and energy 
                    double powerWatts = actualFlow * WATER_DENSITY * GRAVITY * headDifference;
                    double powerMW = Math.min(powerWatts * WATTS_TO_MW, maxPowerMW);
                    double stepEnergy = powerMW * TIME_STEP_HOURS;
                    totalEnergyOutput += stepEnergy;

                    // ===========================
                    // LAGOON LEVEL UPDATE
                    // ===========================

                    double volumeChange = actualFlow * TIME_STEP_SECONDS;
                    double levelChange = volumeChange / lagoonSurfaceArea;
                    
                    if (seaLevel > lagoonLevel) {
                        lagoonLevel += levelChange;
                    } else {
                        lagoonLevel -= levelChange;
                    }

                    // Clamp lagoon level within bounds
                    lagoonLevel = Math.max(MIN_LAGOON_LEVEL, Math.min(lagoonLevel, MAX_LAGOON_LEVEL));
               }
            }  
        }

        return totalEnergyOutput;
    }
}
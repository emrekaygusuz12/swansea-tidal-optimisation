package src.optimisation;

import src.model.Lagoon;
import src.model.SimulationConfig;

import java.util.List;


/**
 * Simulates the operation of a tidal lagoon using a 0D model approach.
 * 
 * This class implements the physics of tidal lagoon operation including:
 * - Enhanced turbine efficiency modeling based on literature
 * - Orifice theory for flow calculations
 * - Mass conservation for lagoon level updates
 * - Power generation calculations
 * - Two-way (bidirectional) turbine operation
 * 
 * Uses backward-difference method for temporal discretisation.
 * 
 * @author Emre Kaygusuz
 * @version 1.2
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

    // ==========================
    // ENCHANGED TURBINE MODEL
    // ==========================

    /** Minimum operating head difference in meters (literature: 1-2m) */
    private static final double MIN_OPERATING_HEAD = 1.0;

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
               // ENHANCED TURBINE CONTROL LOGIC
               // ==========================

               double powerMW = 0.0;

                if (headDifference >= Hs) {
                    // Full operation: head difference above starting threshold
                    powerMW = calculateTurbinePower(headDifference, totalTurbineArea, 
                                                                maxPowerMW, dischargeCoefficient);
                    turbineOn = true;
                    
                } else if (headDifference >= He && turbineOn) {
                    // Continued operation: between ending and starting thresholds (hysteresis)
                    double availablePower = calculateTurbinePower(headDifference, totalTurbineArea, 
                                                                maxPowerMW, dischargeCoefficient);
                    
                    if (Hs > He) {
                        // Graduated operation between thresholds
                        double operationFactor = (headDifference - He) / (Hs - He);
                        powerMW = availablePower * Math.max(0.0, operationFactor);
                    } else {
                        powerMW = availablePower; // Fallback if Hs <= He
                    }
                    
                } else {
                    // Below ending threshold - stop operation
                    powerMW = 0.0;
                    turbineOn = false;
                }

                // ==========================
                // ENERGY ACCUMULATION & LAGOON UPDATE
                // ==========================
                
                if (powerMW > 0) {
                    double stepEnergy = powerMW * TIME_STEP_HOURS;
                    totalEnergyOutput += stepEnergy;

                    // Calculate actual flow from power output
                    double actualFlow = calculateFlowFromPower(powerMW, headDifference);
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

    /**
     * Calculate realistic turbine power output using industry-aligned efficiency curves
     * and explicit generator efficiency (e.g., Andritz Hydro data)
     */
    private static double calculateTurbinePower(double headDifference, double totalTurbineArea,
                                               double maxPowerMW, double dischargeCoefficient) {
        if (headDifference < MIN_OPERATING_HEAD) {
            return 0.0; // Below minimum operating head
        }

        // Hydraulic efficiency from industry hill chart
        double hydraulicEfficiency = calculateTurbineEfficiency(headDifference);

        // Generator efficiency (typical 97% for large hydro)
        double generatorEfficiency = 0.97;

        // Overall efficiency
        double totalEfficiency = hydraulicEfficiency * generatorEfficiency;

        // Flow calculation with discharge coefficient (not including efficiency)
        double theoreticalFlow = totalTurbineArea * Math.sqrt(2 * GRAVITY * headDifference);
        double actualFlow = theoreticalFlow * dischargeCoefficient;

        // Power calculation (apply total efficiency to power, not flow)
        double powerWatts = actualFlow * WATER_DENSITY * GRAVITY * headDifference * totalEfficiency;
        return Math.min(powerWatts * WATTS_TO_MW, maxPowerMW);
    }
    
    /**
     * Industry-aligned turbine efficiency curve (e.g., Andritz Hydro hill chart)
     */
    private static double calculateTurbineEfficiency(double head) {
        if (head < MIN_OPERATING_HEAD) return 0.0;
        if (head < 1.2) {
            return 0.35 + 0.125 * (head - 1.0);  // 35% → 47.5% (slow startup)
        } else if (head < 1.8) {
            return 0.475 + 0.208 * (head - 1.2); // 47.5% → 60% (rapid improvement)
        } else if (head < 2.5) {
            return 0.60 + 0.357 * (head - 1.8);  // 60% → 85% (main operating range)
        } else if (head < 3.2) {
            return 0.85 + 0.143 * (head - 2.5);  // 85% → 95% (optimal range)
        } else if (head <= 4.0) {
            return 0.95; // Peak efficiency plateau
        } else {
            // More gradual decline for robustness
            return 0.95 * Math.exp(-0.03 * (head - 4.0));
        }
    }
    
    /**
     * Calculate flow rate from power output (for lagoon level updates)
     */
    private static double calculateFlowFromPower(double powerMW, double headDifference) {
        if (headDifference <= 0 || powerMW <= 0) return 0.0;
        
        double powerWatts = powerMW / WATTS_TO_MW;
        return powerWatts / (WATER_DENSITY * GRAVITY * headDifference);
    }
}

package src.optimisation;

import src.model.Lagoon;
import java.util.List;

/**
 * Represents the objective function for the optimisation of the Swansea Bay Tidal Lagoon.
 * 
 * Evaluates two conflicting objectives:
 * Objective 1: Maximise annual energy output (MWh/year)
 * Objective 2: Minimise Levelised Cost of Energy (LCOE) (£/MWh)
 * 
 * Uses literature-based LCOE calculation based on Petley & Aggidis (2016)
 * and industry OPEX standards for realistic financial parameters.
 * 
 * @author Emre Kaygusuz
 * @version 3.0
 */
public class ObjectiveFunction {

    // ==========================
    // TEMPORAL CONSTANTS
    // ==========================

    /** Number of hours in a year (365 days) */
    private static final int HOURS_IN_YEAR = 365 * 24; 

    /** Average duration of a half tide in hours */
    private static final double HOURS_PER_HALF_TIDE = 6.12; 

    // ==========================
    // FINANCIAL PARAMETERS
    // ==========================

    /** Project lifetime in years */
    private static final double PROJECT_LIFETIME_YEARS = 25.0;

    /** Discount rate for financial calculations */
    private static final double DISCOUNT_RATE = 0.08;

    /** Annual OPEX as percentage of CAPEX */
    private static final double OPEX_PERCENTAGE = 0.04;

    // ==========================
    // PENALTY PARAMETERS
    // ==========================

    /** Minimum acceptable capacity factor */
    private static final double MIN_CAPACITY_FACTOR = 0.20;

    /** Cost penalty per meter of head variation (£/MWh) */
    private static final double HEAD_VARIATION_COST = 25.0;

    /** Penalty rate for low capacity factor (£/MWh) */
    private static final double CAPACITY_PENALTY_RATE = 300.0;

    /** Scaling factor for maintenance complexity penalty */
    private static final double MAINTENANCE_SCALING = 12.0;

    /** Cost penalty per operational frequency (£/MWh) */
    private static final double FREQUENCY_COST = 8.0;

    /** Environmental impact penalty (£/MWh) */
    private static final double ENVIRONMENTAL_COST = 20.0;

    /** Efficiency threshold for progressive penalty (MWh) */
    private static final double EFFICIENCY_THRESHOLD = 380000.0;

    /** Penalty rate for exceeding efficiency threshold */
    private static final double EFFICIENCY_PENALTY_RATE = 0.5;

    // ==========================
    // EVALUATION METHODS
    // ==========================

    /**
     * Evaluates both objectives for an individual solution.
     * Calculates annual energy output and LCOE, storing results in the individual.
     * 
     * @param tideHeights List of tidal heights for the simulation period
     * @param individual Individual solution to evaluate
     */
    public static void evaluate(List<Double>tideHeights, Individual individual) {
        // Calculate simulation period duration
        int simulationHours = calculateSimulationHours(individual);
        
        // Simulate energy output for the given period
        double periodEnergyOutput =TidalSimulator.simulate(tideHeights, individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours); 

        // Store objectives in individual
        individual.setEnergyOutput(annualEnergyOutput); 
        individual.setUnitCost(calculateLCOE(individual, annualEnergyOutput));
    }

    /**
     * Evaluates objectives with explicit simulation hours (alternative interface).
     * 
     * @param tideHeights List of tidal heights for the simulation
     * @param individual Individual solution to evaluate
     * @param simulationHours Duration of the simulation period in hours
     */
    public static void evaluate(List<Double> tideHeights, Individual individual, int simulationHours) {
        // Simulate energy output for the given period
        double periodEnergyOutput = TidalSimulator.simulate(tideHeights, individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours);

        // Store objectives in individual
        individual.setEnergyOutput(annualEnergyOutput);
        individual.setUnitCost(calculateLCOE(individual, annualEnergyOutput));
    }

    // ==========================
    // TEMPORAL CALCULATIONS
    // ==========================

    /**
     * Calculates the simulation period in hours based on the number of half tides.
     * Each half tide represents approximately 12 hours of tidal flow.
     * 
     * @param individual Individual containing decision variables
     * @return Simulation period duration in hours
     */
    private static int calculateSimulationHours(Individual individual) {
        int halfTides = individual.getDecisionVariables().length / 2;
        return (int) (halfTides * HOURS_PER_HALF_TIDE); 
    }

    /**
     * Extrapolates period energy output to annual energy output.
     *
     * @param periodEnergyOutput Energy output for the simulation period (MWh)
     * @param simulationHours Duration of simulation period (hours)
     * @return Annualised energy output in (MWh/year)
     */
    private static double extrapolate(double periodEnergyOutput, int simulationHours) {
        if (simulationHours <= 0) {
            return 0.0; // Avoid division by zero
        }

        double annualisationFactor = (double) HOURS_IN_YEAR / simulationHours;
        return periodEnergyOutput * annualisationFactor;
    }

    // ==========================
    // LCOE CALCULATION
    // ==========================

    /**
     * Calculates the Levelised Cost of Energy (LCOE) using literature
     * based methodology. Includes capital cost, operational costs,
     * and various penalty factors.
     * 
     * @param individual Individual solution
     * @param annualEnergyOutput Annual energy output (MWh/year)
     * @return LCOE in £/MWh
     */
    private static double calculateLCOE(Individual individual, double annualEnergyOutput) {
        if (annualEnergyOutput <= 0) {
            return Double.MAX_VALUE; // Invalid solution
        }

        // ==========================
        // BASE FINANCIAL COSTS
        // ==========================

        double capitalCost = Lagoon.getTotalCapitalCost();
        double installedCapacityMW = Lagoon.getInstalledCapacityMW();

        // Calculate annualised capital cost (CAPEX) using capital recovery factor
        double capitalRecoveryFactor = (DISCOUNT_RATE * Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS)) / 
                                    (Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS) - 1);
        double annualisedCapitalCost = capitalCost * capitalRecoveryFactor;

        // Annual operational expenditure (4% of CAPEX)
        double annualOPEX = capitalCost * OPEX_PERCENTAGE;

        // Basic LCOE calculation
        double totalAnnualCost = annualisedCapitalCost + annualOPEX;
        double basicLCOE = totalAnnualCost / annualEnergyOutput;

        // ==========================
        // PENALTY CALCULATIONS
        // ==========================

        // Operational complexity penalty based on head variation
        double headVariation = calculateHeadVariation(individual);
        double complexityPenalty = headVariation * HEAD_VARIATION_COST;

        // Environmental impact penalty
        double environmentalCost = headVariation * ENVIRONMENTAL_COST;

        // Maintenance complexity penalty (non-linear with head variation)
        double maintenancePenalty = Math.pow(headVariation, 1.2) * MAINTENANCE_SCALING;

        // Operational frequency penalty
        double operationalFrequency = calculateOperationFrequency(individual);
        double frequencyPenalty = operationalFrequency * FREQUENCY_COST;

        // Capacity factor penalties (realistic operational constraint)
        double capacityFactor = (annualEnergyOutput / 8760.0) / installedCapacityMW;
        double capacityPenalty = calculateCapacityPenalty(capacityFactor);

        // High energy output stress penalty
        double stressPenalty = 0.0;
        if (annualEnergyOutput > 450000) {
            stressPenalty = (annualEnergyOutput - 450000) / 100000.0 * 10.0;
        }

        // Progressive efficiency penalty for very high outputs
        double efficiencyPenalty = 0.0;
        if (annualEnergyOutput > EFFICIENCY_THRESHOLD) {
            double excess = annualEnergyOutput - EFFICIENCY_THRESHOLD;
            efficiencyPenalty = Math.pow(excess / 50000.0, 1.3) * EFFICIENCY_PENALTY_RATE;
        }

        // ==========================
        // FINAL LCOE
        // ==========================

    
        return basicLCOE + complexityPenalty + maintenancePenalty +
                frequencyPenalty + capacityPenalty + environmentalCost + stressPenalty + efficiencyPenalty;
    }

    // ==========================
    // PENALTY HELPERS
    // ==========================

    /**
     * Calculates capacity factor penalty with tiered thresholds.
     * 
     * @param capacityFactor Calculated capacity factor (0-1)
     * @return Capacity penalty in £/MWh
     */
    private static double calculateCapacityPenalty(double capacityFactor) {
        if (capacityFactor < 0.15) {
            return (0.15 - capacityFactor) * 500.0; // Severe penalty
        } else if (capacityFactor < MIN_CAPACITY_FACTOR) {
            return (MIN_CAPACITY_FACTOR - capacityFactor) * CAPACITY_PENALTY_RATE;
        }
        return 0.0; // No penalty if above threshold
    }

    /**
     * Calculates operational frequency penalty based on head variations.
     * 
     * @param individual Individual solution
     * @return Normalised operation frequency (0-1)
     */
    private static double calculateOperationFrequency(Individual individual) {
        int operations = 0;
        int halfTides = individual.getDecisionVariables().length / 2;
        
        for (int i = 0; i < halfTides; i++) {
            double startHead = individual.getStartHead(i);
            double endHead = individual.getEndHead(i);
            
            // Count as operation if there's significant head difference
            if (Math.abs(endHead - startHead) > 0.5) {
                operations++;
            }
        }
        
        return (double) operations / halfTides; // Normalise by number of tides
    }

    /**
     * Calculates the maximum head variation across all half tides.
     * 
     * @param individual Individual solution
     * @return Head variation in meters
     */
    private static double calculateHeadVariation(Individual individual) {
        double maxHead = Double.MIN_VALUE;
        double minHead = Double.MAX_VALUE;

        int halfTides = individual.getDecisionVariables().length / 2;
        for (int i = 0; i < halfTides; i++) {
            double startHead = individual.getStartHead(i);
            double endHead = individual.getEndHead(i);

            maxHead = Math.max(maxHead, Math.max(startHead, endHead));
            minHead = Math.min(minHead, Math.min(startHead, endHead));
        }

        return maxHead - minHead; // head variation in meters
    }

}

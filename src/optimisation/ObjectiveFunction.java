package src.optimisation;

import src.model.Lagoon;
import java.util.List;

/**
 * Represents the objective function for the optimisation of the Swansea Bay Tidal Lagoon.
 * 
 * VERSION 3.0 - LITERATURE-BASED LCOE IMPLEMENTATION
 * 
 * Evaluates two conflicting objectives:
 * 1. Maximise annual energy output (MWh/year)
 * 2. Minimise Levelised Cost of Energy (LCOE) (£/MWh)
 * 
 * MAJOR UPDATE in v3.0:
 * - Replaced artificial cost function with literature-based LCOE calculation
 * - Based on Petley & Aggidis (2016) and indutry OPEX standards
 * - Uses realistic financial parameters for tidal energy projects
 * 
 * @author Emre Kaygusuz
 * @version 3.0
 */
public class ObjectiveFunction {

    private static final int HOURS_IN_YEAR = 365 * 24; 
    private static final double HOURS_PER_HALF_TIDE = 6.12; 
    
    // Literature-based LCOE parameters
    private static final double PROJECT_LIFETIME_YEARS = 25.0; // Typical project lifetime
    private static final double DISCOUNT_RATE = 0.08; // 8% discount rate
    private static final double OPEX_PERCENTAGE = 0.04; // 4% of CAPEX annually (standard for marine renewables)
    private static final double MIN_CAPACITY_FACTOR = 0.20; // 20% minimum capacity factor threshold
    private static final double HEAD_VARIATION_COST = 25.0; // £25/MWh per meter of head variation
    private static final double CAPACITY_PENALTY_RATE = 300.0; // £300/MWh penalty for low capacity factor
    private static final double MAINTENANCE_SCALING = 12.0; // Scaling factor for maintenance complexity penalty
    private static final double FREQUENCY_COST = 8.0; // £8/MWh per operational frequency penalty
    private static final double ENVIRONMENTAL_COST = 20.0; // £20/MWh environmental impact penalty

    private static final double EFFICIENCY_THRESHOLD = 380000.0;
    private static final double EFFICIENCY_PENALTY_RATE = 0.5; // Penalty for efficiency below threshold

    public static void evaluate(List<Double>tideHeights, Individual individual) {
        // Objective 1: Simulate energy output for the given period and annualise it
        double periodEnergyOutput =TidalSimulator.simulate(tideHeights, individual);

        // Calculate simluation period in hours
        int simulationHours = calculateSimulationHours(individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours); 

        // CRITICAL DEBUG - Track the relationship
        System.out.printf("PERIOD ANALYSIS: HalfTides=%d, Hours=%d, Days=%.1f, PeriodEnergy=%.1f MWh, AnnualEnergy=%.1f MWh%n",
                     individual.getDecisionVariables().length / 2, 
                     simulationHours, 
                     simulationHours/24.0,
                     periodEnergyOutput, 
                     annualEnergyOutput);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput); 

        // Objective 2: Calculate unit cost of energy
        double lcoe = calculateLCOE(individual, annualEnergyOutput);
        individual.setUnitCost(lcoe);
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
        int currentHours = (int) (halfTides * HOURS_PER_HALF_TIDE); // 6.12 hours per half tide
        return currentHours;
    }


    /**
     * Extrapolates period energy output to annual energy output.
     * 
     * 
     * @param periodEnergyOutput Energy output for the simulation period in MWh
     * @param simulationHours Duration of simulation period in hours
     * @return Annualised energy output in MWh/year
     */
    private static double extrapolate(double periodEnergyOutput, int simulationHours) {
        // Annualise the energy output based on the simulation period
        if (simulationHours <= 0) {
            return 0.0; // Avoid division by zero
        }


        double annualisationFactor = (double) HOURS_IN_YEAR / simulationHours;
        double annualEnergyOutput = periodEnergyOutput * annualisationFactor;

        if (simulationHours >= HOURS_IN_YEAR) {
        System.out.printf("LONG SIMULATION: %d hours (%.1f days) scaled DOWN by %.3fx to annual: %.1f MWh%n", 
                         simulationHours, simulationHours/24.0, annualisationFactor, annualEnergyOutput);
        } else {
            System.out.printf("SHORT SIMULATION: %d hours (%.1f days) scaled UP by %.3fx to annual: %.1f MWh%n",
                             simulationHours, simulationHours/24.0, annualisationFactor, annualEnergyOutput);
        }


        return annualEnergyOutput; // Annualised energy output in MWh
    }

    private static double calculateLCOE(Individual individual, double annualEnergyOutput) {
        if (annualEnergyOutput <= 0) {
            return Double.MAX_VALUE; // Avoid division by zero
        }

        // Literature-based parameters
        double capitalCost = Lagoon.getTotalCapitalCost(); // Total capital cost in GBP
        double installedCapacityMW = Lagoon.getInstalledCapacityMW(); // Installed capacity in MW

        // Calculate annualised capital cost (CAPEX)
        double capitalRecoveryFactor = (DISCOUNT_RATE * Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS)) / 
                                    (Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS) - 1);
        double annualisedCapitalCost = capitalCost * capitalRecoveryFactor;

        // Literature-based OPEX: 4% of CAPEX annually (standard for renewables)
        double annualOPEX = capitalCost * OPEX_PERCENTAGE;

        // Total annual costs (CAPEX + OPEX)
        double totalAnnualCost = annualisedCapitalCost + annualOPEX;

        // Basic LCOE calculation (£/MWh)
        double basicLCOE = totalAnnualCost / annualEnergyOutput;

        // Operational complexity pentalty based on head variation
        double headVariation = calculateHeadVariation(individual);
        double complexityPenalty = headVariation * HEAD_VARIATION_COST; // £5/MWh per meter of head variation

        // Environmental cost penalty (fixed for all solutions)
        double environmentalCost = headVariation * ENVIRONMENTAL_COST; // £200/MWh per meter of head variation
        // Maintanance complexity pentalty (non-linear with head variation)
        double maintenancePenalty = Math.pow(headVariation, 1.2) * MAINTENANCE_SCALING;

        // Operational frequency penalty
        double operationalFrequency = calculateOperationFrequency(individual);
        double frequencyPenalty = operationalFrequency * FREQUENCY_COST; // £8/MWh per operational frequency
        
        // Capacity factor penalty (realistic operational constraint)
        double capacityFactor = (annualEnergyOutput / 8760.0) / installedCapacityMW; // Capacity factor = Energy output / (Installed capacity * Hours in year)
        double capacityPenalty = 0.0;
        if (capacityFactor < MIN_CAPACITY_FACTOR) {
            capacityPenalty = (MIN_CAPACITY_FACTOR - capacityFactor) * CAPACITY_PENALTY_RATE; // £500/MWh penalty for low capacity factor
        }

        double stressPenalty = 0.0;
        if (annualEnergyOutput > 450000) {
            stressPenalty = (annualEnergyOutput - 450000) / 100000.0 * 10.0; // £0.1/MWh penalty for exceeding 450,000 MWh
        }

        // Progressive efficiency penalty (creates curved trade-off)
        double efficiencyPenalty = 0.0;
        if (annualEnergyOutput > EFFICIENCY_THRESHOLD) {
            double excess = annualEnergyOutput - EFFICIENCY_THRESHOLD;
            efficiencyPenalty = Math.pow(excess / 50000.0, 1.3) * EFFICIENCY_PENALTY_RATE;
        }

        // Variable capacity factor penalty (different thresholds)
        double enhancedCapacityPenalty = 0.0;
        if (capacityFactor < 0.15) {
            enhancedCapacityPenalty = (0.15 - capacityFactor) * 500.0; // Severe penalty
        } else if (capacityFactor < MIN_CAPACITY_FACTOR) {
            enhancedCapacityPenalty = (MIN_CAPACITY_FACTOR - capacityFactor) * CAPACITY_PENALTY_RATE;
        }

        double finalLCOE = basicLCOE + complexityPenalty + maintenancePenalty +
                    frequencyPenalty + capacityPenalty + environmentalCost + stressPenalty + efficiencyPenalty + enhancedCapacityPenalty;

        System.out.printf("LCOE BREAKDOWN: Base=£%.1f, Complexity=£%.1f, Environmental=£%.1f, Maintenance=£%.1f, Frequency=£%.1f, Capacity=£%.1f, Stress=£%.1f, Efficiency=£%.1f, Enhanced Capacity=£%.1f, Total=£%.1f/MWh%n",
                 basicLCOE, complexityPenalty, environmentalCost, maintenancePenalty, frequencyPenalty, capacityPenalty, stressPenalty, efficiencyPenalty, enhancedCapacityPenalty, finalLCOE);

        return finalLCOE;
    }

        /**
     * Calculates operational frequency penalty based on number of operations
     */
    private static double calculateOperationFrequency(Individual individual) {
        int operations = 0;
        int halfTides = individual.getDecisionVariables().length / 2;
        
        for (int i = 0; i < halfTides; i++) {
            double startHead = individual.getStartHead(i);
            double endHead = individual.getEndHead(i);
            
            // Count as operation if there's significant head difference
            if (Math.abs(endHead - startHead) > 0.5) { // More than 0.5m difference
                operations++;
            }
        }
        
        return (double) operations / halfTides; // Normalize by number of tides
    }

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

        // Head variation in meters
        return maxHead - minHead;
    }

    // private static double calculateOperationalComplexityCost(Individual individual, double annualEnergyOutput) {
    //     if (annualEnergyOutput <= 0) {
    //         return Double.MAX_VALUE;
    //     }

    //     // Drastically reduce capital cost component
    //     double baseCapitalCost = Lagoon.getTotalCapitalCost() / (annualEnergyOutput * 500.0);

    //     // Calculate average head across all half-tides
    //     double avgHead = calculateAverageHead(individual);
    //     int halfTides = individual.getDecisionVariables().length / 2;
    //     for (int i = 0; i < halfTides; i++) {
    //         avgHead += individual.getStartHead(i) + individual.getEndHead(i);
    //     }
    //     avgHead /= (halfTides * 2);

    //     // AGGRESSIVE operational penalty that increases dramatically with energy
    //     double operationalPenalty = Math.pow(avgHead, 2.0) * 200.0;
        
    //     // Energy-dependent penalty that creates strong trade-off
    //     double energyPenalty = Math.pow(annualEnergyOutput / EXPECTED_MIN_OUTPUT, 1.5) * 1000.0;

    //     double operationalComplexityCost = baseCapitalCost + operationalPenalty + energyPenalty;

    //     // Add capacity utilization penalty to bias toward higher energy solutions
    //     double avgPowerMW = annualEnergyOutput / 8760.0; // Annual hours
    //     double capacityUtilization = avgPowerMW / 320.0; // 320 MW capacity
    //     if (capacityUtilization < 0.4) { // Below 40% utilization
    //         operationalComplexityCost *= (1.0 + (0.4 - capacityUtilization) * 2.0); // Penalty
    //     }

    //     return operationalComplexityCost;
    // }

    // private static double calculateAverageHead(Individual individual) {
    //     double avgHead = 0;
    //     int halfTides = individual.getDecisionVariables().length / 2;
    //     for (int i = 0; i < halfTides; i++) {
    //         avgHead += individual.getStartHead(i) + individual.getEndHead(i);
    //     }
    //     double averageHead = avgHead / (halfTides * 2);
    //     return averageHead;
    // }

    public static void evaluate(List<Double> tideHeights, Individual individual, int simulationHours) {
        // Objective 1: Simulate energy output for the given period and annualise it
        double periodEnergyOutput = TidalSimulator.simulate(tideHeights, individual);

        // Extrapolate to annual energy output
        double annualEnergyOutput = extrapolate(periodEnergyOutput, simulationHours);

        // Store the annualised energy output
        individual.setEnergyOutput(annualEnergyOutput);

        // Objective 2: Calculate unit cost of energy
        double lcoe = calculateLCOE(individual, annualEnergyOutput);
        individual.setUnitCost(lcoe);
    }

}

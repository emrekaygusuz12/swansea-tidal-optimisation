package src.optimisation;

import src.model.Lagoon;
import java.util.List;

/**
 * Literature-based objective function using Segura et al. (2017) methodology with
 * Real Swansea Bay CfD submission data (2015).
 * 
 * Replaces artificial penalties with realistic Life Cycle Cost (LCC) approach
 * based on "Cost Assessment Methodology and Economic Viability of Tidal Energy Projects"
 * with actual Swansea Bay operational costs from CfD submission.
 * 
 * Two objectives:
 * Objective 1: Maximise annual energy output (MWh/year)
 * Objective 2: Minimise Levelised Cost of Energy (LCOE) (£/MWh)
 * 
 * @author Emre Kaygusuz
 * @version 5.0
 */
public class ObjectiveFunction {

    // ==========================
    // TEMPORAL CONSTANTS
    // ==========================

    private static final int DAYS_IN_YEAR = 365; // Number of days in a year

    private static final int HOURS_IN_DAY = 24; // Number of hours in a day

    /** Number of hours in a year (365 days) */
    private static final int HOURS_IN_YEAR = DAYS_IN_YEAR * HOURS_IN_DAY; // 8760 hours

    private static final double ONE_COMPLETE_TIDAL_CYCLE = 360.0 / 28.984; // 360 degrees / 28.984 degrees per hour


    /** Average duration of a half tide in hours */
    private static final double HOURS_PER_HALF_TIDE = ONE_COMPLETE_TIDAL_CYCLE / 2.0; // 6.21

    // ==========================
    // FINANCIAL PARAMETERS
    // ==========================

    /** Project lifetime in years */
    private static final double PROJECT_LIFETIME_YEARS = 35.0; // based on Swansea Bay CfD submission

    /** Discount rate for financial calculations */
    private static final double DISCOUNT_RATE = 0.06; // 75% debt at 4% + 25% equity at 12% = 6% WACC

    // ==========================
    // LCC COST STRUCTURE (based on industry standards for large-scale tidal projects)
    // based on Segura et al. (2017) methodology adapted for 320MW+ scale.
    // Larger projects have lower relative concept  costs and higher installation costs
    // and lower manufacturing percentage.
    // ==========================

    /** C1: Concept and Definition Costs (5.7% of CAPEX) */
    private static final double CONCEPT_DEFINITION_RATIO = 0.057;

    /** C2: Design and Development Costs (0.2% of CAPEX) */
    private static final double DESIGN_DEVELOPMENT_RATIO = 0.002;

    /** C3: Manufacturing Costs (80% of CAPEX) */
    private static final double MANUFACTURING_BASE_RATIO = 0.80;

    /** C4: Installation Costs (2.2% of CAPEX) */
    private static final double INSTALLATION_BASE_RATIO = 0.022;

    /** C6: Decommissioning Costs (5% of CAPEX) */
    private static final double DECOMMISSIONING_RATIO = 0.05;

    // ==========================
    // C5: OPERATION & MAINTENANCE COSTS (from 2015 CfD submission)
    // ==========================

    /** Turbine maintenance cost (annual total for all 16 turbines) */
    private static final double ANNUAL_TURBINE_MAINTENANCE_BASE = 3486000.0; // £3.486M (checks out)

    /** Marine structures maintenance cost (annual total) */
    private static final double ANNUAL_MARINE_STRUCTURES_MAINTENANCE_BASE = 1459000.0; // £1.459M (checks out)

    /** Other maintenance cost (annual total) */
    private static final double ANNUAL_OTHER_MAINTENANCE_BASE = 633000.0; // £0.633M (checks out)

    /** Periodic maintenance cost (annual total, annualised) */
    private static final double ANNUAL_PERIODIC_MAINTENANCE_BASE = 1777000.0; // £1.777M (checks out)

    // OPERATIONAL COSTS (Annual totals for entire project)

    /** Insurance cost (annual total - FIXED amount, not percentage) */
    private static final double ANNUAL_INSURANCE_COST_BASE = 2300000.0; // £2.3M (checks out)

    /** Land cost (annual total) */
    private static final double ANNUAL_LAND_COST_BASE = 1700000.0; // £1.7M (checks out)

    /** Other operational costs (annual total) */
    private static final double ANNUAL_OTHER_OPERATIONAL_COST_BASE = 1300000.0; // £1.3M (checks out)

    /** Regulatory and business charges (annual total) */
    private static final double ANNUAL_REGULATORY_COST_BASE = 3649000.0; // TNUoS + BSUoS + Business rates (£3.649M, checks out)


    // ==========================
    // OPERATIONAL COMPLEXITY FACTORS
    // ==========================

    /** Maximum physically possible head variation given constraints */
    private static final double MAX_POSSIBLE_HEAD_VARIATION = 3.0; // 4.0 - 1.0

    /** Optimal head variation threshold (from Petley & Aggidis 2016) */
    private static final double OPTIMAL_HEAD_VARIATION_THRESHOLD = 1.5; // m

    /** Engineering-safe head variation limit */
    private static final double MAX_SAFE_HEAD_VARIATION = 2.5; // m (within physical limits)

    /** Operation frequency threshold (operations per half-tide) */
    private static final double HIGH_FREQUENCY_THRESHOLD = 0.8; // Engineering assumption based on turbine operation frequency

    /** Capacity factor threshold for realistic operation */
    private static final double MIN_REALISTIC_CAPACITY_FACTOR = 0.189; // (572GWh / 320MW / 8760h) based on CfD data

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
     * Calculates the Levelised Cost of Energy (LCOE) using Segura
     * et al. (2017) methodology. Pure literature approach: no artificial penalties.
     * 
     * @param individual Individual solution
     * @param annualEnergyOutput Annual energy output (MWh/year)
     * @return LCOE in £/MWh
     */
    private static double calculateLCOE(Individual individual, double annualEnergyOutput) {
        if (annualEnergyOutput <= 0) {
            return Double.MAX_VALUE; // Invalid solution
        }

        // BASE CAPITAL COSTS
        double capitalCost = Lagoon.getTotalCapitalCost();

        // LCC COMPONENTS
        double conceptDefinitionCost = capitalCost * CONCEPT_DEFINITION_RATIO;
        double designDevelopmentCost = capitalCost * DESIGN_DEVELOPMENT_RATIO;
        double manufacturingComplexityFactor = calculateManufacturingComplexityFactor(individual);
        double manufacturingCost = capitalCost * MANUFACTURING_BASE_RATIO * manufacturingComplexityFactor;
        double installationComplexityFactor = calculateInstallationComplexityFactor(individual);
        double installationCost = capitalCost * INSTALLATION_BASE_RATIO * installationComplexityFactor;
        double annualOandMCost = calculateSwanseaBayOMCosts(individual);
        double decommissioningCost = capitalCost * DECOMMISSIONING_RATIO;

        // TOTAL LIFE CYCLE COST
        double totalCapex = conceptDefinitionCost + designDevelopmentCost + manufacturingCost +
                            installationCost + decommissioningCost;

        double capitalRecoveryFactor = (DISCOUNT_RATE * Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS)) / 
                                    (Math.pow(1 + DISCOUNT_RATE, PROJECT_LIFETIME_YEARS) - 1);

        double annualisedCapitalCost = totalCapex * capitalRecoveryFactor;

        // TOTAL ANNUAL COSTS
        double totalAnnualCost = annualisedCapitalCost + annualOandMCost;

        // LCOE CALCULATION (no penalties)
        return totalAnnualCost / annualEnergyOutput;
    }

    // ==========================
    // COMPLEXITY FACTORS
    // ==========================

    /**
     * Calculates manufacturing complexity factor based on expected operational stress.
     * Higher head variations require more robust equipment.
     * 
     * Engineering assumption:
     * - 10% increase for extreme operations (headVariation > MAX_SAFE_HEAD_VARIATION)
     * - 3% per meter above optimal threshold (headVariation > OPTIMAL_HEAD_VARIATION_THRESHOLD)
     */
    private static double calculateManufacturingComplexityFactor(Individual individual) {
        double headVariation = calculateHeadVariation(individual);
        
        // Increased manufacturing costs for high-stress operations
        if (headVariation > MAX_SAFE_HEAD_VARIATION) {
            return 1.10; // Engineering assumption: 10% increase for extreme operations
        } else if (headVariation > OPTIMAL_HEAD_VARIATION_THRESHOLD) {
            return 1.0 + (headVariation - OPTIMAL_HEAD_VARIATION_THRESHOLD) * 0.03; // Engineering assumption: 3% per meter
        }
        
        return 1.0; // No increase for normal operations
    }

    /**
     * Calculates installation complexity factor based on operational requirements.
     */
    private static double calculateInstallationComplexityFactor(Individual individual) {
        double operationFrequency = calculateOperationFrequency(individual);
        
        // More complex installations needed for high-frequency operations
        if (operationFrequency > HIGH_FREQUENCY_THRESHOLD) {
            return 1.10; // 10% increase for high-frequency operations
        }
        
        return 1.0;
    }

    // ==========================
    // REAL SWANSEA BAY O&M COSTS
    // ==========================

    /**
     * Calculates realistic Operation & Maintenance costs using ACTUAL Swansea Bay CfD data.
     * Costs increase with operational complexity and frequency based on real project baseline.
     * 
     * Engineering assumptions:
     * - Turbine maintenance: 15% per meter above optimal threshold
     * - Marine structures: 10% per meter above optimal threshold
     * - Periodic maintenance: 25% more frequent for high-frequency operations
     * - Insurance: 25% increase for high-risk operations, 10% per meter above optimal threshold
     */
    private static double calculateSwanseaBayOMCosts(Individual individual) {
        // Calculate operational complexity factors
        double headVariation = calculateHeadVariation(individual);
        double operationFrequency = calculateOperationFrequency(individual);
        
        // ==========================
        // MAINTENANCE COSTS (based on CfD submission)
        // ==========================
        
        // Turbine maintenance increases with operational stress
        double turbineMaintenanceComplexityFactor = 1.0;
        if (headVariation > OPTIMAL_HEAD_VARIATION_THRESHOLD) {
            turbineMaintenanceComplexityFactor = 1.0 + (headVariation - OPTIMAL_HEAD_VARIATION_THRESHOLD) * 0.15;
        }
        double annualTurbineMaintenanceCosts = ANNUAL_TURBINE_MAINTENANCE_BASE * turbineMaintenanceComplexityFactor;

        // Marine structures maintenance (less affected by operational complexity)
        double marineStructureComplexityFactor = 1.0;
        if (headVariation > OPTIMAL_HEAD_VARIATION_THRESHOLD) {
            marineStructureComplexityFactor = 1.0 + (headVariation - OPTIMAL_HEAD_VARIATION_THRESHOLD) * 0.1;
        }
        double annualMarineStructureMaintenanceCosts = ANNUAL_MARINE_STRUCTURES_MAINTENANCE_BASE * marineStructureComplexityFactor;

        // Other maintenance costs (baseline)
        double annualOtherMaintenanceCosts = ANNUAL_OTHER_MAINTENANCE_BASE;

        // Periodic maintenance increases with operational frequency
        double periodicMaintenanceFrequencyFactor = 1.0;
        if (operationFrequency > HIGH_FREQUENCY_THRESHOLD) {
            periodicMaintenanceFrequencyFactor = 1.25; // Engineering assumption: 25% more frequent
        }
        double annualPeriodicMaintenanceCosts = ANNUAL_PERIODIC_MAINTENANCE_BASE * periodicMaintenanceFrequencyFactor;

        // ==========================
        // OPERATIONAL COSTS (fixed from CfD submission)
        // ==========================
        
        // Insurance costs (FIXED amount, not percentage - may increase with risk)
        double insuranceRiskFactor = 1.0;
        if (headVariation > MAX_SAFE_HEAD_VARIATION) {
            insuranceRiskFactor = 1.25; // Engineering assumption: 25% increase for high-risk operations
        } else if (headVariation > OPTIMAL_HEAD_VARIATION_THRESHOLD) {
            insuranceRiskFactor = 1.0 + (headVariation - OPTIMAL_HEAD_VARIATION_THRESHOLD) * 0.1;
        }
        double annualInsuranceCosts = ANNUAL_INSURANCE_COST_BASE * insuranceRiskFactor;

        // Land costs (fixed)
        double annualLandCosts = ANNUAL_LAND_COST_BASE;

        // Other operational costs (fixed)
        double annualOtherOperationalCosts = ANNUAL_OTHER_OPERATIONAL_COST_BASE;

        // Regulatory and business charges (fixed)
        double annualRegulatoryCosts = ANNUAL_REGULATORY_COST_BASE;

        // ==========================
        // TOTAL ANNUAL O&M COSTS
        // ==========================
        
        return annualTurbineMaintenanceCosts + annualMarineStructureMaintenanceCosts + 
               annualOtherMaintenanceCosts + annualPeriodicMaintenanceCosts +
               annualInsuranceCosts + annualLandCosts + 
               annualOtherOperationalCosts + annualRegulatoryCosts;
    }

    // ==========================
    // OPERATIONAL ANALYSIS HELPERS
    // ==========================

    /**
     * Calculates operational frequency based on head variations.
     * Returns fraction of half-tides with significant operations (0-1).
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
        
        return (double) operations / halfTides;
    }

    /**
     * Calculates the maximum head variation across all half tides.
     * This represents the operational stress on the system.
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

        return maxHead - minHead;
    }
}
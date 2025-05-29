package src.model;

/**
 * Represents the fixed configuration of the Swansea Bay Tidal Lagoon for simulation and optimisation.
 * Constants are grouped by category: turbine, sluice, grid, and economic parameters.
 * This class is not meant ot be instantiated.
 * 
 * All parameters are accessible via static getter methods.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */


public final class Lagoon {

    // ----------------------
    // Turbine configuration 
    // ----------------------
    private static final int MUMBER_OF_TURBINES = 16;
    private static final double TURBINE_CAPACITY_MW = 20; // MW per turbine
    private static final double TURBINE_DIAMETER_M = 7.35; // Diameter in meters
    private static final double TURBINE_DISCHARGE_COEFFICIENT = 1.36; // Efficiency of the turbine (Dimensionless)
    private static final Orientation TURBINE_ORIENTATION = Orientation.BIDIRECTIONAL; // Orientation of the turbines

    // --------------------------
    // Sluice Gate configuration 
    // --------------------------
    private static final double SLUICE_AREA_M2 = 800; // Area of sluice gate in square meters
    private static final double SLUICE_DISCHARGE_COEFFICIENT = 1.0; // Efficiency of sluice gate (Dimensionless)

    // --------------------------
    // Grid configuration
    // --------------------------
    private static final int NUMBER_OF_GP = 95; // Number of grid points
    private static final double GRID_FREQUENCY_HZ = 50; // Frequency of the grid in Hertz

    // --------------------------
    // Economic parameters
    // --------------------------
    private static final double TOTAL_CAPITAL_COST = 1_300_000_000; // Total capital cost in GBP
    private static final double INSTALLED_CAPACITY_MW = 320.0; // Total installed capacity in MW
    

    private Lagoon() {
        throw new UnsupportedOperationException("Lagoon is a utility class and cannot be instantiated.");
    }

    // --------------------------
    // Public getter methods
    // --------------------------
    public static int getNumberOfTurbines() {
        return MUMBER_OF_TURBINES;
    }

    public static double getTurbineCapacityMW() {
        return TURBINE_CAPACITY_MW;
    }

    public static double getTurbineDiameterM() {
        return TURBINE_DIAMETER_M;
    }

    public static double getTurbineDischargeCoefficient() {
        return TURBINE_DISCHARGE_COEFFICIENT;
    }

    public static Orientation getTurbineOrientation() {
        return TURBINE_ORIENTATION;
    }

    public static double getSluiceAreaM2() {
        return SLUICE_AREA_M2;
    }

    public static double getSluiceDischargeCoefficient() {
        return SLUICE_DISCHARGE_COEFFICIENT;
    }

    public static int getNumberOfGridPoints() {
        return numberOfGP;
    }

    public static double getGridFrequencyHz() {
        return GRID_FREQUENCY_HZ;
    }

    public static double getTotalCapitalCost() {
        return TOTAL_CAPITAL_COST;
    }

    public static double getInstalledCapacityMW() {
        return INSTALLED_CAPACITY_MW;
    }

    /**
     * Calculates the unit cost of the lagoon based on total capital cost and installed capacity.
     * To be used in objectivefunction.java as part of fitness evaluation
     * 
     * @return The unit cost in GBP per MW.
     */
    public static double getUnitCost() {
        return TOTAL_CAPITAL_COST / INSTALLED_CAPACITY_MW; // Cost per MW
    }


    // --------------------------
    // Turbine Orientation Enum
    // --------------------------
    public enum Orientation {
        EBB, // EBB: Turbines operate during ebb tide (outgoing tide)
        FLOOD, // FLOOD: Turbines operate during flood tide (incoming tide)
        BIDIRECTIONAL; // BIDIRECTIONAL: Turbines can operate in both directions (incoming and outgoing tides)
    }
}

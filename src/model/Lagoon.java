package src.model;

/**
 * Represents the fixed configuration of the Swansea Bay Tidal Lagoon for simulation and optimisation.
 * Constants are grouped by category: turbine, sluice, grid, and economic parameters.
 * This class is not meant to be instantiated.
 * 
 * All parameters are accessible via static getter methods.
 * 
 * All the configuration parameters are based on Moreira et al. (2022) "Control Optimisation Baselines
 * for Tidal Range Structures—CoBaseTRS".
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public final class Lagoon {

    // ======================
    // LAGOON CONFIGURATION
    // ======================

    /** Surface area of the lagoon in square meters */
    private static final double LAGOON_SURFACE_AREA_M2 = 11_500_000.0; 

    // ======================
    // TURBINE CONFIGURATION
    // ======================

    /** Number of turbines in the lagoon */
    private static final int NUMBER_OF_TURBINES = 16;

    /** Capacity of each turbine in megawatts (MW) */
    private static final double TURBINE_CAPACITY_MW = 20.0; 

    /** Diameter of each turbine in meters */
    private static final double TURBINE_DIAMETER_M = 7.35;

    /** Discharge coefficient of the turbine, representing its efficiency - TUNEABLE parameter */
    private static final double TURBINE_DISCHARGE_COEFFICIENT = 0.9; 

    /** Operating orientation of the turbines */
    private static final Orientation TURBINE_ORIENTATION = Orientation.BIDIRECTIONAL; 

    // ==========================
    // SLUICE GATE CONFIGURATION 
    // ==========================

    /** Area of the sluice gate in square meters */
    private static final double SLUICE_AREA_M2 = 800.0; 

    /** Efficiency of sluice gate (dimensionless) */
    private static final double SLUICE_DISCHARGE_COEFFICIENT = 1.0; 

    // ===================
    // GRID CONFIGURATION
    // ===================

    /** Number of grid points in the simulation */
    private static final int NUMBER_OF_GRID_POINTS = 95; 

    /** Frequency of the grid in Hertz */
    private static final double GRID_FREQUENCY_HZ = 50.0; 

    // ====================
    // ECONOMIC PARAMETERS
    // ====================

    /** Total capital cost of the lagoon in GBP */
    private static final double TOTAL_CAPITAL_COST = 1_300_000_000.0; 

    /** Installed capacity of the lagoon in MW */
    private static final double INSTALLED_CAPACITY_MW = 320.0; 

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods and fields are static and should be accessed directly via the class name.
     * 
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
     */
    private Lagoon() {
        throw new UnsupportedOperationException("Lagoon is a utility class and cannot be instantiated.");
    }

    // ========================
    // LAGOON PROPERTY GETTERS
    // ========================

    public static double getLagoonSurfaceAreaM2() {
        return LAGOON_SURFACE_AREA_M2;
    }

    // =========================
    // TURBINE PROPERTY GETTERS
    // =========================

    public static int getNumberOfTurbines() {
        return NUMBER_OF_TURBINES;
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

    public static double getTotalTurbineCapacityMW() {
        return NUMBER_OF_TURBINES * TURBINE_CAPACITY_MW; 
    }

    // ========================
    // SLUICE PROPERTY GETTERS
    // ========================

    public static double getSluiceAreaM2() {
        return SLUICE_AREA_M2;
    }

    public static double getSluiceDischargeCoefficient() {
        return SLUICE_DISCHARGE_COEFFICIENT;
    }

    // ======================
    // Grid PROPERTY GETTERS
    // ======================

    public static int getNumberOfGridPoints() {
        return NUMBER_OF_GRID_POINTS;
    }

    public static double getGridFrequencyHz() {
        return GRID_FREQUENCY_HZ;
    }

    // ==========================
    // Economic PROPERTY GETTERS
    // ==========================

    public static double getTotalCapitalCost() {
        return TOTAL_CAPITAL_COST;
    }

    public static double getInstalledCapacityMW() {
        return INSTALLED_CAPACITY_MW;
    }

    public static double getUnitCostGBPPerMW() {
        return TOTAL_CAPITAL_COST / INSTALLED_CAPACITY_MW; 
    }


    // ======================
    // ENUMS
    // ======================
    /**
     * Enum representing turbine operating modes during tidal cycles.
     */
    public enum Orientation {
        /** Turbines operate during ebb tide (outgoing tide) */
        EBB, 
        /** Turbines operate during flood tide (incoming tide) */
        FLOOD, 
        /** Turbines can operate in both directions (incoming and outgoing tides) */
        BIDIRECTIONAL; 
    }
}

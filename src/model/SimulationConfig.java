package src.model;

/**
 * Configuration parameters for tidal lagoon simulation scenarios.
 * <p>
 * Centralises all simulation-related constants and provides
 * factory methods for different simulation types.
 * <p>
 * This class is not meant to be instantiated.
 * All parameters are accessible via static getter methods.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public final class SimulationConfig {

    // =======================
    // TEMPORAL CONFIGURATION
    // =======================

    /** Duration of half tide cycle in hours */
    private static final double HOURS_PER_HALF_TIDE = 6.12;

    /** Number of readings per half tide cycle */
    private static final int READINGS_PER_HALF_TIDE = 24;

    /** Time step for simulation in hours */
    private static final double TIME_STEP_HOURS = HOURS_PER_HALF_TIDE / READINGS_PER_HALF_TIDE;

    // ================================
    // PREDEFINED SIMULATION SCENARIOS
    // ================================

    /** Number of half tides in a day */
    private static final int DAILY_HALF_TIDES = (int) Math.round(24.0 / HOURS_PER_HALF_TIDE); 

    /** Number of half tides in a week */
    private static final int WEEKLY_HALF_TIDES = (int) Math.round(168.0 / HOURS_PER_HALF_TIDE); 

    /** Number of half tides in a year */
    private static final int ANNUAL_HALF_TIDES = (int) Math.round(8760.0 / HOURS_PER_HALF_TIDE); 

    /** Number of half tides in seasonal sample (4 weeks) */
    private static final int SEASONAL_SAMPLE_HALF_TIDES = (int) Math.round(672.0 / HOURS_PER_HALF_TIDE); 

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods and fields are static and should be accessed directly via the class name.
     * 
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
     */
    private SimulationConfig() {
        throw new UnsupportedOperationException("SimulationConfig is a utility class and cannot be instantiated.");
    }

    // ==========================
    // TEMPORAL PROPERTY GETTERS
    // ==========================

    public static double getTimeStepHours() {
        return TIME_STEP_HOURS;
    }

    public static int getReadingsPerHalfTide() {
        return READINGS_PER_HALF_TIDE;
    }

    public static double getHoursPerHalfTide() {
        return HOURS_PER_HALF_TIDE;
    }
    
    // ========================
    // FACTORY METHODS
    // ========================

    /**
     * Configuration for daily analysis (24 hours).
     * Suitable for operational parameter testing and quick optimisation runs.
     * 
     * @return SimulationParameters configured for daily operation.
     */
    public static SimulationParameters getDailyConfiguration() {
        return new SimulationParameters(DAILY_HALF_TIDES, "24-hour daily");
    }
    
    /**
     * Configuration for weekly analysis (7 days).
     * Provides a good balance between detail and computational efficiency.
     * 
     * @return SimulationParameters configured for weekly operation.
     */
    public static SimulationParameters getWeeklyConfiguration() {
        return new SimulationParameters(WEEKLY_HALF_TIDES, "1-week analysis");
    }
    
    /**
     * Configuration for annual analysis (365 days).
     * Useful for long-term performance evaluation and seasonal impact studies.
     * 
     * @return SimulationParameters configured for annual operation.
     */
    public static SimulationParameters getAnnualConfiguration() {
        return new SimulationParameters(ANNUAL_HALF_TIDES, "Annual (365 days)");
    }

    /**
     * Configuration for seasonal analysis (4 weeks).
     * Representative seasonal sample with moderate computational cost.
     * 
     * @return SimulationParameters configured for seasonal operation.
     */
    public static SimulationParameters getSeasonalConfiguration() {
        return new SimulationParameters(SEASONAL_SAMPLE_HALF_TIDES, "Seasonal (4 weeks)");
    }

    // =========================
    // UTILITY METHODS
    // =========================

    /**
     * Calculates the number of readings needed for a given number of half tides.
     * 
     * @param halfTides Number of half tides to simulate.
     * @return Total number of readings required.
     */
    public static int getReadingsNeeded(int halfTides) {
        return halfTides * READINGS_PER_HALF_TIDE;
    }
    
    /**
     * Calculates the total simulation duration in hours for a given number of half tides.
     * 
     * @param halfTides Number of half tides to simulate.
     * @return Total duration in hours.
     */
    public static double getSimulationDurationHours(int halfTides) {
        return halfTides * HOURS_PER_HALF_TIDE;
    }
    
    // =============================
    // CONFIGURATION DATA CONTAINER
    // =============================

    /**
     * Immutable container for simulation configuration parameters.
     * Provides convenient access to derived properties.
     */
    public static class SimulationParameters {
        private final int halfTides;
        private final String description;
        
        private SimulationParameters(int halfTides, String description) {
            this.halfTides = halfTides;
            this.description = description;
        }
        
        public int getHalfTides() { 
            return halfTides; 
        }

        public String getDescription() {
             return description;
        }

        public int getReadingsNeeded() {
             return SimulationConfig.getReadingsNeeded(halfTides);
        }
        
        public double getDurationHours() {
             return SimulationConfig.getSimulationDurationHours(halfTides);
        }

        @Override
        public String toString() {
            return String.format("%s (%d half-tides, %.1f hours)", 
                description, halfTides, getDurationHours());
        }
    }
}
    


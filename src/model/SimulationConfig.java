package src.model;

/**
 * Configuration parameters for tidal lagoon simulation scenarios.
 * Centralises all simulation-related constants and provides
 * factory methods for different simulation types.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class SimulationConfig {

    // -----------------------
    // Temporal configuration
    // -----------------------
    private static final double HOURS_PER_HALF_TIDE = 6.12;
    private static final int READINGS_PER_HALF_TIDE = 24;
    private static final double TIME_STEP_HOURS = HOURS_PER_HALF_TIDE / READINGS_PER_HALF_TIDE;

    // -----------------------
    // Predefined simulation scenarios
    // -----------------------
    private static final int DAILY_HALF_TIDES = (int) Math.round(24.0 / HOURS_PER_HALF_TIDE); // 4 half tides
    private static final int WEEKLY_HALF_TIDES = (int) Math.round(168.0 / HOURS_PER_HALF_TIDE); // 27 half tides
    private static final int ANNUAL_HALF_TIDES = (int) Math.round(8760.0 / HOURS_PER_HALF_TIDE); // 1431 half tides

    // Seasonal configuration constants
    private static final int MONTHLY_HALF_TIDES = (int) Math.round(720.0 / HOURS_PER_HALF_TIDE); // 118 half tides (~30 days)
    private static final int QUARTERLY_HALF_TIDES = (int) Math.round(2160.0 / HOURS_PER_HALF_TIDE); // 353 half tides (~90 days)
    private static final int SEASONAL_SAMPLE_HALF_TIDES = (int) Math.round(672.0 / HOURS_PER_HALF_TIDE); // 110 half tides (4 weeks)
    /*
     * Private constructor to prevent instantiation of this utility class.
     */
    private SimulationConfig() {
        throw new UnsupportedOperationException("SimulationConfig is a utility class and cannot be instantiated.");
    }

            // Add this getter method:
        public static double getTimeStepHours() {
            return TIME_STEP_HOURS;
        }

        // Also add for debugging:
        public static int getReadingsPerHalfTide() {
            return READINGS_PER_HALF_TIDE;
        }

        public static double getHoursPerHalfTide() {
            return HOURS_PER_HALF_TIDE;
        }
    
    // -----------------------
    // Factory methods for different simulation types
    // -----------------------
    public static SimulationParameters getDailyConfiguration() {
        return new SimulationParameters(DAILY_HALF_TIDES, "24-hour daily");
    }
    
    public static SimulationParameters getWeeklyConfiguration() {
        return new SimulationParameters(WEEKLY_HALF_TIDES, "1-week analysis");
    }
    
    public static SimulationParameters getAnnualConfiguration() {
        return new SimulationParameters(ANNUAL_HALF_TIDES, "Annual (365 days)");
    }

    // ------------------------
    // Seasonal configurations
    // ------------------------

    public static SimulationParameters getSeasonalConfiguration() {
        return new SimulationParameters(SEASONAL_SAMPLE_HALF_TIDES, "Seasonal (4 weeks)");
    }

    /**
     * Configuration for monthly analysis.
     * Good balance between seasonal representation and computational efficiency.
     */
    public static SimulationParameters getMonthlyConfiguration() {
        return new SimulationParameters(MONTHLY_HALF_TIDES, "Monthly (30 days)");
    }
    
    /**
     * Configuration for quarterly analysis.
     * Comprehensive seasonal representation with moderate scaling.
     */
    public static SimulationParameters getQuarterlyConfiguration() {
        return new SimulationParameters(QUARTERLY_HALF_TIDES, "Quarterly (90 days)");
    }
    
    /**
     * Configuration for custom period analysis.
     * Flexible method for specific day ranges.
     */
    public static SimulationParameters getCustomConfiguration(int days, String description) {
        int halfTides = (int) Math.round((days * 24.0) / HOURS_PER_HALF_TIDE);
        return new SimulationParameters(halfTides, description);
    }

    /**
     * Configuration for extended representative period.
     * Uses specified number of days for better seasonal coverage.
     */
    public static SimulationParameters getExtendedConfiguration(int days) {
        int halfTides = (int) Math.round((days * 24.0) / HOURS_PER_HALF_TIDE);
        return new SimulationParameters(halfTides, 
            String.format("Extended period (%d days)", days));
    }
    
    // -----------------------
    // Utility methods
    // -----------------------
    public static int getReadingsNeeded(int halfTides) {
        return halfTides * READINGS_PER_HALF_TIDE;
    }
    
    public static double getSimulationDurationHours(int halfTides) {
        return halfTides * HOURS_PER_HALF_TIDE;
    }
    
    // -----------------------
    // Configuration data container
    // -----------------------
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
    


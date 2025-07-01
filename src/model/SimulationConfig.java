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
    private static final int HALF_TIDES_PER_DAY = 4;
    private static final int READINGS_PER_HALF_TIDE = 24;
    private static final double TIME_STEP_HOURS = 0.25; 
    
    // -----------------------
    // Predefined simulation scenarios
    // -----------------------
    private static final int DAILY_HALF_TIDES = HALF_TIDES_PER_DAY; // 24 hours  
    private static final int WEEKLY_HALF_TIDES = HALF_TIDES_PER_DAY * 7; // 1 week
    private static final int ANNUAL_HALF_TIDES = HALF_TIDES_PER_DAY * 365; // 1 year
    
    /*
     * Private constructor to prevent instantiation of this utility class.
     */
    private SimulationConfig() {
        throw new UnsupportedOperationException("SimulationConfig is a utility class and cannot be instantiated.");
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
    
    // -----------------------
    // Utility methods
    // -----------------------
    public static int getReadingsNeeded(int halfTides) {
        return halfTides * READINGS_PER_HALF_TIDE;
    }
    
    public static double getSimulationDurationHours(int halfTides) {
        return getReadingsNeeded(halfTides) * TIME_STEP_HOURS;
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
    


package src.optimisation;

import src.model.SimulationConfig;
import java.util.Arrays;

/**
 * Configuration class for NSGA-II algorithm parameters.
 * 
 * Centralises all algorithm settings and provides factory methods
 * for different optimisation scenarios (daily, weekly, annual).
 * Uses the Builder pattern for flexible configuration creation with
 * comprehensive validation and sensible defaults.
 * 
 * Key features:
 * - Adaptive mutation rates based on problem dimensionality
 * - Pre-configured settings for different simulation scenarios
 * - Comprehensive parameter validation
 * - Factory methods for common use cases
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class NSGA2Config {

    // ========================
    // ALGORITHM PARAMETERS
    // ========================

    /** Population size for each generation */
    private final int populationSize;

    /** Maximum number of generations to run */
    private final int maxGenerations;

    /** Probability of crossover operation (0.0 to 1.0) */
    private final double crossoverProbability;

    /** Probability of mutation per gene (0.0 to 1.0) */
    private final double mutationProbability;

    /** Type of crossover operator to use */
    private final String crossoverType;

    /** Type of mutation operator to use */
    private final String mutationType;

    // ========================
    // SIMULATION PARAMETERS
    // ========================

    /** Number of half-tides in the simulation */
    private final int halfTides;

    /** Description of the simulation configuration */
    private final String simulationDescription;

    // ========================
    // CONVERGENCE PARAMETERS
    // ========================

    /** Threshold for convergence detection */
    private final double convergenceThreshold;

    /** Number of generations to check for stagnation */
    private final int stagnationGenerations;

    // ========================
    // ADVANCED PARAMETERS
    // ========================

    /** Tournament size for selection */
    private final int tournamentSize;

    /** Whether to use elitism */
    private final boolean useElitism;

    /** Random seed for reproducible results */
    private final long randomSeed;

    // ========================
    // CONFIGURATION CONSTANTS
    // ========================

    /** Minimum mutation rate to prevent truncation to zero */
    private static final double MIN_MUTATION_RATE = 0.005;

    /** Maximum computational effort warning threshold */
    private static final long COMPUTATIONAL_EFFORT_WARNING = 1_000_000;

    /** High mutation probability warning threshold */
    private static final double HIGH_MUTATION_WARNING = 0.5;

    /** Low crossover probability warning threshold */
    private static final double LOW_CROSSOVER_WARNING = 0.5;

    /** Tolerance for mutation rate recommendation comparison */
    private static final double MUTATION_RATE_TOLERANCE = 0.05;

    /**
     * Private constructor used by Builder pattern.
     *
     * @param builder Builder instance containing configuration parameters
     */
    private NSGA2Config(Builder builder) {
        this.populationSize = builder.populationSize;
        this.maxGenerations = builder.maxGenerations;
        this.crossoverProbability = builder.crossoverProbability;
        this.mutationProbability = builder.mutationProbability;
        this.crossoverType = builder.crossoverType;
        this.mutationType = builder.mutationType;
        this.halfTides = builder.halfTides;
        this.simulationDescription = builder.simulationDescription;
        this.convergenceThreshold = builder.convergenceThreshold;
        this.stagnationGenerations = builder.stagnationGenerations;
        this.tournamentSize = builder.tournamentSize;
        this.useElitism = builder.useElitism;
        this.randomSeed = builder.randomSeed;
    }

    // ========================
    // FACTORY METHODS
    // ========================

    /**
     * Configuration for daily optimisation.
     * 
     * Optimised for 24-hour simulation periods with moderate
     * population size and generations for balanced performance.
     * Finds the optimal day and gets scaled by 365x.
     * 
     * @return NSGA2Config configured for daily optimisation
     */
    public static NSGA2Config getDailyConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getDailyConfiguration();

        double decisionVariables = simParameters.getHalfTides() * 2;
        double adaptiveMutationRate = 1.0 / decisionVariables;
        double finalMutationRate = Math.max(adaptiveMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(200)
                .maxGenerations(100)
                .crossoverProbability(0.85)
                .mutationProbability(finalMutationRate * 1.5) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.01)
                .stagnationGenerations(25)
                .build();
    }

    /**
     * Configuration for weekly optimisation.
     * 
     * Larger population and more generations for more complex 
     * problem with increased half-tide cycles. Finds the 
     * optimal week and gets scaled by 52x.
     * 
     * @return NSGA2Config configured for weekly optimisation
     */
    public static NSGA2Config getWeeklyConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getWeeklyConfiguration();

        double decisionVariables = simParameters.getHalfTides() * 2;
        double adaptiveMutationRate = 1.0 / decisionVariables;
        double finalMutationRate = Math.max(adaptiveMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(500)
                .maxGenerations(250)
                .crossoverProbability(0.85)
                .mutationProbability(finalMutationRate) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.01)
                .stagnationGenerations(30)
                .build();
    }

    /**
     * Configuration for annual optimisation.
     * 
     * Designed for long-term optimisation with a larger population
     * and more generations. Uses the seasonal configuration which 
     * takes the optimal week from each season and scales it by 13x.
     * 
     * @return NSGA2Config configured for annual optimisation
     */
    public static NSGA2Config getAnnualConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getSeasonalConfiguration();

        double decisionVariables = simParameters.getHalfTides() * 2;
        double adaptiveMutationRate = 1.0 / decisionVariables;
        double finalMutationRate = Math.max(adaptiveMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(400)
                .maxGenerations(300)
                .crossoverProbability(0.9)
                .mutationProbability(0.015)
                .crossoverType("HALFTIDE")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.01)
                .stagnationGenerations(50)
                .tournamentSize(4)
                .elitism(true)
                .randomSeed(System.currentTimeMillis())
                .build();
    }

    // ========================
    // PROPERTY ACCESSORS
    // ========================

    public int getPopulationSize() {
        return populationSize;
    }

    public int getMaxGenerations() {
        return maxGenerations;
    }

    public double getCrossoverProbability() {
        return crossoverProbability;
    }

    public double getMutationProbability() {
        return mutationProbability;
    }

    public String getCrossoverType() {
        return crossoverType;
    }

    public String getMutationType() {
        return mutationType;
    }

    public int getHalfTides() {
        return halfTides;
    }

    public String getSimulationDescription() {
        return simulationDescription;
    }

    public double getConvergenceThreshold() {
        return convergenceThreshold;
    }

    public int getStagnationGenerations() {
        return stagnationGenerations;
    }

    public int getTournamentSize() {
        return tournamentSize;
    }

    public boolean isUseElitism() {
        return useElitism;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    // ========================
    // DERIVED PROPERTIES
    // ========================

    /**
     * Gets the number of decision variables.
     * 
     * Each half-tide corresponds to two decision variables (start and end heads).
     * 
     * @return Total number of decision variables
     */
    public int getDecisionVariables() {
        return halfTides * 2;
    }

    /**
     * Gets the total computational effort.
     * 
     * Calculates the maximum number of fitness evaluations
     * based on the population size and maximum generations.
     * 
     * @return Estimated computational effort in fitness evaluations
     */
    public long getComputationalEffort() {
        return (long) populationSize * maxGenerations;
    }

    // ========================
    // CONFIGURATION UTILITIES
    // ========================

    /**
     * Creates a copy of this configuration with modified population size.
     * 
     * @param newPopulationSize New population size
     * @return New NSGA2Config instance with updated population size
     */
    public NSGA2Config withPopulationSize(int newPopulationSize) {
        return new Builder()
                .populationSize(newPopulationSize)
                .maxGenerations(this.maxGenerations)
                .crossoverProbability(this.crossoverProbability)
                .mutationProbability(this.mutationProbability)
                .crossoverType(this.crossoverType)
                .mutationType(this.mutationType)
                .halfTides(this.halfTides)
                .simulationDescription(this.simulationDescription)
                .convergenceThreshold(this.convergenceThreshold)
                .stagnationGenerations(this.stagnationGenerations)
                .tournamentSize(this.tournamentSize)
                .elitism(this.useElitism)
                .randomSeed(this.randomSeed)
                .build();
    }
    
    /**
     * Creates a copy with modified genetic operators.
     * 
     * @param crossoverType New crossover type
     * @param mutationType New mutation type
     * @return New NSGA2Config instance with updated operators
     */
    public NSGA2Config withOperators(String crossoverType, String mutationType) {
        return new Builder()
                .populationSize(this.populationSize)
                .maxGenerations(this.maxGenerations)
                .crossoverProbability(this.crossoverProbability)
                .mutationProbability(this.mutationProbability)
                .crossoverType(crossoverType)
                .mutationType(mutationType)
                .halfTides(this.halfTides)
                .simulationDescription(this.simulationDescription + " - " + crossoverType + "/" + mutationType)
                .convergenceThreshold(this.convergenceThreshold)
                .stagnationGenerations(this.stagnationGenerations)
                .tournamentSize(this.tournamentSize)
                .elitism(this.useElitism)
                .randomSeed(this.randomSeed)
                .build();
    }
    
    /**
     * Validates configuration for potential issues and prints warnings.
     * 
     * Checks for common configuration problems such as excessive
     * computational effort, extreme parameter values, and deviations
     * from recommended settings.
     */
    public void validate() {
        if (getComputationalEffort() > COMPUTATIONAL_EFFORT_WARNING) {
            System.out.printf("Warning: High computational effort (%,d evaluations) - consider reducing population or generations%n", 
                             getComputationalEffort());
        }
        
        if (mutationProbability > HIGH_MUTATION_WARNING) {
            System.out.printf("Warning: High mutation probability (%.3f) may cause excessive disruption%n", mutationProbability);
        }

        if (crossoverProbability < LOW_CROSSOVER_WARNING) {
            System.out.printf("Warning: Low crossover probability (%.3f) may slow convergence%n", crossoverProbability);
        }
        
        double recommendedMutationProb = 1.0 / getDecisionVariables();
        if (Math.abs(mutationProbability - recommendedMutationProb) > 0.05) {
            System.out.printf("Note: Mutation probability (%.4f) differs from recommended 1/n_vars (%.4f)%n", 
                             mutationProbability, recommendedMutationProb);
        }
    }

    // ========================
    // STRING REPRESENTATION
    // ========================

    /**
     * Returns a concise string representation of the configuration.
     * 
     * @return Brief configuration summary
     */
    @Override
    public String toString() {
        return String.format("NSGA2Config[Pop=%d, Gen=%d, Pc=%.2f, Pm=%.4f, %s/%s, HalfTides=%d]",
                populationSize, maxGenerations, crossoverProbability, mutationProbability,
                crossoverType, mutationType, halfTides);
    }

    /**
     * Returns detailed configuration summary.
     * 
     * Provides comprehensive information about all configuration
     * parameters formatted for easy reading and debugging.
     * 
     * @return Detailed configuration summary
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n===NSGA-II Configuration ===\n");
        sb.append(String.format("Population Size: %d\n", populationSize));
        sb.append(String.format("Max Generations: %d\n", maxGenerations));
        sb.append(String.format("Crossover Type: %s (%.2f)\n", crossoverType, crossoverProbability));
        sb.append(String.format("Mutation Type: %s (%.4f)\n", mutationType, mutationProbability));
        sb.append(String.format("Number of half-tides: %d (%d variables)\n", halfTides, getDecisionVariables()));
        sb.append(String.format("Simulation Type: %s\n", simulationDescription));
        sb.append(String.format("Convergence: %.6f threshold, %d stagnation generations\n", 
                                convergenceThreshold, stagnationGenerations));
        sb.append(String.format("Computational Effort: %,d evaluations\n", getComputationalEffort()));
        return sb.toString();
    }

    // ========================
    // BUILDER PATTERN
    // ========================

    /**
     * Builder class for creating NSGA2Config instances.
     * 
     * Provides a flexible way to construct configurations with
     * validation and sensible defaults for all parameters.
     */
    public static class Builder {
        // Required parameters with defaults
        private int populationSize = 50;
        private int maxGenerations = 100;
        private int halfTides = 4;

        // Optional parameters with defaults
        private double crossoverProbability = 0.9;
        private double mutationProbability = 0.1;
        private String crossoverType = "SBX";
        private String mutationType = "GAUSSIAN"; 
        private String simulationDescription = "Default Simulation";
        private double convergenceThreshold = 0.001;
        private int stagnationGenerations = 15;
        private int tournamentSize = 2;
        private boolean useElitism = true;
        private long randomSeed = System.currentTimeMillis();

        /**
         * Sets the population size.
         * 
         * @param populationSize Size of the population (must be at least 4)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if population size is invalid
         */
        public Builder populationSize(int populationSize) {
            if (populationSize < 4) {
                throw new IllegalArgumentException("Population size must be at least 4");
            }
            this.populationSize = populationSize;
            return this;
        }

        /**
         * Sets the maximum generations.
         * 
         * @param maxGenerations Maximum generations (must be at least 1)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if max generations is invalid
         */
        public Builder maxGenerations(int maxGenerations) {
            if (maxGenerations < 1) {
                throw new IllegalArgumentException("Max generations must be at least 1");
            }
            this.maxGenerations = maxGenerations;
            return this;
        }
        
        /**
         * Sets the crossover probability.
         * 
         * @param crossoverProbability Crossover probability (0.0 to 1.0)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if probability is out of range
         */
        public Builder crossoverProbability(double crossoverProbability) {
            if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
                throw new IllegalArgumentException("Crossover probability must be between 0.0 and 1.0");
            }
            this.crossoverProbability = crossoverProbability;
            return this;
        }
        
        /**
         * Sets the mutation probability.
         * 
         * @param mutationProbability Mutation probability (0.0 to 1.0)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if probability is out of range
         */
        public Builder mutationProbability(double mutationProbability) {
            if (mutationProbability < 0.0 || mutationProbability > 1.0) {
                throw new IllegalArgumentException("Mutation probability must be between 0.0 and 1.0");
            }
            this.mutationProbability = mutationProbability;
            return this;
        }
        
        /**
         * Sets the crossover type.
         * 
         * @param crossoverType Crossover type (SBX, UNIFORM, HALFTIDE)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if unsupported crossover type is provided
         */
        public Builder crossoverType(String crossoverType) {
            if (!Arrays.asList("SBX", "UNIFORM", "HALFTIDE").contains(crossoverType.toUpperCase())) {
                throw new IllegalArgumentException("Unsupported crossover type: " + crossoverType);
            }
            this.crossoverType = crossoverType.toUpperCase();
            return this;
        }
        
        /**
         * Sets the mutation type.
         * 
         * @param mutationType Mutation type (POLYNOMIAL, GAUSSIAN, OPERATIONAL)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if unsupported mutation type is provided
         */
        public Builder mutationType(String mutationType) {
            if (!Arrays.asList("POLYNOMIAL", "GAUSSIAN", "OPERATIONAL").contains(mutationType.toUpperCase())) {
                throw new IllegalArgumentException("Unsupported mutation type: " + mutationType);
            }
            this.mutationType = mutationType.toUpperCase();
            return this;
        }
        
        /**
         * Sets the number of half-tides.
         * 
         * @param halfTides Number of half-tides (must be at least 1)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if half-tides is less than 1
         */
        public Builder halfTides(int halfTides) {
            if (halfTides < 1) {
                throw new IllegalArgumentException("Half-tides must be at least 1");
            }
            this.halfTides = halfTides;
            return this;
        }
        
        /**
         * Sets the simulation description.
         * 
         * @param simulationDescription Description of the simulation
         * @return Builder instance for method chaining
         */
        public Builder simulationDescription(String simulationDescription) {
            this.simulationDescription = simulationDescription;
            return this;
        }
        
        /**
         * Sets the convergence threshold.
         * 
         * @param convergenceThreshold Threshold for convergence detection (must be non-negative)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if threshold is negative
         */
        public Builder convergenceThreshold(double convergenceThreshold) {
            if (convergenceThreshold < 0.0) {
                throw new IllegalArgumentException("Convergence threshold must be non-negative");
            }
            this.convergenceThreshold = convergenceThreshold;
            return this;
        }
        
        /**
         * Sets the number of generations to check for stagnation.
         * 
         * @param stagnationGenerations Number of generations (must be at least 1)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if stagnation generations is less than 1
         */
        public Builder stagnationGenerations(int stagnationGenerations) {
            if (stagnationGenerations < 1) {
                throw new IllegalArgumentException("Stagnation generations must be at least 1");
            }
            this.stagnationGenerations = stagnationGenerations;
            return this;
        }
        
        /**
         * Sets the tournament size for selection.
         * 
         * @param tournamentSize Size of the tournament (must be at least 2)
         * @return Builder instance for method chaining
         * @throws IllegalArgumentException if tournament size is less than 2
         */
        public Builder tournamentSize(int tournamentSize) {
            if (tournamentSize < 2) {
                throw new IllegalArgumentException("Tournament size must be at least 2");
            }
            this.tournamentSize = tournamentSize;
            return this;
        }
        
        /**
         * Sets whether to use elitism.
         * 
         * @param elitism True to use elitism, false otherwise
         * @return Builder instance for method chaining
         */
        public Builder elitism(boolean elitism) {
            this.useElitism = elitism;
            return this;
        }
        
        /**
         * Sets the random seed for reproducibility.
         * 
         * @param randomSeed Seed value for random number generation
         * @return Builder instance for method chaining
         */
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }
        
        /**
         * Builds the NSGA2Config instance with the current settings.
         * 
         * Validates parameters and ensures sensible defaults are applied.
         * 
         * @return New NSGA2Config instance
         * @throws IllegalArgumentException if any parameters are invalid
         */
        public NSGA2Config build() {
            // Validation
            if (populationSize % 2 != 0) {
                throw new IllegalArgumentException("Population size should be even for proper pairing");
            }
            
            return new NSGA2Config(this);
        }
    } 
}

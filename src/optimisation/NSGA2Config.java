package src.optimisation;

import src.model.SimulationConfig;
import java.util.Arrays;

/**
 * Configuration class for NSGA-II algorithm parameters.
 * 
 * Centralises all algorithm settings and provides factory methods
 * for different optimisation scenarios (testing, daily, weekly, annual)
 * 
 * @author Emre Kaygusuz
 * @version 1.1 - Fixed mutation rate calculation with minimum threshold.
 */
public class NSGA2Config {

    // NSGA-II parameters
    private final int populationSize;
    private final int maxGenerations;
    private final double crossoverProbability;
    private final double mutationProbability;
    private final String crossoverType;
    private final String mutationType;

    // Simulation parameters
    private final int halfTides;
    private final String simulationDescription;

    // Convergence parameters
    private final double convergenceThreshold;
    private final int stagnationGenerations;

    // Advanced parameters
    private final int tournamentSize;
    private final boolean useElitism;
    private final long randomSeed;

    // Minimum mutation rate to prevent truncation to 0
    private static final double MIN_MUTATION_RATE = 0.005;


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

    // == Factory Methods ==

    /**
     * Quick testing configuration. Small population and few generations.
     */
    public static NSGA2Config getTestConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getTestConfiguration();

        return new Builder()
                .populationSize(20)
                .maxGenerations(50)
                .crossoverProbability(0.9)
                .mutationProbability(0.1)
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.001)
                .stagnationGenerations(10)
                .build();
    }


    /**
     * Configuration for daily optimisation. Moderate population and generations.
     */
    public static NSGA2Config getDailyConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getDailyConfiguration();

        // Calculate mutation rate with minimum threshold
        double calculateMutationRate = Math.max(0.005, 0.1 / Math.sqrt(simParameters.getHalfTides()));
        double finalMutationRate = Math.max(calculateMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(200)
                .maxGenerations(500)
                .crossoverProbability(0.80)
                .mutationProbability(finalMutationRate) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.001)
                .stagnationGenerations(50)
                .build();
    }


    /**
     * Configuration for weekly optimisation. Larger population and more generations for more complex problem.
     */
    public static NSGA2Config getWeeklyConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getWeeklyConfiguration();

        // Calculate mutation rate with minimum threshold (consistent with other configs)
        double calculateMutationRate = Math.max(0.005, 0.1 / Math.sqrt(simParameters.getHalfTides()));
        double finalMutationRate = Math.max(calculateMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(100)
                .maxGenerations(200)
                .crossoverProbability(0.9)
                .mutationProbability(finalMutationRate) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.001)
                .stagnationGenerations(100)
                .build();
    }


    /**
     * Configuration for annual optimisation. Large population and many generations for comprehensive search.
     */
    public static NSGA2Config getAnnualConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getAnnualConfiguration();

        double calculateMutationRate = Math.max(0.005, 0.1 / Math.sqrt(simParameters.getHalfTides()));
        double finalMutationRate = Math.max(calculateMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(1000)
                .maxGenerations(1500)
                .crossoverProbability(0.80)
                .mutationProbability(finalMutationRate) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription(simParameters.getDescription())
                .convergenceThreshold(0.001)
                .stagnationGenerations(200)
                .build();
    }


    /*
     * High-performance configuration for research purposes.
     */
    public static NSGA2Config getResearchConfig() {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getAnnualConfiguration();

        double calculatedMutationRate = 1.0 / (simParameters.getHalfTides() * 2);
        double finalMutationRate = Math.max(calculatedMutationRate, MIN_MUTATION_RATE);

        return new Builder()
                .populationSize(500)
                .maxGenerations(1000)
                .crossoverProbability(0.9)
                .mutationProbability(finalMutationRate) 
                .crossoverType("SBX")
                .mutationType("GAUSSIAN")
                .halfTides(simParameters.getHalfTides())
                .simulationDescription("Research Configuration - annual optimisation")
                .convergenceThreshold(0.00001)
                .stagnationGenerations(50)
                .build();
    }


    public static NSGA2Config compareConfigs(String crossoverType, String mutationType) {
        SimulationConfig.SimulationParameters simParameters = SimulationConfig.getDailyConfiguration();

        return new Builder()
                .populationSize(50)
                .maxGenerations(100)
                .crossoverProbability(0.9)
                .mutationProbability(0.1) 
                .crossoverType(crossoverType)
                .mutationType(mutationType)
                .halfTides(simParameters.getHalfTides())
                .simulationDescription("Operator comparison: " + crossoverType + " and " + mutationType)
                .convergenceThreshold(0.001)
                .stagnationGenerations(15)
                .build();
    }
    
    // == Getters ==
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

    public int getDecisionVariables() {
        return halfTides * 2; // Each half tide has two decision variables
    }

    public long getComputationalEffort() {
        return (long) populationSize * maxGenerations;
    }

    @Override
    public String toString() {
        return String.format("NSGA2Config[Pop=%d, Gen=%d, Pc=%.2f, Pm=%.4f, %s/%s, HalfTides=%d]",
                populationSize, maxGenerations, crossoverProbability, mutationProbability,
                crossoverType, mutationType, halfTides);
    }

    /**
     * Returns detailed configuration summary.
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

    // == Builder Pattern ==
    
    public static class Builder {
        // Required parameters
        private int populationSize = 50;
        private int maxGenerations = 100;
        private int halfTides = 4; // Default to 4 half-tides

        // Optional parameters
        private double crossoverProbability = 0.9;
        private double mutationProbability = 0.1;
        private String crossoverType = "SBX";
        private String mutationType = "GAUSSIAN"; 
        private String simulationDescription = "Custom Configuration";
        private double convergenceThreshold = 0.001;
        private int stagnationGenerations = 15;
        private int tournamentSize = 2; // Default tournament size
        private boolean useElitism = true; // Default to using elitism
        private long randomSeed = System.currentTimeMillis(); // Default to current time

        public Builder populationSize(int populationSize) {
            if (populationSize < 4) {
                throw new IllegalArgumentException("Population size must be at least 4");
            }
            this.populationSize = populationSize;
            return this;
        }

        public Builder maxGenerations(int maxGenerations) {
            if (maxGenerations < 1) {
                throw new IllegalArgumentException("Max generations must be at least 1");
            }
            this.maxGenerations = maxGenerations;
            return this;
        }
        
        public Builder crossoverProbability(double crossoverProbability) {
            if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
                throw new IllegalArgumentException("Crossover probability must be between 0.0 and 1.0");
            }
            this.crossoverProbability = crossoverProbability;
            return this;
        }
        
        public Builder mutationProbability(double mutationProbability) {
            if (mutationProbability < 0.0 || mutationProbability > 1.0) {
                throw new IllegalArgumentException("Mutation probability must be between 0.0 and 1.0");
            }
            this.mutationProbability = mutationProbability;
            return this;
        }
        
        public Builder crossoverType(String crossoverType) {
            if (!Arrays.asList("SBX", "UNIFORM", "HALFTIDE").contains(crossoverType.toUpperCase())) {
                throw new IllegalArgumentException("Unsupported crossover type: " + crossoverType);
            }
            this.crossoverType = crossoverType.toUpperCase();
            return this;
        }
        
        public Builder mutationType(String mutationType) {
            if (!Arrays.asList("POLYNOMIAL", "GAUSSIAN", "OPERATIONAL").contains(mutationType.toUpperCase())) {
                throw new IllegalArgumentException("Unsupported mutation type: " + mutationType);
            }
            this.mutationType = mutationType.toUpperCase();
            return this;
        }
        
        public Builder halfTides(int halfTides) {
            if (halfTides < 1) {
                throw new IllegalArgumentException("Half-tides must be at least 1");
            }
            this.halfTides = halfTides;
            return this;
        }
        
        public Builder simulationDescription(String simulationDescription) {
            this.simulationDescription = simulationDescription;
            return this;
        }
        
        public Builder convergenceThreshold(double convergenceThreshold) {
            if (convergenceThreshold < 0.0) {
                throw new IllegalArgumentException("Convergence threshold must be non-negative");
            }
            this.convergenceThreshold = convergenceThreshold;
            return this;
        }
        
        public Builder stagnationGenerations(int stagnationGenerations) {
            if (stagnationGenerations < 1) {
                throw new IllegalArgumentException("Stagnation generations must be at least 1");
            }
            this.stagnationGenerations = stagnationGenerations;
            return this;
        }
        
        public Builder tournamentSize(int tournamentSize) {
            if (tournamentSize < 2) {
                throw new IllegalArgumentException("Tournament size must be at least 2");
            }
            this.tournamentSize = tournamentSize;
            return this;
        }
        
        public Builder elitism(boolean elitism) {
            this.useElitism = elitism;
            return this;
        }
        
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }
        
        public NSGA2Config build() {
            // Validation
            if (populationSize % 2 != 0) {
                throw new IllegalArgumentException("Population size should be even for proper pairing");
            }
            
            return new NSGA2Config(this);
        }
    }
    
    // ======================== Utility Methods ========================
    
    /**
     * Creates a copy of this configuration with modified parameters.
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
     * Validates configuration for potential issues.
     */
    public void validate() {
        if (getComputationalEffort() > 1_000_000) {
            System.out.printf("Warning: High computational effort (%,d evaluations) - consider reducing population or generations%n", 
                             getComputationalEffort());
        }
        
        if (mutationProbability > 0.5) {
            System.out.printf("Warning: High mutation probability (%.3f) may cause excessive disruption%n", mutationProbability);
        }
        
        if (crossoverProbability < 0.5) {
            System.out.printf("Warning: Low crossover probability (%.3f) may slow convergence%n", crossoverProbability);
        }
        
        double recommendedMutationProb = 1.0 / getDecisionVariables();
        if (Math.abs(mutationProbability - recommendedMutationProb) > 0.05) {
            System.out.printf("Note: Mutation probability (%.4f) differs from recommended 1/n_vars (%.4f)%n", 
                             mutationProbability, recommendedMutationProb);
        }
    }
}

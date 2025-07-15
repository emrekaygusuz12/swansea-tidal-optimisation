package src.optimisation;

import java.util.*;

/**
 * Implements genetic operators (crossover and mutation) for tidal lagoon optimisation.
 * 
 * Operators are designed specifically for tidal lagoon decision variables:
 * - Each half-tide has two control parameters: Hs (starting head) and He (ending head)
 * - Decision vector: [Hs_0, He_0, Hs_1, He_1, ..., Hs_N, He_N]
 * - Constraints: 1.0m ≤ head ≤ 4.0m for all head values
 * 
 * Based on real-coded genetic algorithms for continuous optimisation problems.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class GeneticOperators {

    // ==========================
    // CONSTRAINT CONSTANTS
    // ==========================

    /** Minimum head value in meters */
    private static final double MIN_HEAD = 1.0;

    /** Maximum head value in meters */
    private static final double MAX_HEAD = 4.0;

    // ==========================
    // CROSSOVER PARAMETERS
    // ==========================

    /** Distribution index for Simulated Binary Crossover - lower value for more exploration */
    private static final double SBX_ETA = 0.9;

    // ==========================
    // MUTATION PARAMETERS
    // ==========================

    /** Distribution index for polynomial mutation */
    private static final double MUTATION_ETA = 18;

    // ==========================
    // THREAD LOCAL RANDOM
    // ==========================

    /**
     * Thread-local random generator for better performance in multi-threaded environments.
     * 
     */
    private static final ThreadLocal<Random> threadLocalRandom =
            ThreadLocal.withInitial(Random::new);

    // ==========================
    // OPERATOR CONFIGURATION
    // ==========================

    /**
     * Configuration parameters for genetic operators.
     * Provides default values for various mutation strategies.
     */
    public static class OperatorConfig{
        /** Default range for mutation perturbation */
        public static final double DEFAULT_PERTURBATION_RANGE = 0.5;

        /** Default threshold for operational mutation strategy */
        public static final double DEFAULT_STRATEGY_THRESHOLD = 0.7;

        /** Default strength for difference adjustment in operational mutation */
        public static final double DEFAULT_DIFFERENCE_STRENGTH = 0.2;

        /** Default standard deviation for Gaussian mutation */
        public static final double DEFAULT_GAUSSIAN_STRENGTH = 0.77;

        private OperatorConfig() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods are static and should be accessed directly via the class name.
     * 
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
     */
    private GeneticOperators() {
        throw new UnsupportedOperationException("GeneticOperators is a utility class and cannot be instantiated");
    }

    // ==========================
    // CROSSOVER OPERATORS
    // ==========================

    /**
     * Performs Simulated Binary Crossover (SBX) between two parents.
     * 
     * SBX maintains the relationship between parent and offspring distances
     * and is particularly effective for real-valued optimisation problems.
     * 
     * @param parent1 The first parent individual
     * @param parent2 The second parent individual
     * @param crossoverProbability Probability of performing crossover
     * @return An array containing two offspring individuals
     * @throws IllegalArgumentException if parents are null or crossover probability is invalid
     */
    public static Individual[] simulatedBinaryCrossover(Individual parent1, Individual parent2,
            double crossoverProbability) {
        if (parent1 == null || parent2 == null) {
            throw new IllegalArgumentException("Parents cannot be null");
        }

        if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
            throw new IllegalArgumentException("Crossover probability must be between 0 and 1");
        }

        double[] vars1 = parent1.getDecisionVariables().clone();
        double[] vars2 = parent2.getDecisionVariables().clone();

        if (threadLocalRandom.get().nextDouble() <= crossoverProbability) {
            for (int i = 0; i < vars1.length; i++) {
                if (threadLocalRandom.get().nextDouble() <= 0.5) { // Gene-wise crossover probability
                    double y1 = Math.min(vars1[i], vars2[i]);
                    double y2 = Math.max(vars1[i], vars2[i]);

                    if (Math.abs(y2 - y1) > 1e-14) { // Avoid division by zero
                        double rand = threadLocalRandom.get().nextDouble();
                        double beta;

                        if (rand <= 0.5) {
                            beta = Math.pow(2.0 * rand, 1.0 / (SBX_ETA + 1.0));
                        } else {
                            beta = Math.pow(1.0 / (2.0 * (1.0 - rand)), 1.0 / (SBX_ETA + 1.0));
                        }

                        double child1 = 0.5 * ((y1 + y2) - beta * (y2 - y1));
                        double child2 = 0.5 * ((y1 + y2) + beta * (y2 - y1));

                        // Apply constraints
                        vars1[i] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, child1));
                        vars2[i] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, child2));
                    }
                }
            }
        }

        // Create offspring
        Individual offSpring1 = new Individual(vars1.length / 2);
        Individual offSpring2 = new Individual(vars2.length / 2);

        // Copy decision variables
        System.arraycopy(vars1, 0, offSpring1.getDecisionVariables(), 0, vars1.length);
        System.arraycopy(vars2, 0, offSpring2.getDecisionVariables(), 0, vars2.length);

        return new Individual[] { offSpring1, offSpring2 };
    }

    /**
     * Performs uniform crossover between two parents.
     * 
     * Simple crossover where each gene is randomly inherited from either parent.
     * Good for maintaining diversity in the population.
     *
     * @param parent1 The first parent individual
     * @param parent2 The second parent individual
     * @param crossoverProbability Probability of performing crossover
     * @return An array containing two offspring individuals
     * @throws IllegalArgumentException if parents are null or crossover probability is invalid
     */
    public static Individual[] uniformCrossover(Individual parent1, Individual parent2,
            double crossoverProbability) {
        if (parent1 == null || parent2 == null) {
            throw new IllegalArgumentException("Parents cannot be null");
        }

        if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
            throw new IllegalArgumentException("Crossover probability must be between 0 and 1");
        }


        double[] vars1 = parent1.getDecisionVariables().clone();
        double[] vars2 = parent2.getDecisionVariables().clone();

        if (threadLocalRandom.get().nextDouble() <= crossoverProbability) {
            for (int i = 0; i < vars1.length; i++) {
                if (threadLocalRandom.get().nextDouble() < 0.5) { // Gene-wise crossover
                    // Swap genes
                    double temp = vars1[i];
                    vars1[i] = vars2[i];
                    vars2[i] = temp;
                }
            }
        }

        // Create offspring
        Individual offSpring1 = new Individual(vars1.length / 2);
        Individual offSpring2 = new Individual(vars2.length / 2);

        // Copy decision variables
        System.arraycopy(vars1, 0, offSpring1.getDecisionVariables(), 0, vars1.length);
        System.arraycopy(vars2, 0, offSpring2.getDecisionVariables(), 0, vars2.length);

        return new Individual[] { offSpring1, offSpring2 };
    }

    /**
     * Performs half-tide crossover that maintains operational relationships.
     * 
     * Crosses over complete half-tide parameters (Hs, He pairs) rather than
     * individual variables to preserve operational feasibility.
     * 
     * @param parent1 The first parent individual
     * @param parent2 The second parent individual
     * @param crossoverProbability Probability of performing crossover
     * @return An array containing two offspring individuals
     * @throws IllegalArgumentException if parents are null or crossover probability is invalid
     */
    public static Individual[] halfTideCrossover(Individual parent1, Individual parent2,
            double crossoverProbability) {
        if (parent1 == null || parent2 == null) {
            throw new IllegalArgumentException("Parents cannot be null");
        }

        if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
            throw new IllegalArgumentException("Crossover probability must be between 0 and 1");
        }

        double[] vars1 = parent1.getDecisionVariables().clone();
        double[] vars2 = parent2.getDecisionVariables().clone();

        if (threadLocalRandom.get().nextDouble() <= crossoverProbability) {
            int numHalfTides = vars1.length / 2; // Each half-tide has two variables (Hs and He)

            for (int halfTide = 0; halfTide < numHalfTides; halfTide++) { // Process half-tide pairs
                if (threadLocalRandom.get().nextDouble() < 0.5) { // Gene-wise crossover
                    // Swap entire half-tide parameters
                    int hsIndex = halfTide * 2; // Hs index
                    int heIndex = hsIndex + 1; // He index

                    // Swap Hs values
                    double tempHs = vars1[hsIndex];
                    vars1[hsIndex] = vars2[hsIndex];
                    vars2[hsIndex] = tempHs;

                    // Swap He values
                    double tempHe = vars1[heIndex];
                    vars1[heIndex] = vars2[heIndex];
                    vars2[heIndex] = tempHe;
                }
            }
        }

        // Create offspring
        Individual offSpring1 = new Individual(vars1.length / 2);
        Individual offSpring2 = new Individual(vars2.length / 2);

        // Copy decision variables
        System.arraycopy(vars1, 0, offSpring1.getDecisionVariables(), 0, vars1.length);
        System.arraycopy(vars2, 0, offSpring2.getDecisionVariables(), 0, vars2.length);

        return new Individual[] { offSpring1, offSpring2 };
    }

    // ==========================
    // MUTATION OPERATORS
    // ==========================

    /**
     * Performs polynomial mutation on an individual.
     * 
     * Polynomial mutation is designed for real-valued variables
     * and produces offspring closer to parents with higher probability.
     * 
     * @param individual The individual to mutate
     * @param mutationProbability Probability of mutating each gene
     * @return The mutated individual
     * @throws IllegalArgumentException if individual is null or mutation probability is invalid
     */
    public static Individual polynomialMutation(Individual individual, double mutationProbability) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual cannot be null");
        }

        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("Mutation probability must be between 0 and 1");
        }

        double[] vars = individual.getDecisionVariables().clone();

        for (int i = 0; i < vars.length; i++) {
            if (threadLocalRandom.get().nextDouble() <= mutationProbability) {
                double y = vars[i];
                double yL = MIN_HEAD; // Lower bound
                double yU = MAX_HEAD; // Upper bound

                double rand = threadLocalRandom.get().nextDouble();
                double delta1 = (y - yL) / (yU - yL);
                double delta2 = (yU - y) / (yU - yL);

                double mutPow = 1.0 / (MUTATION_ETA + 1.0);
                double deltaq;

                if (rand <= 0.5) {
                    double xy = 1.0 - delta1;
                    double val = 2.0 * rand + (1.0 - 2.0 * rand) * Math.pow(xy, MUTATION_ETA + 1.0);
                    deltaq = Math.pow(val, mutPow) - 1.0;
                } else {
                    double xy = 1.0 - delta2;
                    double val = 2.0 * (1.0 - rand) + 2.0 * (rand - 0.5) * Math.pow(xy, MUTATION_ETA + 1.0);
                    deltaq = 1.0 - Math.pow(val, mutPow);
                }

                y = y + deltaq * (yU - yL);

                // Apply constraints
                vars[i] = Math.max(yL, Math.min(yU, y));
            }
        }

        // Create mutated individual
        Individual mutateIndividual = new Individual(vars.length / 2);
        System.arraycopy(vars, 0, mutateIndividual.getDecisionVariables(), 0, vars.length);

        return mutateIndividual;
    }

    /**
     * Performs Gaussian mutation on an individual.
     * 
     * Adds gaussian noise to each gene with specified probability.
     * Simpler than polynomial mutation, but effective for local search.
     * 
     * @param individual The individual to mutate
     * @param mutationProbability Probability of mutating each gene
     * @param mutationStrength Standard deviation of the Gaussian noise
     * @return The mutated individual
     * @throws IllegalArgumentException if individual is null or mutation probability is invalid
     */
    public static Individual gaussianMutation(Individual individual, double mutationProbability, double mutationStrength) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual cannot be null");
        }

        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("Mutation probability must be between 0 and 1");
        }

        if (mutationStrength <= 0.0) {
            throw new IllegalArgumentException("Mutation strength must be positive");
        }

        double[] vars = individual.getDecisionVariables().clone();

        for (int i = 0; i < vars.length; i++) {
            if (threadLocalRandom.get().nextDouble() <= mutationProbability) {
                double currentValue = vars[i];

                // If value is near boundaries, use strong mutation to escape
                double distanceFromBounds = Math.min(currentValue - MIN_HEAD, MAX_HEAD - currentValue);
                double adaptiveStrength = mutationStrength;

                // Increase mutation strength if too close to boundaries 
                double rangePadding = (MAX_HEAD - MIN_HEAD) * 0.2; 
                if (distanceFromBounds < rangePadding) {
                    adaptiveStrength *= 2.0; 
                }

                // Generate Gaussian noise
                double noise = threadLocalRandom.get().nextGaussian() * mutationStrength;
                double newValue = currentValue + noise;

                newValue = Math.max(MIN_HEAD, Math.min(MAX_HEAD, newValue)); // Clamp to bounds

                if ((newValue <= MIN_HEAD + 0.1 || newValue >= MAX_HEAD - 0.1) && threadLocalRandom.get().nextDouble() < 0.05) {
                    // Random reinitialisation near boundaries with 5% chance
                    vars[i] = MIN_HEAD + threadLocalRandom.get().nextDouble() * (MAX_HEAD - MIN_HEAD);
                } else {
                    vars[i] = newValue; // Apply new value if not reflecting
                }
            }
        }

        // Create mutated individual
        Individual mutatedIndividual = new Individual(vars.length / 2);
        System.arraycopy(vars, 0, mutatedIndividual.getDecisionVariables(), 0, vars.length);

        return mutatedIndividual;
    }

    /**
     * Performs operational constraint-aware mutation.
     * 
     * Ensures that Hs and He values maintain realistic operational relationships
     * while introducing variation. Uses two strategies:
     * - Strategy 1 (70% chance): Perturb both Hs and He together 
     * - Strategy 2 (30% chance): Adjust the difference between Hs and He
     * 
     * @param individual Individual to mutate
     * @param mutationProbability Probability of mutating each half-tide
     * @return Mutated individual
     * @throws IllegalArgumentException if individual is null or mutation probability is invalid
     */
    public static Individual operationalMutation(Individual individual, double mutationProbability) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual cannot be null");
        }

        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("Mutation probability must be between 0 and 1");
        }

        double[] vars = individual.getDecisionVariables().clone();
        int numHalfTides = vars.length / 2;

        for (int halfTide = 0; halfTide < numHalfTides; halfTide++) {
            if (threadLocalRandom.get().nextDouble() <= mutationProbability) {
                int hsIndex = halfTide * 2; // Hs index
                int heIndex = hsIndex + 1; // He index

                double currentHs = vars[hsIndex];
                double currentHe = vars[heIndex];

                // Strategy 1: Perturb Hs and He while maintaining relationship (70% chance)
                if (threadLocalRandom.get().nextDouble() < 0.7) {
                    double perturbation = (threadLocalRandom.get().nextDouble() - 0.5) * 0.5;
                    vars[hsIndex] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, currentHs + perturbation));
                    vars[heIndex] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, currentHe + perturbation));
                }
                // Strategy 2: Adjust the difference between Hs and He (30% chance)
                else {
                    double currentDifference = currentHe - currentHs;
                    double newDifference = currentDifference + (threadLocalRandom.get().nextGaussian() * 0.2);
                    double midPoint = (currentHs + currentHe) / 2.0;

                    vars[hsIndex] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, midPoint - newDifference / 2.0));
                    vars[heIndex] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, midPoint + newDifference / 2.0));
                }
            }
        }

        // Create mutated individual
        Individual mutatedIndividual = new Individual(vars.length / 2);
        System.arraycopy(vars, 0, mutatedIndividual.getDecisionVariables(), 0, vars.length);
        return mutatedIndividual;
    }

    // ==========================
    // OFFSPRING CREATION
    // ==========================

    /**
     * Creates offspring using genetic operators with specified probabilities.
     * 
     * @param parents List of parent individuals (must be even number for pairing)
     * @param crossoverProbability Probability of performing crossover
     * @param mutationProbability Probability of performing mutation
     * @param crossoverType Type of crossover ("SBX", "UNIFORM", "HALFTIDE")
     * @param mutationType Type of mutation ("POLYNOMIAL", "GAUSSIAN", "OPERATIONAL")
     * @return List of offspring individuals
     * @throws IllegalArgumentException if parents list is null, empty, or has odd number of parents
     */
    public static List<Individual> createOffspring(List<Individual> parents, double crossoverProbability,
            double mutationProbability, String crossoverType, String mutationType) {

        if (parents == null || parents.isEmpty()) {
            throw new IllegalArgumentException("Parents list cannot be null or empty");
        }

        if (parents.size() % 2 != 0) {
            throw new IllegalArgumentException("Number of parents must be even for pairing");
        }

        if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
            throw new IllegalArgumentException("Crossover probability must be between 0 and 1");
        }

        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("Mutation probability must be between 0 and 1");
        }

        List<Individual> offspring = new ArrayList<>();

        // Ensure even number of parents for pairing
        int numPairs = parents.size() / 2;

        for (int i = 0; i < numPairs; i++) {
            Individual parent1 = parents.get(i * 2);
            Individual parent2 = parents.get(i * 2 + 1);

            // Perform crossover
            Individual[] children;

            switch (crossoverType.toUpperCase()) {
                case "SBX":
                    children = simulatedBinaryCrossover(parent1, parent2, crossoverProbability);
                    break;
                case "UNIFORM":
                    children = uniformCrossover(parent1, parent2, crossoverProbability);
                    break;
                case "HALFTIDE":
                    children = halfTideCrossover(parent1, parent2, crossoverProbability);
                    break;
                default:
                    children = simulatedBinaryCrossover(parent1, parent2, crossoverProbability);
            }

            // Perform mutation
            for (Individual child : children) {
                Individual mutatedChild;

                switch (mutationType.toUpperCase()) {
                    case "POLYNOMIAL":
                        mutatedChild = polynomialMutation(child, mutationProbability);
                        break;
                    case "GAUSSIAN":
                        double enhancedMutationStrength = (MAX_HEAD - MIN_HEAD) * 0.75;
                        mutatedChild = gaussianMutation(child, mutationProbability, enhancedMutationStrength);
                        break;
                    case "OPERATIONAL":
                        mutatedChild = operationalMutation(child, mutationProbability);
                        break;
                    default:
                        mutatedChild = polynomialMutation(child, mutationProbability);
                }

                offspring.add(mutatedChild);
            }
        }

        return offspring;
    }

    /**
     * Creates offspring using default NSGA-II operators (SBX + Polynomial Mutation).
     * 
     * @param parents List of parent individuals
     * @param crossoverProbability Probability of crossover (typically 0.9)
     * @param mutationProbability  Probability of mutation per gene (typically 1/n where n is number of variables)
     * @return List of offspring individuals
     */
    public static List<Individual> createOffspringNSGAII(List<Individual> parents,
            double crossoverProbability, double mutationProbability) {
        return createOffspring(parents, crossoverProbability, mutationProbability, "SBX", "POLYNOMIAL");
    }

    // ==========================
    // CONSTRAINT VALIDATION
    // ==========================

    /**
     * Validates that an individual's decision variables are within constraints.
     * 
     * @param individual The individual to validate
     * @return true if all constraints are satisfied, false otherwise
     * @throws IllegalArgumentException if individual is null
     */
    public static boolean validateConstraints(Individual individual) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual cannot be null");
        }

        double[] vars = individual.getDecisionVariables();

        for (double var : vars) {
            if (var < MIN_HEAD || var > MAX_HEAD) {
                return false; // Constraint violated
            }
        }

        return true; // All constraints satisfied
    }

    /**
     * Repairs an individual by ensuring all variables are within constraints.
     * 
     * @param individual Individual to repair
     * @return Repaired individual with all variables within bounds
     * @throws IllegalArgumentException if individual is null
     */
    public static Individual repairConstraints(Individual individual) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual cannot be null");
        }

        double[] vars = individual.getDecisionVariables();
        boolean needsRepair = false;

        for (int i = 0; i < vars.length; i++) {
            if (vars[i] < MIN_HEAD || vars[i] > MAX_HEAD) {
                vars[i] = Math.max(MIN_HEAD, Math.min(MAX_HEAD, vars[i])); 
                needsRepair = true;
            }
        }

        if (needsRepair) {
            Individual repairedIndividual = new Individual(vars.length / 2);
            System.arraycopy(vars, 0, repairedIndividual.getDecisionVariables(), 0, vars.length);
            return repairedIndividual;
        }

        return individual; // No repair needed, return original
    }

    // ==========================
    // STATISTICS AND UTILITIES
    // ==========================

    /**
     * Gets genetic operator statistics for monitoring performance.
     * 
     * @param population Population to analyse
     * @return OperatorStats object containing key metrics
     * @throws IllegalArgumentException if population is null
     */
    public static OperatorStats getOperatorStatistics(List<Individual> population) {
        if (population == null) {
            throw new IllegalArgumentException("Population cannot be null");
        }
        if (population.isEmpty()) {
            return new OperatorStats(0, 0, 0, 0, 0);
        }

        double[] allValues = population.stream()
                .flatMapToDouble(ind -> Arrays.stream(ind.getDecisionVariables()))
                .toArray();

        double mean = Arrays.stream(allValues).average().orElse(0.0);
        double variance = Arrays.stream(allValues)
                .map(val -> Math.pow(val - mean, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        double min = Arrays.stream(allValues).min().orElse(0.0);
        double max = Arrays.stream(allValues).max().orElse(0.0);

        return new OperatorStats(population.size(), mean, stdDev, min, max);
    }

    /**
     * Calculates mutation probability per gene for NSGA-II.
     * Standard recommendation: 1/n where n is the number of decision variables.
     * 
     * @param numVariables Number of decision variables in the individual
     * @return Mutation probability per gene
     * @throws IllegalArgumentException if numVariables is less than 1
     */
    public static double calculateMutationProbability(int numVariables) {
        if (numVariables <= 0) {
            throw new IllegalArgumentException("Number of variables must be positive");
        }
        return 1.0 / numVariables;
    }

    /**
     * Calculates mutation probability per half-tide for tidal-specific operators.
     * 
     * @param numHalfTides Number of half-tides per individual
     * @return Recommended mutation probability per half-tide
     * @throws IllegalArgumentException if numHalfTides is less than 1
     */
    public static double calculateTidalMutationProbability(int numHalfTides) {
        if (numHalfTides <= 0) {
            throw new IllegalArgumentException("Number of half-tides must be positive");
        }
        return Math.min(0.2, 2.0 / numHalfTides);
    }

    // ==========================
    // INNER CLASSES
    // ==========================

    /**
     * Data class for genetic operator statistics.
     * Provides metrics for monitoring operator performance and population diversity.
     */
    public static class OperatorStats {
        /** Size of the population */
        public final int populationSize;

        /** Mean value of decision variables */
        public final double meanValue;

        /** Standard deviation of decision variables */
        public final double standardDeviation;

        /** Minimum value of decision variables */
        public final double minValue;

        /** Maximum value of decision variables */
        public final double maxValue;

        /**
         * Constructs operator statistics with key metrics.
         * 
         * @param populationSize Size of the population
         * @param meanValue Mean value of decision variables
         * @param standardDeviation Standard deviation of decision variables
         * @param minValue Minimum value of decision variables
         * @param maxValue Maximum value of decision variables
         */
        public OperatorStats(int populationSize, double meanValue, double standardDeviation,
                double minValue, double maxValue) {
            this.populationSize = populationSize;
            this.meanValue = meanValue;
            this.standardDeviation = standardDeviation;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        /**
         * Gets normalised diversity measure (0 = no diversity, 1 = maximum diversity).
         * 
         * @return Diversity metric (0.0 to 1.0)
         */
        public double getDiversity() {
            double range = maxValue - minValue;
            if (range == 0) {
                return 0.0; // Avoid division by zero
            }
            return (standardDeviation / range);
        }

        /**
         * Checks if population maintains good diversity.
         * 
         * @return true if diversity is above threshold (0.1), false otherwise
         */
        public boolean hasGoodDiversity() {
            return getDiversity() > 0.1;
        }

        @Override
            public String toString() {
                return String.format("OperatorStats[size=%d, mean=%.3f, stdDev=%.3f, range=[%.3f, %.3f]]",
                        populationSize, meanValue, standardDeviation, minValue, maxValue);
            }
    }
}

package src.optimisation;

import java.util.stream.Collectors;
import java.util.*;

/**
 * Main NSGA-II algorithm implementation for Tidal Lagoon optimisation.
 * 
 * Implement the complete NSGA-II procedure including:
 * - Population initialisation with random decision variables
 * - Fast non-dominated sorting with Pareto ranking
 * - Crowding distance calculation for diversity maintenance
 * - Environmental selection for next generation
 * - Genetic operators (crossover and mutation)
 * - Convergence tracking and termination criteria
 * 
 * The algorithm optimises two conflicting objectives:
 * - Maximise annual energy output (MWh/year)
 * - Minimise levelised cost of energy (£/MWh)
 * 
 * Based on Deb et al. (2002) "A Fast and Elitist multiobjective genetic algorithm: NSGA-II"
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 * */
public class NSGA2Algorithm {

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

    /** Tidal height data for simulation */
    private final List<Double> tideData;

    /** Number of half-tides per individual */
    private final int halfTides;

    // ========================
    // ALGORITHM STATE
    // ========================

    /** Current population of individuals */
    private Population currentPopulation;

    /** Current generation number */
    private int currentGeneration;

    /** Evolution history for analysis */
    private List<AlgorithmStats> evolutionHistory;

    // ========================
    // TERMINATION CRITERIA
    // ========================

    /** Flag indicating if convergence has been achieved */
    private boolean convergenceAchieved;

    // ========================
    // CONVERGENCE TRACKING
    // ========================

    /** Tracks convergence progress throughout evolution */
    private ConvergenceTracker convergenceTracker;

    // ========================
    // PERFORMANCE CONSTANTS
    // ========================

    /** Conversion factor from milliseconds to seconds */
    private static final double MILLIS_TO_SECONDS = 1000.0;

    /** Progress logging interval (every N generations) */
    private static final int PROGRESS_LOG_INTERVAL = 10;
    
    /**
     * Constructs NSGA-II algorithm with specified configuration and tidal data.
     * 
     * @param config Configuration parameters for the algorithm
     * @param tideData List of tidal height data for simulation
     * @throws IllegalArgumentException if config or tideData is null
     */
    public NSGA2Algorithm(NSGA2Config config, List<Double> tideData) {
        // Input validation
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (tideData == null || tideData.isEmpty()) {
            throw new IllegalArgumentException("Tide data cannot be null or empty");
        }

        // Algorithm parameters
        this.populationSize = config.getPopulationSize();
        this.maxGenerations = config.getMaxGenerations();
        this.crossoverProbability = config.getCrossoverProbability();
        this.mutationProbability = config.getMutationProbability();
        this.crossoverType = config.getCrossoverType();
        this.mutationType = config.getMutationType();

        // Initialise convergence tracker
        this.convergenceTracker = new ConvergenceTracker(
            config.getStagnationGenerations(),
            config.getConvergenceThreshold()
        );

        // Simulation parameters
        this.tideData = new ArrayList<>(tideData);
        this.halfTides = config.getHalfTides();

        // Algorithm state
        this.currentGeneration = 0;
        this.convergenceAchieved = false;
        this.evolutionHistory = new ArrayList<>();

        System.out.printf("NSGA-II initialised with:\n" + 
                          "Population Size: %d\n" +
                          "Max Generations: %d\n" + 
                          "Crossover Probability: %.2f\n" + 
                          "Mutation Probability: %.2f\n",
                populationSize, maxGenerations, crossoverProbability, mutationProbability);
    }

    // ========================
    // MAIN OPTIMISATION PROCESS
    // ========================

    /**
     * Executes the complete NSGA-II optimisation algorithm.
     * 
     * The algorithm follows these mains steps:
     * 1. Initialise random population
     * 2. Evaluate initial population fitness
     * 3. Main optimisation loop with selection, crossover, mutation
     * 4. Track convergence and log progress
     * 5. Return optimisation results
     * 
     * @return OptimisationResult containing final population, evolution history, and statistics
     */
    public OptimisationResult optimise() {
        printAlgorithmConfiguration();

        long startTime = System.currentTimeMillis();
        System.out.println("\nStarting NSGA-II optimisation...\n");

        // Step 1: Initialise population
        initialisePopulation();

        // Step 2: Evaluate initial population
        evaluatePopulation(currentPopulation);

        // Step 3: Record initial generation
        recordInitialGeneration();

        // Step 4: Main optimisation loop
        while (!isTerminationCriteriaMet()) {
            executeGeneration();

            // convergence step
            trackConvergence();

            // log progress every 10 generations
            if (currentGeneration % 10 == 0) {
                logProgress();
            }
        }

        // Step 5: Final evaluation and results
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / MILLIS_TO_SECONDS;

        OptimisationResult result = new OptimisationResult(
            currentPopulation,
            evolutionHistory,
            currentGeneration,
            executionTime,
            convergenceAchieved
        );

        printFinalConvergenceAnalysis(executionTime);

        System.out.printf("Optimisation completed in %.2f seconds after %d generations%n",
                executionTime, currentGeneration);
        return result;
    }

    // =======================
    // POPULATION OPERATIONS
    // =======================

    /**
     * Initialises the population with random decision variables.
     * 
     * Creates a population of specified size where each individual
     * has random head values within the allowed constraints.
     */
    private void initialisePopulation() {
        System.out.println("Initialising population...");
        currentPopulation = new Population(populationSize);
        currentPopulation.initialiseRandom(populationSize, halfTides);

        System.out.printf("Initialised population: %d individuals with %d half tides each%n",
                populationSize, halfTides);
    }

    /**
     * Evaluates all individuals in the population for their fitness.
     * 
     * Only evaluates individuals that haven't been evaluated yet
     * to avoid redundant calculations.
     */
    private void evaluatePopulation(Population population) {
        System.out.println("Evaluating population...");
        
        int evaluatedCount = 0;
        for (Individual individual : population.getIndividuals()) {
            // Only evaluate if not already evaluated
            if (individual.getEnergyOutput() == 0) {
                ObjectiveFunction.evaluate(tideData, individual);
                evaluatedCount++;
            }
        }

        if (evaluatedCount > 0) {
            System.out.printf("Evaluated %d individuals%n", evaluatedCount);
        } else {
            System.out.println("All individuals already evaluated, skipping evaluation.");
        }
    }

    // =======================
    // GENERATION OPERATIONS
    // =======================

    /**
     * Executes one complete generation of the NSGA-II algorithm.
     * 
     * This method performs the following steps:
     * 1. Parent selection using tournament selection
     * 2. Offspring generation using crossover and mutation
     * 3. Offspring evaluation
     * 4. Population combination (parents + offspring)
     * 5. Fast non-dominated sorting
     * 6. Crowding distance calculation
     * 7. Environmental selection for next generation
     */
    private void executeGeneration() {
        // Step 1: Parent selection and offspring generation
        List<Individual> parents = NextGenerationSelection.selectParents(currentPopulation, populationSize);
        List<Individual> offspring = GeneticOperators.createOffspring(parents, crossoverProbability, mutationProbability
            , crossoverType, mutationType);

        // Step 2: Create offspring population and evaluate
        Population offspringPopulation = new Population(offspring.size());
        for (Individual child : offspring) {
            offspringPopulation.addIndividual(child);
        }
        evaluatePopulation(offspringPopulation);

        // Step 3: Combine parent and offspring populations
        Population combinedPopulation = NextGenerationSelection.combinePopulations(
            currentPopulation, offspringPopulation);
        
        // Step 4: Perform fast non-dominated sorting
        ParetoDominance.fastNonDominatedSort(combinedPopulation);

        // Step 5: Calculate crowding distance for each front
        CrowdingDistance.calculateCrowdingDistance(combinedPopulation.getIndividuals());

        // Step 6: Environmental selection for next generation
        List<Individual> nextGeneration = NextGenerationSelection.selectNextGeneration(
            combinedPopulation, populationSize);

        // Step 7: Create new population for next generation
        Population newPopulation = new Population(populationSize);
        for (Individual individual : nextGeneration) {
            newPopulation.addIndividual(individual);
        }

        // Step 8: Update algorithm state
        currentPopulation = newPopulation;
        currentGeneration++;

        // Step 9: Log evolution history
        recordGenerationStats();
    }

    // =======================
    // CONVERGENCE TRACKING
    // =======================

    /**
     * Records the initial generation for convergence baseline.
     * 
     * Establishes the starting point for convergence tracking
     * and records initial Pareto front statistics.
     */
    private void recordInitialGeneration() {
        List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);
        double hypervolume = calculateHypervolume(paretoFront);

        printDetailedParetoFront(currentGeneration, paretoFront);
        
        // Record initial state (generation 0)
        convergenceTracker.recordGeneration(0, paretoFront, hypervolume);
        
        // Record generation stats for evolution history
        recordGenerationStats();
    }

    /**
     * Tracks convergence after each generation and updates convergence status.
     * 
     * Uses the convergence tracker to monitor algorithm progress
     * and detect when convergence criteria are met.
     */
    private void trackConvergence() {
        List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);
        double hypervolume = calculateHypervolume(paretoFront);
        
        // Check convergence using the tracker
        boolean hasConverged = convergenceTracker.recordGeneration(
            currentGeneration, 
            paretoFront, 
            hypervolume
        );
        
        // Update convergence status
        if (hasConverged && !convergenceAchieved) {
            convergenceAchieved = true;
            System.out.println("\n" + convergenceTracker.getConvergenceSummary());
        }
        
        // Record generation stats for evolution history
        recordGenerationStats();
    }

    /**
     * Records statistics for the current generation.
     * 
     * Collects comprehensive metrics including population statistics,
     * selection statistics, and Pareto front quality measures.
     */
    private void recordGenerationStats() {
        // Get Pareto front
        List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);

        // Calculate statistics
        Population.PopulationStats populationStats = currentPopulation.getStatistics();
        NextGenerationSelection.SelectionStats selectionStats = 
            NextGenerationSelection.getSelectionStats(currentPopulation);
        
        AlgorithmStats algorithmStats = new AlgorithmStats(
            currentGeneration,
            populationStats,
            selectionStats,
            paretoFront.size(),
            calculateHypervolume(paretoFront),
            calculateSpacing(paretoFront)
        );

        evolutionHistory.add(algorithmStats);
    }

    // =======================
    // TERMINATION CRITERIA
    // =======================

     /**
     * Enhanced termination criteria that includes convergence tracking.
     */
    private boolean isTerminationCriteriaMet() {
        // Check maximum generations
        if (currentGeneration >= maxGenerations) {
            System.out.printf("Maximum generations (%d) reached%n", maxGenerations);
            return true;
        }
        
        // Check convergence from tracker
        if (convergenceAchieved) {
            return true;
        }
        
        return false;
    }

    // =======================
    // QUALITY METRICS
    // =======================

    /**
     * Calculates hypervolume indicator for Pareto front quality.
     * 
     * Hypervolume measures the volume of objective space dominated
     * by the Pareto front relative to a reference point.
     * 
     * @param paretoFront List of non-dominated individuals
     * @return Hypervolume value (higher is better)
     */
    private double calculateHypervolume(List<Individual> paretoFront) {
        if (paretoFront.isEmpty()) {
            return 0.0;
        }

        // Reference point (worst possible values)
        double refEnergy = 0.0;
        double refCost = 100000.0;
        
        // Simple 2D hypervolume calculation
        List<Individual> sortedFront = new ArrayList<>(paretoFront);
        sortedFront.sort((a, b) -> Double.compare(a.getEnergyOutput(), b.getEnergyOutput()));

        double hypervolume = 0.0;
        double prevEnergy = refEnergy;

        for (Individual ind : sortedFront) {
            if (ind.getUnitCost() != Double.MAX_VALUE && ind.getUnitCost() < refCost) {
                double width = ind.getEnergyOutput() - prevEnergy;
                double height = refCost - ind.getUnitCost();
                hypervolume += width * height;
                prevEnergy = ind.getEnergyOutput();
            }
        }

        return hypervolume;
    }

    /**
     * Calculates spacing metric for diversity assessment.
     * 
     * Spacing measures the uniformity of distribution of solutions
     * along the Pareto front (lower values indicate better distribution).
     * 
     * @param paretoFront List of non-dominated individuals
     * @return Spacing value (lower is better)
     */
    private double calculateSpacing(List<Individual> paretoFront) {
        if (paretoFront.size() < 2) {
            return 0.0;
        }

        List<Double> distances = new ArrayList<>();

        for (int i = 1; i < paretoFront.size(); i++) {
            Individual current = paretoFront.get(i);
            double minDistance = Double.MAX_VALUE;

            for (int j = 0; j < paretoFront.size(); j++) {
                if (i != j) {
                    Individual other = paretoFront.get(j);
                    double distance = Math.sqrt(
                        Math.pow(current.getEnergyOutput() - other.getEnergyOutput(), 2) +
                        Math.pow(current.getUnitCost() - other.getUnitCost(), 2)
                    );
                    minDistance = Math.min(minDistance, distance);
                }
            }

            distances.add(minDistance);
        }

        double meanDistance = distances.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        double variance = distances.stream()
            .mapToDouble(d -> Math.pow(d - meanDistance, 2))
            .average().orElse(0.0);

        return Math.sqrt(variance);
    }

    // =======================
    // PROGRESS LOGGING
    // =======================

    /**
     * Prints algorithm configuration at startup.
     */
    private void printAlgorithmConfiguration() {
        System.out.println("=== ALGORITHM CONFIGURATION ===");
        System.out.printf("Population Size: %d\n", populationSize);
        System.out.printf("Mutation Rate: %.4f\n", mutationProbability);
        System.out.printf("Crossover Rate: %.4f\n", crossoverProbability);
        System.out.printf("Half Tides: %d\n", halfTides);
        System.out.println("================================");
    }

    /**
     * Logs optimisation progress with population diversity metrics.
     * 
     * Provides detailed information about population state,
     * Pareto front quality, and convergence progress.
     */
    private void logProgress() {
        if (evolutionHistory.isEmpty()) {
            System.out.println("No evolution history available yet.");
            return;
        }

        // Population diversity analysis
        System.out.println("=== Population Diversity Debug ===");

        List<Individual> individuals = currentPopulation.getIndividuals();
    
        double minEnergy = individuals.stream().mapToDouble(Individual::getEnergyOutput).min().orElse(0);
        double maxEnergy = individuals.stream().mapToDouble(Individual::getEnergyOutput).max().orElse(0);
        double minCost = individuals.stream().mapToDouble(Individual::getUnitCost).min().orElse(0);
        double maxCost = individuals.stream().mapToDouble(Individual::getUnitCost).max().orElse(0);
        
        System.out.printf("Population diversity: Energy [%.1f - %.1f], Cost [%.0f - %.0f]\n", 
                        minEnergy, maxEnergy, minCost, maxCost);
        
        // Count unique objective values
        Set<String> uniqueObjectives = individuals.stream()
            .map(i -> String.format("%.1f,%.0f", i.getEnergyOutput(), i.getUnitCost()))
            .collect(Collectors.toSet());
        
        System.out.printf("Unique objective combinations: %d / %d\n", 
                        uniqueObjectives.size(), individuals.size());
        
        // Rank distribution analysis
        Map<Integer, Long> rankCounts = individuals.stream()
            .collect(Collectors.groupingBy(Individual::getRank, Collectors.counting()));
        
        System.out.println("Rank distribution:");
        rankCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.printf("  Rank %d: %d individuals\n", 
                                            entry.getKey(), entry.getValue()));
        
        System.out.println("=====================================\n");

        // Generation statistics
        AlgorithmStats stats = evolutionHistory.get(evolutionHistory.size() - 1);
        Population.PopulationStats popStats = stats.populationStats;

        System.out.printf("Generation %d: Pareto Size = %d, " +
                          "MaxEnergy = %.1f, MinCost = %.0f, " +
                          "Hypervolume = %.2e\n",
                currentGeneration,
                stats.paretoFrontSize,
                popStats.maxEnergy,
                popStats.minCost,
                stats.hypervolume
        );

        // Detailed reporting every 10 generations
        if (currentGeneration % PROGRESS_LOG_INTERVAL == 0) {
            List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);
            printDetailedParetoFront(currentGeneration, paretoFront);
            printConvergenceStatus();
        }
    }

    /**
     * Prints detailed information about all Pareto front solutions.
     * 
     * @param generation Current generation number
     * @param paretoFront List of non-dominated individuals in the Pareto front
     */
    private void printDetailedParetoFront(int generation, List<Individual> paretoFront) {
        System.out.printf("\n=== DETAILED PARETO FRONT - Generation %d ===\n", generation);
        System.out.printf("Pareto Front Size: %d solutions\n", paretoFront.size());
        
        if (paretoFront.isEmpty()) {
            System.out.println("No Pareto front solutions found.");
            return;
        }
        
        // Header
        System.out.printf("%-4s %-12s %-12s %-10s %-25s\n", 
                        "Rank", "Energy(MWh)", "Impact", "HeadRange", "Sample Strategy");
        System.out.println("=".repeat(80));
        
        // Sort by energy for better readability
        List<Individual> sorted = new ArrayList<>(paretoFront);
        sorted.sort((a, b) -> Double.compare(b.getEnergyOutput(), a.getEnergyOutput())); // Descending energy
        
        for (int i = 0; i < sorted.size(); i++) {
            Individual ind = sorted.get(i);
            
            // Calculate head range for this individual
            double minHead = Double.MAX_VALUE, maxHead = Double.MIN_VALUE;
            int halfTides = ind.getDecisionVariables().length / 2;
            
            for (int j = 0; j < halfTides; j++) {
                double hs = ind.getStartHead(j);
                double he = ind.getEndHead(j);
                minHead = Math.min(minHead, Math.min(hs, he));
                maxHead = Math.max(maxHead, Math.max(hs, he));
            }
            
            // Sample strategy (first 3 half-tides)
            StringBuilder strategy = new StringBuilder();
            for (int j = 0; j < Math.min(3, halfTides); j++) {
                strategy.append(String.format("(%.2f,%.2f)", 
                            ind.getStartHead(j), ind.getEndHead(j)));
                if (j < Math.min(2, halfTides - 1)) strategy.append(" ");
            }
            if (halfTides > 3) strategy.append("...");
            
            System.out.printf("%-4d %-12.1f %-12.6f %-10s %-25s\n",
                            i + 1,
                            ind.getEnergyOutput(),
                            ind.getUnitCost(),
                            String.format("%.2f-%.2f", minHead, maxHead),
                            strategy.toString());
        }
        
        // Summary statistics
        double minEnergy = sorted.stream().mapToDouble(Individual::getEnergyOutput).min().orElse(0);
        double maxEnergy = sorted.stream().mapToDouble(Individual::getEnergyOutput).max().orElse(0);
        double minImpact = sorted.stream().mapToDouble(Individual::getUnitCost).min().orElse(0);
        double maxImpact = sorted.stream().mapToDouble(Individual::getUnitCost).max().orElse(0);
        
        System.out.println("=".repeat(80));
        System.out.printf("Energy range: %.1f - %.1f MWh (Δ=%.1f)\n", 
                        minEnergy, maxEnergy, maxEnergy - minEnergy);
        System.out.printf("Impact range: %.6f - %.6f (Δ=%.6f)\n", 
                        minImpact, maxImpact, maxImpact - minImpact);
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Prints current convergence status.
     */
    private void printConvergenceStatus() {
        ConvergenceTracker.ConvergenceSummary summary = convergenceTracker.getConvergenceSummary();
        
        if (summary.converged) {
            System.out.println("STATUS: Converged - " + summary.reason);
        } else {
            // Show progress indicators
            if (summary.firstGeneration != null && summary.lastGeneration != null) {
                double energyImprovement = summary.lastGeneration.maxEnergy - summary.firstGeneration.maxEnergy;
                double costImprovement = summary.firstGeneration.minCost - summary.lastGeneration.minCost;
                
                System.out.printf("STATUS: Optimizing - Energy +%.1f MWh, Cost -£%.0f/MWh since start%n",
                                energyImprovement, costImprovement);
            }
        }
    }

     /**
     * Prints comprehensive final convergence analysis.
     * 
     * @param executionTime Total execution time in seconds
     */
    private void printFinalConvergenceAnalysis(double executionTime) {
        System.out.printf("Optimisation completed in %.2f seconds after %d generations%n",
                executionTime, currentGeneration);

        List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);
        printDetailedParetoFront(currentGeneration, paretoFront);
        
        System.out.println("\n=== DETAILED CONVERGENCE ANALYSIS ===");
        ConvergenceTracker.ConvergenceSummary summary = convergenceTracker.getConvergenceSummary();
        System.out.println(summary);
        
        if (summary.converged) {
            double generationsToConvergence = summary.convergenceGeneration;
            double convergenceEfficiency = (generationsToConvergence / currentGeneration) * 100;
            
            System.out.printf("Convergence efficiency: %.1f%% (converged at generation %d of %d)%n",
                            convergenceEfficiency, summary.convergenceGeneration, currentGeneration);
        } else {
            System.out.println("Algorithm terminated before convergence - consider increasing max generations");
        }
        
        // Print optimization trajectory
        if (summary.firstGeneration != null && summary.lastGeneration != null) {
            System.out.println("\n=== OPTIMIZATION TRAJECTORY ===");
            System.out.printf("Initial: Energy %.1f GWh, Cost £%.0f/MWh, PF %d solutions%n",
                            summary.firstGeneration.maxEnergy / 1000.0,
                            summary.firstGeneration.minCost,
                            summary.firstGeneration.paretoSize);
            System.out.printf("Final:   Energy %.1f GWh, Cost £%.0f/MWh, PF %d solutions%n",
                            summary.lastGeneration.maxEnergy / 1000.0,
                            summary.lastGeneration.minCost,
                            summary.lastGeneration.paretoSize);
        }
    }

    // =======================
    // STATE ACCESS
    // =======================

    /**
     * Gets the current state of the optimisation algorithm.
     * 
     * @return OptimisationState containing current algorithm state
     */
    public OptimisationState getCurrentState() {
        return new OptimisationState(
            currentGeneration,
            maxGenerations,
            currentPopulation.copy(),
            convergenceAchieved,
            new ArrayList<>(evolutionHistory)
        );
    }

    // =======================
    // INNER CLASSES
    // =======================

    /**
     * Data class for algorithm statistics per generation.
     * 
     * Contains comprehensive metrics for monitoring NSGA-II performance
     * including population statistics, selection metrics, and quality measures.
     */
    public static class AlgorithmStats {
        /** Generation number */
        public final int generation;

        /** Population-level statistics */
        public final Population.PopulationStats populationStats;

        /** Selection-level statistics */
        public final NextGenerationSelection.SelectionStats selectionStats;

        /** Size of the Pareto front */
        public final int paretoFrontSize;

        /** Hypervolume quality indicator */
        public final double hypervolume;

        /** Spacing diversity indicator */
        public final double spacing;

        /**
         * Constructs algorithm statistics for a generation.
         * 
         * @param generation Generation number
         * @param populationStats Population statistics
         * @param selectionStats Selection statistics
         * @param paretoFrontSize Size of the Pareto front
         * @param hypervolume Hypervolume indicator
         * @param spacing Spacing indicator
         */
        public AlgorithmStats(int generation, Population.PopulationStats populationStats,
                              NextGenerationSelection.SelectionStats selectionStats,
                              int paretoFrontSize, double hypervolume, double spacing) {
            this.generation = generation;
            this.populationStats = populationStats;
            this.selectionStats = selectionStats;
            this.paretoFrontSize = paretoFrontSize;
            this.hypervolume = hypervolume;
            this.spacing = spacing;
        }

        @Override
        public String toString() {
            return String.format("generation %d[PF=%d, HV=%.2e, spacing=%.3f]",
                    generation, paretoFrontSize, hypervolume, spacing);
        }
    }

    /**
     * Data class for optimisation result.
     * 
     * Contains the final outcome of the NSGA-II optimisation process
     * including the final population, evolution history, and performance metrics.
     */
    public static class OptimisationResult {
        /** Final population after optimisation */
        public final Population finalPopulation;

        /** Complete evolution history */
        public final List<AlgorithmStats> evolutionHistory;

        /** Total generations executed */
        public final int generationsRun;

        /** Total execution time in seconds */
        public final double executionTimeSeconds;

        /** Whether convergence was achieved */
        public final boolean convergenceAchieved;

        /**
         * Constructs optimisation result.
         * 
         * @param finalPopulation Final population
         * @param evolutionHistory Evolution history
         * @param generationsRun Number of generations executed
         * @param executionTime Execution time in seconds
         * @param convergenceAchieved Whether convergence was achieved
         */
        public OptimisationResult(Population finalPopulation, List<AlgorithmStats> evolutionHistory,
                                  int generationsRun, double executionTime, boolean convergenceAchieved) {
            this.finalPopulation = finalPopulation;
            this.evolutionHistory = evolutionHistory;
            this.generationsRun = generationsRun;
            this.executionTimeSeconds = executionTime;
            this.convergenceAchieved = convergenceAchieved;
        }

        /**
         * Gets the Pareto front from the optimisation.
         * 
         * @return List of non-dominated individuals
         */
        public List<Individual> getParetoFront() {
            return ParetoDominance.getParetoFront(finalPopulation);
        }

        @Override
        public String toString() {
            return String.format("OptimisationResult[Generations=%d, Runtime=%.2fs, Converged=%s, ParetoSize=%d]",
                    generationsRun, executionTimeSeconds, convergenceAchieved, getParetoFront().size());
        }
    }

    /**
     * Data class for current optimisation state.
     * 
     * Provides a snapshot of the algorithm's current state for
     * monitoring and intermediate analysis during the optimisation process.
     */
    public static class OptimisationState {
        /** Current generation number */
        public final int currentGeneration;

        /** Maximum generations allowed */
        public final int maxGenerations;

        /** Current population */
        public final Population currentPopulation;

        /** Whether convergence has been achieved */
        public final boolean convergenceAchieved;

        /** Evolution history up to current generation */
        public final List<AlgorithmStats> evolutionHistory;

        /**
         * Constructs the optimisation state.
         * 
         * @param currentGeneration Current generation
         * @param maxGenerations Maximum generations
         * @param currentPopulation Current population
         * @param convergenceAchieved Convergence status
         * @param evolutionHistory Evolution history
         */
        public OptimisationState(int currentGeneration, int maxGenerations, Population currentPopulation,
                                 boolean convergenceAchieved, List<AlgorithmStats> evolutionHistory) {
            this.currentGeneration = currentGeneration;
            this.maxGenerations = maxGenerations;
            this.currentPopulation = currentPopulation;
            this.convergenceAchieved = convergenceAchieved;
            this.evolutionHistory = evolutionHistory;
        }

        /**
         * Gets the progress as a percentage (0.0 to 1.0).
         * 
         * @return Progress ratio
         */
        public double getProgress() {
            return (double) currentGeneration / maxGenerations;
        }
    }
}

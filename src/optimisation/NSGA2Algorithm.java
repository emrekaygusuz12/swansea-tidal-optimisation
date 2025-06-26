package src.optimisation;

import src.optimisation.NextGenerationSelection;
import src.model.SimulationConfig;


import java.util.*;

/**
 * Main NSGA-II algorithm implementation for Tidal Lagoon optimisation.
 * 
 * Implement the complete NSGA-II procedure including:
 * - Population initialisation
 * - Fast non-dominated sorting
 * - Crowding distance calculation
 * - Environmental selection
 * - Genetic operators (crossover and mutation)
 * - Termination criteria
 * 
 * Based on Deb et al. (2002) "A Fast and Elitist multiobjective genetic algorithm: NSGA-II"
 * @author Emre Kaygusuz
 * @version 1.0
 * */
public class NSGA2Algorithm {

    // Algorithm parameters
    private final int populationSize;
    private final int maxGenerations;
    private final double crossoverProbability;
    private final double mutationProbability;
    private final String crossoverType;
    private final String mutationType;

    // Simulation parameters
    private final List<Double> tideData;
    private final int halfTides;

    // Algorithm state
    private Population currentPopulation;
    private int currentGeneration;
    private List<AlgorithmStats> evolutionHistory;

    // Termination criteria
    private boolean convergenceAchieved;
    private final double convergenceThreshold;
    private final int stagnationGenerations;

    // Convergence tracker
    private ConvergenceTracker convergenceTracker;
    

    /**
     * Constructs NSGA-II algorithm with specified parameters.
     */
    public NSGA2Algorithm(NSGA2Config config, List<Double> tideData) {
        this.populationSize = config.getPopulationSize();
        this.maxGenerations = config.getMaxGenerations();
        this.crossoverProbability = config.getCrossoverProbability();
        this.mutationProbability = config.getMutationProbability();
        this.crossoverType = config.getCrossoverType();
        this.mutationType = config.getMutationType();
        this.convergenceThreshold = config.getConvergenceThreshold();
        this.stagnationGenerations = config.getStagnationGenerations();

        this.convergenceTracker = new ConvergenceTracker(
            config.getStagnationGenerations(),
            config.getConvergenceThreshold()
        );

        this.tideData = new ArrayList<>(tideData);
        this.halfTides = config.getHalfTides();

        this.currentGeneration = 0;
        this.convergenceAchieved = false;
        this.evolutionHistory = new ArrayList<>();

        System.out.printf("NSGA-II initialised with\n \nPopulation Size: %d" + 
                             "\nMax generations: %d \nCrossover Probability: %.2f \nMutation Probability: %.2f%n",
                populationSize, maxGenerations, crossoverProbability, mutationProbability);
    }


    /**
     * Executes the complete NSGA-II optimisation algorithm
     * 
     * @return OptimisationResult containing final population, evolution history, and statistics
     */
    public OptimisationResult optimise() {
        // Add debug output at the very beginning
        System.out.println("=== ALGORITHM CONFIGURATION ===");
        System.out.printf("Population Size: %d%n", populationSize);
        System.out.printf("Mutation Rate: %.4f%n", mutationProbability);
        System.out.printf("Crossover Rate: %.4f%n", crossoverProbability);
        System.out.printf("Half Tides: %d%n", halfTides);
        System.out.println("================================");

        long startTime = System.currentTimeMillis();

        System.out.println("\nStarting NSGA-II optimisation...\n");

        // Step 1: Initialise population
        initialisePopulation();

        // for (int i = 0; i < populationSize; i++) {
        //     double[] decisionVariables = currentPopulation.getIndividual(i).getDecisionVariables();
        //     System.out.printf("Individual %d: %s%n", i + 1, Arrays.toString(decisionVariables));
        // }

        // Step 2: Evaluate initial population
        evaluatePopulation(currentPopulation);

        // convergence step
        recordInitialGeneration();

        // Step 3: Main optimisation loop
        while (!isTerminationCriteriaMet()) {
            executeGeneration();

            // convergence step
            trackConvergence();

            // log progress every 10 generations
            if (currentGeneration % 10 == 0) {
                logProgress();
            }
        }

        // Step 4: Final evaluation and results
        long endTime = System.currentTimeMillis();
        double inSeconds = 1000.0; // convert milliseconds to seconds
        double executionTime = (endTime - startTime) / inSeconds; 

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

    /**
     * Records the initial generation for convergence baseline.
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
        
        // Add any other termination criteria you have
        return false;
    }

    private void initialisePopulation() {
        System.out.println("Initialising population...");
        currentPopulation = new Population(populationSize);
        currentPopulation.initialiseRandom(populationSize, halfTides);

        System.out.printf("Initalised population: %d individuals with %d half tides each%n",
                populationSize, halfTides);
    }

    /**
     * Prints detailed information about all Pareto front solutions.
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
        System.out.println("─".repeat(80));
        
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
        
        System.out.println("─".repeat(80));
        System.out.printf("Energy range: %.1f - %.1f MWh (Δ=%.1f)\n", 
                        minEnergy, maxEnergy, maxEnergy - minEnergy);
        System.out.printf("Impact range: %.6f - %.6f (Δ=%.6f)\n", 
                        minImpact, maxImpact, maxImpact - minImpact);
        System.out.println("-".repeat(80) + "\n");
    }

    /*
     * Evaluates all individuals in the population.
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


    /*
     * Executes one generation of the NSGA-II algorithm.
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

        // Step 10: Check convergence
        //checkConvergence();
    }


    /*
     * Records statistics for the current generation.
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


    // /*
    //  * Checks if termination criteria are met.
    //  */
    // private void checkConvergence() {
    //     if (evolutionHistory.size() < stagnationGenerations) {
    //         return; // Not enough data to determine convergence
    //     }

    //     // Check if hypervolume has improved in last N generations
    //     double currentHypervolume = evolutionHistory.get(evolutionHistory.size() - 1).hypervolume;
    //     double previousHypervolume = evolutionHistory.get(evolutionHistory.size() - stagnationGenerations).hypervolume;

    //     double improvement = (currentHypervolume - previousHypervolume) / previousHypervolume;

    //     if (improvement < convergenceThreshold) {
    //         convergenceAchieved = true;
    //         System.out.printf("Convergence achieved: %.6f improvement in last %d generations%n",
    //                 improvement, stagnationGenerations);
    //     }
    // }


    /*
     * Calculates hypervolume indicator for Pareto front quality.
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


    /*
     * Calculates spacing for diversity assessment.
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


    // /**
    //  * Checks if termination criteria are met.
    //  * 
    //  * Criteria:
    //  * - Maximum generations reached
    //  * - Convergence achieved based on hypervolume improvement
    //  * 
    //  * @return true if termination criteria are met, false otherwise
    //  */
    // private boolean isTerminationCriteriaMet() {
    //     return currentGeneration >= maxGenerations || convergenceAchieved;
    // }


    /*
     * Logs optimisation progress.
     */
    private void logProgress() {
        if (evolutionHistory.isEmpty()) {
            System.out.println("No evolution history available yet.");
            return;
        }

        AlgorithmStats stats = evolutionHistory.get(evolutionHistory.size() - 1);
        Population.PopulationStats popStats = stats.populationStats;

        System.out.printf("generation %d: Pareto Size = %d, " +
                          "MaxEnergy = %.1f, MinCost = %.0f, " +
                          "hypervolume = %.2e%n",
                currentGeneration,
                stats.paretoFrontSize,
                popStats.maxEnergy,
                popStats.minCost,
                stats.hypervolume
        );

        if (currentGeneration % 10 == 0) {
            List<Individual> paretoFront = ParetoDominance.getParetoFront(currentPopulation);
            printDetailedParetoFront(currentGeneration, paretoFront);
            printConvergenceStatus();
            System.exit(0); // Exit after printing detailed status
        }
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


    /*
     * Gets the current state of the optimisation algorithm.
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


    /**
     * Data class for algorithm statistics per generation.
     */
    public static class AlgorithmStats {
        public final int generation;
        public final Population.PopulationStats populationStats;
        public final NextGenerationSelection.SelectionStats selectionStats;
        public final int paretoFrontSize;
        public final double hypervolume;
        public final double spacing;

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


    /*
     * Data class for optimisation result.
     */
    public static class OptimisationResult {
        public final Population finalPopulation;
        public final List<AlgorithmStats> evolutionHistory;
        public final int generationsRun;
        public final double executionTimeSeconds; // in seconds
        public final boolean convergenceAchieved;

        public OptimisationResult(Population finalPopulation, List<AlgorithmStats> evolutionHistory,
                                  int generationsRun, double executionTime, boolean convergenceAchieved) {
            this.finalPopulation = finalPopulation;
            this.evolutionHistory = evolutionHistory;
            this.generationsRun = generationsRun;
            this.executionTimeSeconds = executionTime;
            this.convergenceAchieved = convergenceAchieved;
        }

        public List<Individual> getParetoFront() {
            return ParetoDominance.getParetoFront(finalPopulation);
        }

        @Override
        public String toString() {
            return String.format("OptimisationResult[Generations=%d, Runtime=%.2fs, Converged=%s, ParetoSize=%d]",
                    generationsRun, executionTimeSeconds, convergenceAchieved, getParetoFront().size());
        }
    }

    /*
     * Data class for current optimisation state.
     */
    public static class OptimisationState {
        public final int currentGeneration;
        public final int maxGenerations;
        public final Population currentPopulation;
        public final boolean convergenceAchieved;
        public final List<AlgorithmStats> evolutionHistory;

        public OptimisationState(int currentGeneration, int maxGenerations, Population currentPopulation,
                                 boolean convergenceAchieved, List<AlgorithmStats> evolutionHistory) {
            this.currentGeneration = currentGeneration;
            this.maxGenerations = maxGenerations;
            this.currentPopulation = currentPopulation;
            this.convergenceAchieved = convergenceAchieved;
            this.evolutionHistory = evolutionHistory;
        }

        public double getProgress() {
            return (double) currentGeneration / maxGenerations;
        }
    }
}

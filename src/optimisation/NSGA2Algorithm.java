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

        this.tideData = new ArrayList<>(tideData);
        this.halfTides = config.getHalfTides();

        this.currentGeneration = 0;
        this.convergenceAchieved = false;
        this.evolutionHistory = new ArrayList<>();

        System.out.printf("NSGA-II initialised: Population Size: %d," + 
                             "Max generations: %d, Crossover Probability: %.2f, Mutation Probability: %.2f%n",
                populationSize, maxGenerations, crossoverProbability, mutationProbability);
    }


    /**
     * Executes the complete NSGA-II optimisation algorithm
     * 
     * @return OptimisationResult containing final population, evolution history, and statistics
     */
    public OptimisationResult optimise() {
        long startTime = System.currentTimeMillis();

        System.out.println("\n Starting NSGA-II optimisation...");

        // Step 1: Initialise population
        initialisePopulation();

        // Step 2: Evaluate initial population
        evaluatePopulation(currentPopulation);

        // Step 3: Main optimisation loop
        while (!isTerminationCriteriaMet()) {
            executeGeneration();

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

        System.out.printf("Optimisation completed in %.2f seconds after %d generations%n",
                executionTime, currentGeneration);
        return result;
    }

    private void initialisePopulation() {
        System.out.println("Initialising population...");
        currentPopulation = new Population(populationSize);
        currentPopulation.initialiseRandom(populationSize, halfTides);
        
        System.out.printf("Initalised population: %d individuals with %d half tides each%n",
                populationSize, halfTides);
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
        checkConvergence();
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


    /*
     * Checks if termination criteria are met.
     */
    private void checkConvergence() {
        if (evolutionHistory.size() < stagnationGenerations) {
            return; // Not enough data to determine convergence
        }

        // Check if hypervolume has improved in last N generations
        double currentHypervolume = evolutionHistory.get(evolutionHistory.size() - 1).hypervolume;
        double previousHypervolume = evolutionHistory.get(evolutionHistory.size() - stagnationGenerations).hypervolume;

        double improvement = (currentHypervolume - previousHypervolume) / previousHypervolume;

        if (improvement < convergenceThreshold) {
            convergenceAchieved = true;
            System.out.printf("Convergence achieved: %.6f improvement in last %d generations%n",
                    improvement, stagnationGenerations);
        }
    }


    /*
     * Calculates hypervolume indicator for Pareto front quality.
     */
    private double calculateHypervolume(List<Individual> paretoFront) {
        if (paretoFront.isEmpty()) {
            return 0.0;
        }

        // Reference point (worst possible values)
        double refEnergy = 0.0;
        double refCost = paretoFront.stream()
            .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
            .mapToDouble(Individual::getUnitCost)
            .max().orElse(1000000.0) * 1.1; // 10% above max cost
        
        // Simple 2D hypervolume calculation
        List<Individual> sortedFront = new ArrayList<>(paretoFront);
        sortedFront.sort((a, b) -> Double.compare(a.getEnergyOutput(), b.getEnergyOutput()));

        double hypervolume = 0.0;
        double prevEnergy = refEnergy;

        for (Individual ind : sortedFront) {
            if (ind.getUnitCost() != Double.MAX_VALUE) {
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


    /**
     * Checks if termination criteria are met.
     * 
     * Criteria:
     * - Maximum generations reached
     * - Convergence achieved based on hypervolume improvement
     * 
     * @return true if termination criteria are met, false otherwise
     */
    private boolean isTerminationCriteriaMet() {
        return currentGeneration >= maxGenerations || convergenceAchieved;
    }


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

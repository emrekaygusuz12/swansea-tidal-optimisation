package src.optimisation;

import java.util.*;

/**
 * Implements environmental selection for NSGA-II algorithm.
 * 
 * Combines fast non-dominated sorting with crowding distance
 * to select the best individuals for the next generation,
 * maintaining both convergence and diversity in the population.
 * 
 * For tidal lagoon optimisation with objectives:
 * - Energy output (maximised) - higher values preferred
 * - Unit cost (minimised) - lower values preferred
 * 
 * Based on the NSGA-II selection mechanism from Deb et al. (2002).
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class NextGenerationSelection {

    // ==========================
    // SELECTION PARAMETERS
    // ==========================

    /** Default size of tournament for selection */
    private static final int DEFAULT_TOURNAMENT_SIZE = 2;

    /** Numerical tolerance for individual comparison */
    private static final double COMPARISON_TOLERANCE = 1e-9;

    // ==========================
    // THREAD-LOCAL RANDOM
    // ==========================

    /**
     * Thread-local random generator for better performance in multi-threaded environments.
     * 
     */
    private static final ThreadLocal<Random> threadLocalRandom = ThreadLocal.withInitial(Random::new);

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods are static and should be accessed directly via the class name.
     * 
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
     */
    private NextGenerationSelection() {
        throw new UnsupportedOperationException("NextGenerationSelection is a utility class and cannot be instantiated.");
    }
    
    // ==========================
    // NEXT GENERATION SELECTION
    // ==========================

    /**
     * Selects the next generation using NSGA-II environmental selection.
     * 
     * Combines fast non-dominated sorting with crowding distance calculation
     * to maintain both convergence and diversity in the selected population.
     * 
     * @param combinedPopulation The combined population of individuals
     * @param targetSize The desired size of the next generation
     * @return A list of individuals selected for the next generation
     * @throws IllegalArgumentException if targetSize is less than or equal to zero
     */
    public static List<Individual> selectNextGeneration(Population combinedPopulation, int targetSize) {
        // Input validation
        if (combinedPopulation == null) {
            throw new IllegalArgumentException("Combined population cannot be null.");
        }

        if (targetSize < 0) {
            throw new IllegalArgumentException("Target size must be non-negative.");
        }

        if (targetSize <= 0) {
            return new ArrayList<>();
        }

        if (combinedPopulation.size() <= targetSize) {
            return new ArrayList<>(combinedPopulation.getIndividuals());
        }

        // Step 1: Perform fast non-dominated sorting
        List<List<Individual>> paretoFronts = ParetoDominance.fastNonDominatedSort(combinedPopulation);

        // Step 2: Select individuals front by front
        List<Individual> selectedIndividuals = new ArrayList<>();
        int frontIndex = 0;

        // Add complete fronts until we reach the target size
        while (frontIndex < paretoFronts.size()){
            List<Individual> currentFront = paretoFronts.get(frontIndex);

            if (selectedIndividuals.size() + currentFront.size() <= targetSize) {
                // Add the entire front
                selectedIndividuals.addAll(currentFront);
                frontIndex++;
            } else {
                // Current front would exceed target size, apply crowding distance
                int remainingSlots = targetSize - selectedIndividuals.size();
                List<Individual> partialFront = selectByCrowdingDistance(currentFront, remainingSlots);
                selectedIndividuals.addAll(partialFront);
                break; // the target size is now met
            }
        }
        return selectedIndividuals;
    }

    /**
     * Selects individuals from a front using crowding distance.
     * 
     * Higher crowding distance (more isolated) individuals are preferred to
     * maintain population diversity within the same Pareto front.
     * 
     * @param front The list of individuals in the same Pareto front.
     * @param count The number of individuals to select.
     * @return Selected individuals sorted by crowding distance (descending)
     * @throws IllegalArgumentException if front is null or count is negative
     */
    public static List<Individual> selectByCrowdingDistance(List<Individual> front, int count) {
        // Input validation
        if (front == null) {
            throw new IllegalArgumentException("Front cannot be null.");
        }

        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative.");
        }
        if (count >= front.size()) {
            return new ArrayList<>(front);
        }

        if (count <= 0) {
            return new ArrayList<>();
        }

        // Calculate crowding distances for all individuals in the front
        CrowdingDistance.calculateCrowdingDistance(front);

        // Sort by crowding distance (descending) and select top count
        List<Individual> sortedFront = new ArrayList<>(front);
        CrowdingDistance.sortByCrowdingDistance(sortedFront);

        return sortedFront.subList(0, count);
    }

    // ==========================
    // TOURNAMENT SELECTION
    // ==========================

    /**
     * Performs tournament selection based on NSGA-II crowded comparison.
     * 
     * Comparison order:
     * 1. Lower rank (better Pareto front) wins
     * 2. If ranks are equal, higher crowding distance wins
     * 
     * @param population Population to select from.
     * @param tournamentSize Number of individuals in each tournament.
     * @param numOfSelections Number of tournaments to run.
     * @return List of tournament winners.
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static List<Individual> tournamentSelection(Population population, int tournamentSize, int numOfSelections) {
        // Input validation
        if (population == null) {
            throw new IllegalArgumentException("Population cannot be null.");
        }

        if (tournamentSize <= 0) {
            throw new IllegalArgumentException("Tournament size must be positive.");
        }

        if (numOfSelections <= 0) {
            throw new IllegalArgumentException("Number of selections must be positive.");
        }

        if (population.size() == 0) {
            return new ArrayList<>(); 
        }
        
        List<Individual> selected = new ArrayList<>();
        List<Individual> individuals = population.getIndividuals();

        for (int i = 0; i < numOfSelections; i++) {
            // Create tournament group
            List<Individual> tournament = new ArrayList<>();
            for (int j = 0; j < tournamentSize; j++) {
                int index = threadLocalRandom.get().nextInt(individuals.size());
                tournament.add(individuals.get(index));
            }

            // Determine winner using crowded comparison
            Individual winner = tournament.get(0);
            for (int j = 1; j < tournament.size(); j++) {
                Individual competitor = tournament.get(j);
                if (CrowdingDistance.crowdedCompare(competitor, winner) < 0) {
                    winner = competitor;
                }
            }

            selected.add(winner.clone()); // Clone to avoid reference issues
        }

        return selected;
    }


    /**
     * Selects parents for the next generation using binary tournament selection.
     * 
     * Uses the standard NSGA-II tournament size of 2 for parent selection,
     * providing good selection pressure while maintaining diversity.
     * 
     * @param population The population to select from.
     * @param numParents Number of parents to select.
     * @return List of selected parent individuals for crossover and mutation.
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static List<Individual> selectParents (Population population, int numParents) {
        return tournamentSelection(population, DEFAULT_TOURNAMENT_SIZE, numParents);
    }

    // ==========================
    // POPULATION OPERATIONS
    // ==========================

    /**
     * Combines parent and offspring populations for environmental selection.
     * 
     * Creates a unified population containing all individuals from both 
     * parent and offspring populations, preparing for NSGA-II selection.
     * 
     * @param parents The parent population.
     * @param offspring The offspring population.
     * @return Combined population containing all individuals from both parents and offspring.
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static Population combinePopulations(Population parents, Population offspring) {
        // Input validation

        if (parents == null) {
            throw new IllegalArgumentException("Parents population cannot be null.");
        }

        if (offspring == null) {
            throw new IllegalArgumentException("Offspring population cannot be null.");
        }

        Population combined = new Population(parents.size() + offspring.size());

        // Add all parents
        for (Individual parent : parents.getIndividuals()) {
            combined.addIndividual(parent.clone());
        }

        // Add all offspring
        for (Individual child : offspring.getIndividuals()) {
            combined.addIndividual(child.clone());
        }

        return combined;
    }

    // ==========================
    // STATISTICS AND VALIDATION
    // ==========================

    /**
     * Gets selection statistics for monitoring NSGA-II performance.
     * 
     * Calculates key metrics including front distribution, crowding distance,
     * and convergence indicators for algorithm monitoring and tuning.
     * 
     * @param population Population to analyse
     * @return SelectionStats object containing key metrics
     * @throws IllegalArgumentException if population is null
     */
    public static SelectionStats getSelectionStats(Population population) {
        if (population == null) {
            throw new IllegalArgumentException("Population cannot be null.");
        }

        // Perform non-dominated sorting
        List<List<Individual>> fronts = ParetoDominance.fastNonDominatedSort(population);

        // Calculate crowding distances for all individuals
        CrowdingDistance.calculateCrowdingDistance(population.getIndividuals());

        // Collect statistics
        int totalIndividuals = population.size();
        int frontCount = fronts.size();
        int paretoFrontSize = fronts.isEmpty() ? 0 : fronts.get(0).size();

        // Calculate diversity metrics
        double averageCrowdingDistance = population.getIndividuals().stream()
                .mapToDouble(Individual::getCrowdingDistance)
                .filter(d -> d != Double.POSITIVE_INFINITY)
                .average()
                .orElse(0.0);

        long infiniteDistanceCount = population.getIndividuals().stream()
                .mapToLong(ind -> ind.getCrowdingDistance() == Double.POSITIVE_INFINITY ? 1 : 0)
                .sum();

        // Calculate convergence metrics
        double averageRank = population.getIndividuals().stream()
                .mapToDouble(Individual::getRank)
                .average()
                .orElse(0.0);

        return new SelectionStats(totalIndividuals, frontCount, paretoFrontSize,
                averageCrowdingDistance, infiniteDistanceCount, averageRank);
        
    }

    /**
     * Verifies that selection maintains population diversity and convergence.
     * 
     * Validates that the selection process correctly preserves individuals
     * and maintains the integrity of the NSGA-II algorithm
     * 
     * @param beforeSelection The population before selection.
     * @param afterSelection The population after selection.
     * @return true if the selection is valid, false otherwise.
     * @throws IllegalArgumentException if populations are null or invalid
     */
    public static boolean validateSelection(Population beforeSelection, Population afterSelection) {
        // Input validation

        if (beforeSelection == null) {
            throw new IllegalArgumentException("Before selection population cannot be null.");
        }

        if (afterSelection == null) {
            throw new IllegalArgumentException("After selection population cannot be null.");
        }

        if(afterSelection.size() > beforeSelection.size()) {
            System.err.println("Error: After selection size exceeds before selection size.");
            return false;
        }

        // Check if all selected individuals exist in the original population
        List<Individual> originalIndividuals = beforeSelection.getIndividuals();
        for (Individual selected : afterSelection.getIndividuals()) {
            boolean exists = originalIndividuals.stream()
                .anyMatch(original -> areEquivalent(original, selected));
            if (!exists) {
                System.err.println("Error: Selected individual does not exist in the original population.");
                return false;
            }
        }
        return true;
    }

    // ==========================
    // HELPER METHODS
    // ==========================

    /**
     * Checks if two individuals are equivalent based on their decision variables.
     * 
     * Uses numerical tolerance to account for floating-point precision issues when
     * comparing decision variable values.
     * 
     * @param a First individual to compare.
     * @param b Second individual to compare.
     * @return true if they are equivalent, false otherwise.
     */
    private static boolean areEquivalent(Individual a, Individual b) {
        double[] varsA = a.getDecisionVariables();
        double[] varsB = b.getDecisionVariables();

        if (varsA.length != varsB.length) {
            return false; // Different number of decision variables
        }

        for (int i = 0; i < varsA.length; i++) {
            if (Math.abs(varsA[i] - varsB[i]) > 1e-9) { // Allow small numerical tolerance
                return false; // Decision variables differ
            }
        }
        return true;
    }

    // ==========================
    // INNER CLASSES
    // ==========================

    /**
     * Data class for selection statistics.
     * 
     * Contains metrics for monitoring NSGA-II selection performance,
     * including convergence and diversity indicators.
     */
    public static class SelectionStats {

        /** Total number of individuals in the population */
        public final int totalIndividuals;

        /** Number of Pareto fronts in the population */
        public final int frontCount;

        /** Size of the Pareto front */
        public final int paretoFrontSize;

        /** Average crowding distance (excluding infinite values) */
        public final double averageCrowdingDistance;

        /** Number of individuals with infinite crowding distance */
        public final long infiniteDistanceCount;

        /** Average rank across all individuals */
        public final double averageRank;

        /**
         * Constructs a selection statistics with key metrics.
         * 
         * @param totalIndividuals Total number of individuals
         * @param frontCount Number of Pareto fronts
         * @param paretoFrontSize Size of the Pareto front
         * @param averageCrowdingDistance Average crowding distance (excluding infinite values)
         * @param infiniteDistanceCount Number of individuals with infinite crowding distance
         * @param averageRank Average rank across all individuals
         */
        public SelectionStats(int totalIndividuals, int frontCount, int paretoFrontSize,
                              double averageCrowdingDistance, long infiniteDistanceCount, double averageRank) {
            this.totalIndividuals = totalIndividuals;
            this.frontCount = frontCount;
            this.paretoFrontSize = paretoFrontSize;
            this.averageCrowdingDistance = averageCrowdingDistance;
            this.infiniteDistanceCount = infiniteDistanceCount;
            this.averageRank = averageRank;
        }

        /**
         * Gets the diversity ratio (higher means better diversity).
         * 
         * Calculates the proportion of individuals with infinite crowding distance,
         * indicating boundary individuals that promote diversity.
         * 
         * @return Ratio of individuals with infinite crowding distance
         */
        public double getDiversityRatio() {
            return totalIndividuals > 0 ? (double) infiniteDistanceCount / totalIndividuals : 0.0;
        }

        /**
         * Gets the convergence ratio (lower means better convergence).
         * 
         * Calculates the normalised average rank, where lower values indicate
         * better convergence to the Pareto front.
         * 
         * @return Normalised average rank indicating convergence quality
         */
        public double getConvergenceRatio() {
            return totalIndividuals > 0 ? (double) averageRank / totalIndividuals : 0.0;
        }

        /**
         * Checks if the population maintains good diversity.
         * 
         * @return true if diversity ratio is above threshold (0.1), false otherwise
         */
        public boolean hasGoodDiversity() {
            return getDiversityRatio() > 0.1;
        }

        /**
         * Checks if the population shows good convergence.
         * 
         * @return true if convergence ratio is below threshold (0.5), false otherwise
         */
        public boolean hasGoodConvergence() {
            return getConvergenceRatio() < 0.5;
        }

        @Override
        public String toString() {
            return String.format("SelectionStats{totalIndividuals=%d, frontCount=%d, " +
                    "paretoFrontSize=%d, averageCrowdingDistance=%.3f, infiniteDistanceCount=%d, " +
                    "averageRank=%.2f}",
                    totalIndividuals, frontCount, paretoFrontSize,
                    averageCrowdingDistance, infiniteDistanceCount, averageRank);
        }
    }

}
   
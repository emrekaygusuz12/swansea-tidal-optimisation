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
 * - Energy output (maximised) = higher values preferred.
 * - Unit cost (minimised) = lower values preferred.
 * 
 * Based on the NSGA-II selection mechanism from Deb et al. (2002).
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class NextGenerationSelection {


    public static List<Individual> selectNextGeneration(Population combinedPopulation, int targetSize) {
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
                break; // We have filled the target size
            }
        }
        return selectedIndividuals;
    }

    /**
     * Selects individuals from a front using crowding distance.
     * Higher crowding distance (more isolated) individuals are preferred.
     * 
     * @param front The list of individuals in the same Pareto front.
     * @param count The number of individuals to select.
     * @return Selected individuals sorted by crowding distance (descending).
     */
    public static List<Individual> selectByCrowdingDistance(List<Individual> front, int count) {
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

    /**
     * Performs tournament selection based on NSGA-II crowded comparison.
     * Comparison order:
     * 1. Lower rank (better Pareto front) wins
     * 2. If ranks are equal, higher crowding distance wins
     * 
     * @param population Population to select from.
     * @param tournamentSize Number of individuals in each tournament.
     * @param numOfSelections Number of tournaments to run.
     * @return List of tournament winners.
     */
    public static List<Individual> tournamentSelection(Population population, int tournamentSize, int numOfSelections) {
        List<Individual> selected = new ArrayList<>();
        List<Individual> individuals = population.getIndividuals();
        Random random = new Random();

        for (int i = 0; i < numOfSelections; i++) {
            // Create tournament group
            List<Individual> tournament = new ArrayList<>();
            for (int j = 0; j < tournamentSize; j++) {
                int index = random.nextInt(individuals.size());
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
     * @param population The population to select from.
     * @param numParents Number of parents to select.
     * @return List of selected parent individuals for crossover and mutation.
     */
    public static List<Individual> selectParents (Population population, int numParents) {
        return tournamentSelection(population, 2, numParents);
    }

    public static Population combinePopulations(Population parents, Population offspring) {
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

    public static SelectionStats getSelectionStats(Population population) {
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
     * @param beforeSelection The population before selection.
     * @param afterSelection The population after selection.
     * @return true if the selection is valid, false otherwise.
     */
    public static boolean validateSelection(Population beforeSelection, Population afterSelection) {
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


    /**
     * Checks if two individuals are equivalent based on their decision variables.
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


    /**
     * Data class for selection statistics.
     * 
     * Contains metrics for monitoring NSGA-II selection performance.
     */
    public static class SelectionStats {
        public final int totalIndividuals;
        public final int frontCount;
        public final int paretoFrontSize;
        public final double averageCrowdingDistance;
        public final long infiniteDistanceCount;
        public final double averageRank;

        public SelectionStats(int totalIndividuals, int frontCount, int paretoFrontSize,
                              double averageCrowdingDistance, long infiniteDistanceCount, double averageRank) {
            this.totalIndividuals = totalIndividuals;
            this.frontCount = frontCount;
            this.paretoFrontSize = paretoFrontSize;
            this.averageCrowdingDistance = averageCrowdingDistance;
            this.infiniteDistanceCount = infiniteDistanceCount;
            this.averageRank = averageRank;
        }

        @Override
        public String toString() {
            return String.format("SelectionStats{totalIndividuals=%d, frontCount=%d, " +
                    "paretoFrontSize=%d, averageCrowdingDistance=%.3f, infiniteDistanceCount=%d, " +
                    "averageRank=%.2f}",
                    totalIndividuals, frontCount, paretoFrontSize,
                    averageCrowdingDistance, infiniteDistanceCount, averageRank);
        }

        /**
         * Gets the diversity ratio (higher means better diversity).
         * 
         * @return Ratio of individuals with infinite crowding distance
         */
        public double getDiversityRatio() {
            return totalIndividuals > 0 ? (double) infiniteDistanceCount / totalIndividuals : 0.0;
        }


        /**
         * Gets the convergence ratio (lower means better convergence).
         * 
         * @return Ratio of individuals in Pareto front
         */
        public double getConvergenceRatio() {
            return totalIndividuals > 0 ? (double) averageRank / totalIndividuals : 0.0;
        }
    }

}
   
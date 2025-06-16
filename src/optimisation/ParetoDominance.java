package src.optimisation;

import java.util.*;


/**
 * Handles Pareto dominance comparison and fast non-dominated sorting for NSGA-II.
 * 
 * Implements the multi-objective optimisation logic for tidal lagoon optimisation:
 * 
 * - Objective 1: Maximise energy output (MWh) - higher is better
 * - Objective 2: Minimise unit cost (GBP/MWh) - lower is better
 * 
 * Based on the fast non-dominated sorting algorithm from Deb et al. (2002).
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class ParetoDominance {

    /**
     * Determines if individual A dominates individual B.
     * 
     * For tidal lagoon optimisation:
     * - A has greater or equal energy output AND
     * - A has less or equal unit cost AND
     * - At least one inequality is strict
     * 
     * @param a First individual
     * @param b Second individual
     * @return true if A dominates B, false otherwise
     */
    public static boolean dominates(Individual a, Individual b) {
        double energyA = a.getEnergyOutput();
        double energyB = b.getEnergyOutput();
        double costA = a.getUnitCost();
        double costB = b.getUnitCost();

        // Handle invalid costs (individuals that generated no energy)
        if (costA == Double.MAX_VALUE && costB != Double.MAX_VALUE) {
            return false; // B is better (has valid cost)
        } else if (costA != Double.MAX_VALUE && costB == Double.MAX_VALUE) {
            return true; // A is better (has valid cost)
        } else if (costA == Double.MAX_VALUE && costB == Double.MAX_VALUE) {
            return energyA > energyB; // Both invalid, prefer higher energy
        }

        // Standard dominance comparison
        boolean energyBetter = energyA >= energyB; // higher energy is better
        boolean costBetter = costA <= costB; // lower cost is better

        // At least one objective must be strictly better
        boolean energyStrictlyBetter = energyA > energyB;
        boolean costStrictlyBetter = costA < costB;

        return energyBetter && costBetter && (energyStrictlyBetter || costStrictlyBetter);
    }


    /**
     * Compares dominance relationship between two individuals.
     * 
     * @param a First individual
     * @param b Second individual
     * @return -1 if A dominates B, 1 if B dominates A, 0 if neither dominates
     */
    public static int compare(Individual a, Individual b) {
        if (dominates(a, b)) {
            return -1; // A dominates B
        } else if (dominates(b, a)) {
            return 1; // B dominates A
        } else {
            return 0; // Neither dominates the other
        }
    }

    public static List<List<Individual>> fastNonDominatedSort(Population population){
        List<Individual> individuals = new ArrayList<>(population.getIndividuals());
        List<List<Individual>> fronts = new ArrayList<>();

        // Initialise dominance data for each individual
        Map<Individual, Set<Individual>> dominatedSolutions = new HashMap<>();
        Map<Individual, Integer> dominationCount = new HashMap<>();

        for (Individual individual : individuals) {
            dominatedSolutions.put(individual, new HashSet<>());
            dominationCount.put(individual, 0);
        }

        // Calculate dominance relationships
        for (int i = 0; i < individuals.size(); i++) {
            Individual p = individuals.get(i);

            for (int j = 0; j < individuals.size(); j++) {
                if (i == j) continue; // Skip self-comparison

                Individual q = individuals.get(j);

                if (dominates(p, q)){
                    // p dominates q
                    dominatedSolutions.get(p).add(q);
                } else if (dominates(q, p)) {
                    // q dominates p
                    dominationCount.put(p, dominationCount.get(p) + 1);
                }
            }

            // If p belongs to the first front
            if (dominationCount.get(p) == 0) {
                p.setRank(0); // Rank 0 for first front
                if (fronts.isEmpty()) {
                    fronts.add(new ArrayList<>());
                }
                fronts.get(0).add(p);
            }
        }

        // Build subsequent fronts
        int frontIndex = 0;
        while (frontIndex < fronts.size()) {
            List<Individual> nextFront = new ArrayList<>();

            for (Individual p : fronts.get(frontIndex)){
                for (Individual q : dominatedSolutions.get(p)) {
                    int qDominationCount = dominationCount.get(q) - 1;
                    dominationCount.put(q, qDominationCount);

                    if (qDominationCount == 0) {
                        q.setRank(frontIndex + 1); // Assign next rank
                        nextFront.add(q);
                    }
                }
            }

            if (!nextFront.isEmpty()) {
                fronts.add(nextFront);
            }

            frontIndex++;
        }

        return fronts;
    }


    /**
     * Gets all non-dominated individuals from the population (Pareto front).
     * 
     * @param population Population to evaluate
     * @return List of non-dominated individuals (Pareto front)
     */
    public static List<Individual> getParetoFront(Population population){
        List<List<Individual>> fronts = fastNonDominatedSort(population);
        return fronts.isEmpty() ? new ArrayList<>() : fronts.get(0); // Return first front (Pareto front)
    }


    /**
     * Checks if an individual is dominated by any individual in a collection.
     * 
     * @param individual Individual to check
     * @param others Collection of individuals to compare against
     * @return true if individual is dominated by any in the collection, false otherwise
     */
    public static boolean isDominated(Individual individual, Collection<Individual> others){
        for (Individual other : others){
            if (dominates(other, individual)){
                return true;
            }
        }

        return false; // Not dominated by any other individual
    }

    /**
     * Counts how many individuals in a collection dominate the given individual.
     * 
     * @param individual Individual to check
     * @param others Collection of individuals to compare against
     * @return Number of individuals that dominate the given individual
     */
    public static int countDominatingIndividuals(Individual individual, Collection<Individual> others){
        int count = 0;

        for (Individual other : others) {
            if (dominates(other, individual)) {
                count++;
            }
        }

        return count; // Return the number of individuals that dominate the given individual
    }

    /**
     * Gets dominance statistics for monitoring optimisation progress.
     * 
     * @param population Population to evaluate
     * @return DominanceStats object containing key metrics
     */
    public static DominanceStats getDominanceStatistics(Population population){
        List<List<Individual>> fronts = fastNonDominatedSort(population);

        int totalIndividuals = population.size();
        int frontCount = fronts.size();
        int paretoFrontSize = fronts.isEmpty() ? 0 : fronts.get(0).size();

        // Calculate average rank
        double averageRank = 0.0;
        for (Individual individual : population.getIndividuals()) {
            averageRank += individual.getRank();
        }
        averageRank /= totalIndividuals;

        return new DominanceStats(totalIndividuals, frontCount, paretoFrontSize, averageRank, fronts);
    }

    /**
     * Data class for dominance analysis statistics.
     */
    public static class DominanceStats {
        public final int totalIndividuals;
        public final int frontCount;
        public final int paretoFrontSize;
        public final double averageRank;
        public final List<List<Individual>> fronts;

        public DominanceStats(int totalIndividuals, int frontCount, int paretoFrontSize, 
                              double averageRank, List<List<Individual>> fronts) {
            this.totalIndividuals = totalIndividuals;
            this.frontCount = frontCount;
            this.paretoFrontSize = paretoFrontSize;
            this.averageRank = averageRank;
            this.fronts = new ArrayList<>(fronts);
        }

        @Override
        public String toString() {
            return String.format("DominanceStats[totalIndividuals=%d, frontCount=%d, paretoFrontSize=%d, averageRank=%.1f]",
                                 totalIndividuals, frontCount, paretoFrontSize, averageRank);
        }

        /**
         * Gets the sizes of each front for evaluation.
         * 
         * @return Array of front sizes
         */
        public int[] getFrontSizes(){
            return fronts.stream().mapToInt(List::size).toArray();
        }

        /**
         * Gets the individuals in a specific front.
         * 
         * @param frontIndex Front index (0 = Pareto front)
         * @return List of individuals in the specified front
         */
        public List<Individual> getFront(int frontIndex){
            if (frontIndex >= 0 && frontIndex < fronts.size()){
                return new ArrayList<>(fronts.get(frontIndex));
            }
            return new ArrayList<>(); 
        }
    }
}
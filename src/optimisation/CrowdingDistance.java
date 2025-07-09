package src.optimisation;

import java.util.*;


/**
 * Calculates crowding distance for diversity preservation in NSGA-II.
 * 
 * Crowding distance measures how close an individual is to its neighbours
 * in objective space. Higher crowding distance indicates more isolated solutions,
 * which helps maintain diversity in the Pareto front.
 * 
 * For tidal lagoon optimisation with objectives:
 * - Energy Output (maximise) 
 * - Unit Cost (minimise)
 * 
 * Based on the crowding distance algorithm from Deb et al. (2002).
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class CrowdingDistance {

    /**
     * Calculates crowding distance for all individuals in a population.
     * 
     * @param individuals List of individuals in the population
     */
    public static void calculateCrowdingDistance(List<Individual> individuals) {
        int populationSize = individuals.size();

        // Initialise all crowding distances to 0
        for (Individual individual : individuals) {
            individual.setCrowdingDistance(0.0);
        }

        if (populationSize == 0) {
            return;
        }

        if (populationSize == 1) {
            individuals.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
            return;
        }

        calculateCrowdingDistanceForEnergyObjective(individuals);
        calculateCrowdingDistanceForCostObjective(individuals);
    }

    /**
     * Calculates crowding distance contribution for energy output objective.
     * 
     * @param individuals List of individuals to evaluate
     */
    private static void calculateCrowdingDistanceForEnergyObjective(List<Individual> individuals) {
        if (individuals.size() < 2) {
            return;
        }

        // Sort by energy output (ascending order)
        List<Individual> sortedByEnergy = new ArrayList<>(individuals);
        sortedByEnergy.sort(Comparator.comparingDouble(Individual::getEnergyOutput));

        // Set boundary solutions to infinite distance
        sortedByEnergy.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        sortedByEnergy.get(sortedByEnergy.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

        if (sortedByEnergy.size() == 2) {
            return;
        }

        // Calculate range of energy values
        double minEnergy = sortedByEnergy.get(0).getEnergyOutput();
        double maxEnergy = sortedByEnergy.get(sortedByEnergy.size() - 1).getEnergyOutput();
        double energyRange = maxEnergy - minEnergy;

        if (energyRange == 0) {
            return;
        }

        // Calculate crowding distance for intermediate solutions
        for (int i = 1; i < sortedByEnergy.size() - 1; i++) {
            Individual current = sortedByEnergy.get(i);

            if (current.getCrowdingDistance() == Double.POSITIVE_INFINITY) {
                continue;
            }

            double energyBefore = sortedByEnergy.get(i - 1).getEnergyOutput();
            double energyAfter = sortedByEnergy.get(i + 1).getEnergyOutput();

            // Add normalised distance between neighbours
            double normalisedDistance = (energyAfter - energyBefore) / energyRange;
            current.setCrowdingDistance(current.getCrowdingDistance() + normalisedDistance);
        }
    }
    
    /**
     * Calculates crowding distance contribution for unit cost objective.
     * 
     * @param individuals List of individuals to evaluate
     */
    private static void calculateCrowdingDistanceForCostObjective(List<Individual> individuals) {
        // Filter out individuals with invalid costs
        List<Individual> validCostIndividuals = individuals.stream()
                .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                .toList();

        if (validCostIndividuals.size() < 3) {
            return; 
        }

        // Sort by unit cost (ascending - lower cost is better)
        List<Individual> sortedByCost = new ArrayList<>(validCostIndividuals);
        sortedByCost.sort(Comparator.comparingDouble(Individual::getUnitCost));

        // Set boundary solutions to infinite distance
        sortedByCost.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        sortedByCost.get(sortedByCost.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

        // Calculate range of cost values
        double minCost = sortedByCost.get(0).getUnitCost();
        double maxCost = sortedByCost.get(sortedByCost.size() - 1).getUnitCost();
        double costRange = maxCost - minCost;

        if (costRange == 0) {
            return;
        }

        // Calculate crowding distance for intermediate solutions
        for (int i = 1; i < sortedByCost.size() - 1; i++) {
            Individual current = sortedByCost.get(i);

            if (current.getCrowdingDistance() == Double.POSITIVE_INFINITY) {
                continue;
            }

            double costBefore = sortedByCost.get(i - 1).getUnitCost();
            double costAfter = sortedByCost.get(i + 1).getUnitCost();

            // Add normalised distance between neighbours
            double normalisedDistance = (costAfter - costBefore) / costRange;
            current.setCrowdingDistance(current.getCrowdingDistance() + normalisedDistance);
        }
    }

    /**
     * Sorts individuals by their crowding distance in descending order.
     * Higher crowding distance (more isolated) individuals come first.
     * 
     * @param individuals List of individuals to sort
     */
    public static void sortByCrowdingDistance(List<Individual> individuals) {
        individuals.sort((a, b) -> Double.compare(b.getCrowdingDistance(), a.getCrowdingDistance()));
    }

    
    /**
     * Compares two individuals based on their crowding distance.
     * 
     * @param a First individual
     * @param b Second individual
     * @return -1 if a has higher crowding distance, 1 if b has higher, 0 if equal
     */
    public static int crowdedCompare(Individual a, Individual b) {
        int rankComparison = Integer.compare(a.getRank(), b.getRank());
        if (rankComparison != 0) {
            return rankComparison; // Compare by rank first
        }

        // If ranks are equal, compare by crowding distance (higher is better)
        return Double.compare(b.getCrowdingDistance(), a.getCrowdingDistance());
    }

    /**
     * Creates a comparator for the crowded comparison operator.
     * 
     * @return Comparator that implements crowded comparison
     */
    public static Comparator<Individual> getCrowdedComparator() {
        return CrowdingDistance::crowdedCompare;
    }

    /**
     * Selects individuals with highest crowding distance from a front.
     * 
     * @param front List of individuals in the current Pareto front
     * @param count Number of individuals to select
     * @return List of selected individuals sorted by crowding distance
     */
    public static List<Individual> selectByCrowdingDistance(List<Individual> front, int count) {
        if (count >= front.size()) {
            return new ArrayList<>(front);
        }

        calculateCrowdingDistance(front);

        // Sort by crowding distance (descending) and select top count
        List<Individual> sortedFront = new ArrayList<>(front);
        sortByCrowdingDistance(sortedFront);
        return sortedFront.subList(0, count);
    }

    /**
     * Gets crowding distance statistics for monitoring diversity.
     * 
     * @param individuals List of individuals to evaluate
     * @return CrowdingStats object containing diversity metrics
     */
    public static CrowdingStats getCrowdingStatistics(List<Individual> individuals) {
        if (individuals.isEmpty()) {
            return new CrowdingStats(0, 0, 0, 0, 0);
        }

        calculateCrowdingDistance(individuals);

        double minDistance = Double.POSITIVE_INFINITY;
        double maxDistance = 0.0;
        double sumDistance = 0.0;
        int infiniteCount = 0;

        for (Individual individual : individuals) {
            double distance = individual.getCrowdingDistance();
            
            if (distance == Double.POSITIVE_INFINITY) {
                infiniteCount++;
            } else {
                minDistance = Math.min(minDistance, distance);
                maxDistance = Math.max(maxDistance, distance);
                sumDistance += distance;
            }
        }

        int finiteCount = individuals.size() - infiniteCount;
        double avgDistance = finiteCount > 0 ? sumDistance / finiteCount : 0.0;

        return new CrowdingStats(individuals.size(), infiniteCount,
                                 minDistance == Double.POSITIVE_INFINITY ? 0 : minDistance,
                                 maxDistance, avgDistance);
    }

    /**
     * Data class for crowding distance statistics.
     * Contains metrics for monitoring diversity in the population.
     */
    public static class CrowdingStats {
        public final int totalIndividuals;
        public final int infiniteDistanceCount;
        public final double minFiniteDistance;
        public final double maxFiniteDistance;
        public final double averageFiniteDistance;

        public CrowdingStats(int totalIndividuals, int infiniteDistanceCount,
                             double minFiniteDistance, double maxFiniteDistance, double averageFiniteDistance) {
            this.totalIndividuals = totalIndividuals;
            this.infiniteDistanceCount = infiniteDistanceCount;
            this.minFiniteDistance = minFiniteDistance;
            this.maxFiniteDistance = maxFiniteDistance;
            this.averageFiniteDistance = averageFiniteDistance;
        }

        @Override
        public String toString() {
            return String.format("CrowdingStats[total=%d, infinite=%d, finite=%.3f-%.3f(avg=%.3f)]",
                totalIndividuals, infiniteDistanceCount, minFiniteDistance, 
                maxFiniteDistance, averageFiniteDistance);
        }

        /**
         * Gets the diversity ratio (higher indicates better diversity).
         * 
         * @return Ratio of infinite distance individuals to total
         */
        public double getDiversityRatio() {
            return totalIndividuals > 0 ? (double) infiniteDistanceCount / totalIndividuals : 0.0;
        }
    }
}

package src.optimisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/**
 * Manages a population of individuals for NSGA-II optimisation.
 * 
 * Provides utilities for population initialisation, statistics, 
 * and integration with the NSGA-II algorithm components.
 * 
 * @author Emre Kaygusuz
 * @version 1.1
 */
public class Population {

    private List<Individual> individuals;
    private int maxSize; // Maximum population size

    /**
     * Constructs a new population with a specified maximum size.
     * 
     * @param maxSize The maximum number of individuals in the population.
     */
    public Population(int maxSize) {
        this.maxSize = maxSize;
        this.individuals = new ArrayList<>(maxSize);
    }

    /**
     * Constructs a population from an existing list of individuals.
     * 
     * @param individuals The list of individuals to initialise the population with.
     */
    public Population(List<Individual> individuals) {
        this.individuals = new ArrayList<>(individuals);
        this.maxSize = individuals.size();
    }

    public void initialiseSmartRandom(int populationSize, int numberOfHalfTides) {
        individuals.clear();

        for (int i = 0; i < populationSize; i++) {
            Individual individual;

            double initType = (double) i / populationSize;

            if (initType < 0.3) {
                individual = IndividualGenerator.createRandomIndividual(numberOfHalfTides, "ENERGY_FOCUSED");
            } else if (initType < 0.6) {
                individual = IndividualGenerator.createRandomIndividual(numberOfHalfTides, "BALANCED");
            } else {
                individual = IndividualGenerator.createRandomIndividual(numberOfHalfTides, "DIVERSE");
            }
            individuals.add(individual);
        }
        System.out.printf("Initialised smart population: %d energy-focused, %d balanced, %d diverse individuals%n", 
                     (int)(populationSize * 0.3), (int)(populationSize * 0.3), (int)(populationSize * 0.4));

    }

    public void initialiseRandom(int populationSize, int numberOfHalfTides) {
        initialiseSmartRandom(populationSize, numberOfHalfTides);
    }

    // /**
    //  * Initialises population with random individuals.
    //  * 
    //  * @param populationSize The number of individuals to generate.
    //  * @param numberOfHalfTides The number of half-tides for each individual.
    //  */
    // public void initialiseRandom(int populationSize, int numberOfHalfTides) {
    //     individuals.clear();

    //     for (int i = 0; i < populationSize; i++) {
    //         Individual individual = IndividualGenerator.createRandomIndividual(numberOfHalfTides);
    //         individuals.add(individual);
    //     }

    //     System.out.printf("Initialised population with %d random individuals %n", populationSize);

    // }

    /**
     * Adds an individual to the population.
     * @param individual Individual to add
     * @throws IllegalStateException if the population is at maximum capacity.
     */
    public void addIndividual(Individual individual) {
        if (individuals.size() >= maxSize) {
            throw new IllegalStateException("Population is at maximum capacity: " + maxSize);
        }
        individuals.add(individual);
    }

    /**
     * Removes an individual from the population.
     * 
     * @param individual Individual to remove
     * @return true if the individual was successfully removed, false if it was not found.
     */
    public boolean removeIndividual(Individual individual) {
        return individuals.remove(individual);
    }

    /**
     * Gets an individual at the specified index.
     * 
     * @param index Index of the individual
     * @return Individual at the specified index
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public Individual getIndividual(int index) {
        if (index < 0 || index >= individuals.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        return individuals.get(index);
    }

    /**
     * Gets all individuals in the population.
     * 
     * @return List of all individuals.
     */
    public List<Individual> getIndividuals() {
        return Collections.unmodifiableList(individuals);
    }

    /**
     * Gets the current size of the population.
     * 
     * @return The number of individuals in the population.
     */
    public int size() {
        return individuals.size();
    }

    /**
     * Gets the maximum allowed size of the population.
     * @return Maximum population size.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Checks if the population is empty.
     * 
     * @return true if the population is empty, false otherwise.
     */
    public boolean isEmpty() {
        return individuals.isEmpty();
    }

    /**
     * Clears all individuals from the population.
     */
    public void clear(){
        individuals.clear();
    }

    /**
     * Creates a copy of this population.
     * 
     * @return Deep copy of the population with cloned individuals.
     */
    public Population copy() {
        Population copy = new Population(maxSize);
        for (Individual individual : individuals) {
            copy.addIndividual(individual.clone());
        }
        return copy;
    }

    /**
     * Sorts the population using the specified comparator.
     * 
     * @param comparator Comparator to use for sorting
     */
    public void sort(Comparator<Individual> comparator) {
        Collections.sort(individuals, comparator);
    }

    public PopulationStats getStatistics() {
        if (individuals.isEmpty()) {
            return new PopulationStats(0, 0, 0, 0, 0, 0, 0);
        }

        double minEnergy = Double.MAX_VALUE;
        double maxEnergy = Double.MIN_VALUE;
        double sumEnergy = 0;

        double minCost = Double.MAX_VALUE;
        double maxCost = Double.MIN_VALUE;
        double sumCost = 0;
        int validCostCount = 0; // Track individuals with valid costs

        for (Individual individual : individuals) {
            double energy = individual.getEnergyOutput();
            double cost = individual.getUnitCost();

            minEnergy = Math.min(minEnergy, energy);
            maxEnergy = Math.max(maxEnergy, energy);
            sumEnergy += energy;

            if (cost != Double.MAX_VALUE){
                minCost = Math.min(minCost, cost);
                maxCost = Math.max(maxCost, cost);
                sumCost += cost;
                validCostCount++;
            }
        }

        double avgEnergy = sumEnergy / individuals.size();
        double avgCost = (validCostCount > 0) ? sumCost / validCostCount : Double.MAX_VALUE;

        if (validCostCount == 0) {
            minCost = Double.MAX_VALUE;
            maxCost = Double.MAX_VALUE; // Indicates no valid costs
        }

        return new PopulationStats(individuals.size(), minEnergy, maxEnergy, avgEnergy, 
                                   minCost, maxCost, avgCost);
    }

    public Population combine(Population other){
        Population combined = new Population(this.size() + other.size());

        // Add all individuals from both populations
        for (Individual individual : this.individuals) {
            combined.addIndividual(individual.clone());
        }

        for (Individual individual : other.individuals) {
            combined.addIndividual(individual.clone());
        }

        return combined;
    }

    /**
     * Gets the best individuals by best energy output.
     * 
     * @param count Number of best individuals to retrieve
     * @return List of the best individuals sorted by energy output in descending order.
     */
    public List<Individual> getBestByEnergy(int count){
        return individuals.stream()
                .sorted((a, b) -> Double.compare(b.getEnergyOutput(), a.getEnergyOutput()))
                .limit(count)
                .toList();
    }

    /**
     * Gets the best individuals based on unit cost (lowest cost).
     * 
     * @param count Number of best individuals to return
     * @return List of individuals with lowest unit cost.
     */
    public List<Individual> getBestByCost(int count){
        return individuals.stream()
                .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
                .sorted((a, b)-> Double.compare(a.getUnitCost(), b.getUnitCost()))
                .limit(count)
                .toList();
    }

    @Override
    public String toString() {
        return String.format("Population[size=%d, maxSize=%d]", 
                             individuals.size(), maxSize);
    }

    /**
     * Data class for population statistics.
     */
    public static class PopulationStats {
        public final int size;
        public final double minEnergy, maxEnergy, avgEnergy;
        public final double minCost, maxCost, avgCost;

        public PopulationStats(int size, double minEnergy, double maxEnergy, double avgEnergy,
                               double minCost, double maxCost, double avgCost) {
            this.size = size;
            this.minEnergy = minEnergy;
            this.maxEnergy = maxEnergy;
            this.avgEnergy = avgEnergy;
            this.minCost = minCost;
            this.maxCost = maxCost;
            this.avgCost = avgCost;
        }

        @Override
        public String toString() {
            return String.format("PopulationStats[size=%d, energy=%.1f-%.1f(avg=%.1f), cost=%.0f-%.0f(avg=%.0f)]",
                size, minEnergy, maxEnergy, avgEnergy, minCost, maxCost, avgCost);
        }
    }

}

package src.optimisation;

import java.util.Random;


/**
 * Utility class for generating individuals with randomised control parameters.
 * Each individual represents a candidate solution to the optimisation problem
 * using the NSGA-II algorithm. This class generates feasible starting values for:
 * 
 * - Hs (starting head)
 * - He (ending head)
 * 
 * for each half-tide in the simulation window.
 * 
 * All values are generated within the constraint range defined in the Individual class:
 * - Hs: [0.5, 4.0] meters
 * - He: [0.5, 4.0] meters
 * 
 * @see Individual
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class IndividualGenerator {

    // ==========================
    // RANDOM GENERATOR
    // ==========================

    /** Random number generator for creating diverse initial populations */
    private static final Random RANDOM = new Random();

    // ==========================
    // FACTORY METHODS
    // ==========================

    /**
     * Creates a random individual based on the specified initialisation strategy.
     * 
     * @param numberOfHalfTides the number of half-tides in the simulation window
     * @param initialisationType the type of initialisation strategy to use.
     * @return a new Individual with randomised control parameters
     */
    public static Individual createRandomIndividual(int numberOfHalfTides, String initialisationType) {
        switch (initialisationType.toUpperCase()){
            case "ENERGY_FOCUSED":
                return createEnergyFocusedIndividual(numberOfHalfTides);
            case "BALANCED":
                return createBalancedIndividual(numberOfHalfTides);
            case "DIVERSE":
                return createDiverseIndividual(numberOfHalfTides);
            default:
                return createRandomIndividual(numberOfHalfTides);
        } 
    }

    /**
     * Creates a random individual using the diverse strategy (default behaviour).
     * 
     * @param numberOfHalfTides The number of half-tides in the simulation window.
     * @return a new Individual with diverse randomised control parameters.
     */
    public static Individual createRandomIndividual(int numberOfHalfTides) {
        return createDiverseIndividual(numberOfHalfTides);
    }

    // ==============================
    // PRIVATE GENERATION STRATEGIES
    // ==============================

    /**
     * Creates an individual with energy-focused parameter values.
     * Biases towards higher head values to maximise energy output potential.
     * 
     * @param numberOfHalfTides The number of half-tides in the simulation window.
     * @return a new Individual optimised for energy generation.
     */
    private static Individual createEnergyFocusedIndividual(int numberOfHalfTides){
        Individual individual = new Individual(numberOfHalfTides);

        // Focus on high energy output values
        for (int i = 0; i < numberOfHalfTides; i++) {
            double energyBiasedMin = 1.5;
            double energyBiasedMax = 4.0; // Higher range for energy output
            double hs = RANDOM.nextDouble() * (energyBiasedMax - energyBiasedMin) + energyBiasedMin;
            double he = RANDOM.nextDouble() * (energyBiasedMax - energyBiasedMin) + energyBiasedMin;

            if (RANDOM.nextDouble() < 0.1) {
                // Occasionally introduce a low value to simulate variability
                hs = RANDOM.nextDouble() * 3.5 + 0.5; // Low head value
                he = RANDOM.nextDouble() * 3.5 + 0.5; // Low head value
            }
            individual.setStartHead(i, hs);
            individual.setEndHead(i, he);
        }

        return individual;
    }

    /**
     * Creates an individual with balanced parameter values.
     * Provides a mix of energy focused and cost efficient strategies.
     * 
     * @param numberOfHalfTides The number of half-tides in the simulation window.
     * @return a new Individual with balanced control parameters.
     */
    private static Individual createBalancedIndividual(int numberOfHalfTides){
        Individual individual = new Individual(numberOfHalfTides);

        // Balanced strategy with mid-range values
        for (int i = 0; i < numberOfHalfTides; i++) {
            double hs, he;

            if (RANDOM.nextDouble() < 0.6) { // 60% energy focused
                hs = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
                he = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
            } else {
                // Occasionally use high values
                hs = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
                he = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
            }

            individual.setStartHead(i, hs);
            individual.setEndHead(i, he);
        }

        return individual;
    }

    /**
     * Creates an individual with diverse parameter values.
     * Uses multiple strategies to ensure population diversity for better exploration.
     * 
     * @param numberOfHalfTides The number of half-tides in the simulation window.
     * @return a new Individual with diverse control parameters.
     */
    private static Individual createDiverseIndividual(int numberOfHalfTides){
        Individual individual = new Individual(numberOfHalfTides);

        double strategyType = RANDOM.nextDouble();

        // Diverse strategy with a mix of low, mid, and high values
        for (int i = 0; i < numberOfHalfTides; i++) {
            double hs, he;

            if (strategyType < 0.15) {
                // Occasionally use low head values for cost efficiency
                hs = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
                he = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
            } else if (strategyType < 0.45) {
                // Mid-range values for balanced performance
                hs = RANDOM.nextDouble() * 1.5 + 2.5; // [2.5, 4.0]
                he = RANDOM.nextDouble() * 1.5 + 2.5; // [2.5, 4.0]
            } else if (strategyType < 0.75) {
                // High head values for energy maximisation
                hs = RANDOM.nextDouble() * 2.0 + 1.5; // [1.5, 3.5]
                he = RANDOM.nextDouble() * 2.0 + 1.5; // [1.5, 3.5]
            } else {
                if (RANDOM.nextDouble() < 0.7) {
                    // Random high values for energy focus
                    hs = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
                    he = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
                } else {
                    // Full range random values for maximum diversity
                    hs = RANDOM.nextDouble() * 3.5 + 0.5; // [0.5, 4.0]
                    he = RANDOM.nextDouble() * 3.5 + 0.5; // [0.5, 4.0]
                }
            }

            individual.setStartHead(i, hs);
            individual.setEndHead(i, he);
        }

        return individual;
    }
}


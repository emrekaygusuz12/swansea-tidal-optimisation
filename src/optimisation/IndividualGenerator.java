package src.optimisation;

import java.util.Random;


/**
 * Utility class for generating individuals with randomised control parameters.
 * Each individual represents a candidate solution to the optimisation problem
 * using the NSGA-II algorithm. This class generates feasible starting values for:
 * 
 * * - Hs (starting head)
 * * - He (ending head)
 * for each half-tide in the simulation window.
 * 
 * All values are generated within the constraint range defined in the Individual class:
 * 
 * * - Hs: [0.5, 4.0] meters
 * * - He: [0.5, 4.0] meters
 * 
 * @link Individual
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class IndividualGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Creates a new individual with randomly generated Hs and He values
     * for each half-tide. Values are guaranteed to be within the pysical constraints
     * defined in the Individual class.
     * 
     * @param numberOfHalfTides The number of half-tide cycles to encode (48 for 1 day).
     * @return A fully initialised individual with control parameters.
     */
    public static Individual createRandomIndividual(int numberOfHalfTides) {
        Individual individual = new Individual(numberOfHalfTides);

        // Create different strategy archetypes for better diversity
        double strategyType = RANDOM.nextDouble();

        for (int i = 0; i < numberOfHalfTides; i++) {
            double hs, he;
        
            if (strategyType < 0.25) {
                // Conservative strategy: Low head values [0.5, 2.0]
                hs = RANDOM.nextDouble() * 1.5 + 0.5;
                he = RANDOM.nextDouble() * 1.5 + 0.5;
            } else if (strategyType < 0.5) {
                // Aggressive strategy: High head values [2.5, 4.0]
                hs = RANDOM.nextDouble() * 1.5 + 2.5;
                he = RANDOM.nextDouble() * 1.5 + 2.5;
            } else if (strategyType < 0.75) {
                // Balanced strategy: Mid-range values [1.5, 3.5]
                hs = RANDOM.nextDouble() * 2.0 + 1.5;
                he = RANDOM.nextDouble() * 2.0 + 1.5;
            } else {
                // Random strategy: Full range [0.5, 4.0]
                hs = RANDOM.nextDouble() * 3.5 + 0.5;
                he = RANDOM.nextDouble() * 3.5 + 0.5;
            }

                // Set the decision variables for this half-tide
                individual.setStartHead(i, hs); // Hs
                individual.setEndHead(i, he); // He
        }
        
        return individual;
    }
}


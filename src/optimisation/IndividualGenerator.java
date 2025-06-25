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
        
        for (int i = 0; i < numberOfHalfTides; i++) {
            // Generate random Hs and He values within the defined range [0.5, 4.0]
            double hs = 3.5 * RANDOM.nextDouble() + 0.5;
            double he = 3.5 * RANDOM.nextDouble() + 0.5;
            // Set the decision variables for this half-tide
            individual.setStartHead(i, hs); // Hs
            individual.setEndHead(i, he); // He
        }
        
        return individual;
    }
    
}

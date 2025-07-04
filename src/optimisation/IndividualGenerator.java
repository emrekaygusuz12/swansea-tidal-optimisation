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

    private static Individual createDiverseIndividual(int numberOfHalfTides){
        Individual individual = new Individual(numberOfHalfTides);

        double strategyType = RANDOM.nextDouble();

        // Diverse strategy with a mix of low, mid, and high values
        for (int i = 0; i < numberOfHalfTides; i++) {
            double hs, he;

            if (strategyType < 0.15) {
                // Low head values
                hs = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
                he = RANDOM.nextDouble() * 1.5 + 0.5; // [0.5, 2.0]
            } else if (strategyType < 0.45) {
                // Mid-range values
                hs = RANDOM.nextDouble() * 1.5 + 2.5; // [2.5, 4.0]
                he = RANDOM.nextDouble() * 1.5 + 2.5; // [2.5, 4.0]
            } else if (strategyType < 0.75) {
                // High head values
                hs = RANDOM.nextDouble() * 2.0 + 1.5; // [1.5, 3.5]
                he = RANDOM.nextDouble() * 2.0 + 1.5; // [1.5, 3.5]
            } else {
                if (RANDOM.nextDouble() < 0.7) {
                    // Randomly choose low values
                    hs = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
                    he = RANDOM.nextDouble() * 2.0 + 2.0; // [2.0, 4.0]
                } else {
                    // Randomly choose high values
                    hs = RANDOM.nextDouble() * 3.5 + 0.5; // [0.5, 4.0]
                    he = RANDOM.nextDouble() * 3.5 + 0.5; // [0.5, 4.0]
                }
            }

            individual.setStartHead(i, hs);
            individual.setEndHead(i, he);
        }

        return individual;
    }

    public static Individual createRandomIndividual(int numberOfHalfTides) {
        return createDiverseIndividual(numberOfHalfTides);
    }

}

//     /**
//      * Creates a new individual with randomly generated Hs and He values
//      * for each half-tide. Values are guaranteed to be within the pysical constraints
//      * defined in the Individual class.
//      * 
//      * @param numberOfHalfTides The number of half-tide cycles to encode (48 for 1 day).
//      * @return A fully initialised individual with control parameters.
//      */
//     public static Individual createRandomIndividual(int numberOfHalfTides) {
//         Individual individual = new Individual(numberOfHalfTides);

//         // Create different strategy archetypes for better diversity
//         double strategyType = RANDOM.nextDouble();

//         for (int i = 0; i < numberOfHalfTides; i++) {
//             double hs, he;
        
//             if (strategyType < 0.25) {
//                 // Conservative strategy: Low head values [0.5, 2.0]
//                 hs = RANDOM.nextDouble() * 1.5 + 0.5;
//                 he = RANDOM.nextDouble() * 1.5 + 0.5;
//             } else if (strategyType < 0.5) {
//                 // Aggressive strategy: High head values [2.5, 4.0]
//                 hs = RANDOM.nextDouble() * 1.5 + 2.5;
//                 he = RANDOM.nextDouble() * 1.5 + 2.5;
//             } else if (strategyType < 0.75) {
//                 // Balanced strategy: Mid-range values [1.5, 3.5]
//                 hs = RANDOM.nextDouble() * 2.0 + 1.5;
//                 he = RANDOM.nextDouble() * 2.0 + 1.5;
//             } else {
//                 // Random strategy: Full range [0.5, 4.0]
//                 hs = RANDOM.nextDouble() * 3.5 + 0.5;
//                 he = RANDOM.nextDouble() * 3.5 + 0.5;
//             }

//                 // Set the decision variables for this half-tide
//                 individual.setStartHead(i, hs); // Hs
//                 individual.setEndHead(i, he); // He
//         }
        
//         return individual;
//     }
// }


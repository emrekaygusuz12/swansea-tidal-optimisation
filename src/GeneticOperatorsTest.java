package src;

import src.optimisation.Individual;
import src.optimisation.GeneticOperators;

import java.util.*;

/**
 * Test suite for GeneticOperators class focusing on critical functionality
 * and bug validation for tidal lagoon optimization.
 * 
 * Tests cover:
 * - Critical index calculation fixes
 * - Constraint boundary handling
 * - Input validation
 * - Basic operator functionality
 * 
 * @author Emre Kaygusuz
 */
public class GeneticOperatorsTest {

    private static final double TOLERANCE = 1e-10;
    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("🧪 Starting GeneticOperators Test Suite...\n");

        // Run all test categories
        testConstraintHandling();
        testIndexCalculations();
        testInputValidation();
        testBasicFunctionality();
        testThreadSafety();
        testStatistics();

        // Print final results
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("🎯 Test Results: %d/%d tests passed (%.1f%%)\n", 
                         testsPassed, testsTotal, (double)testsPassed/testsTotal*100);
        
        if (testsPassed == testsTotal) {
            System.out.println("ALL TESTS PASSED! Genetic operators are ready! 🚀");
        } else {
            System.out.println("Some tests failed. Review implementation.");
        }
    }

    /**
     * CRITICAL: Test constraint handling (0.5m ≤ head ≤ 4.0m)
     */
    private static void testConstraintHandling() {
        System.out.println("--- Testing Constraint Handling ---");

        // Test 1: SBX with extreme values
        Individual parent1 = new Individual(1);
        Individual parent2 = new Individual(1);
        parent1.getDecisionVariables()[0] = 0.5;  // Min boundary
        parent1.getDecisionVariables()[1] = 4.0;  // Max boundary
        parent2.getDecisionVariables()[0] = 2.0;  // Middle
        parent2.getDecisionVariables()[1] = 2.0;  // Middle

        Individual[] offspring = GeneticOperators.simulatedBinaryCrossover(parent1, parent2, 1.0);
        
        boolean constraintsValid = true;
        for (Individual child : offspring) {
            for (double var : child.getDecisionVariables()) {
                if (var < 0.5 || var > 4.0) {
                    constraintsValid = false;
                    break;
                }
            }
        }
        assertTest("SBX respects constraints", constraintsValid);

        // Test 2: Polynomial mutation with boundary values
        Individual individual = new Individual(2);
        individual.getDecisionVariables()[0] = 0.5;  // Min
        individual.getDecisionVariables()[1] = 4.0;  // Max
        individual.getDecisionVariables()[2] = 2.0;  // Middle
        individual.getDecisionVariables()[3] = 2.0;  // Middle

        Individual mutated = GeneticOperators.polynomialMutation(individual, 1.0);
        
        constraintsValid = true;
        for (double var : mutated.getDecisionVariables()) {
            if (var < 0.5 || var > 4.0) {
                constraintsValid = false;
                break;
            }
        }
        assertTest("Polynomial mutation respects constraints", constraintsValid);

        // Test 3: Constraint repair function
        Individual violatingIndividual = new Individual(1);
        violatingIndividual.getDecisionVariables()[0] = -1.0;  // Below min
        violatingIndividual.getDecisionVariables()[1] = 5.0;   // Above max
        
        Individual repaired = GeneticOperators.repairConstraints(violatingIndividual);
        boolean repairedCorrectly = repaired.getDecisionVariables()[0] == 0.5 && 
                                   repaired.getDecisionVariables()[1] == 4.0;
        assertTest("Constraint repair works correctly", repairedCorrectly);
    }

    /**
     * CRITICAL: Test index calculations (the bugs we fixed!)
     */
    private static void testIndexCalculations() {
        System.out.println("\n--- Testing Index Calculations ---");

        // Test half-tide crossover index calculations
        Individual parent1 = new Individual(3); // 6 variables total
        Individual parent2 = new Individual(3);
        
        // Set distinct values to track swapping
        for (int i = 0; i < 6; i++) {
            parent1.getDecisionVariables()[i] = 1.0 + i * 0.1;
            parent2.getDecisionVariables()[i] = 2.0 + i * 0.1;
        }

        // Force crossover to happen by running multiple times
        boolean crossoverWorked = false;
        for (int attempt = 0; attempt < 100; attempt++) {
            Individual[] result = GeneticOperators.halfTideCrossover(parent1, parent2, 1.0);
            
            // Check if any half-tide pairs were swapped correctly
            for (int halfTide = 0; halfTide < 3; halfTide++) {
                int hsIndex = halfTide * 2;
                int heIndex = hsIndex + 1;
                
                // If crossover happened, check that pairs stayed together
                if (Math.abs(result[0].getDecisionVariables()[hsIndex] - parent2.getDecisionVariables()[hsIndex]) < TOLERANCE) {
                    // Hs was swapped, so He should also be swapped
                    if (Math.abs(result[0].getDecisionVariables()[heIndex] - parent2.getDecisionVariables()[heIndex]) < TOLERANCE) {
                        crossoverWorked = true;
                        break;
                    }
                }
            }
            if (crossoverWorked) break;
        }
        assertTest("Half-tide crossover maintains Hs/He pairs", crossoverWorked);

        // Test operational mutation index calculations
        Individual testIndividual = new Individual(2); // 4 variables
        testIndividual.getDecisionVariables()[0] = 1.5; // Hs_0
        testIndividual.getDecisionVariables()[1] = 2.5; // He_0
        testIndividual.getDecisionVariables()[2] = 1.8; // Hs_1
        testIndividual.getDecisionVariables()[3] = 2.8; // He_1

        Individual mutatedResult = GeneticOperators.operationalMutation(testIndividual, 1.0);
        
        // Check that mutation didn't cause array index errors (would throw exception)
        boolean mutationWorked = mutatedResult != null;
        assertTest("Operational mutation completes without index errors", mutationWorked);
    }

    /**
     * Test input validation
     */
    private static void testInputValidation() {
        System.out.println("\n--- Testing Input Validation ---");

        Individual validParent = new Individual(1);
        
        // Test null parent validation
        boolean nullExceptionThrown = false;
        try {
            GeneticOperators.simulatedBinaryCrossover(null, validParent, 0.9);
        } catch (IllegalArgumentException e) {
            nullExceptionThrown = true;
        }
        assertTest("Null parent throws exception", nullExceptionThrown);

        // Test invalid probability validation
        boolean probExceptionThrown = false;
        try {
            GeneticOperators.simulatedBinaryCrossover(validParent, validParent, 1.5);
        } catch (IllegalArgumentException e) {
            probExceptionThrown = true;
        }
        assertTest("Invalid probability throws exception", probExceptionThrown);

        // Test empty parent list validation
        boolean emptyListExceptionThrown = false;
        try {
            GeneticOperators.createOffspring(new ArrayList<>(), 0.9, 0.1, "SBX", "POLYNOMIAL");
        } catch (IllegalArgumentException e) {
            emptyListExceptionThrown = true;
        }
        assertTest("Empty parent list throws exception", emptyListExceptionThrown);
    }

    /**
     * Test basic functionality
     */
    private static void testBasicFunctionality() {
        System.out.println("\n--- Testing Basic Functionality ---");

        // Test that crossover produces offspring
        Individual parent1 = new Individual(2);
        Individual parent2 = new Individual(2);
        
        Individual[] offspring = GeneticOperators.simulatedBinaryCrossover(parent1, parent2, 1.0);
        assertTest("SBX produces two offspring", offspring.length == 2);
        assertTest("Offspring are not null", offspring[0] != null && offspring[1] != null);

        // Test that mutation changes values (run multiple times to ensure it happens)
        Individual original = new Individual(1);
        original.getDecisionVariables()[0] = 2.0;
        original.getDecisionVariables()[1] = 2.0;
        
        boolean mutationOccurred = false;
        for (int i = 0; i < 100; i++) {
            Individual mutated = GeneticOperators.polynomialMutation(original, 1.0);
            if (!Arrays.equals(original.getDecisionVariables(), mutated.getDecisionVariables())) {
                mutationOccurred = true;
                break;
            }
        }
        assertTest("Polynomial mutation changes values", mutationOccurred);

        // Test createOffspringNSGAII
        List<Individual> parents = Arrays.asList(parent1, parent2);
        List<Individual> offspringList = GeneticOperators.createOffspringNSGAII(parents, 0.9, 0.1);
        assertTest("NSGA-II offspring creation works", offspringList.size() == 2);
    }

    /**
     * Test thread safety
     */
    private static void testThreadSafety() {
        System.out.println("\n--- Testing Thread Safety ---");

        // Simple test: run operations concurrently and check for exceptions
        List<Thread> threads = new ArrayList<>();
        final boolean[] success = {true};

        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                try {
                    Individual p1 = new Individual(1);
                    Individual p2 = new Individual(1);
                    GeneticOperators.simulatedBinaryCrossover(p1, p2, 0.9);
                    GeneticOperators.polynomialMutation(p1, 0.1);
                } catch (Exception e) {
                    success[0] = false;
                }
            }));
        }

        threads.forEach(Thread::start);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                success[0] = false;
            }
        });

        assertTest("Concurrent operations complete successfully", success[0]);
    }

    /**
     * Test statistics functionality
     */
    private static void testStatistics() {
        System.out.println("\n--- Testing Statistics ---");

        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Individual ind = new Individual(1);
            ind.getDecisionVariables()[0] = 1.0 + i;
            ind.getDecisionVariables()[1] = 2.0 + i;
            population.add(ind);
        }

        GeneticOperators.OperatorStats stats = GeneticOperators.getOperatorStatistics(population);
        assertTest("Statistics calculation works", stats != null);
        assertTest("Population size correct", stats.populationSize == 5);
        assertTest("Mean calculation reasonable", stats.meanValue > 0);

        // Test empty population
        GeneticOperators.OperatorStats emptyStats = GeneticOperators.getOperatorStatistics(new ArrayList<>());
        assertTest("Empty population statistics handled", emptyStats.populationSize == 0);
    }

    /**
     * Helper method for test assertions
     */
    private static void assertTest(String testName, boolean condition) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("Passed " + testName);
        } else {
            System.out.println("Failed " + testName);
        }
    }
}

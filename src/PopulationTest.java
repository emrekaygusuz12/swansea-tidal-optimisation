package src;

import src.optimisation.Population;
import src.optimisation.Individual;
import src.optimisation.IndividualGenerator;
import src.optimisation.ObjectiveFunction;
import src.model.SimulationConfig;
import src.utils.TideDataReader;

import java.util.List;
import java.io.IOException;


/**
 * Test class for Population.java.
 * 
 * Tests all major functionality including edge cases, statistics calculation,
 * and integration with tidal lagoon simulation.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class PopulationTest {

    private static List<Double> tideData; // shared test data
    public static void main(String[] args){
        System.out.println("===Population Class Testing===");

        try {
            // Load tide data for testing
            tideData = loadTestData();
            // Run all the tests
            testBasicFunctionality();
            testStatisticsCalculation();
            testEdgeCases();
            testPopulationOperations();
            testPerformance();

            System.out.println("\n All tests completed successfully! Population.java is ready for NSGA-II!");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }   
    }

    /**
     * Loads tidal data for testing.
     */
    private static List<Double> loadTestData() throws IOException {
        System.out.println("\n--- Loading Test Data ---");
        
        List<Double> tideHeights = TideDataReader.readTideHeights("data/b1111463.txt");
        SimulationConfig.SimulationParameters config = SimulationConfig.getTestConfiguration();
        List<Double> testData = tideHeights.subList(0, Math.min(config.getReadingsNeeded(), tideHeights.size()));

        System.out.printf("Loaded %d tide height readings for testing.%n", testData.size());
        return testData;
    }

    /*
     * Tests basic Population functionality.
     */
    private static void testBasicFunctionality() {
        System.out.println("\n --- Testing Basic Functionality ---");

        // Test 1: Constructor and Basic operations
        Population population = new Population(10);
        assert population.isEmpty() : "New population should be empty";
        assert population.size() == 0 : "Population size should be 0 after initialization";
        assert population.getMaxSize() == 10 : "Max size should be set to 10";
        System.out.println("Constructor and basic getters work as expected.");

        // Test 2: Adding individuals
        Individual individual1 = IndividualGenerator.createRandomIndividual(4);
        Individual individual2 = IndividualGenerator.createRandomIndividual(4);

        population.addIndividual(individual1);
        population.addIndividual(individual2);

        assert population.size() == 2 : "Population size should be 2 after adding two individuals";
        assert !population.isEmpty() : "Population should not be empty after adding individuals";
        assert population.getIndividual(0) == individual1 : "First individual should match the one added first";
        assert population.getIndividual(1) == individual2 : "Second individual should match the one added second";
        System.out.println("Adding individuals working correctly.");

        // Test 3: Removing individuals
        boolean removed = population.removeIndividual(individual1);
        assert removed : "Should successfully remove individual1";
        assert population.size() == 1 : "Population size should be 1 after removing one individual";
        assert population.getIndividual(0) == individual2 : "Remaining individual should be individual2";
        System.out.println("Removing individuals working correctly.");

        // Test 4: Random initialisation
        population.clear();
        population.initialiseRandom(5, 4);
        assert population.size() == 5 : "Should have 5 random individuals";

        // Verify all individuals are different
        boolean allDifferent = true;
        for (int i = 0; i < population.size(); i++) {
            for (int j = i + 1; j < population.size(); j++) {
                Individual a = population.getIndividual(i);
                Individual b = population.getIndividual(j);
                if (java.util.Arrays.equals(a.getDecisionVariables(), b.getDecisionVariables())) {
                    allDifferent = false;
                    break;
                }
            }
        }
        System.out.printf("Random initialisation working (diversity check: %s)%n", allDifferent
                ? "PASSED" : "Note: Some individuals identical by chance");

    }

    /**
     * Tests statistics calculation with real simulation data.
     */
    private static void testStatisticsCalculation() {
        System.out.println("\n --- Testing Statistics Calculation ---");

        // Create population with evaluated individuals
        Population population = new Population(10);
        population.initialiseRandom(10, 48);

        // Evaluate individuals
        for (Individual individual : population.getIndividuals()) {
            ObjectiveFunction.evaluate(tideData, individual);
        }

        // Test statistics
        Population.PopulationStats stats = population.getStatistics();

        assert stats.size == 10 : "Stats size should match population size";
        assert stats.minEnergy >= 0 : "Min energy should be non-negative";
        assert stats.maxEnergy >= stats.minEnergy : "Max energy should be greater than or equal to min energy";
        assert stats.avgEnergy >= stats.minEnergy : "Avg energy should be greater than or equal to min energy";
        assert stats.avgEnergy <= stats.maxEnergy : "Avg energy should be less than or equal to max energy";

        System.out.printf("Energy statistics: min=%.1f, max=%.1f, avg=%.1f%n", stats.minEnergy,
            stats.maxEnergy, stats.avgEnergy);

        // Cost statistics (should be valid since we evaluated individuals)
        if (stats.avgCost != Double.MAX_VALUE) {
            assert stats.minCost > 0 : "Min cost should be positive";
            assert stats.maxCost >= stats.minCost : "Max cost should be greater than or equal to min cost";
            System.out.printf("Cost statistics: min=%.0f, max=%.0f, avg=%.0f%n", stats.minCost,
                stats.maxCost, stats.avgCost);
        }

        System.out.println("Statistics calculation working correctly.");

    }

    /*
     * Tests edge cases and error conditions.
     */
    private static void testEdgeCases(){
        System.out.println("\n--- Testing Edge Cases---");

        // Test 1: Empty population statistics
        Population emptyPopulation = new Population(5);
        Population.PopulationStats emptyStats = emptyPopulation.getStatistics();
        assert emptyStats.size == 0 : "Empty population stats size should be 0";
        System.out.println("Empty population statistics handled correctly.");

        // Test 2: Population at max capacity
        Population maxPopulation = new Population(2);
        maxPopulation.addIndividual(IndividualGenerator.createRandomIndividual(4));
        maxPopulation.addIndividual(IndividualGenerator.createRandomIndividual(4));

        try {
            maxPopulation.addIndividual(IndividualGenerator.createRandomIndividual(4));
            assert false : "Should not be able to add more individuals than max size";
        } catch (IllegalStateException e) {
            System.out.println("Correctly caught exception when exceeding max population size.");
        }

        // Test 3: Index out of bounds
        try {
            maxPopulation.getIndividual(5);
            assert false : "Should throw exception for invalid index";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Correctly caught exception for invalid index.");
        }

        // Test 4: Population with all invalid costs
        Population invalidCostPopulation = new Population(3);
        for (int i = 0; i < 3; i++) {
            Individual ind = IndividualGenerator.createRandomIndividual(4);
            ind.setEnergyOutput(100 * (i + 1)); // Set valid energy
            ind.setUnitCost(Double.MAX_VALUE); // Set invalid cost
            invalidCostPopulation.addIndividual(ind);
        }

        Population.PopulationStats invalidStats = invalidCostPopulation.getStatistics();
        assert invalidStats.avgCost == Double.MAX_VALUE : "Should handle invalid costs correctly";
        assert invalidStats.minCost == Double.MAX_VALUE : "Min cost should be Double.MAX_VALUE for invalid costs";
        assert invalidStats.maxCost == Double.MAX_VALUE : "Max cost should be Double.MAX_VALUE for invalid costs";
        System.out.println("Population with invalid costs handled correctly.");
    }

    /**
     * Tests population operations like combining and selecting best individuals.
     */
    private static void testPopulationOperations() {
        System.out.println("\n--- Testing Population Operations ---");

        // Create and evaluate two populations
        Population population1 = new Population(5);
        Population population2 = new Population(5);

        population1.initialiseRandom(3, 48);
        population2.initialiseRandom(4, 48);

        // Evaluate individuals
        for (Individual individual : population1.getIndividuals()) {
            ObjectiveFunction.evaluate(tideData, individual);
        }
        for (Individual individual : population2.getIndividuals()) {
            ObjectiveFunction.evaluate(tideData, individual);
        }

        // Test combining populations
        Population combinedPopulation = population1.combine(population2);
        assert combinedPopulation.size() == 7 : "Combined population should have 7 individuals";
        assert combinedPopulation.getMaxSize() == 7 : "Combined population max size should be 7";
        System.out.println("Population combining works correctly.");

        // Test getting best individuals by energy
        List<Individual> bestEnergy = combinedPopulation.getBestByEnergy(3);
        assert bestEnergy.size() == 3 : "Should return 3 best individuals by energy";

        // Verify the are sorted by energy (descending order)
        for (int i = 0; i < bestEnergy.size() - 1; i++) {
            assert bestEnergy.get(i).getEnergyOutput() >= bestEnergy.get(i + 1).getEnergyOutput() :
                "Best energy list should be sorted in descending order";
        }
        System.out.printf("Best by energy: %.1f >= %.1f >= %.1f MWh%n",
            bestEnergy.get(0).getEnergyOutput(),
            bestEnergy.get(1).getEnergyOutput(),
            bestEnergy.get(2).getEnergyOutput());

        // Test getting best individuals by cost
        List<Individual> bestCost = combinedPopulation.getBestByCost(3);
        assert bestCost.size() <= 3 : "Should return at most 3 best cost individuals";

        if(!bestCost.isEmpty()) {
            // Verify they are sorted by cost (ascending order - lower cost is better)
            for (int i = 0; i < bestCost.size() - 1; i++) {
                assert bestCost.get(i).getUnitCost() <= bestCost.get(i + 1).getUnitCost() :
                    "Best cost list should be sorted in ascending order";
            }
            System.out.printf("Best by cost: %.0f <= %.0f <= %.0f \u00A3/MWh%n",
                bestCost.get(0).getUnitCost(),
                bestCost.size() > 1 ? bestCost.get(1).getUnitCost() : 0,
                bestCost.size() > 2 ? bestCost.get(2).getUnitCost() : 0);
        }

        // Test population copying 
        Population copy = combinedPopulation.copy();
        assert copy.size() == combinedPopulation.size() : "Copied population should have same size";
        assert copy.getMaxSize() == combinedPopulation.getMaxSize() : "Copied population should have same max size";

        // Verify it's a deep copy (individuals are cloned)
        Individual original = combinedPopulation.getIndividual(0);
        Individual copied = copy.getIndividual(0);
        assert original != copied : "Individuals should be different objects";
        assert java.util.Arrays.equals(original.getDecisionVariables(), copied.getDecisionVariables()) :
            "But decision variables should be equal";
        System.out.println("Population operations (combine, best selection, copy) work correctly.");
    }


    /**
     * Tests performance with larger population.
     */
    private static void testPerformance() {
        System.out.println("\n--- Testing Performance ---");

        long startTime = System.currentTimeMillis();

        // Create a large population
        Population largePopulation = new Population(1000);
        largePopulation.initialiseRandom(1000, 48);

        // Test statistics calculation performance
        Population.PopulationStats stats = largePopulation.getStatistics();

        // Test sorting performance
        largePopulation.sort((a, b) -> Double.compare(a.getDecisionVariables()[0], b.getDecisionVariables()[0]));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("Performance test: 1000 individuals processed in %d ms%n", duration);
        assert duration < 5000 : "Performance test failed - should complete in under 5 seconds";
    }
    
}

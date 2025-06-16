package src;

import src.optimisation.ParetoDominance;
import src.optimisation.Individual;
import src.optimisation.Population;
import src.optimisation.IndividualGenerator;

import java.util.List;

/**
 * Test class to verify Pareto dominance functionality.
 * 
 * This class will contain unit tests to verify the correctness of the ParetoDominance methods.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class ParetoDominanceTest {
    public static void main(String[] args){
        System.out.println("=== Pareto Dominance Test ===");

        try {
            testDominanceLogic();
            testFastNonDominatedSort();
            testParetoFrontIdentification();

            System.out.println("All tests passed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void testDominanceLogic() {
        System.out.println("--- Testing dominance logic ---");

        // Create test individuals with known energy and cost values
        Individual a = IndividualGenerator.createRandomIndividual(4);
        Individual b = IndividualGenerator.createRandomIndividual(4);
        Individual c = IndividualGenerator.createRandomIndividual(4);
        Individual d = IndividualGenerator.createRandomIndividual(4);

        // Test case 1: A dominates B (higher energy, lower cost)
        a.setEnergyOutput(1000); a.setUnitCost(100);
        b.setEnergyOutput(800); b.setUnitCost(150);

        assert ParetoDominance.dominates(a, b) : "A should dominate B (higher energy, lower cost)";
        assert !ParetoDominance.dominates(b, a) : "B should not dominate A";
        System.out.println("Test 1: A(1000, 100) dominates B(800, 150) - PASSED");

        // Test case 2: A dominates C (Higher energy, equal cost)
        c.setEnergyOutput(800); c.setUnitCost(100);

        assert ParetoDominance.dominates(a, c) : "A should dominate C (higher energy, equal cost)";
        assert !ParetoDominance.dominates(c, a) : "C should not dominate A";
        System.out.println("Test 2: A(1000, 100) dominates C(800, 100) - PASSED");

        // Test case 3: Non-dominated solutions
        d.setEnergyOutput(1200); d.setUnitCost(200);

        assert !ParetoDominance.dominates(a, d) : "A should not dominate D (trade-off case)";
        assert !ParetoDominance.dominates(d, a) : "D should not dominate A (trade-off case)";
        System.out.println("Test 3: A(1000, 100) does not dominate D(1200, 200) are non-dominated - PASSED");

        // Test case 4: Invalid cost handling (no energy generated)
        Individual e = IndividualGenerator.createRandomIndividual(4);
        a.setEnergyOutput(500); e.setUnitCost(Double.MAX_VALUE); 

        assert ParetoDominance.dominates(a, e) : "A should dominate E (valid cost vs invalid cost)";
        assert !ParetoDominance.dominates(e, a) : "E should not dominate A (invalid cost vs valid cost)";
        System.out.println("Test 4: Invalid cost handling - PASSED");
    }

    private static void testFastNonDominatedSort() {
        System.out.println("--- Testing fast non-dominated sort ---");

        // Create population with known dominance relationships
        Population population = new Population(6);

        // Front 0 (Pareto front): A and B (non-dominated)
        Individual a = IndividualGenerator.createRandomIndividual(4);
        Individual b = IndividualGenerator.createRandomIndividual(4);
        a.setEnergyOutput(1000); a.setUnitCost(100); // High energy, low cost
        b.setEnergyOutput(800); b.setUnitCost(50); // Lower energy, very lower cost

        // Front 1: C and D (dominated by Front 0)
        Individual c = IndividualGenerator.createRandomIndividual(4);
        Individual d = IndividualGenerator.createRandomIndividual(4);
        c.setEnergyOutput(900); c.setUnitCost(150); // Dominated by A
        d.setEnergyOutput(700); d.setUnitCost(80); // Dominated by both A and B

        // Front 2: E (dominated by Front 1)
        Individual e = IndividualGenerator.createRandomIndividual(4);
        e.setEnergyOutput(600); e.setUnitCost(200); // Dominated by C and D

        // Add to population
        population.addIndividual(a);
        population.addIndividual(b);
        population.addIndividual(c);
        population.addIndividual(d);
        population.addIndividual(e);

        // Perform fast non-dominated sorting
        List<List<Individual>> fronts = ParetoDominance.fastNonDominatedSort(population);

        // Verify front structure
        assert fronts.size() >= 2 : "Should have at least 2 fronts";
        assert fronts.get(0).size() == 2 : "Front 0 should have 2 individuals (A and B)";

        // Verify ranks are assigned correctly
        assert a.getRank() == 0 : "A should be in rank 0";
        assert b.getRank() == 0 : "B should be in rank 0";
        assert c.getRank() > 0 : "C should be in rank > 0 (dominated by A)";
        assert d.getRank() > 0 : "D should be in rank > 0 (dominated by A and B)";
        assert e.getRank() > 1 : "E should be in rank > 1 (dominated by C and D)";

        System.out.printf("Fast non-dominated sorting: %d fronts created%n", fronts.size());
        System.out.printf("  Front 0: %d individuals (A,B)%n", fronts.get(0).size());
        System.out.printf("  Front 1: %d individuals (C,D)%n", fronts.size() > 1 ? fronts.get(1).size() : 0);
        System.out.printf("  Front 2: %d individuals (E)%n", fronts.size() > 2 ? fronts.get(2).size() : 0);
    }

    /**
     * Tests Pareto front identification.
     */
    private static void testParetoFrontIdentification() {
        System.out.println("\n--- Testing Pareto Front Identification ---");
        
        // Create population with clear Pareto front
        Population population = new Population(4);
        
        // Pareto front solutions
        Individual p1 = IndividualGenerator.createRandomIndividual(4);
        Individual p2 = IndividualGenerator.createRandomIndividual(4);
        p1.setEnergyOutput(1000); p1.setUnitCost(100);
        p2.setEnergyOutput(800);  p2.setUnitCost(50);
        
        // Dominated solutions
        Individual d1 = IndividualGenerator.createRandomIndividual(4);
        Individual d2 = IndividualGenerator.createRandomIndividual(4);
        d1.setEnergyOutput(900); d1.setUnitCost(150);
        d2.setEnergyOutput(700); d2.setUnitCost(120);
        
        population.addIndividual(p1);
        population.addIndividual(p2);
        population.addIndividual(d1);
        population.addIndividual(d2);
        
        // Get Pareto front
        List<Individual> paretoFront = ParetoDominance.getParetoFront(population);
        
        assert paretoFront.size() == 2 : "Pareto front should have 2 solutions";
        assert paretoFront.contains(p1) : "Pareto front should contain p1";
        assert paretoFront.contains(p2) : "Pareto front should contain p2";
        assert !paretoFront.contains(d1) : "Pareto front should not contain d1";
        assert !paretoFront.contains(d2) : "Pareto front should not contain d2";
        
        System.out.printf("✓ Pareto front identification: %d solutions correctly identified%n", paretoFront.size());
        
        // Test dominance statistics
        ParetoDominance.DominanceStats stats = ParetoDominance.getDominanceStatistics(population);
        System.out.printf("✓ Dominance stats: %s%n", stats);
        
        assert stats.paretoFrontSize == 2 : "Stats should report Pareto front size of 2";
        assert stats.frontCount >= 2 : "Stats should report at least 2 fronts";
    }
    
}

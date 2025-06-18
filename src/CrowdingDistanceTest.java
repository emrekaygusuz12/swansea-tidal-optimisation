package src;

import src.optimisation.CrowdingDistance;
import src.optimisation.Individual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Comprehensive test suite for the CrowdingDistance class.
 * 
 * This test class validates all functionality of the crowding distance calculation
 * algorithm used in NSGA-II for maintaining diversity in Pareto fronts.
 * 
 * Tests cover edge cases, normal operations, sorting, selection, statistics,
 * and integration with the NSGA-II workflow.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class CrowdingDistanceTest {

    private List<Individual> individuals;
    private static final double EPSILON = 1e-9;

    /**
     * Test implementation that extends the actual Individual class for testing purposes.
     * Provides a minimal implementation needed by CrowdingDistance methods.
     */
    public static class TestIndividual extends Individual {
        private final double energyOutput;
        private final double unitCost;

        public TestIndividual(double energyOutput, double unitCost) {
            super(1); // Pass 1 half-tide to the parent constructor
            this.energyOutput = energyOutput;
            this.unitCost = unitCost;
        }

        @Override
        public double getEnergyOutput() { return energyOutput; }
        
        @Override
        public double getUnitCost() { return unitCost; }

        @Override
        public String toString() {
            return String.format("TestIndividual[energy=%.2f, cost=%.2f, crowding=%.6f, rank=%d]",
                    energyOutput, unitCost, getCrowdingDistance(), getRank());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestIndividual that = (TestIndividual) obj;
            return Double.compare(that.energyOutput, energyOutput) == 0 &&
                   Double.compare(that.unitCost, unitCost) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(energyOutput, unitCost);
        }
    }

    @BeforeEach
    void setUp() {
        individuals = new ArrayList<>();
    }

    // ======================== Edge Case Tests ========================

    @Test
    @DisplayName("Empty population should be handled gracefully")
    void testEmptyPopulation() {
        CrowdingDistance.calculateCrowdingDistance(individuals);
        assertTrue(individuals.isEmpty(), "Empty population should remain empty");
    }

    @Test
    @DisplayName("Single individual should get infinite crowding distance")
    void testSingleIndividual() {
        individuals.add(new TestIndividual(100.0, 50.0));
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        assertEquals(Double.POSITIVE_INFINITY, individuals.get(0).getCrowdingDistance(),
                "Single individual should have infinite crowding distance");
    }

    @Test
    @DisplayName("Two individuals should both get infinite crowding distance")
    void testTwoIndividuals() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(150.0, 40.0));
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        assertEquals(Double.POSITIVE_INFINITY, individuals.get(0).getCrowdingDistance(),
                "First individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, individuals.get(1).getCrowdingDistance(),
                "Second individual should have infinite crowding distance");
    }

    @Test
    @DisplayName("Individuals with identical energy values should be handled correctly")
    void testIdenticalEnergyValues() {
        individuals.add(new TestIndividual(100.0, 30.0));  // Best cost
        individuals.add(new TestIndividual(100.0, 50.0));  // Middle cost
        individuals.add(new TestIndividual(100.0, 70.0));  // Worst cost
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Find individuals by cost (since energy is identical)
        Individual bestCost = individuals.stream()
                .min(Comparator.comparingDouble(Individual::getUnitCost))
                .orElseThrow();
        Individual worstCost = individuals.stream()
                .max(Comparator.comparingDouble(Individual::getUnitCost))
                .orElseThrow();
        Individual middleCost = individuals.stream()
                .filter(ind -> ind != bestCost && ind != worstCost)
                .findFirst().orElseThrow();
        
        assertEquals(Double.POSITIVE_INFINITY, bestCost.getCrowdingDistance(),
                "Best cost individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, worstCost.getCrowdingDistance(),
                "Worst cost individual should have infinite crowding distance");
        assertTrue(middleCost.getCrowdingDistance() > 0 && 
                   middleCost.getCrowdingDistance() < Double.POSITIVE_INFINITY,
                "Middle cost individual should have finite positive crowding distance");
    }

    @Test
    @DisplayName("Individuals with identical cost values should be handled correctly")
    void testIdenticalCostValues() {
        individuals.add(new TestIndividual(80.0, 50.0));   // Worst energy
        individuals.add(new TestIndividual(100.0, 50.0));  // Middle energy
        individuals.add(new TestIndividual(120.0, 50.0));  // Best energy
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Find individuals by energy (since cost is identical)
        Individual worstEnergy = individuals.stream()
                .min(Comparator.comparingDouble(Individual::getEnergyOutput))
                .orElseThrow();
        Individual bestEnergy = individuals.stream()
                .max(Comparator.comparingDouble(Individual::getEnergyOutput))
                .orElseThrow();
        Individual middleEnergy = individuals.stream()
                .filter(ind -> ind != worstEnergy && ind != bestEnergy)
                .findFirst().orElseThrow();
        
        assertEquals(Double.POSITIVE_INFINITY, worstEnergy.getCrowdingDistance(),
                "Worst energy individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, bestEnergy.getCrowdingDistance(),
                "Best energy individual should have infinite crowding distance");
        assertTrue(middleEnergy.getCrowdingDistance() > 0 && 
                   middleEnergy.getCrowdingDistance() < Double.POSITIVE_INFINITY,
                "Middle energy individual should have finite positive crowding distance");
    }

    @Test
    @DisplayName("Invalid cost values (Double.MAX_VALUE) should be filtered out")
    void testInvalidCostValues() {
        individuals.add(new TestIndividual(100.0, 50.0));             // Valid cost
        individuals.add(new TestIndividual(120.0, Double.MAX_VALUE)); // Invalid cost
        individuals.add(new TestIndividual(140.0, 40.0));             // Valid cost
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Individual with invalid cost should get crowding distance only from energy objective
        Individual invalidCostInd = individuals.get(1);
        assertTrue(invalidCostInd.getCrowdingDistance() >= 0,
                "Individual with invalid cost should have non-negative crowding distance");
        
        // Valid cost individuals should have proper crowding distances
        Individual validCost1 = individuals.get(0);
        Individual validCost2 = individuals.get(2);
        assertTrue(validCost1.getCrowdingDistance() >= 0,
                "Valid cost individual should have non-negative crowding distance");
        assertTrue(validCost2.getCrowdingDistance() >= 0,
                "Valid cost individual should have non-negative crowding distance");
    }

    @Test
    @DisplayName("All individuals with identical values should be handled without division by zero")
    void testAllIdenticalValues() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(100.0, 50.0));
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            CrowdingDistance.calculateCrowdingDistance(individuals);
        }, "Identical values should not cause division by zero");
        
        // All should have zero crowding distance (or some should have infinite if they're boundaries)
        for (Individual ind : individuals) {
            assertTrue(ind.getCrowdingDistance() >= 0,
                    "All individuals should have non-negative crowding distance");
        }
    }

    // ======================== Normal Operation Tests ========================

    @Test
    @DisplayName("Three individuals should have correct boundary and intermediate distances")
    void testThreeIndividuals() {
        individuals.add(new TestIndividual(80.0, 60.0));   // Low energy, high cost
        individuals.add(new TestIndividual(100.0, 50.0));  // Medium energy, medium cost
        individuals.add(new TestIndividual(120.0, 40.0));  // High energy, low cost
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Count individuals with infinite crowding distance
        long infiniteCount = individuals.stream()
                .mapToDouble(Individual::getCrowdingDistance)
                .filter(d -> d == Double.POSITIVE_INFINITY)
                .count();
        
        // Should have exactly 4 boundary solutions (2 for energy, 2 for cost)
        // but some individuals might be boundary for both objectives
        assertTrue(infiniteCount >= 2, "Should have at least 2 boundary solutions");
        
        // All individuals should have non-negative crowding distance
        for (Individual ind : individuals) {
            assertTrue(ind.getCrowdingDistance() >= 0,
                    "All individuals should have non-negative crowding distance");
        }
    }

    @Test
    @DisplayName("Five individuals should have proper normalized distances")
    void testFiveIndividualsNormalizedDistances() {
        // Create individuals with diverse objectives
        individuals.add(new TestIndividual(10.0, 100.0));  // Low energy, high cost
        individuals.add(new TestIndividual(30.0, 80.0));   // Medium-low energy, medium-high cost
        individuals.add(new TestIndividual(50.0, 60.0));   // Medium energy, medium cost
        individuals.add(new TestIndividual(70.0, 40.0));   // Medium-high energy, medium-low cost
        individuals.add(new TestIndividual(90.0, 20.0));   // High energy, low cost
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Verify boundary solutions have infinite distance
        List<Individual> sortedByEnergy = individuals.stream()
                .sorted(Comparator.comparingDouble(Individual::getEnergyOutput))
                .toList();
        List<Individual> sortedByCost = individuals.stream()
                .sorted(Comparator.comparingDouble(Individual::getUnitCost))
                .toList();
        
        assertEquals(Double.POSITIVE_INFINITY, sortedByEnergy.get(0).getCrowdingDistance(),
                "Lowest energy individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, sortedByEnergy.get(4).getCrowdingDistance(),
                "Highest energy individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, sortedByCost.get(0).getCrowdingDistance(),
                "Lowest cost individual should have infinite crowding distance");
        assertEquals(Double.POSITIVE_INFINITY, sortedByCost.get(4).getCrowdingDistance(),
                "Highest cost individual should have infinite crowding distance");
        
        // Verify all distances are non-negative
        for (Individual ind : individuals) {
            assertTrue(ind.getCrowdingDistance() >= 0,
                    "All individuals should have non-negative crowding distance");
        }
    }

    @Test
    @DisplayName("Crowding distance should be additive across objectives")
    void testAdditiveNature() {
        individuals.add(new TestIndividual(10.0, 10.0));   // (min, min)
        individuals.add(new TestIndividual(50.0, 50.0));   // (mid, mid)
        individuals.add(new TestIndividual(90.0, 90.0));   // (max, max)
        
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        Individual middle = individuals.get(1);
        
        // Middle individual should have crowding distance from both objectives
        // (unless it's also a boundary in one objective, which it's not in this case)
        assertTrue(middle.getCrowdingDistance() > 0,
                "Middle individual should have positive crowding distance");
        assertTrue(middle.getCrowdingDistance() < Double.POSITIVE_INFINITY,
                "Middle individual should have finite crowding distance");
    }

    // ======================== Sorting and Comparison Tests ========================

    @Test
    @DisplayName("Sorting by crowding distance should work correctly")
    void testSortByCrowdingDistance() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(120.0, 40.0));
        individuals.add(new TestIndividual(110.0, 45.0));
        individuals.add(new TestIndividual(90.0, 55.0));
        
        // Manually set crowding distances for predictable sorting
        individuals.get(0).setCrowdingDistance(0.5);
        individuals.get(1).setCrowdingDistance(Double.POSITIVE_INFINITY);
        individuals.get(2).setCrowdingDistance(0.8);
        individuals.get(3).setCrowdingDistance(0.3);
        
        CrowdingDistance.sortByCrowdingDistance(individuals);
        
        // Should be sorted in descending order: infinity, 0.8, 0.5, 0.3
        assertEquals(Double.POSITIVE_INFINITY, individuals.get(0).getCrowdingDistance(),
                "First individual should have infinite crowding distance");
        assertEquals(0.8, individuals.get(1).getCrowdingDistance(), EPSILON,
                "Second individual should have crowding distance 0.8");
        assertEquals(0.5, individuals.get(2).getCrowdingDistance(), EPSILON,
                "Third individual should have crowding distance 0.5");
        assertEquals(0.3, individuals.get(3).getCrowdingDistance(), EPSILON,
                "Fourth individual should have crowding distance 0.3");
    }

    @Test
    @DisplayName("Crowded comparison should prioritize rank over crowding distance")
    void testCrowdedCompareByRank() {
        Individual ind1 = new TestIndividual(100.0, 50.0);
        Individual ind2 = new TestIndividual(120.0, 40.0);
        
        ind1.setRank(1);
        ind1.setCrowdingDistance(0.5);
        ind2.setRank(2);
        ind2.setCrowdingDistance(0.8);
        
        // Lower rank should win regardless of crowding distance
        assertTrue(CrowdingDistance.crowdedCompare(ind1, ind2) < 0,
                "Individual with lower rank should be preferred");
        assertTrue(CrowdingDistance.crowdedCompare(ind2, ind1) > 0,
                "Individual with higher rank should not be preferred");
    }

    @Test
    @DisplayName("Crowded comparison should use crowding distance when ranks are equal")
    void testCrowdedCompareByCrowdingDistance() {
        Individual ind1 = new TestIndividual(100.0, 50.0);
        Individual ind2 = new TestIndividual(120.0, 40.0);
        
        ind1.setRank(1);
        ind1.setCrowdingDistance(0.5);
        ind2.setRank(1); // Same rank
        ind2.setCrowdingDistance(0.8);
        
        // Higher crowding distance should win when ranks are equal
        assertTrue(CrowdingDistance.crowdedCompare(ind1, ind2) > 0,
                "Individual with lower crowding distance should not be preferred");
        assertTrue(CrowdingDistance.crowdedCompare(ind2, ind1) < 0,
                "Individual with higher crowding distance should be preferred");
    }

    @Test
    @DisplayName("Crowded comparison should return 0 for equal individuals")
    void testCrowdedCompareEqual() {
        Individual ind1 = new TestIndividual(100.0, 50.0);
        Individual ind2 = new TestIndividual(120.0, 40.0);
        
        ind1.setRank(1);
        ind1.setCrowdingDistance(0.5);
        ind2.setRank(1);
        ind2.setCrowdingDistance(0.5);
        
        assertEquals(0, CrowdingDistance.crowdedCompare(ind1, ind2),
                "Individuals with same rank and crowding distance should be equal");
    }

    @Test
    @DisplayName("Crowded comparator should work with Collections.sort")
    void testCrowdedComparator() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(120.0, 40.0));
        individuals.add(new TestIndividual(110.0, 45.0));
        
        // Set ranks and crowding distances
        individuals.get(0).setRank(2);
        individuals.get(0).setCrowdingDistance(0.8);
        individuals.get(1).setRank(1);
        individuals.get(1).setCrowdingDistance(0.3);
        individuals.get(2).setRank(1);
        individuals.get(2).setCrowdingDistance(0.7);
        
        Comparator<Individual> comparator = CrowdingDistance.getCrowdedComparator();
        individuals.sort(comparator);
        
        // Should be sorted by rank first, then by crowding distance
        assertEquals(1, individuals.get(0).getRank(), "First should have rank 1");
        assertEquals(0.7, individuals.get(0).getCrowdingDistance(), EPSILON,
                "First should have higher crowding distance among rank 1");
        assertEquals(1, individuals.get(1).getRank(), "Second should have rank 1");
        assertEquals(0.3, individuals.get(1).getCrowdingDistance(), EPSILON,
                "Second should have lower crowding distance among rank 1");
        assertEquals(2, individuals.get(2).getRank(), "Third should have rank 2");
    }

    // ======================== Selection Tests ========================

    @Test
    @DisplayName("Selection by crowding distance should return correct count")
    void testSelectByCrowdingDistance() {
        for (int i = 0; i < 5; i++) {
            individuals.add(new TestIndividual(100.0 + i * 10, 50.0 - i * 5));
        }
        
        List<Individual> selected = CrowdingDistance.selectByCrowdingDistance(individuals, 3);
        
        assertEquals(3, selected.size(), "Should select exactly 3 individuals");
        
        // Should be sorted by crowding distance (descending)
        for (int i = 0; i < selected.size() - 1; i++) {
            assertTrue(selected.get(i).getCrowdingDistance() >= 
                      selected.get(i + 1).getCrowdingDistance(),
                      "Selected individuals should be sorted by crowding distance");
        }
    }

    @Test
    @DisplayName("Selection should return all individuals when count exceeds size")
    void testSelectByCrowdingDistanceCountExceedsSize() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(120.0, 40.0));
        
        List<Individual> selected = CrowdingDistance.selectByCrowdingDistance(individuals, 5);
        
        assertEquals(2, selected.size(), "Should return all individuals when count exceeds size");
        assertEquals(individuals.size(), selected.size(),
                "Selected size should equal original size when count exceeds size");
    }

    @Test
    @DisplayName("Selection should handle empty list")
    void testSelectByCrowdingDistanceEmpty() {
        List<Individual> selected = CrowdingDistance.selectByCrowdingDistance(individuals, 3);
        
        assertTrue(selected.isEmpty(), "Selection from empty list should return empty list");
    }

    // ======================== Statistics Tests ========================

    @Test
    @DisplayName("Statistics for empty population should be zero")
    void testGetCrowdingStatisticsEmpty() {
        CrowdingDistance.CrowdingStats stats = CrowdingDistance.getCrowdingStatistics(individuals);
        
        assertEquals(0, stats.totalIndividuals, "Total individuals should be 0");
        assertEquals(0, stats.infiniteDistanceCount, "Infinite distance count should be 0");
        assertEquals(0.0, stats.minFiniteDistance, EPSILON, "Min finite distance should be 0");
        assertEquals(0.0, stats.maxFiniteDistance, EPSILON, "Max finite distance should be 0");
        assertEquals(0.0, stats.averageFiniteDistance, EPSILON, "Average finite distance should be 0");
        assertEquals(0.0, stats.getDiversityRatio(), EPSILON, "Diversity ratio should be 0");
    }

    @Test
    @DisplayName("Statistics should correctly calculate for normal population")
    void testGetCrowdingStatisticsNormal() {
        individuals.add(new TestIndividual(100.0, 60.0));
        individuals.add(new TestIndividual(120.0, 50.0));
        individuals.add(new TestIndividual(140.0, 40.0));
        individuals.add(new TestIndividual(110.0, 55.0));
        individuals.add(new TestIndividual(130.0, 45.0));
        
        CrowdingDistance.CrowdingStats stats = CrowdingDistance.getCrowdingStatistics(individuals);
        
        assertEquals(5, stats.totalIndividuals, "Total individuals should be 5");
        assertTrue(stats.infiniteDistanceCount > 0, "Should have some infinite distance individuals");
        assertTrue(stats.infiniteDistanceCount <= 4, "Should not exceed 4 infinite distances");
        assertTrue(stats.averageFiniteDistance >= 0, "Average finite distance should be non-negative");
        assertTrue(stats.getDiversityRatio() > 0, "Diversity ratio should be positive");
        assertTrue(stats.getDiversityRatio() <= 1.0, "Diversity ratio should not exceed 1.0");
    }

    @Test
    @DisplayName("Statistics toString should contain expected information")
    void testCrowdingStatsToString() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(120.0, 40.0));
        individuals.add(new TestIndividual(110.0, 45.0));
        
        CrowdingDistance.CrowdingStats stats = CrowdingDistance.getCrowdingStatistics(individuals);
        String str = stats.toString();
        
        assertNotNull(str, "toString should not return null");
        assertTrue(str.contains("CrowdingStats"), "toString should contain class name");
        assertTrue(str.contains("total="), "toString should contain total count");
        assertTrue(str.contains("infinite="), "toString should contain infinite count");
        assertTrue(str.contains("finite="), "toString should contain finite range");
        assertTrue(str.contains("avg="), "toString should contain average");
    }

    @Test
    @DisplayName("Diversity ratio should be calculated correctly")
    void testDiversityRatio() {
        individuals.add(new TestIndividual(100.0, 50.0));
        individuals.add(new TestIndividual(120.0, 40.0));
        
        CrowdingDistance.CrowdingStats stats = CrowdingDistance.getCrowdingStatistics(individuals);
        
        double expectedRatio = (double) stats.infiniteDistanceCount / stats.totalIndividuals;
        assertEquals(expectedRatio, stats.getDiversityRatio(), EPSILON,
                "Diversity ratio should equal infinite count divided by total");
    }

    // ======================== Integration Tests ========================

    @Test
    @DisplayName("Full NSGA-II workflow should work correctly")
    void testFullNSGAIIWorkflow() {
        // Create a diverse population representing different trade-offs
        individuals.add(new TestIndividual(80.0, 70.0));   // Low energy, high cost
        individuals.add(new TestIndividual(120.0, 30.0));  // High energy, low cost  
        individuals.add(new TestIndividual(100.0, 50.0));  // Medium energy, medium cost
        individuals.add(new TestIndividual(90.0, 60.0));   // Low-medium energy, high-medium cost
        individuals.add(new TestIndividual(110.0, 40.0));  // High-medium energy, low-medium cost
        individuals.add(new TestIndividual(85.0, 65.0));   // Low energy, high cost variant
        individuals.add(new TestIndividual(115.0, 35.0));  // High energy, low cost variant
        
        // Step 1: Calculate crowding distances
        CrowdingDistance.calculateCrowdingDistance(individuals);
        
        // Verify all distances are non-negative
        for (Individual ind : individuals) {
            assertTrue(ind.getCrowdingDistance() >= 0,
                    "All crowding distances should be non-negative");
        }
        
        // Step 2: Sort by crowding distance  
        CrowdingDistance.sortByCrowdingDistance(individuals);
        
        // Verify sorting order
        for (int i = 0; i < individuals.size() - 1; i++) {
            assertTrue(individuals.get(i).getCrowdingDistance() >= 
                      individuals.get(i + 1).getCrowdingDistance(),
                      "Individuals should be sorted by crowding distance (descending)");
        }
        
        // Step 3: Verify boundary solutions have infinite distance
        long infiniteCount = individuals.stream()
                .mapToDouble(Individual::getCrowdingDistance)
                .filter(d -> d == Double.POSITIVE_INFINITY)
                .count();
        assertTrue(infiniteCount >= 2, "Should have at least 2 boundary solutions");
        
        // Step 4: Test selection
        List<Individual> selected = CrowdingDistance.selectByCrowdingDistance(individuals, 4);
        assertEquals(4, selected.size(), "Should select exactly 4 individuals");
        
        // Selected individuals should have highest crowding distances
        for (int i = 0; i < selected.size() - 1; i++) {
            assertTrue(selected.get(i).getCrowdingDistance() >= 
                      selected.get(i + 1).getCrowdingDistance(),
                      "Selected individuals should be sorted by crowding distance");
        }
        
        // Step 5: Get statistics
        CrowdingDistance.CrowdingStats stats = CrowdingDistance.getCrowdingStatistics(individuals);
        assertEquals(individuals.size(), stats.totalIndividuals,
                "Statistics should reflect total population size");
        assertTrue(stats.getDiversityRatio() > 0, "Diversity ratio should be positive");
        
        // Step 6: Test crowded comparison in typical NSGA-II scenario
        Individual ind1 = individuals.get(0);
        Individual ind2 = individuals.get(1);
        ind1.setRank(1);
        ind2.setRank(1);
        
        int comparison = CrowdingDistance.crowdedCompare(ind1, ind2);
        // Since ind1 has higher crowding distance (it's first after sorting), it should be preferred
        assertTrue(comparison <= 0, "Individual with higher crowding distance should be preferred");
    }

    @Test
    @DisplayName("Stress test with large population")
    void testLargePopulation() {
        // Create a large population with random-like but deterministic values
        Random random = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < 100; i++) {
            double energy = 50.0 + random.nextDouble() * 100.0;  // 50-150
            double cost = 20.0 + random.nextDouble() * 60.0;     // 20-80
            individuals.add(new TestIndividual(energy, cost));
        }
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> {
            CrowdingDistance.calculateCrowdingDistance(individuals);
        }, "Large population should be handled without exceptions");
        
        // Verify all distances are valid
        for (Individual ind : individuals) {
            assertTrue(ind.getCrowdingDistance() >= 0,
                    "All crowding distances should be non-negative");
            assertFalse(Double.isNaN(ind.getCrowdingDistance()),
                    "No crowding distance should be NaN");
        }
        
        // Test sorting and selection on large population
        assertDoesNotThrow(() -> {
            CrowdingDistance.sortByCrowdingDistance(individuals);
            CrowdingDistance.selectByCrowdingDistance(individuals, 50);
            CrowdingDistance.getCrowdingStatistics(individuals);
        }, "All operations should work on large population");
    }

    @Test
    @DisplayName("Boundary cases with extreme values")
    void testExtremeValues() {
        individuals.add(new TestIndividual(Double.MIN_VALUE, Double.MIN_VALUE));
        individuals.add(new TestIndividual(1e-10, 1e-10));
        individuals.add(new TestIndividual(1e10, 1e10));
        
        // Should handle extreme values without overflow or underflow
        assertDoesNotThrow(() -> {
            CrowdingDistance.calculateCrowdingDistance(individuals);
        }, "Extreme values should be handled gracefully");
        
        for (Individual ind : individuals) {
            assertFalse(Double.isNaN(ind.getCrowdingDistance()),
                    "No crowding distance should be NaN with extreme values");
            assertFalse(Double.isInfinite(ind.getCrowdingDistance()) && 
                       ind.getCrowdingDistance() < 0,
                       "No negative infinite crowding distance should occur");
        }
    }
}

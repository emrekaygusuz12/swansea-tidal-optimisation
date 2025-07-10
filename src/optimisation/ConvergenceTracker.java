package src.optimisation;

import java.util.*;

public class ConvergenceTracker {

    private final List<ConvergenceMetrics> generationHistory;
    private final int stagnationThreshold;
    private final double improvementThreshold;

    private boolean convergenceAchieved;
    private int convergenceGeneration;
    private String convergenceReason;

    public ConvergenceTracker(int stagnationThreshold, double improvementThreshold) {
        this.generationHistory = new ArrayList<>();
        this.stagnationThreshold = stagnationThreshold;
        this.improvementThreshold = improvementThreshold;
        this.convergenceAchieved = false;
        this.convergenceGeneration = -1;
        this.convergenceReason = "Not converged";
    }

    public boolean recordGeneration(int generation, List<Individual> paretoFront, double hypervolume) {
        ConvergenceMetrics metrics = calculateMetrics(generation, paretoFront, hypervolume);
        generationHistory.add(metrics);

        if (!convergenceAchieved) {
            checkConvergence(generation, metrics);
        }

        printProgress(metrics);

        return convergenceAchieved;
    }

     /**
     * Calculates comprehensive metrics for convergence analysis.
     */
    private ConvergenceMetrics calculateMetrics(int generation, List<Individual> paretoFront, double hypervolume) {
        if (paretoFront.isEmpty()) {
            return new ConvergenceMetrics(generation, 0, 0.0, 0.0, hypervolume, 0.0, 0.0, 0.0);
        }
        
        // Basic metrics
        int paretoSize = paretoFront.size();
        
        // Energy metrics
        double maxEnergy = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).max().orElse(0.0);
        double avgEnergy = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).average().orElse(0.0);
        
        // Cost metrics  
        double minCost = paretoFront.stream()
            .filter(ind -> ind.getUnitCost() != Double.MAX_VALUE)
            .mapToDouble(Individual::getUnitCost)
            .min().orElse(Double.MAX_VALUE);
        
        // Diversity metrics
        double energySpread = calculateEnergySpread(paretoFront);
        double costSpread = calculateCostSpread(paretoFront);
        
        return new ConvergenceMetrics(generation, paretoSize, maxEnergy, avgEnergy, 
                                    hypervolume, minCost, energySpread, costSpread);
    }
    
    /**
     * Checks multiple convergence criteria.
     */
    private void checkConvergence(int generation, ConvergenceMetrics current) {
        if (generationHistory.size() < stagnationThreshold) {
            return; // Not enough history
        }
        
        // Get comparison point (N generations ago)
        ConvergenceMetrics comparison = generationHistory.get(generationHistory.size() - stagnationThreshold);
        
        // Check different convergence criteria
        checkHypervolumeConvergence(generation, current, comparison);
        checkEnergyConvergence(generation, current, comparison);
        checkDiversityConvergence(generation, current, comparison);
    }
    
    /**
     * Check hypervolume-based convergence.
     */
    private void checkHypervolumeConvergence(int generation, ConvergenceMetrics current, ConvergenceMetrics comparison) {
        if (convergenceAchieved) return;
        
        double hvImprovement = (current.hypervolume - comparison.hypervolume) / Math.abs(comparison.hypervolume);
        
        if (Math.abs(hvImprovement) < improvementThreshold) {
            convergenceAchieved = true;
            convergenceGeneration = generation;
            convergenceReason = String.format("Hypervolume stagnation: %.6f improvement in last %d generations", 
                                            hvImprovement, stagnationThreshold);
        }
    }
    
    /**
     * Check energy-based convergence.
     */
    private void checkEnergyConvergence(int generation, ConvergenceMetrics current, ConvergenceMetrics comparison) {
        if (convergenceAchieved) return;
        
        double energyImprovement = (current.maxEnergy - comparison.maxEnergy) / comparison.maxEnergy;
        
        if (energyImprovement < improvementThreshold) {
            convergenceAchieved = true;
            convergenceGeneration = generation;
            convergenceReason = String.format("Energy stagnation: %.6f improvement in last %d generations", 
                                            energyImprovement, stagnationThreshold);
        }
    }
    
    /**
     * Check diversity-based convergence.
     */
    private void checkDiversityConvergence(int generation, ConvergenceMetrics current, ConvergenceMetrics comparison) {
        if (convergenceAchieved) return;
        
        // If Pareto front size hasn't changed significantly
        double sizeChange = Math.abs(current.paretoSize - comparison.paretoSize) / (double) comparison.paretoSize;
        
        if (sizeChange < improvementThreshold && current.energySpread < 0.001) {
            convergenceAchieved = true;
            convergenceGeneration = generation;
            convergenceReason = String.format("Diversity stagnation: Pareto size stable, energy spread %.6f", 
                                            current.energySpread);
        }
    }
    
    /**
     * Calculate energy spread (diversity measure).
     */
    private double calculateEnergySpread(List<Individual> paretoFront) {
        if (paretoFront.size() < 2) return 0.0;
        
        double min = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).min().orElse(0.0);
        double max = paretoFront.stream().mapToDouble(Individual::getEnergyOutput).max().orElse(0.0);
        
        return (max - min) / Math.max(max, 1.0); // Normalized spread
    }
    
    /**
     * Calculate cost spread (diversity measure).
     */
    private double calculateCostSpread(List<Individual> paretoFront) {
        if (paretoFront.size() < 2) return 0.0;
        
        List<Double> validCosts = paretoFront.stream()
            .mapToDouble(Individual::getUnitCost)
            .filter(cost -> cost != Double.MAX_VALUE)
            .boxed()
            .toList();
            
        if (validCosts.size() < 2) return 0.0;
        
        double min = validCosts.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = validCosts.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        return (max - min) / Math.max(max, 1.0); // Normalized spread
    }
    
    /**
     * Print detailed convergence progress.
     */
    private void printProgress(ConvergenceMetrics metrics) {
        System.out.printf("Generation %d: PF=%d, Energy=%.1f GWh, Cost=\u00A3%.0f/MWh, HV=%.2e, Spread=%.4f%n",
                         metrics.generation, 
                         metrics.paretoSize, 
                         metrics.maxEnergy / 1000.0, // Convert to GWh for display
                         metrics.minCost,
                         metrics.hypervolume,
                         metrics.energySpread);
    }
    
    /**
     * Get convergence summary for final reporting.
     */
    public ConvergenceSummary getConvergenceSummary() {
        if (generationHistory.isEmpty()) {
            return new ConvergenceSummary(false, -1, "No data", null, null);
        }
        
        ConvergenceMetrics first = generationHistory.get(0);
        ConvergenceMetrics last = generationHistory.get(generationHistory.size() - 1);
        
        return new ConvergenceSummary(convergenceAchieved, convergenceGeneration, 
                                    convergenceReason, first, last);
    }
    
    /**
     * Data class for storing metrics per generation.
     */
    public static class ConvergenceMetrics {
        public final int generation;
        public final int paretoSize;
        public final double maxEnergy;
        public final double avgEnergy;
        public final double hypervolume;
        public final double minCost;
        public final double energySpread;
        public final double costSpread;
        
        public ConvergenceMetrics(int generation, int paretoSize, double maxEnergy, double avgEnergy,
                                double hypervolume, double minCost, double energySpread, double costSpread) {
            this.generation = generation;
            this.paretoSize = paretoSize;
            this.maxEnergy = maxEnergy;
            this.avgEnergy = avgEnergy;
            this.hypervolume = hypervolume;
            this.minCost = minCost;
            this.energySpread = energySpread;
            this.costSpread = costSpread;
        }
    }
    
    /**
     * Data class for convergence summary.
     */
    public static class ConvergenceSummary {
        public final boolean converged;
        public final int convergenceGeneration;
        public final String reason;
        public final ConvergenceMetrics firstGeneration;
        public final ConvergenceMetrics lastGeneration;
        
        public ConvergenceSummary(boolean converged, int convergenceGeneration, String reason,
                                ConvergenceMetrics firstGeneration, ConvergenceMetrics lastGeneration) {
            this.converged = converged;
            this.convergenceGeneration = convergenceGeneration;
            this.reason = reason;
            this.firstGeneration = firstGeneration;
            this.lastGeneration = lastGeneration;
        }
        
        @Override
        public String toString() {
            if (!converged) {
                return "Convergence: Not achieved - " + reason;
            }
            
            double energyImprovement = lastGeneration.maxEnergy - firstGeneration.maxEnergy;
            double costImprovement = firstGeneration.minCost - lastGeneration.minCost;
            
            return String.format("Convergence: Achieved at generation %d%n" +
                               "Reason: %s%n" +
                               "Progress: Energy +%.1f MWh, Cost -\u00A3%.0f/MWh, PF %d->%d solutions",
                               convergenceGeneration, reason, energyImprovement, costImprovement,
                               firstGeneration.paretoSize, lastGeneration.paretoSize);
        }
    }
}
    


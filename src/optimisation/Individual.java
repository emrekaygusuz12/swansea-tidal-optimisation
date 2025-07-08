package src.optimisation;

import java.util.Arrays;

/**
 * Represents a single solution in the NSGA-II algorithm for tidal lagoon optimisation.
 * Each individual contains per-half-tide control parameters used for simulating
 * and optimising the operation of the tidal lagoon using a 0-D model.
 * 
 * Each half-tide has two control parameters:
 * - Hs (starting head)
 * - He (ending head)
 * 
 * The full decision vector is stored as:
 * [Hs_0, He_0, Hs_1, He_1, ..., Hs_N, He_N]
 * Where N is the number of half-tides in the simulation window.
 * 
 * Objective values and NSGA-II metadata are also stored within this class.
 * 
 * @author Emre Kaygusuz
 * @version 1.0
 */
public class Individual {

    // ====================
    // CONSTANT PARAMETERS
    // ====================

    /** Minimum head value */
    private static final double MIN_HEAD = 0.5;

    /** Maximum head value */
    private static final double MAX_HEAD = 4.0;

    /** Each half tide has two control parameters (Hs and He) */
    private static final int PARAMETERS_PER_HALF_TIDE = 2; 

    // ===================
    // DECISION VARIABLES
    // ===================

    /** Encodes [Hs, He] pairs for each half-tide */
    private final double[] decisionVariables; 

    // =================
    // OBJECTIVE VALUES
    // =================

    /** Total energy output in MWh (Objective 1) */
    private double energyOutput;

    /** Unit cost in GBP per MWh (Objective 2) */
    private double unitCost;

    // =================
    // NSGA-II METADATA
    // =================

    /** NSGA-II Pareto front rank */
    private int rank;

    /** NSGA-II diversity measure */
    private double crowdingDistance;

    // ============================
    // CONSTRUCTOR
    // ============================

    /**
     * Constructs an Individual with a specified number of half-tides.
     * 
     * @param numberOfHalfTides The number of half-tides in the simulation window.
     */
    public Individual(int numberOfHalfTides) {
        this.decisionVariables = new double[numberOfHalfTides * PARAMETERS_PER_HALF_TIDE]; 
        this.energyOutput = 0.0;
        this.unitCost = 0.0;
        this.rank = -1; // Unranked initially
        this.crowdingDistance = 0.0; // No crowding distance initially
    }

    // ============================
    // DECISION VARIABLE ACCESSORS
    // ============================
    
    /**
     * Gets the decision variables of this individual.
     * 
     * @return double[] array containing [Hs_0, He_0, Hs_1, He_1, ..., Hs_N, He_N].
     */
    public double[] getDecisionVariables() {
        return decisionVariables;
    }

    /**
     * Gets the starting head (Hs) for a specific half-tide.
     * @param halfTideIndex The index of the half-tide.
     * @return Starting head value.
     */
    public double getStartHead(int halfTideIndex) {
        return decisionVariables[halfTideIndex * PARAMETERS_PER_HALF_TIDE];
    }

    /**
     * Gets the ending head (He) for a specific half-tide.
     * @param halfTideIndex The index of the half-tide.
     * @return Ending head value.
     */
    public double getEndHead(int halfTideIndex) {
        return decisionVariables[halfTideIndex * PARAMETERS_PER_HALF_TIDE + 1];
    }

    /**
     * Sets the starting head (Hs) for a specific half-tide.
     * @param halfTideIndex The index of the half-tide.
     * @param value The new starting head.
     */
    public void setStartHead(int halfTideIndex, double value) {
        if (!isValidHead(value)) {
            throw new IllegalArgumentException("Invalid Hs: must be between " + MIN_HEAD + " and " + MAX_HEAD);
        }
        decisionVariables[halfTideIndex * PARAMETERS_PER_HALF_TIDE] = value;
    }

    /**
     * Sets the ending head (He) for a specific half-tide.
     * @param halfTideIndex The index of the half-tide.
     * @param value The new ending head.
     */
    public void setEndHead(int halfTideIndex, double value) {
        if (!isValidHead(value)) {
            throw new IllegalArgumentException("Invalid He: must be between " + MIN_HEAD + " and " + MAX_HEAD);
        }
        decisionVariables[halfTideIndex * PARAMETERS_PER_HALF_TIDE + 1] = value;
    }

    public static double getMinHead() {
        return MIN_HEAD;
    }

    public static double getMaxHead() {
        return MAX_HEAD;
    }

    // ==========================
    // OBJECTIVE VALUE ACCESSORS
    // ==========================


    public double getEnergyOutput() {
        return energyOutput;
    }

    public void setEnergyOutput(double energyOutput) {
        this.energyOutput = energyOutput;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    // ===========================
    // NSGA-II METADATA ACCESSORS
    // ===========================

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getCrowdingDistance() {
        return crowdingDistance;
    }

    public void setCrowdingDistance(double crowdingDistance) {
        this.crowdingDistance = crowdingDistance;
    }

    // ===================
    // VALIDATION METHODS
    // ===================

    /**
    * Validates the head values (Hs and He) for each half-tide.
    */
    private static boolean isValidHead(double head) {
        return head >= MIN_HEAD && head <= MAX_HEAD;
    }

    // ========
    // CLONING
    // ========


    /**
     * Creates a deep clone of the individual including decision variables and metadata.
     * So the new generation doesn't overwrite their values during crossover and mutation.
     * 
     * @return cloned individual.
     */
    public Individual clone() {
        Individual clone = new Individual(decisionVariables.length / PARAMETERS_PER_HALF_TIDE);
        System.arraycopy(this.decisionVariables, 0, clone.decisionVariables, 0, this.decisionVariables.length);
        clone.energyOutput = this.energyOutput;
        clone.unitCost = this.unitCost;
        clone.rank = this.rank;
        clone.crowdingDistance = this.crowdingDistance;
        return clone;
    }

    // ======================
    // STRING REPRESENTATION
    // ======================

    
    /**
     * Returns a string representation of the individual for debugging and logging.
     * Includes decision variables, energy output, unit cost, rank, and crowding distance.
     * 
     * @return String representation of the individual.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Individual:\n");
        sb.append (" decisionVariables: ").append(Arrays.toString(decisionVariables)).append("\n");
        sb.append(", EnergyOutput: ").append(energyOutput).append("MWh\n");
        sb.append(", Unit Cost: ").append(unitCost).append(" GBP/MWh\n");
        sb.append(", Rank: ").append(rank).append("\n");
        sb.append(", crowdingDistance: ").append(crowdingDistance).append("\n");
        return sb.toString();
    }
}

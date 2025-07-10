package src.utils;

import java.io.*;
import java.util.*;

/**
 * Utility class for reading tidal elevation data from BODC (British Oceanographic Data Centre) files.
 * 
 * Handles the parsing of BODC tidal height data files which contains:
 * - Extensive metadata headers with SDN references
 * - Column header definitions
 * - Station metadata line
 * - Tidal measurement data with multiple sensors
 * 
 * The parser extracts tidal height values from the HTSeaLvl column
 * and automatically filters out headers and metadata lines.
 * 
 * Expected data line format (after headers):
 * ```
 * 2455563.000000	1	4.677	1	4.679	1	5.153	1
 * ```
 * Where columns are: JulianDate QV1 SeaLvl_bubbler QV2 SeaLvl_bubbler2 QV3 HTSeaLvl QV4
 *  
 * @author Emre Kaygusuz
 * @version 2.0
 */
public class TideDataReader {

    // ======================
    // DATA EXTRACTION METHODS
    // ======================

    /**
     * Reads tidal height values from a BODC data file.
     * Extracts height measurements from column 2 for simulation use.
     * 
     * @param filePath path to the tidal data file
     * @return List of tide heights in meters
     * @throws IOException if file reading fails
     */
    public static List<Double> readTideHeights(String filePath) throws IOException {
        List<Double> tideHeights = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipHeader = true; // Skip the first line if it contains headers

            while ((line = br.readLine()) != null) {
                if (skipHeader && (line.contains("Cruise") || line.contains("unspecified") ||
                    line.trim().isEmpty())) {
                    continue;
                }
                skipHeader = false; // After the first valid line, we stop skipping
                String[] parts = line.trim().split("\\s+");

                if (parts.length >= 8) {
                    try {
                        double height = Double.parseDouble(parts[2]);
                        tideHeights.add(height);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } 
        
        return tideHeights;
    }

    /**
     * Reads tidal data with timestamps for temporal analysis.
     * 
     * @param filePath Path to the data file
     * @return Map with Julian date as key and tide height as value
     * @throws IOException if file reading fails
     */
    public static Map<Double, Double> readTideData(String filePath) throws IOException {
        Map<Double, Double> tideData = new LinkedHashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipHeader = true;

            while ((line = br.readLine()) != null) {
                if (skipHeader && (line.contains("Cruise") || line.contains("unspecified") || line.trim().isEmpty())) {
                    continue;
                }
                skipHeader = false;
                
                String[] parts = line.trim().split("\\s+");
                
                if (parts.length >= 8) {
                    try {
                        double julianDate = Double.parseDouble(parts[0]);
                        double height = Double.parseDouble(parts[2]);
                        tideData.put(julianDate, height);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid data in line: " + line);
                    }
                }
            }
        }
        
        return tideData;
    }
}
    


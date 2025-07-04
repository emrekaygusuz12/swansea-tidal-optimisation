package src.utils;

import java.io.*;
import java.util.*;

/**
 * Utility class for reading tidal elevation data from a file.
 * This file is expected to have three columns: date, time, and tide height in meters.
 * 
 * Example line:
 * * 2023-10-01, 12:00, 2.5
 * 
 * Only the tide height (third column) is extracted for further processing.
 * 
 * Usage:
 * List<Double> tideHeights = FileReader.readTideHeights("data/b1111463.txt");
 * 
 * @author Emre Kaygusuz
 */
public class TideDataReader {


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
                        System.err.println("Invalid tide height value: " + parts[2] + " in line: " + line);
                    }
                }
            }
        } 
        
        return tideHeights;
    }

    /**
     * Read tidal data with timestamps (Julian dates converted to readable format)
     * @param filePath path to the data file
     * @return Map with Julian date as key and tide height as value
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
    


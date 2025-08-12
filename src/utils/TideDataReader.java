package src.utils;

import java.io.*;
import java.util.*;

/**
 * Utility class for reading tidal elevation data from BODC (British Oceanographic Data Centre) files.
 * 
 * Handles the parsing of BODC tidal height data files with a fixed 11-line header.
 * 
 * Expected data line format (after headers):
 *   1) 2011/01/01 00:00:00     4.679     -0.176  
 * Columns: [CycleNo) Date Time ASLVBG02 Residual]
 *  
 * @author Emre Kaygusuz
 * @version 2.1
 */
public class TideDataReader {

    /**
     * Reads tidal height values from a 2011/2012MUM data file.
     * Extracts height measurements from column 4 (ASLVBG02).
     * 
     * @param filePath path to the tidal data file
     * @return List of tide heights in meters
     * @throws IOException if file reading fails
     */
    public static List<Double> readTideHeights(String filePath) throws IOException {
    List<Double> tideHeights = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        // Skip the first 11 lines (header)
        for (int i = 0; i < 11; i++) {
            br.readLine();
        }
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts.length < 5) continue; // Defensive: skip malformed lines
            try {
                String heightStr = parts[3].replaceAll("[^\\d.-]", "");
                double height = Double.parseDouble(heightStr);
                if (height > -10 ) {
                    tideHeights.add(height);
                }
            } catch (NumberFormatException e) {
                // skip lines that don't parse
            }
        }
    }
    return tideHeights;
    }

    /**
     * Reads tidal data with timestamps for temporal analysis.
     * 
     * @param filePath Path to the data file
     * @return Map with date-time string as key and tide height as value
     * @throws IOException if file reading fails
     */
    public static Map<String, Double> readTideData(String filePath) throws IOException {
        Map<String, Double> tideData = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip the first 11 lines (header)
            for (int i = 0; i < 11; i++) {
                br.readLine();
            }
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 5) continue;
                try {
                    String dateTime = parts[1] + " " + parts[2];
                    String heightStr = parts[3].replaceAll("[^\\d.-]", "");
                    double height = Double.parseDouble(heightStr);
                    tideData.put(dateTime, height);
                } catch (NumberFormatException e) {
                    // skip lines that don't parse
                }
            }
        }
        return tideData;
    }
}
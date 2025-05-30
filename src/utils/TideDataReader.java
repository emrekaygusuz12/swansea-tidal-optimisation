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

            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    try {
                        double height = Double.parseDouble(parts[2]);
                        tideHeights.add(height);
                    } catch (NumberFormatException e) {
                        //System.err.println("Invalid tide height value: " + parts[2]);
                    }
                }
            }
        } 
        
        return tideHeights;
    }
    

}

package src;

import src.utils.TideDataReader;

import java.util.Collections;
import java.util.List;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "data/b1111463.txt"; // Path to the tide height data file
        try {
            List<Double> tideHeights = TideDataReader.readTideHeights(filePath);
            //System.out.println("Tide Heights: " + tideHeights);
            System.out.printf("Total readings: %d%n", tideHeights.size());
            System.out.printf("Min: %.2f m, Max: %.2f m%n",
            Collections.min(tideHeights), Collections.max(tideHeights));


            for (int i =0; i< Math.min(50, tideHeights.size()); i++) {
                System.out.printf("Tide %d: %.2f m%n", i + 1, tideHeights.get(i));
            }
        } catch (IOException e) {
            System.err.println("Error reading tide heights: " + e.getMessage());
        }
    }
    
}

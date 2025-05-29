package com.example.paralelizacion;

import java.io.*;
import java.util.Properties;

public class PrediccionUtils {

    private PrediccionUtils() {
        
    }

    public static double predecirConsumoIA(String jsonData) throws IOException {
        File tempFile = File.createTempFile("pred_input_", ".json");
        InputStream input = PrediccionUtils.class.getResourceAsStream("/config.txt");
        if (input == null) {
            throw new FileNotFoundException("Archivo config.txt no encontrado en resources");
        }
        Properties config = new Properties();
        config.load(input);
        String scriptPath = config.getProperty("scriptPath");
        

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(jsonData);  
        }

        ProcessBuilder pb = new ProcessBuilder("python", scriptPath, tempFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String result = output.toString().trim();

            if (result.startsWith("Traceback")) {
                System.err.println("------ ERROR PYTHON ------");
                System.err.println(result);
                System.err.println("--------------------------");
                return -1;
            }

            return Double.parseDouble(result);
        }
    }
}

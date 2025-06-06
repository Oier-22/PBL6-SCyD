package com.example.paralelizacion;

import java.io.*;
import java.util.Properties;

public class PrediccionUtils {

    private static final String SCRIPT_PATH;

    static {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("rabbitMQ/config/config.txt")) {
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("❌ No se pudo cargar config.txt", e);
        }

        SCRIPT_PATH = config.getProperty("scriptPath");
        if (SCRIPT_PATH == null || SCRIPT_PATH.isBlank()) {
            throw new RuntimeException("❌ 'scriptPath' no definido en config.txt");
        }
    }

    private PrediccionUtils() {
        // Clase utilitaria, no instanciable
    }

    public static double predecirConsumoIA(String jsonData) throws IOException {
        File tempFile = File.createTempFile("pred_input_", ".json");

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(jsonData);
        }

        ProcessBuilder pb = new ProcessBuilder("python", SCRIPT_PATH, tempFile.getAbsolutePath());
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

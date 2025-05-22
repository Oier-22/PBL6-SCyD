package com.example.paralelizacion;

import java.io.*;
import java.util.Locale;

public class PrediccionUtils {

    public static double predecirConsumoIA(String jsonData) throws IOException {
        File tempFile = File.createTempFile("pred_input_", ".json");
        tempFile.deleteOnExit();
        String scriptPath = "C:\\mondragon\\3. maila\\2 seihilekoa\\PBL6\\programa\\PBL6-SCyD\\modelo_ia\\predecir.py";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(jsonData);  // Escribir el JSON directamente
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

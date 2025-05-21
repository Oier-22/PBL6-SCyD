package common;

import java.io.*;
import java.util.Locale;

public class PrediccionUtils {

    public static double predecirConsumoIA(double[] datos) throws IOException {
        if (datos.length != 10) {
            throw new IllegalArgumentException("Se esperaban 10 variables de entrada, pero se recibieron " + datos.length);
        }

        File tempFile = File.createTempFile("pred_input_", ".json");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            String inputJson = String.format(Locale.US,
                "{" +
                    "\"temp\":%.2f," +
                    "\"humedad\":%.2f," +
                    "\"viento\":%.2f," +
                    "\"radiacion\":%.2f," +
                    "\"precipitacion\":%.2f," +
                    "\"tipo_planta\":%d," +
                    "\"etapa_crecimiento\":%d," +
                    "\"tipo_suelo\":%d," +
                    "\"humedad_suelo\":%.2f," +
                    "\"dia_del_ano\":%d" +
                "}",
                datos[0],  // temp
                datos[1],  // humedad
                datos[2],  // viento
                datos[3],  // radiacion
                datos[4],  // precipitacion
                (int) datos[5],  // tipo_planta
                (int) datos[6],  // etapa_crecimiento
                (int) datos[7],  // tipo_suelo
                datos[8],  // humedad_suelo
                (int) datos[9]   // dia_del_ano
            );

            writer.write(inputJson);
        }

        ProcessBuilder pb = new ProcessBuilder("python", "../modelo_ia/predecir.py", tempFile.getAbsolutePath());
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
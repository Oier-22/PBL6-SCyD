package estrategias;

import common.PrediccionUtils;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkParalelizacion {

    static final int[] tamanos = { 20, 50, 100, 200 };

    public static void main(String[] args) throws Exception {
        String output = "resultados_paralelizacion.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.println("num_parcelas,estrategia,tiempo_ms");

            for (int numParcelas : tamanos) {
                List<double[]> datosList = new ArrayList<>();
                for (int i = 0; i < numParcelas; i++) {
                    datosList.add(PrediccionSecuencial.generarDatos());
                }

                // Secuencial
                long t1 = System.currentTimeMillis();
                for (double[] datos : datosList) {
                    PrediccionUtils.predecirConsumoIA(datos);
                }
                long t2 = System.currentTimeMillis();
                writer.printf("%d,Secuencial,%d%n", numParcelas, t2 - t1);

                // Executor
                long t3 = System.currentTimeMillis();
                PrediccionExecutor.runWithDatosList(datosList);
                long t4 = System.currentTimeMillis();
                writer.printf("%d,ExecutorService,%d%n", numParcelas, t4 - t3);

                // ForkJoin
                long t5 = System.currentTimeMillis();
                PrediccionForkJoin.runWithDatosList(datosList);
                long t6 = System.currentTimeMillis();
                writer.printf("%d,ForkJoin,%d%n", numParcelas, t6 - t5);

                System.out.println("-----");
            }
        }

        System.out.println("Benchmark completado. Resultados guardados en: " + output);
    }
}
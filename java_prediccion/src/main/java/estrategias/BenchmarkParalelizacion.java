package estrategias;

import common.PrediccionUtils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkParalelizacion {

    static final int[] tamanos = {20, 50, 100, 200};

    public static void main(String[] args) throws Exception {
        String output = "resultados_paralelizacion.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.println("num_parcelas,estrategia,tiempo_ms");

            for (int numParcelas : tamanos) {
                System.out.println("Ejecutando benchmark con " + numParcelas + " parcelas...");
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

                // ExecutorService
                long t3 = System.currentTimeMillis();
                PrediccionExecutor.runWithDatosList(datosList);
                long t4 = System.currentTimeMillis();
                writer.printf("%d,ExecutorService,%d%n", numParcelas, t4 - t3);

                // ForkJoin
                long t5 = System.currentTimeMillis();
                PrediccionForkJoin.runWithDatosList(datosList);
                long t6 = System.currentTimeMillis();
                writer.printf("%d,ForkJoin,%d%n", numParcelas, t6 - t5);

                // ParallelStream
                long t7 = System.currentTimeMillis();
                PrediccionParallelStream.runWithDatosList(datosList);
                long t8 = System.currentTimeMillis();
                writer.printf("%d,ParallelStream,%d%n", numParcelas, t8 - t7);

                // CompletableFuture
                long t9 = System.currentTimeMillis();
                PrediccionCompletableFuture.runWithDatosList(datosList);
                long t10 = System.currentTimeMillis();
                writer.printf("%d,CompletableFuture,%d%n", numParcelas, t10 - t9);

                // IntStream.parallel
                long t11 = System.currentTimeMillis();
                PrediccionIntParallelStream.runWithDatosList(datosList);
                long t12 = System.currentTimeMillis();
                writer.printf("%d,IntParallelStream,%d%n", numParcelas, t12 - t11);

                System.out.println("-----");
            }
        }

        System.out.println("Benchmark completado. Resultados guardados en: resultados_paralelizacion.csv");
    }
}

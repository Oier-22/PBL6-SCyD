package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrediccionExecutor {
    private static final int NUM_PARCELAS = 100;
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> resultados = new ArrayList<>();

        List<double[]> datosList = new ArrayList<>();
        for (int i = 1; i <= NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        long start = System.currentTimeMillis();

        for (int i = 1; i <= NUM_PARCELAS; i++) {
            final int id = i;
            final double[] datos = datosList.get(i - 1);

            resultados.add(executor.submit(() -> {
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    return "Parcela P" + id + ": " + pred + " L/m²";
                } catch (IOException e) {
                    return "Parcela P" + id + ": ERROR";
                }
            }));
        }

        for (Future<String> f : resultados) {
            try {
                System.out.println(f.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long end = System.currentTimeMillis();
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }

    public static void runWithDatosList(List<double[]> datosList) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<Future<String>> resultados = new ArrayList<>();
        int numParcelas = datosList.size();

        for (int i = 1; i <= numParcelas; i++) {
            final int id = i;
            final double[] datos = datosList.get(i - 1);

            resultados.add(executor.submit(() -> {
                try {
                    double pred = common.PrediccionUtils.predecirConsumoIA(datos);
                    return "Parcela P" + id + ": " + pred + " L/m²";
                } catch (Exception e) {
                    return "Parcela P" + id + ": ERROR";
                }
            }));
        }

        for (Future<String> f : resultados) {
            try {
                System.out.println("[Executor] " + f.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
    }
}
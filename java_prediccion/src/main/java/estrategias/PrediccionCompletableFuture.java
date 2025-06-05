package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrediccionCompletableFuture {
    private static final int NUM_PARCELAS = 60;

    public static void main(String[] args) throws InterruptedException {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

        List<double[]> datosList = new ArrayList<>();
        for (int i = 1; i <= NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        long start = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_PARCELAS; i++) {
            final int id = i + 1;
            final double[] datos = datosList.get(i);

            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    return "Parcela P" + id + ": " + pred + " L/m²";
                } catch (IOException e) {
                    return "Parcela P" + id + ": ERROR";
                }
            }, executor).thenAccept(System.out::println);

            futures.add(future);
        }

        // Esperar a que todas las predicciones terminen
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();

        long end = System.currentTimeMillis();
        System.out.println("CompletableFuture:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }

    public static void runWithDatosList(List<double[]> datosList) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

        long start = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int numParcelas = datosList.size();

        for (int i = 0; i < numParcelas; i++) {
            final int id = i + 1;
            final double[] datos = datosList.get(i);

            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    return "[Completable] Parcela P" + id + ": " + pred + " L/m²";
                } catch (IOException e) {
                    return "[Completable] Parcela P" + id + ": ERROR";
                }
            }, executor).thenAccept(System.out::println);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();

        long end = System.currentTimeMillis();
        System.out.println("CompletableFuture:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }
}

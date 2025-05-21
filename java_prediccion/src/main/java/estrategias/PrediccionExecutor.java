package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrediccionExecutor {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> resultados = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            final int id = i;
            final double[] datos = PrediccionSecuencial.generarDatos();

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
}
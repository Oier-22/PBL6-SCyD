package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PrediccionIntParallelStream {
    private static final int NUM_PARCELAS = 60;

    public static void main(String[] args) {
        List<double[]> datosList = new ArrayList<>();
        for (int i = 0; i < NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        long start = System.currentTimeMillis();

        IntStream.range(0, NUM_PARCELAS).parallel()
            .forEach(i -> {
                int id = i + 1;
                double[] datos = datosList.get(i);
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    System.out.println("Parcela P" + id + ": " + pred + " L/m²");
                } catch (IOException e) {
                    System.out.println("Parcela P" + id + ": ERROR");
                }
            });

        long end = System.currentTimeMillis();
        System.out.println("IntStream.parallel:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }

    public static void runWithDatosList(List<double[]> datosList) {
        int total = datosList.size();

        long start = System.currentTimeMillis();

        IntStream.range(0, total).parallel()
            .forEach(i -> {
                int id = i + 1;
                double[] datos = datosList.get(i);
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    System.out.println("[IntParallel] Parcela P" + id + ": " + pred + " L/m²");
                } catch (IOException e) {
                    System.out.println("[IntParallel] Parcela P" + id + ": ERROR");
                }
            });

        long end = System.currentTimeMillis();
        System.out.println("IntStream.parallel:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }
}

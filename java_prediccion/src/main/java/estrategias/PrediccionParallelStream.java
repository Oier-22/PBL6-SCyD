package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrediccionParallelStream {
    private static final int NUM_PARCELAS = 60;

    public static void main(String[] args) {
        List<double[]> datosList = new ArrayList<>();
        for (int i = 1; i <= NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        long start = System.currentTimeMillis();

        datosList.parallelStream()
            .map(datos -> {
                int id = datosList.indexOf(datos) + 1;
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    return "Parcela P" + id + ": " + pred + " L/m²";
                } catch (IOException e) {
                    return "Parcela P" + id + ": ERROR";
                }
            })
            .forEach(System.out::println);

        long end = System.currentTimeMillis();
        System.out.println("ParallelStream:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }

    public static void runWithDatosList(List<double[]> datosList) {
        long start = System.currentTimeMillis();

        datosList.parallelStream()
            .map(datos -> {
                int id = datosList.indexOf(datos) + 1;
                try {
                    double pred = PrediccionUtils.predecirConsumoIA(datos);
                    return "[Parallel] Parcela P" + id + ": " + pred + " L/m²";
                } catch (IOException e) {
                    return "[Parallel] Parcela P" + id + ": ERROR";
                }
            })
            .forEach(System.out::println);

        long end = System.currentTimeMillis();
        System.out.println("ParallelStream:");
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }
}

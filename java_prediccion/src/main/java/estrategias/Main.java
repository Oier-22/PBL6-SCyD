package estrategias;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int NUM_PARCELAS = 100;

    public static void main(String[] args) throws Exception {
        // Generar datos una sola vez
        List<double[]> datosList = new ArrayList<>();
        for (int i = 0; i < NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        // // Secuencial
        // long startSec = System.currentTimeMillis();
        // for (int i = 0; i < NUM_PARCELAS; i++) {
        //     try {
        //         double pred = common.PrediccionUtils.predecirConsumoIA(datosList.get(i));
        //         System.out.println("[Secuencial] Parcela P" + (i + 1) + ": " + pred + " L/m²");
        //     } catch (Exception e) {
        //         System.out.println("[Secuencial] Parcela P" + (i + 1) + ": ERROR");
        //     }
        // }
        // long endSec = System.currentTimeMillis();
        // System.out.println("Tiempo total Secuencial: " + (endSec - startSec) + " ms\n");

        // ExecutorService
        long startExec = System.currentTimeMillis();
        PrediccionExecutor.runWithDatosList(datosList);
        long endExec = System.currentTimeMillis();
        System.out.println("Tiempo total ExecutorService: " + (endExec - startExec) + " ms\n");

        // ForkJoin
        long startFj = System.currentTimeMillis();
        PrediccionForkJoin.runWithDatosList(datosList);
        long endFj = System.currentTimeMillis();
        System.out.println("Tiempo total ForkJoin: " + (endFj - startFj) + " ms\n");
    }
}
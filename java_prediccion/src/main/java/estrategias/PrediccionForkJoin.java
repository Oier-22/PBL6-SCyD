package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrediccionForkJoin {
    private static final int NUM_PARCELAS = 20;

    static class TareaPrediccion extends RecursiveTask<String> {
        int id;
        double[] datos;

        public TareaPrediccion(int id, double[] datos) {
            this.id = id;
            this.datos = datos;
        }

        @Override
        protected String compute() {
            try {
                double pred = PrediccionUtils.predecirConsumoIA(datos);
                return "Parcela P" + id + ": " + pred + " L/m²";
            } catch (IOException e) {
                return "Error en parcela P" + id;
            }
        }
    }

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        List<TareaPrediccion> tareas = new ArrayList<>();

        List<double[]> datosList = new ArrayList<>();
        for (int i = 1; i <= NUM_PARCELAS; i++) {
            datosList.add(PrediccionSecuencial.generarDatos());
        }

        long start = System.currentTimeMillis();

        for (int i = 1; i <= NUM_PARCELAS; i++) {
            double[] datos = datosList.get(i - 1);
            tareas.add(new TareaPrediccion(i, datos));
        }

        List<ForkJoinTask<String>> results = new ArrayList<>();
        for (TareaPrediccion t : tareas) {
            results.add(pool.submit(t));
        }

        for (ForkJoinTask<String> r : results) {
            System.out.println(r.join());
        }

        long end = System.currentTimeMillis();
        System.out.println("Tiempo total: " + (end - start) + " ms");

        pool.shutdown();
    }
}
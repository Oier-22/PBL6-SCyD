package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrediccionForkJoin {
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

        long start = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            double[] datos = PrediccionSecuencial.generarDatos();
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
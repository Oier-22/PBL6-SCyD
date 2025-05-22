package estrategias;

import common.PrediccionUtils;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class PrediccionSecuencial {
    private static final int NUM_PARCELAS = 100;

    public static void main(String[] args) {
        double[][] datosList = new double[NUM_PARCELAS][];
        for (int i = 0; i < NUM_PARCELAS; i++) {
            datosList[i] = generarDatos();
        }

        long start = System.currentTimeMillis();

        for (int i = 1; i <= NUM_PARCELAS; i++) {
            double[] datos = datosList[i - 1];
            try {
                double pred = PrediccionUtils.predecirConsumoIA(datos);
                System.out.println("Parcela P" + i + ": " + pred + " L/m²");
            } catch (IOException e) {
                System.out.println("Parcela P" + i + ": ERROR");
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Tiempo total: " + (end - start) + " ms");
    }

    public static double[] generarDatos() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new double[] {
                r.nextDouble(20, 40), // temp
                r.nextDouble(40, 90), // humedad
                r.nextDouble(0.5, 4), // viento
                r.nextDouble(300, 800), // radiación
                r.nextDouble(0, 20), // precipitación
                r.nextInt(1, 4), // tipo_planta
                r.nextInt(1, 4), // etapa_crecimiento
                r.nextInt(1, 4), // tipo_suelo
                r.nextDouble(10, 60), // humedad_suelo
                r.nextInt(1, 366) // día del año
        };
    }
}

package com.example.paralelizacion;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.Channel;

public class PrediccionExecutor {

    private PrediccionExecutor() {

    }

    public static class JsonConId {
        public final String id;
        public final String json;
        public JsonConId(String id, String json) {
            this.id = id;
            this.json = json;
        }
    }

    public static void runWithDatosList(List<JsonConId> datosList, Channel channel) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<Future<String>> resultados = new ArrayList<>();

        for (JsonConId item : datosList) {
            resultados.add(executor.submit(() -> {
                try {
                    System.out.println("[Executor] Procesando Parcela P" + item.id);
                    System.out.println("[Executor] JSON recibido: " + item.json);

                    if (Math.random() < 0.01) {
                        System.out.println("[Executor] ⚠️ Simulando fallo: Parcela " + item.id + " no responderá.");
                        return "No responde: " + item.id;
                    }

                    double pred;
                    if (Math.random() < 0.01) {
                        System.out.println("[Executor] ⚠️ Simulando valor anomalo");
                        pred = 99999.0;
                    } else {
                        pred = PrediccionUtils.predecirConsumoIA(item.json);
                    }

                    String resultado = item.id + ":" + pred;
                    System.out.println("[Executor] Resultado predicción: " + resultado);
                    channel.basicPublish("parcelas_response", "", null, resultado.getBytes());
                    return resultado;
                } catch (Exception e) {
                    String error = "Parcela P" + item.id + ": ERROR -> " + e.getMessage();
                    System.err.println("[Executor] " + error);
                    e.printStackTrace();
                    return error;
                }
            }));
        }

        for (Future<String> f : resultados) {
            try {
                System.out.println("[Executor] Resultado final: " + f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Executor] Error interrumpido al obtener resultado.");
            } catch (Exception e) {
                System.err.println("[Executor] Error obteniendo resultado: " + e.getMessage());
                e.printStackTrace();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}

// EstabilidadWorker.java - escucha estabilidad.parcela y simula un análisis con sleep
package com.example;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;

public class EstabilidadWorker {
    private static final String EXCHANGE = "post_prediccion_x";
    private static final String RESPONSE_KEY = "alerta.estabilidad";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.73.245");
        factory.setUsername("testuser");
        factory.setPassword("testpassword");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, EXCHANGE, "estabilidad.parcela");

        System.out.println(" [*] Analizando estabilidad de parcelas...");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] partes = mensaje.split(":");
            if (partes.length == 2) {
                String id = partes[0].trim();
                System.out.println(" [Estabilidad] Analizando parcela " + id + "...");
                try {
                    Thread.sleep(10000); 
                    System.out.println(" [Estabilidad] Análisis completo para parcela " + id + ".");

                    String resultado = id + ": ESTABLE";
                    channel.basicPublish(EXCHANGE, RESPONSE_KEY, null, resultado.getBytes());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(queue, false, callback, consumerTag -> {});

        Thread.sleep(Long.MAX_VALUE);
    }
}

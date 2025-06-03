// AlertaWorker.java - escucha alerta.inicio y alerta.estabilidad y muestra mensajes
package com.example;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;

public class AlertaWorker {
    private static final String EXCHANGE = "post_prediccion_x";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.73.245");
        factory.setUsername("testuser");
        factory.setPassword("testpassword");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, EXCHANGE, "alerta.inicio");
        channel.queueBind(queue, EXCHANGE, "alerta.estabilidad");

        System.out.println(" [*] Esperando mensajes de alerta...");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();

            if (routingKey.equals("alerta.inicio")) {
                String[] partes = mensaje.split(":");
                if (partes.length == 2) {
                    String id = partes[0].trim();
                    String agua = partes[1].trim();
                    System.out.println(" [ALERTA] Parcela " + id + " necesita mucha agua: " + agua + "L. Realizando análisis de estabilidad...");
                }
            } else if (routingKey.equals("alerta.estabilidad")) {
                String[] partes = mensaje.split(":");
                if (partes.length == 2) {
                    String id = partes[0].trim();
                    String resultado = partes[1].trim();
                    System.out.println(" [ALERTA] Resultado de estabilidad para parcela " + id + ": " + resultado);
                }
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(queue, false, callback, consumerTag -> {});

        Thread.sleep(Long.MAX_VALUE);
    }
}

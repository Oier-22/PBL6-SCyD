package com.example;

import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public class AlertaWorker {
    private static final String EXCHANGE = "post_prediccion";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("testuser");
        factory.setPassword("testpassword");

        char[] truststorePassword = "changeit".toCharArray();
        KeyStore trustStore = KeyStore.getInstance("JKS");

        InputStream tsStream = AlertaWorker.class.getClassLoader().getResourceAsStream("tls/truststore.jks");
        trustStore.load(tsStream, truststorePassword);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        factory.setPort(5671);
        factory.useSslProtocol(sslContext);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
        String queue = channel.queueDeclare().getQueue();

        String usuarioId = "1"; // ← Cambia esto según el usuario
        String routingKey = "alerta." + usuarioId;
        channel.queueBind(queue, EXCHANGE, routingKey);

        System.out.println(" [*] Escuchando alertas para usuario: " + usuarioId);

        DeliverCallback callback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String receivedRoutingKey = delivery.getEnvelope().getRoutingKey();

            System.out.println(" [ALERTA] (" + receivedRoutingKey + ") " + mensaje);

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(queue, false, callback, consumerTag -> {});
        Thread.sleep(Long.MAX_VALUE);
    }
}

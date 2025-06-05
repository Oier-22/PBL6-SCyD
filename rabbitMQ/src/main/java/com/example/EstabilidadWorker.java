package com.example;

import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public class EstabilidadWorker {
    private static final String EXCHANGE = "post_prediccion";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("testuser");
        factory.setPassword("testpassword");

        char[] truststorePassword = "changeit".toCharArray();
        KeyStore trustStore = KeyStore.getInstance("JKS");

        InputStream tsStream = EstabilidadWorker.class.getClassLoader().getResourceAsStream("tls/truststore.jks");
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
        channel.queueBind(queue, EXCHANGE, "estabilidad.parcela");

        System.out.println(" [*] Analizando estabilidad de parcelas...");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] partes = mensaje.split(":");

            if (partes.length == 3) {
                String id = partes[0].trim();
                String consumo = partes[1].trim(); // no se usa, pero se podría
                String usuarioId = partes[2].trim();

                System.out.println(" [Estabilidad] Analizando parcela " + id + " para usuario " + usuarioId + "...");

                try {
                    Thread.sleep(10000);

                    String resultado = id + ": plan para ajustar";
                    String userRoutingKey = "alerta." + usuarioId;

                    channel.basicPublish(EXCHANGE, userRoutingKey, null, resultado.getBytes(StandardCharsets.UTF_8));

                    System.out.println(" [Estabilidad] Resultado enviado a " + userRoutingKey);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            } else {
                System.out.println("⚠️ Formato inesperado: " + mensaje);
            }

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(queue, false, callback, consumerTag -> {});
        Thread.sleep(Long.MAX_VALUE);
    }
}

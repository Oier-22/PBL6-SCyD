package com.example;

import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Properties;

public class EstabilidadWorker {
    private static final String EXCHANGE = "post_prediccion";

    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("rabbitMQ/config/config.txt")) {
            config.load(input);
        }

        String host = config.getProperty("rabbitmq.host");
        String username = config.getProperty("rabbitmq.username");
        String password = config.getProperty("rabbitmq.password");
        int port = Integer.parseInt(config.getProperty("rabbitmq.port"));
        String truststorePath = config.getProperty("truststore.path");
        char[] truststorePassword = config.getProperty("truststore.password").toCharArray();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream tsStream = new FileInputStream(truststorePath)) {
            trustStore.load(tsStream, truststorePassword);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        factory.setPort(port);
        factory.useSslProtocol(sslContext);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
            String queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, EXCHANGE, "estabilidad.parcela");

            System.out.println(" [*] Analizando estabilidad de parcelas...");

            DeliverCallback callback = (consumerTag, delivery) -> {
                String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String[] partes = mensaje.split(":");

                if (partes.length == 3) {
                    String id = partes[0].trim();
                    String consumo = partes[1].trim();
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
                    }
                } else {
                    System.out.println("⚠️ Formato inesperado: " + mensaje);
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(queue, false, callback, consumerTag -> {});
            Thread.sleep(Long.MAX_VALUE); // Mantener vivo el hilo
        }
    }
}

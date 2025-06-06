package com.example;

import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Properties;

public class AlertaWorker {
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
}

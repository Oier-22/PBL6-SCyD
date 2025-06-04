package com.example;

import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public class EstabilidadWorker {
    private static final String EXCHANGE = "post_prediccion";
    private static final String RESPONSE_KEY = "alerta.estabilidad";

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
        
        factory.setPort(5671); // TLS
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
            if (partes.length == 2) {
                String id = partes[0].trim();
                System.out.println(" [Estabilidad] Analizando parcela " + id + "...");
                try {
                    Thread.sleep(10000);
                    System.out.println(" [Estabilidad] Análisis completo para parcela " + id + ".");

                    String resultado = id + ": plan para ajustar";
                    channel.basicPublish(EXCHANGE, RESPONSE_KEY, null, resultado.getBytes());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(queue, false, callback, consumerTag -> {});

        Thread.sleep(Long.MAX_VALUE);
    }
}

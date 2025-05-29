package com.example;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.nio.charset.StandardCharsets;

public class Publisher {
    private static final String EXCHANGE_NAME = "parcelas_direct";
    private static final String RESPONSE_EXCHANGE_NAME = "parcelas_response";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sistema_riego";
    private static final String DB_USER = "root";
    private static String dbPassword;
    

    public void enviarParcelas(int numSubscribers, String host, String username, String password) 
    throws TimeoutException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);

        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            channel.exchangeDeclare(RESPONSE_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

            List<Parcela> parcelas = cargarParcelasDesdeDB();
            List<List<Parcela>> grupos = dividirParcelas(parcelas, numSubscribers);

            CountDownLatch latch = new CountDownLatch(parcelas.size());

            String nombreColaRespuestas = channel.queueDeclare().getQueue();
            channel.queueBind(nombreColaRespuestas, RESPONSE_EXCHANGE_NAME, "");

            channel.basicConsume(nombreColaRespuestas, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                        AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String response = new String(body, StandardCharsets.UTF_8);
                    System.out.println(" [x] Respuesta recibida: " + response);

                    try (java.sql.Connection dbConn = DriverManager.getConnection(DB_URL, DB_USER, dbPassword)) {
                        String[] partes = response.split(":");
                        if (partes.length == 2) {
                            String id = partes[0].trim();
                            double consumo = Double.parseDouble(partes[1].trim());
                    
                            try (PreparedStatement stmt = dbConn.prepareStatement(
                                    "UPDATE Parcela SET consumoAgua = ? WHERE id = ?")) {
                                stmt.setDouble(1, consumo);
                                stmt.setString(2, id);
                                stmt.executeUpdate();
                            }
                    
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } else {
                            System.out.println("Formato incorrecto en la respuesta: " + response);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    

                    latch.countDown();
                }
            });

            for (int i = 0; i < grupos.size(); i++) {
                String routingKey = "subscriber" + (i + 1);
                byte[] message = serialize(grupos.get(i));
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message);
                System.out.println(" [x] Enviado a " + routingKey + ": " + grupos.get(i));
            }

            latch.await();
            System.out.println(" [x] Todas las respuestas recibidas y almacenadas. Finalizando.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static List<Parcela> cargarParcelasDesdeDB() {
        List<Parcela> parcelas = new ArrayList<>();
        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Parcela")) {

            while (rs.next()) {
                parcelas.add(new Parcela(
                    rs.getString("id"),
                    rs.getDouble("temperatura"),
                    rs.getDouble("humedad"),
                    rs.getDouble("viento"),
                    rs.getDouble("radiacion"),
                    rs.getDouble("precipitacion"),
                    rs.getString("tipoDePlanta"),
                    rs.getString("etapaCrecimiento"),
                    rs.getDouble("humedadSuelo"),
                    rs.getInt("diaDelAnio")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parcelas;
    }

    public static List<List<Parcela>> dividirParcelas(List<Parcela> parcelas, int numGrupos) {
        List<List<Parcela>> grupos = new ArrayList<>();
        for (int i = 0; i < numGrupos; i++) {
            grupos.add(new ArrayList<>());
        }
        for (int i = 0; i < parcelas.size(); i++) {
            grupos.get(i % numGrupos).add(parcelas.get(i));
        }
        return grupos;
    }

    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static void main(String[] args) throws Exception {
        InputStream input = Publisher.class.getResourceAsStream("/config.txt");

        Properties config = new Properties();
        config.load(input);
        dbPassword = config.getProperty("password");
        System.out.println(dbPassword);
        int numSubscribers = 1;
        String host = "192.168.73.245";
        String username = "testuser";
        String password = "testpassword";
    
        new Publisher().enviarParcelas(numSubscribers, host, username, password);
    }
    
}

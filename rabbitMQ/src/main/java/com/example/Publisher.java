package com.example;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;

public class Publisher {
    private static final String EXCHANGE_NAME = "parcelas_direct";
    private static final String RESPONSE_EXCHANGE_NAME = "parcelas_response";
    private static final String POST_ANALYSIS_EXCHANGE = "post_prediccion_x";
    private static String dbPassword = "root";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sistema_riego";
    private static final String DB_USER = "root";

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
            channel.exchangeDeclare(POST_ANALYSIS_EXCHANGE, BuiltinExchangeType.DIRECT);
            
            List<Parcela> parcelas = cargarParcelasDesdeDB();
            List<List<Parcela>> grupos = dividirParcelas(parcelas, numSubscribers);

            AtomicInteger respuestasRestantes = new AtomicInteger(parcelas.size());
            final Object lock = new Object();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable timeoutTask = () -> {
                System.out.println("⚠️ No se han recibido respuestas en los últimos 10 segundos. Posible fallo en el subscriber.");
                synchronized (lock) {
                    lock.notify();
                }
            };
            final ScheduledFuture<?>[] timeoutFuture = new ScheduledFuture<?>[1];
            timeoutFuture[0] = scheduler.schedule(timeoutTask, 10, TimeUnit.SECONDS);

            String nombreColaRespuestas = channel.queueDeclare().getQueue();
            channel.queueBind(nombreColaRespuestas, RESPONSE_EXCHANGE_NAME, "");

            channel.basicConsume(nombreColaRespuestas, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                        AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String response = new String(body, StandardCharsets.UTF_8);
                    System.out.println(" [x] Respuesta recibida: " + response);

                    timeoutFuture[0].cancel(false);
                    timeoutFuture[0] = scheduler.schedule(timeoutTask, 60, TimeUnit.SECONDS);

                    try (java.sql.Connection dbConn = DriverManager.getConnection(DB_URL, DB_USER, dbPassword)) {
                        String[] partes = response.split(":");
                        if (partes.length == 2) {
                            String id = partes[0].trim();
                            double consumo = Double.parseDouble(partes[1].trim());

                            if (consumo == 99999.0) {
                                System.out.println("⚠️ Valor anómalo detectado en parcela " + id);
                                Optional<Parcela> parcelaReenviar = parcelas.stream()
                                        .filter(p -> p.getId().equals(id))
                                        .findFirst();
                            
                                if (parcelaReenviar.isPresent()) {
                                    List<Parcela> listaReenvio = new ArrayList<>();
                                    listaReenvio.add(parcelaReenviar.get());
                                    byte[] mensaje = serialize(listaReenvio);
                                    channel.basicPublish(EXCHANGE_NAME, "subscriber1", null, mensaje);
                                    System.out.println("🔁 Parcela " + id + " reenviada para nuevo cálculo");
                                } else {
                                    System.out.println("❌ Parcela con ID " + id + " no encontrada para reenvío");
                                    if (respuestasRestantes.decrementAndGet() == 0) {
                                        synchronized (lock) {
                                            lock.notify();
                                        }
                                    }
                                }
                            
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }   

                            try (PreparedStatement stmt = dbConn.prepareStatement(
                                    "UPDATE Parcela SET consumoAgua = ? WHERE id = ?")) {
                                stmt.setDouble(1, consumo);
                                stmt.setString(2, id);
                                stmt.executeUpdate();
                            }

                            if (consumo > 26) {
                                channel.basicPublish(POST_ANALYSIS_EXCHANGE, "alerta.inicio", null, response.getBytes());
                                channel.basicPublish(POST_ANALYSIS_EXCHANGE, "estabilidad.parcela", null, response.getBytes());
                            } else {
                                channel.basicPublish(POST_ANALYSIS_EXCHANGE, "registro.normal", null, response.getBytes());
                            }

                            channel.basicAck(envelope.getDeliveryTag(), false);

                            if (respuestasRestantes.decrementAndGet() == 0) {
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }

                        } else {
                            System.out.println("Formato incorrecto en la respuesta: " + response);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            for (int i = 0; i < grupos.size(); i++) {
                String routingKey = "subscriber" + (i + 1);
                byte[] message = serialize(grupos.get(i));
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message);
                System.out.println(" [x] Enviado a " + routingKey + ": " + grupos.get(i));
            }

            synchronized (lock) {
                lock.wait();
            }

            scheduler.shutdownNow();

            if (respuestasRestantes.get() > 0) {
                System.out.println("⚠️ No se completaron todas las respuestas. Quedaron pendientes: " + respuestasRestantes.get());
            } else {
                System.out.println("✅ Todas las respuestas recibidas y almacenadas.");
            }

            System.out.println(" [x] Finalizando.");

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
        int numSubscribers = 1;
        String host = "localhost";
        String username = "testuser";
        String password = "testpassword";

        new Publisher().enviarParcelas(numSubscribers, host, username, password);
    }
}

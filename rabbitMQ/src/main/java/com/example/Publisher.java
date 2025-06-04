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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class Publisher {
    private static final String EXCHANGE_NAME = "parcelas_direct";
    private static final String RESPONSE_EXCHANGE_NAME = "parcelas_response";
    private static final String POST_ANALYSIS_EXCHANGE = "post_prediccion";
    private static String dbPassword = "root";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sistema_riego";
    private static final String DB_USER = "root";

    public void enviarParcelas(int numSubscribers, String host, String username, String password)
        throws TimeoutException, InterruptedException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyManagementException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        char[] truststorePassword = "changeit".toCharArray();
        KeyStore trustStore = KeyStore.getInstance("JKS");
        
        InputStream tsStream = Publisher.class.getClassLoader().getResourceAsStream("tls/truststore.jks");
        trustStore.load(tsStream, truststorePassword);
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        
        factory.setPort(5671); // Puerto TLS
        factory.useSslProtocol(sslContext);
        


        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            channel.exchangeDeclare(RESPONSE_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            channel.exchangeDeclare(POST_ANALYSIS_EXCHANGE, BuiltinExchangeType.TOPIC);

            List<Parcela> parcelas = cargarParcelasDesdeDB();
            List<List<Parcela>> grupos = dividirParcelas(parcelas, numSubscribers);

            Map<String, Parcela> pendientes = new HashMap<>();
            Map<String, String> idToRoutingKey = new HashMap<>();
            Map<String, Integer> intentosPorParcela = new HashMap<>();
            final int MAX_REINTENTOS = 3;

            for (Parcela p : parcelas) {
                pendientes.put(p.getId(), p);
            }

            AtomicInteger respuestasRestantes = new AtomicInteger(parcelas.size());
            final Object lock = new Object();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            final ScheduledFuture<?>[] timeoutFuture = new ScheduledFuture<?>[1];

            Runnable timeoutTask = new Runnable() {
                @Override
                public void run() {
                    System.out.println("⚠️ No se han recibido respuestas en los últimos 60 segundos. Reintentando parcelas pendientes...");
                    boolean hayPendientes = false;
                    for (Parcela parcela : new ArrayList<>(pendientes.values())) {
                        String id = parcela.getId();
                        String routingKey = idToRoutingKey.getOrDefault(id, "subscriber1");
                        int intentos = intentosPorParcela.getOrDefault(id, 0);
                        if (intentos < MAX_REINTENTOS) {
                            try {
                                byte[] mensaje = serialize(List.of(parcela));
                                channel.basicPublish(EXCHANGE_NAME, routingKey, null, mensaje);
                                intentosPorParcela.put(id, intentos + 1);
                                System.out.println("🔁 Reintentando parcela " + id + " (intento " + (intentos + 1) + ")");
                                hayPendientes = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("❌ Parcela " + id + " ha superado el número máximo de reintentos.");
                            pendientes.remove(id);
                            respuestasRestantes.decrementAndGet();
                        }
                    }
                    if (respuestasRestantes.get() == 0) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    } else if (hayPendientes) {
                        timeoutFuture[0] = scheduler.schedule(this, 120, TimeUnit.SECONDS);
                    }
                }
            };

            timeoutFuture[0] = scheduler.schedule(timeoutTask, 120, TimeUnit.SECONDS);

            String nombreColaRespuestas = channel.queueDeclare().getQueue();
            channel.queueBind(nombreColaRespuestas, RESPONSE_EXCHANGE_NAME, "");

            channel.basicConsume(nombreColaRespuestas, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                        AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String response = new String(body, StandardCharsets.UTF_8);
                    System.out.println(" [x] Respuesta recibida: " + response);

                    timeoutFuture[0].cancel(false);
                    timeoutFuture[0] = scheduler.schedule(timeoutTask, 120, TimeUnit.SECONDS);

                    try (java.sql.Connection dbConn = DriverManager.getConnection(DB_URL, DB_USER, dbPassword)) {
                        String[] partes = response.split(":");
                        if (partes.length == 2) {
                            String id = partes[0].trim();
                            double consumo = Double.parseDouble(partes[1].trim());

                            Parcela parcela = pendientes.get(id);
                            if (parcela == null) {
                                System.out.println("⚠️ Parcela " + id + " no está registrada como pendiente. Ignorando respuesta duplicada.");
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }

                            if (consumo > 30 || consumo < 0) {
                                System.out.println("⚠️ Valor anómalo detectado en parcela " + id);
                                int intentos = intentosPorParcela.getOrDefault(id, 0);
                                if (intentos < MAX_REINTENTOS) {
                                    String routingKey = idToRoutingKey.getOrDefault(id, "subscriber1");
                                    byte[] mensaje = serialize(List.of(parcela));
                                    channel.basicPublish(EXCHANGE_NAME, routingKey, null, mensaje);
                                    intentosPorParcela.put(id, intentos + 1);
                                    System.out.println("🔁 Parcela " + id + " reenviada para nuevo cálculo (intento " + (intentos + 1) + ")");
                                } else {
                                    System.out.println("❌ Parcela " + id + " ha superado el número máximo de reintentos.");
                                    pendientes.remove(id);
                                    respuestasRestantes.decrementAndGet();
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

                            pendientes.remove(id);
                            respuestasRestantes.decrementAndGet();
                            channel.basicAck(envelope.getDeliveryTag(), false);

                            if (respuestasRestantes.get() == 0) {
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
                List<Parcela> grupo = grupos.get(i);
                for (Parcela p : grupo) {
                    idToRoutingKey.put(p.getId(), routingKey);
                }
                byte[] message = serialize(grupo);
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message);
                System.out.println(" [x] Enviado a " + routingKey + ": " + grupo);
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

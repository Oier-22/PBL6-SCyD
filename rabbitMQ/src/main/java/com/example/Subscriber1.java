package com.example;

import com.example.paralelizacion.PrediccionExecutor;
import com.rabbitmq.client.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Subscriber1 {
    private static final String EXCHANGE_NAME = "parcelas_direct";
    private static final String RESPONSE_EXCHANGE_NAME = "parcelas_response";

    private static final String RABBIT_HOST;
    private static final String RABBIT_USER;
    private static final String RABBIT_PASS;
    private static final int RABBIT_PORT;
    private static final String TRUSTSTORE_PATH;
    private static final char[] TRUSTSTORE_PASSWORD;

    static {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("rabbitMQ/config/config.txt")) {
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.txt", e);
        }

        RABBIT_HOST = config.getProperty("rabbitmq.host");
        RABBIT_USER = config.getProperty("rabbitmq.username");
        RABBIT_PASS = config.getProperty("rabbitmq.password");
        RABBIT_PORT = Integer.parseInt(config.getProperty("rabbitmq.port"));
        TRUSTSTORE_PATH = config.getProperty("truststore.path");
        TRUSTSTORE_PASSWORD = config.getProperty("truststore.password").toCharArray();
    }

    public void recibirParcelas(String routingKey) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBIT_HOST);
        factory.setUsername(RABBIT_USER);
        factory.setPassword(RABBIT_PASS);

        try {
            // 🔐 Configurar conexión TLS
            KeyStore trustStore = KeyStore.getInstance("JKS");
            InputStream tsStream = new FileInputStream(TRUSTSTORE_PATH);
            trustStore.load(tsStream, TRUSTSTORE_PASSWORD);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            factory.setPort(RABBIT_PORT);
            factory.useSslProtocol(sslContext);

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.exchangeDeclare(EXCHANGE_NAME, "direct");
                channel.exchangeDeclare(RESPONSE_EXCHANGE_NAME, "direct");

                String nombreCola = channel.queueDeclare().getQueue();
                channel.queueBind(nombreCola, EXCHANGE_NAME, routingKey);

                System.out.println(" [*] Esperando mensajes para " + routingKey + ". Para salir presione CTRL+C");

                MiConsumer consumer = new MiConsumer(channel);
                channel.basicQos(1);
                channel.basicConsume(nombreCola, false, consumer);

                synchronized (this) {
                    wait();
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error en la conexión TLS o configuración del TrustStore");
            e.printStackTrace();
        }
    }

    class MiConsumer extends DefaultConsumer {
        private final Channel channel;

        public MiConsumer(Channel channel) {
            super(channel);
            this.channel = channel;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
            try {
                List<Parcela> parcelas = Subscriber1.deserialize(body);

                List<PrediccionExecutor.JsonConId> datosList = new ArrayList<>();
                for (Parcela parcela : parcelas) {
                    String id = parcela.getId();
                    String jsonData = generateJsonForParcela(parcela);
                    System.out.println("[Subscriber1] Parcela ID: " + id);
                    System.out.println("[Subscriber1] JSON generado: " + jsonData);
                    datosList.add(new PrediccionExecutor.JsonConId(id, jsonData));
                }
                PrediccionExecutor.runWithDatosList(datosList, channel);

                channel.basicAck(envelope.getDeliveryTag(), false);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String generateJsonForParcela(Parcela parcela) {
            return String.format(Locale.US,
                    "{" +
                            "\"temp\": %.2f," +
                            "\"humedad\": %.2f," +
                            "\"viento\": %.2f," +
                            "\"radiacion\": %.2f," +
                            "\"precipitacion\": %.2f," +
                            "\"tipo_planta\": %d," +
                            "\"etapa_crecimiento\": %d," +
                            "\"tipo_suelo\": %d," +
                            "\"humedad_suelo\": %.2f," +
                            "\"dia_del_ano\": %d" +
                            "}",
                    parcela.getTemperatura(),
                    parcela.getHumedad(),
                    parcela.getViento(),
                    parcela.getRadiacion(),
                    parcela.getPrecipitacion(),
                    mapTipoPlantaToNumeric(parcela.getTipoDePlanta()),
                    mapEtapaCrecimientoToNumeric(parcela.getEtapaCrecimiento()),
                    0,
                    parcela.getHumedadSuelo(),
                    parcela.getDiaDelAnio()
            );
        }

        private int mapTipoPlantaToNumeric(String tipoPlanta) {
            switch (tipoPlanta) {
                case "Tomate": return 1;
                case "Lechuga": return 2;
                case "Pepino": return 3;
                case "Zanahoria": return 4;
                default: return 0;
            }
        }

        private int mapEtapaCrecimientoToNumeric(String etapaCrecimiento) {
            switch (etapaCrecimiento) {
                case "Germinación": return 1;
                case "Crecimiento": return 2;
                case "Floración": return 3;
                case "Madurez": return 4;
                default: return 0;
            }
        }
    }

    public static List<Parcela> deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (List<Parcela>) objectInputStream.readObject();
        }
    }

    public static void main(String[] args) throws Exception {
        String routingKey = "subscriber1";
        new Subscriber1().recibirParcelas(routingKey);
    }
}
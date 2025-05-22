package com.example;

import com.example.paralelizacion.PrediccionExecutor;
import com.rabbitmq.client.*;
import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class Subscriber1 {
    private final static String EXCHANGE_NAME = "parcelas_direct";
    private final static String RESPONSE_EXCHANGE_NAME = "parcelas_response";

    public Subscriber1(String routingKey) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
    }

    public void recibirParcelas(String routingKey) throws TimeoutException {
        try (Connection connection = new ConnectionFactory().newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            channel.exchangeDeclare(RESPONSE_EXCHANGE_NAME, "direct");

            String nombreCola = channel.queueDeclare().getQueue();
            channel.queueBind(nombreCola, EXCHANGE_NAME, routingKey);

            System.out.println(" [*] Esperando mensajes para " + routingKey + ". Para salir presione CTRL+C");

            MiConsumer consumer = new MiConsumer(channel, routingKey);
            channel.basicQos(1);  // Solo 1 mensaje a la vez
            channel.basicConsume(nombreCola, false, consumer); // Ack manual
            
            synchronized (this) {
                wait();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    class MiConsumer extends DefaultConsumer {
        private final Channel channel;
        private final String routingKey;

        public MiConsumer(Channel channel, String routingKey) {
            super(channel);
            this.channel = channel;
            this.routingKey = routingKey;
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

            } catch (Exception e) {
                e.printStackTrace();
            }
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

        private String generateJsonForParcela(Parcela parcela) {
                return String.format(Locale.US,  // <---- clave aquí
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
    }

    public static List<Parcela> deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (List<Parcela>) objectInputStream.readObject();
        }
    }

    public static void main(String[] args) throws TimeoutException {
        Subscriber1 subscriber = new Subscriber1("subscriber1");
        subscriber.recibirParcelas("subscriber1");
    }
}

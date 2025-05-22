package com.example;

import com.rabbitmq.client.*;
import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Subscriber1 {
    private final static String EXCHANGE_NAME = "parcelas_direct";  // direct exchange
    private final static String RESPONSE_EXCHANGE_NAME = "parcelas_response";  // direct exchange para respuestas

    public Subscriber1(String routingKey) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
    }

    public void recibirParcelas(String routingKey) throws TimeoutException {
        try (Connection connection = new ConnectionFactory().newConnection();
             Channel channel = connection.createChannel()) {
            // Declarar el exchange de tipo direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            channel.exchangeDeclare(RESPONSE_EXCHANGE_NAME, "direct");

            // Crear una cola exclusiva para este subscriber
            String nombreCola = channel.queueDeclare().getQueue();

            // Vincular la cola al exchange con el routingKey específico
            channel.queueBind(nombreCola, EXCHANGE_NAME, routingKey);

            System.out.println(" [*] Esperando mensajes para " + routingKey + ". Para salir presione CTRL+C");

            // Definir el consumidor
            MiConsumer consumer = new MiConsumer(channel, routingKey);
            // Consumir los mensajes
            channel.basicConsume(nombreCola, true, consumer);

            // Mantener el programa ejecutándose para esperar indefinidamente
            synchronized (this) {
                wait();  // Espera indefinida hasta que se reciba una señal para continuar o finalizar
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
                System.out.println("Parcelas deserializadas: " + parcelas);
        
                Random rand = new Random();
        
                // Responder individualmente por cada parcela
                for (Parcela parcela : parcelas) {
                    int resultado = rand.nextInt(100);
                    String respuesta = "Resultado para parcela ID=" + parcela.getId() + ": " + resultado;
                    channel.basicPublish(RESPONSE_EXCHANGE_NAME, "", null, respuesta.getBytes());
                    System.out.println(" [x] Respuesta enviada: " + respuesta);
                }
        
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

    public static void main(String[] args) throws TimeoutException {
        // Crear suscriptores con diferentes routing keys
        Subscriber1 subscriber = new Subscriber1("subscriber1");
        subscriber.recibirParcelas("subscriber1");

        // Si quisieras agregar otro suscriptor, también lo harías aquí:
        // Subscriber subscriber2 = new Subscriber("subscriber2");
        // subscriber2.recibirParcelas("subscriber2");
    }

    // Función para deserializar el mensaje
    public static List<Parcela> deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (List<Parcela>) objectInputStream.readObject();
        }
    }
}

package com.samuel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsumidorSuporte {

    private static final String EXCHANGE = "mensagens.topic";
    private static final String ROUTING_KEY = "support.request";
    private static final String FILA = "fila.suporte";

    private static final String DLX = "mensagens.dlx";
    private static final String FILA_DLQ = "fila.suporte.dlq";

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, "topic", true);

        // Cada consumidor tem sua própria DLQ, pra não misturar erro de um tipo de mensagem com o outro
        channel.exchangeDeclare(DLX, "fanout", true);
        channel.queueDeclare(FILA_DLQ, true, false, false, null);
        channel.queueBind(FILA_DLQ, DLX, "");

        Map<String, Object> argsFila = new HashMap<>();
        argsFila.put("x-dead-letter-exchange", DLX);

        channel.queueDeclare(FILA, true, false, false, argsFila);
        channel.queueBind(FILA, EXCHANGE, ROUTING_KEY);

        channel.basicQos(1);

        System.out.println("Consumidor de suporte aguardando mensagens...");

        DeliverCallback callback = (consumerTag, delivery) -> {
            try {
                Mensagem mensagem = mapper.readValue(delivery.getBody(), Mensagem.class);

                TimeUnit.MILLISECONDS.sleep(800);

                String categoria = classificarProvisorio(mensagem.getConteudo());
                System.out.println("[" + mensagem.getId() + "] \"" + mensagem.getConteudo()
                        + "\" -> " + categoria);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            } catch (Exception e) {
                System.err.println("Erro ao processar mensagem, enviando para DLQ: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };

        channel.basicConsume(FILA, false, callback, consumerTag -> {});
    }

    // Placeholder - próximo passo substituímos pela classificação real com Smile
    private static String classificarProvisorio(String conteudo) {
        return "(classificação pendente)";
    }
}
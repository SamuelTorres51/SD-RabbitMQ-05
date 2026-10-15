package com.samuel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsumidorSentimento {

    private static final String EXCHANGE = "mensagens.topic";
    private static final String ROUTING_KEY = "review.product";
    private static final String FILA = "fila.avaliacoes";

    private static final String DLX = "mensagens.dlx";
    private static final String FILA_DLQ = "fila.avaliacoes.dlq";

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ClassificadorTexto classificador = criarClassificadorSentimento();
        mapper.registerModule(new JavaTimeModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Exchange principal (idempotente - se já existir com a mesma config, não faz nada)
        channel.exchangeDeclare(EXCHANGE, "topic", true);

        // --- Dead Letter Exchange + fila de erro ---
        channel.exchangeDeclare(DLX, "fanout", true);
        channel.queueDeclare(FILA_DLQ, true, false, false, null);
        channel.queueBind(FILA_DLQ, DLX, "");

        // --- Fila principal deste consumidor, já apontando pra DLQ em caso de erro ---
        Map<String, Object> argsFila = new HashMap<>();
        argsFila.put("x-dead-letter-exchange", DLX);

        channel.queueDeclare(FILA, true, false, false, argsFila);
        channel.queueBind(FILA, EXCHANGE, ROUTING_KEY);

        // Processa uma mensagem de cada vez antes de pedir a próxima (bom pra ver a fila crescer)
        channel.basicQos(1);

        System.out.println("Consumidor de sentimento aguardando mensagens...");

        DeliverCallback callback = (consumerTag, delivery) -> {
            try {
                Mensagem mensagem = mapper.readValue(delivery.getBody(), Mensagem.class);

                // Simula processamento mais lento que a chegada das mensagens,
                // de propósito, pra fila crescer visivelmente na interface do RabbitMQ.
                TimeUnit.MILLISECONDS.sleep(800);

                String sentimento = classificador.classificar(mensagem.getConteudo());
                System.out.println("[" + mensagem.getId() + "] \"" + mensagem.getConteudo()
                        + "\" -> " + sentimento);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            } catch (Exception e) {
                System.err.println("Erro ao processar mensagem, enviando para DLQ: " + e.getMessage());
                // requeue = false -> não volta pra fila original, vai pro DLX configurado acima
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };

        channel.basicConsume(FILA, false, callback, consumerTag -> {});
    }

    private static ClassificadorTexto criarClassificadorSentimento() {
        Map<String, List<String>> exemplos = new HashMap<>();

        exemplos.put("positivo", List.of(
                "Produto excelente, superou minhas expectativas",
                "Entrega rápida e produto de ótima qualidade",
                "Adorei o produto, vale cada centavo",
                "Super recomendo, qualidade excelente",
                "Fiquei muito satisfeito com a compra",
                "Ótimo atendimento e produto de qualidade"
        ));

        exemplos.put("negativo", List.of(
                "Não gostei, o produto veio com defeito",
                "Péssima experiência, não recomendo",
                "Produto horrível, não funciona direito",
                "Decepcionado com a compra, não vale o preço",
                "Qualidade muito ruim, não recomendo a ninguém",
                "Chegou quebrado, péssimo serviço"
        ));

        exemplos.put("neutro", List.of(
                "O produto é razoável, cumpre o que promete",
                "Nada de especial, mas também não é ruim",
                "Produto dentro do esperado, nada excepcional",
                "Entrega dentro do prazo, produto comum",
                "É ok, atende a necessidade básica",
                "Produto mediano, sem grandes destaques"
        ));

        return new ClassificadorTexto(exemplos);
    }
}
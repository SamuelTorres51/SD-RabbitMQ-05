package com.samuel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsumidorSuporte {

    private static final String EXCHANGE = "mensagens.topic";
    private static final String ROUTING_KEY = "support.request";
    private static final String FILA = "fila.suporte";

    private static final String DLX = "mensagens.dlx.suporte";
    private static final String FILA_DLQ = "fila.suporte.dlq";

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ClassificadorTexto classificador = criarClassificadorSuporte();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
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

                String suporte = classificador.classificar(mensagem.getConteudo());
                System.out.println("[" + mensagem.getId() + "] \"" + mensagem.getConteudo()
                        + "\" -> " + suporte);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            } catch (Exception e) {
                System.err.println("Erro ao processar mensagem, enviando para DLQ: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };

        channel.basicConsume(FILA, false, callback, consumerTag -> {});
    }

    private static ClassificadorTexto criarClassificadorSuporte() {
        Map<String, List<String>> exemplos = new HashMap<>();

        exemplos.put("Problema de acesso", List.of(
                "Não consigo fazer login na minha conta",
                "Esqueci minha senha e o link de recuperação não chega",
                "Minha conta foi bloqueada sem motivo aparente",
                "Não consigo redefinir minha senha",
                "O sistema não reconhece meu usuário",
                "Erro de autenticação ao tentar entrar"
        ));

        exemplos.put("Problema de desempenho", List.of(
                "O sistema está extremamente lento para carregar as páginas",
                "O aplicativo trava ao abrir o relatório mensal",
                "A tela fica congelada por vários segundos",
                "O carregamento dos dados demora demais",
                "O sistema trava constantemente durante o uso",
                "A resposta do servidor está muito demorada"
        ));

        exemplos.put("Problema de conexão", List.of(
                "A conexão cai constantemente durante o uso",
                "Não consigo me conectar ao servidor, erro de timeout",
                "A internet cai toda hora durante o uso do app",
                "Erro de conexão ao tentar sincronizar os dados",
                "Perco a conexão com o servidor com frequência",
                "Falha de rede impede o acesso ao sistema"
        ));

        return new ClassificadorTexto(exemplos);
    }
}
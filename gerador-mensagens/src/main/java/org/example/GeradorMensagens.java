package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GeradorMensagens {

    private static final String EXCHANGE = "mensagens.topic";
    private static final String ROUTING_KEY_AVALIACAO = "review.product";
    private static final String ROUTING_KEY_SUPORTE = "support.request";

    private static final List<String> AVALIACOES = List.of(
            "Produto excelente, superou minhas expectativas!",
            "Entrega rápida e produto de ótima qualidade.",
            "Não gostei, o produto veio com defeito.",
            "Péssima experiência, não recomendo.",
            "O produto é razoável, cumpre o que promete.",
            "Nada de especial, mas também não é ruim."
    );

    private static final List<String> SUPORTE = List.of(
            "Não consigo fazer login na minha conta.",
            "Esqueci minha senha e o link de recuperação não chega.",
            "O sistema está extremamente lento para carregar as páginas.",
            "O aplicativo trava ao abrir o relatório mensal.",
            "A conexão cai constantemente durante o uso.",
            "Não consigo me conectar ao servidor, erro de timeout."
    );

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq"); // dentro do docker-compose isso vira o nome do serviço "rabbitmq"
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE, "topic", true);

            Random random = new Random();
            System.out.println("Gerador iniciado. Publicando mensagens continuamente...");

            while (true) {
                boolean ehAvaliacao = random.nextBoolean();
                Mensagem mensagem;
                String routingKey;

                if (ehAvaliacao) {
                    String conteudo = AVALIACOES.get(random.nextInt(AVALIACOES.size()));
                    mensagem = Mensagem.criar(Mensagem.Tipo.AVALIACAO_PRODUTO, conteudo);
                    routingKey = ROUTING_KEY_AVALIACAO;
                } else {
                    String conteudo = SUPORTE.get(random.nextInt(SUPORTE.size()));
                    mensagem = Mensagem.criar(Mensagem.Tipo.SOLICITACAO_SUPORTE, conteudo);
                    routingKey = ROUTING_KEY_SUPORTE;
                }

                byte[] corpo = mapper.writeValueAsBytes(mensagem);
                channel.basicPublish(EXCHANGE, routingKey, null, corpo);

                System.out.println("Publicada: " + routingKey + " -> " + mensagem.getConteudo());

                TimeUnit.MILLISECONDS.sleep(150); // ~6-7 mensagens/segundo, acima do mínimo de 5
            }
        }
    }
}

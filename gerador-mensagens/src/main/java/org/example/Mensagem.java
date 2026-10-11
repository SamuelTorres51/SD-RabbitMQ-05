package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class Mensagem {

    // Enum = uma lista fechada de valores válidos. Evita erro de digitação
    // tipo "avaliacao" vs "Avaliacao" vs "AVALIACAO".
    public enum Tipo {
        AVALIACAO_PRODUTO,
        SOLICITACAO_SUPORTE
    }

    private final String id;
    private final LocalDateTime dataHora;
    private final Tipo tipo;
    private final String conteudo;

    @JsonCreator
    public Mensagem(
            @JsonProperty("id") String id,
            @JsonProperty("dataHora") LocalDateTime dataHora,
            @JsonProperty("tipo") Tipo tipo,
            @JsonProperty("conteudo") String conteudo) {
        this.id = id;
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.conteudo = conteudo;
    }

    public static Mensagem criar(Tipo tipo, String conteudo) {
        return new Mensagem(UUID.randomUUID().toString(), LocalDateTime.now(), tipo, conteudo);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getConteudo() {
        return conteudo;
    }
}

package com.cadernosegredos.model;

import java.time.LocalDateTime; // Importe LocalDateTime

public class Log {
    private String id; // ID para o MongoDB
    private String tipo; // Ex: INFO, WARN, ERROR
    private String acao; // Ex: Pessoa criada, Pessoa buscada
    private String detalhes; // Informações adicionais
    private LocalDateTime timestamp; // Data e hora do log

    // Construtor que você está tentando usar
    public Log(String tipo, String acao, String detalhes) {
        this.tipo = tipo;
        this.acao = acao;
        this.detalhes = detalhes;
        this.timestamp = LocalDateTime.now(); // Define o timestamp automaticamente
    }

    // Construtor vazio para desserialização (se necessário para frameworks)
    public Log() {
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Log{" +
               "id='" + id + '\'' +
               ", tipo='" + tipo + '\'' +
               ", acao='" + acao + '\'' +
               ", detalhes='" + detalhes + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
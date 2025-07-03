package com.cadernosegredos.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID; // Importe UUID

public class Pessoa {
    private UUID id; // <--- MUDANÇA CRÍTICA: Tipo UUID
    private String nome;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;

    // Construtor padrão (pode ser removido se o ID for sempre gerado pelo BD)
    public Pessoa() {
        this.id = null; // O ID será populado após salvar no PostgreSQL
    }

    // Construtor completo (para ler do banco ou para testes com ID pré-definido)
    public Pessoa(UUID id, String nome, String email, String cpf, LocalDate dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
    }

    // Construtor para criação de nova Pessoa (ID será null e gerado pelo BD)
    public Pessoa(String nome, String email, String cpf, LocalDate dataNascimento) {
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.id = null; // O ID será populado após salvar no PostgreSQL
    }

    // --- GETTERS ---
    public UUID getId() { // Retorna UUID
        return id;
    }

    public String getNome() { // <--- ESTE MÉTODO É NECESSÁRIO
        return nome;
    }

    public String getEmail() { // <--- ESTE MÉTODO É NECESSÁRIO
        return email;
    }

    public String getCpf() { // <--- ESTE MÉTODO É NECESSÁRIO
        return cpf;
    }

    public LocalDate getDataNascimento() { // <--- ESTE MÉTODO É NECESSÁRIO
        return dataNascimento;
    }

    // --- SETTERS ---
    public void setId(UUID id) { // Recebe UUID
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEmail(String email) { // <--- ESTE MÉTODO É NECESSÁRIO
        this.email = email;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    @Override
    public String toString() {
        return "Pessoa{" +
               "id=" + id +
               ", nome='" + nome + '\'' +
               ", email='" + email + '\'' +
               ", cpf='" + cpf + '\'' +
               ", dataNascimento=" + dataNascimento +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pessoa pessoa = (Pessoa) o;
        return Objects.equals(id, pessoa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
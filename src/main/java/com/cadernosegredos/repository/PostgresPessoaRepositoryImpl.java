package com.cadernosegredos.repository;

import com.cadernosegredos.config.PostgresConfig;
import com.cadernosegredos.model.Log;
import com.cadernosegredos.model.Pessoa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Importe UUID

public class PostgresPessoaRepositoryImpl implements PessoaRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostgresPessoaRepositoryImpl.class);
    private final MongoLogRepositoryImpl logRepository = new MongoLogRepositoryImpl();

    @Override
    public Pessoa save(Pessoa pessoa) {
        String sql = "INSERT INTO pessoas (nome, email, cpf, dataNascimento) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = PostgresConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // Statement.RETURN_GENERATED_KEYS é geralmente implícito com RETURNING no PostgreSQL
            pstmt.setString(1, pessoa.getNome());
            pstmt.setString(2, pessoa.getEmail());
            pstmt.setString(3, pessoa.getCpf());
            pstmt.setDate(4, Date.valueOf(pessoa.getDataNascimento()));

            ResultSet rs = pstmt.executeQuery(); // Use executeQuery para RETURNING
            if (rs.next()) {
                UUID generatedId = (UUID) rs.getObject("id"); // Obtém o UUID gerado
                pessoa.setId(generatedId); // Define o ID na sua Pessoa
                logger.info("Pessoa salva no PostgreSQL com ID: {}", generatedId);
                logRepository.saveLog(new Log("INFO", "Pessoa salva no PostgreSQL", "ID: " + generatedId + ", Nome: " + pessoa.getNome()));
                return pessoa;
            } else {
                logger.error("Falha ao obter ID gerado para a pessoa: {}", pessoa.getNome());
                logRepository.saveLog(new Log("ERROR", "Falha ao salvar pessoa no PostgreSQL", "Nenhum ID retornado para " + pessoa.getNome()));
                return null;
            }
        } catch (SQLException e) {
            logger.error("Erro ao salvar pessoa no PostgreSQL: {}", e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao salvar pessoa", e.getMessage()));
            return null;
        }
    }

    @Override
    public Optional<Pessoa> findById(UUID id) { // <--- Mude para UUID
        String sql = "SELECT id, nome, email, cpf, dataNascimento FROM pessoas WHERE id = ?";
        try (Connection conn = PostgresConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id); // Use setObject para UUID
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToPessoa(rs));
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar pessoa por ID {} no PostgreSQL: {}", id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao buscar pessoa por ID", "ID: " + id + ", Erro: " + e.getMessage()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pessoa> findByCpf(String cpf) {
        String sql = "SELECT id, nome, email, cpf, dataNascimento FROM pessoas WHERE cpf = ?";
        try (Connection conn = PostgresConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToPessoa(rs));
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar pessoa por CPF {} no PostgreSQL: {}", cpf, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao buscar pessoa por CPF", "CPF: " + cpf + ", Erro: " + e.getMessage()));
        }
        return Optional.empty();
    }

    @Override
    public Pessoa update(Pessoa pessoa) {
        String sql = "UPDATE pessoas SET nome = ?, email = ?, cpf = ?, dataNascimento = ? WHERE id = ?";
        try (Connection conn = PostgresConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pessoa.getNome());
            pstmt.setString(2, pessoa.getEmail());
            pstmt.setString(3, pessoa.getCpf());
            pstmt.setDate(4, Date.valueOf(pessoa.getDataNascimento()));
            pstmt.setObject(5, pessoa.getId()); // Use setObject para UUID
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Pessoa atualizada no PostgreSQL com ID: {}", pessoa.getId());
                logRepository.saveLog(new Log("INFO", "Pessoa atualizada no PostgreSQL", "ID: " + pessoa.getId()));
                return pessoa;
            }
        } catch (SQLException e) {
            logger.error("Erro ao atualizar pessoa com ID {} no PostgreSQL: {}", pessoa.getId(), e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao atualizar pessoa", "ID: " + pessoa.getId() + ", Erro: " + e.getMessage()));
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) { // <--- Mude para UUID e retorne boolean
        String sql = "DELETE FROM pessoas WHERE id = ?";
        try (Connection conn = PostgresConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id); // Use setObject para UUID
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Pessoa deletada do PostgreSQL com ID: {}", id);
                logRepository.saveLog(new Log("INFO", "Pessoa deletada do PostgreSQL", "ID: " + id));
                return true;
            }
        } catch (SQLException e) {
            logger.error("Erro ao deletar pessoa com ID {} do PostgreSQL: {}", id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao deletar pessoa", "ID: " + id + ", Erro: " + e.getMessage()));
        }
        return false;
    }

    @Override
    public List<Pessoa> findAll() {
        String sql = "SELECT id, nome, email, cpf, dataNascimento FROM pessoas";
        List<Pessoa> pessoas = new ArrayList<>();
        try (Connection conn = PostgresConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pessoas.add(mapResultSetToPessoa(rs));
            }
        } catch (SQLException e) {
            logger.error("Erro ao listar todas as pessoas do PostgreSQL: {}", e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro SQL ao listar todas as pessoas", e.getMessage()));
        }
        return pessoas;
    }

    private Pessoa mapResultSetToPessoa(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id"); // Cast para UUID
        String nome = rs.getString("nome");
        String email = rs.getString("email");
        String cpf = rs.getString("cpf");
        LocalDate dataNascimento = rs.getDate("dataNascimento").toLocalDate();
        return new Pessoa(id, nome, email, cpf, dataNascimento);
    }
}
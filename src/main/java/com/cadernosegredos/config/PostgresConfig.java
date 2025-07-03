package com.cadernosegredos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresConfig {
    private static final Logger logger = LoggerFactory.getLogger(PostgresConfig.class);

    private static final String URL = "jdbc:postgresql://localhost:5432/cadernosegredos_db";
    private static final String USER = "cadernosegredos_user";
    private static final String PASSWORD = "102030";

    private static Connection connection; // Conexão única para facilitar fechamento

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                logger.info("Conexão com PostgreSQL estabelecida com sucesso.");
            } catch (SQLException e) {
                logger.error("Erro ao conectar ao PostgreSQL: {}", e.getMessage());
            } catch (ClassNotFoundException e) {
                logger.error("Driver JDBC do PostgreSQL não encontrado: {}", e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Conexão com PostgreSQL fechada.");
            } catch (SQLException e) {
                logger.error("Erro ao fechar conexão com PostgreSQL: {}", e.getMessage());
            }
        }
    }

    // Método opcional para compatibilidade com chamada antiga
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                logger.info("Conexão com PostgreSQL fechada (manual).");
            } catch (SQLException e) {
                logger.error("Erro ao fechar conexão com PostgreSQL: {}", e.getMessage());
            }
        }
    }
}

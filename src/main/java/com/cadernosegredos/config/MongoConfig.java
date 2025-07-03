package com.cadernosegredos.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoConfig {
    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "cadernosegredos_mongo_db";

    public static MongoClient getMongoClient() {
        MongoClient mongoClient = null;
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            mongoClient.listDatabaseNames().first(); // Testa a conexão listando os nomes dos bancos
            logger.info("Conexão com MongoDB estabelecida com sucesso.");
        } catch (Exception e) {
            logger.error("Erro ao conectar ao MongoDB: {}", e.getMessage());
            if (mongoClient != null) {
                mongoClient.close(); // Garante que a conexão seja fechada em caso de erro
            }
            mongoClient = null;
        }
        return mongoClient;
    }

    public static MongoDatabase getMongoDatabase(MongoClient mongoClient) {
        if (mongoClient != null) {
            return mongoClient.getDatabase(DATABASE_NAME);
        }
        return null;
    }

    public static void closeMongoClient(MongoClient mongoClient) {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("Conexão com MongoDB fechada.");
            } catch (Exception e) {
                logger.error("Erro ao fechar conexão com MongoDB: {}", e.getMessage());
            }
        }
    }
}
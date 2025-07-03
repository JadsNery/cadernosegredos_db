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

    private static MongoClient mongoClient; // instância reutilizável

    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            try {
                mongoClient = MongoClients.create(CONNECTION_STRING);
                mongoClient.listDatabaseNames().first(); // Testa conexão
                logger.info("Conexão com MongoDB estabelecida com sucesso.");
            } catch (Exception e) {
                logger.error("Erro ao conectar ao MongoDB: {}", e.getMessage());
                if (mongoClient != null) {
                    mongoClient.close();
                }
                mongoClient = null;
            }
        }
        return mongoClient;
    }

    public static MongoDatabase getMongoDatabase() {
        if (mongoClient != null) {
            return mongoClient.getDatabase(DATABASE_NAME);
        }
        return null;
    }

    public static void closeMongoClient() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("Conexão com MongoDB fechada.");
            } catch (Exception e) {
                logger.error("Erro ao fechar conexão com MongoDB: {}", e.getMessage());
            }
        }
    }

    // Versão com argumento ainda mantida para compatibilidade, se necessário
    public static void closeMongoClient(MongoClient externalClient) {
        if (externalClient != null) {
            try {
                externalClient.close();
                logger.info("Conexão externa com MongoDB fechada.");
            } catch (Exception e) {
                logger.error("Erro ao fechar conexão externa com MongoDB: {}", e.getMessage());
            }
        }
    }
}

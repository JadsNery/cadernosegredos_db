package com.cadernosegredos.repository;

import com.cadernosegredos.config.MongoConfig;
import com.cadernosegredos.model.Log;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime; // Importe para LocalDateTime
import java.util.ArrayList;     // Importe para ArrayList
import java.util.List;          // Importe para List
import java.util.Date;          // Importe para java.util.Date (necessário para conversão)
import java.sql.Timestamp;      // Importe para java.sql.Timestamp (necessário para conversão)


public class MongoLogRepositoryImpl {
    private static final Logger logger = LoggerFactory.getLogger(MongoLogRepositoryImpl.class);
    private MongoCollection<Document> logCollection;

    public MongoLogRepositoryImpl() {
        MongoDatabase database = MongoConfig.getMongoClient().getDatabase("cadernosegredos_mongo_db");
        this.logCollection = database.getCollection("logs");
    }

    public void saveLog(Log log) { // Nome do método é saveLog, não save
        try {
            Document doc = new Document("tipo", log.getTipo())
                            .append("acao", log.getAcao())
                            .append("detalhes", log.getDetalhes())
                            .append("timestamp", log.getTimestamp());

            logCollection.insertOne(doc);
            logger.info("Log registrado no MongoDB: Tipo={}, Ação='{}'", log.getTipo(), log.getAcao());
        } catch (Exception e) {
            logger.error("Erro ao salvar log no MongoDB: {}", e.getMessage());
        }
    }

    public List<Log> findAllLogs() { // Este é o método que o App.java tenta chamar
        List<Log> logs = new ArrayList<>();
        try {
            for (Document doc : logCollection.find()) {
                Log log = new Log();
                // O ID do MongoDB é um ObjectId, convertemos para String
                if (doc.containsKey("_id") && doc.get("_id") != null) {
                    log.setId(doc.getObjectId("_id").toString());
                }
                if (doc.containsKey("tipo")) {
                    log.setTipo(doc.getString("tipo"));
                }
                if (doc.containsKey("acao")) {
                    log.setAcao(doc.getString("acao"));
                }
                if (doc.containsKey("detalhes")) {
                    log.setDetalhes(doc.getString("detalhes"));
                }
                // Lida com a conversão de timestamp (pode ser Date do Mongo ou LocalDateTime)
                if (doc.containsKey("timestamp") && doc.get("timestamp") instanceof Date) {
                    Date date = (Date) doc.get("timestamp");
                    log.setTimestamp(new Timestamp(date.getTime()).toLocalDateTime());
                } else if (doc.containsKey("timestamp") && doc.get("timestamp") instanceof LocalDateTime) {
                    log.setTimestamp((LocalDateTime) doc.get("timestamp"));
                }
                logs.add(log);
            }
            logger.info("Total de {} logs encontrados no MongoDB.", logs.size());
        } catch (Exception e) {
            logger.error("Erro ao buscar todos os logs do MongoDB: {}", e.getMessage());
        }
        return logs;
    }
}
package com.cadernosegredos.repository;

import com.cadernosegredos.config.Neo4jConfig;
import com.cadernosegredos.model.Log;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

public class Neo4jRelationshipRepositoryImpl {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jRelationshipRepositoryImpl.class);
    private final Driver driver;
    private final MongoLogRepositoryImpl logRepository = new MongoLogRepositoryImpl();

    public Neo4jRelationshipRepositoryImpl() {
        this.driver = Neo4jConfig.getDriver();
        logger.info("Neo4jRelationshipRepositoryImpl inicializado e driver obtido.");
    }

    public void createFriendship(UUID person1Id, UUID person2Id) {
        String query = "MERGE (p1:Person {id: $person1Id}) " +
                       "MERGE (p2:Person {id: $person2Id}) " +
                       "MERGE (p1)-[:FRIENDS_WITH]->(p2)";
        try (Session session = driver.session()) {
            session.run(query, parameters("person1Id", person1Id.toString(), "person2Id", person2Id.toString()));
            logger.info("Amizade criada entre pessoas com IDs: {} e {}", person1Id, person2Id);
            logRepository.saveLog(new Log("INFO", "Amizade Neo4j criada", "Entre " + person1Id + " e " + person2Id));
        } catch (Neo4jException e) {
            logger.error("Erro Neo4j ao criar amizade entre {} e {}: {}", person1Id, person2Id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro Neo4j", "Falha ao criar amizade: " + e.getMessage()));
        }
    }

    public List<UUID> findFriends(UUID personId) {
        String query = "MATCH (p:Person {id: $personId})-[:FRIENDS_WITH]->(f:Person) RETURN f.id AS friendId";
        try (Session session = driver.session()) {
            Result result = session.run(query, parameters("personId", personId.toString()));
            List<UUID> friendIds = result.stream()
                                         .map(record -> UUID.fromString(record.get("friendId").asString()))
                                         .collect(Collectors.toList());
            logger.info("Encontrados {} amigos para a pessoa com ID: {}", friendIds.size(), personId);
            logRepository.saveLog(new Log("INFO", "Amigos Neo4j listados", "Para " + personId + ": " + friendIds.size() + " amigos."));
            return friendIds;
        } catch (Neo4jException e) {
            logger.error("Erro Neo4j ao buscar amigos para {}: {}", personId, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro Neo4j", "Falha ao buscar amigos: " + e.getMessage()));
            return List.of();
        }
    }

    // --- NOVO MÉTODO: removeFriendship() ---
    public void removeFriendship(UUID person1Id, UUID person2Id) {
        String query = "MATCH (p1:Person {id: $person1Id})-[r:FRIENDS_WITH]->(p2:Person {id: $person2Id}) DELETE r";
        try (Session session = driver.session()) {
            session.run(query, parameters("person1Id", person1Id.toString(), "person2Id", person2Id.toString()));
            logger.info("Amizade removida entre pessoas com IDs: {} e {}", person1Id, person2Id);
            logRepository.saveLog(new Log("INFO", "Amizade Neo4j removida", "Entre " + person1Id + " e " + person2Id));
        } catch (Neo4jException e) {
            logger.error("Erro Neo4j ao remover amizade entre {} e {}: {}", person1Id, person2Id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro Neo4j", "Falha ao remover amizade: " + e.getMessage()));
        }
    }

    public void closeDriver() {
        if (driver != null) {
            try {
                driver.close();
                logger.info("Driver Neo4j fechado com sucesso.");
            } catch (Exception e) {
                logger.error("Erro ao fechar driver Neo4j: {}", e.getMessage());
            }
        }
    }
}
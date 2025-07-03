package com.cadernosegredos.service;

import com.cadernosegredos.model.Log; // Importação essencial para a classe Log
import com.cadernosegredos.model.Pessoa;
import com.cadernosegredos.repository.MongoLogRepositoryImpl;
import com.cadernosegredos.repository.Neo4jRelationshipRepositoryImpl;
import com.cadernosegredos.repository.PostgresPessoaRepositoryImpl; // Importe este para buscar detalhes das pessoas
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional; // Importação essencial para a classe Optional
import java.util.UUID; // Importação essencial para UUID
import java.util.stream.Collectors; // Para usar Collectors.toList()

public class RelacionamentoService {

    private static final Logger logger = LoggerFactory.getLogger(RelacionamentoService.class);

    private final Neo4jRelationshipRepositoryImpl neo4jRelationshipRepository;
    private final MongoLogRepositoryImpl mongoLogRepository;
    private final PostgresPessoaRepositoryImpl postgresPessoaRepository; // Adicionado para buscar os detalhes da Pessoa

    // Construtor para Injeção de Dependências
    // Este construtor permite que o App.java "injete" as instâncias dos repositórios
    public RelacionamentoService(Neo4jRelationshipRepositoryImpl neo4jRelationshipRepository,
                                 MongoLogRepositoryImpl mongoLogRepository,
                                 PostgresPessoaRepositoryImpl postgresPessoaRepository) {
        this.neo4jRelationshipRepository = neo4jRelationshipRepository;
        this.mongoLogRepository = mongoLogRepository;
        this.postgresPessoaRepository = postgresPessoaRepository; // Atribui a dependência
        logger.info("RelacionamentoService inicializado com dependências injetadas.");
    }

    /**
     * Estabelece uma relação de amizade entre duas pessoas no Neo4j.
     * Os IDs das pessoas devem ser UUIDs.
     * @param pessoa1Id O ID da primeira pessoa.
     * @param pessoa2Id O ID da segunda pessoa.
     */
    public void estabelecerAmizade(UUID pessoa1Id, UUID pessoa2Id) {
        logger.info("Tentando estabelecer amizade entre ID {} e ID {}", pessoa1Id, pessoa2Id);
        try {
            // Verifica se as pessoas existem no PostgreSQL antes de criar o relacionamento
            Optional<Pessoa> p1 = postgresPessoaRepository.findById(pessoa1Id);
            Optional<Pessoa> p2 = postgresPessoaRepository.findById(pessoa2Id);

            if (p1.isPresent() && p2.isPresent()) {
                neo4jRelationshipRepository.createFriendship(pessoa1Id, pessoa2Id);
                mongoLogRepository.saveLog(new Log("INFO", "Amizade estabelecida", "Entre " + p1.get().getNome() + " (ID: " + pessoa1Id + ") e " + p2.get().getNome() + " (ID: " + pessoa2Id + ")"));
                logger.info("Amizade estabelecida entre {} e {}.", p1.get().getNome(), p2.get().getNome());
            } else {
                String missingPerson = "";
                if (!p1.isPresent()) missingPerson += "Pessoa 1 (ID: " + pessoa1Id + ") não encontrada. ";
                if (!p2.isPresent()) missingPerson += "Pessoa 2 (ID: " + pessoa2Id + ") não encontrada. ";
                logger.warn("Não foi possível estabelecer amizade: {}", missingPerson);
                mongoLogRepository.saveLog(new Log("WARN", "Falha ao estabelecer amizade", missingPerson + " IDs: " + pessoa1Id + ", " + pessoa2Id));
            }
        } catch (Exception e) {
            logger.error("Erro inesperado ao estabelecer amizade entre {} e {}: {}", pessoa1Id, pessoa2Id, e.getMessage(), e);
            mongoLogRepository.saveLog(new Log("ERROR", "Erro no serviço ao estabelecer amizade", "Entre " + pessoa1Id + " e " + pessoa2Id + ": " + e.getMessage()));
        }
    }

    /**
     * Lista todos os amigos de uma pessoa com base em seu ID.
     * @param pessoaId O ID da pessoa cujos amigos serão listados.
     * @return Uma lista de objetos Pessoa que são amigos.
     */
    public List<Pessoa> listarAmigos(UUID pessoaId) {
        logger.info("Tentando listar amigos para a pessoa com ID: {}", pessoaId);
        List<UUID> amigoIds = neo4jRelationshipRepository.findFriends(pessoaId);

        if (amigoIds.isEmpty()) {
            logger.info("Nenhum amigo encontrado para a pessoa com ID: {}", pessoaId);
            mongoLogRepository.saveLog(new Log("INFO", "Amigos listados", "Para ID " + pessoaId + ": Nenhum amigo encontrado."));
            return List.of(); // Retorna uma lista vazia imutável
        }

        // Busca os detalhes completos das pessoas no PostgreSQL para cada ID de amigo
        List<Pessoa> amigos = amigoIds.stream()
                .map(id -> postgresPessoaRepository.findById(id)) // Chama o findById que retorna Optional<Pessoa>
                .filter(Optional::isPresent) // Filtra apenas os Optionals que contêm uma Pessoa
                .map(Optional::get) // Extrai a Pessoa do Optional
                .collect(Collectors.toList()); // Coleta em uma lista

        logger.info("Encontrados {} amigos para a pessoa com ID: {}", amigos.size(), pessoaId);
        mongoLogRepository.saveLog(new Log("INFO", "Amigos listados", "Para ID " + pessoaId + ": " + amigos.size() + " amigos."));
        return amigos;
    }

    /**
     * Remove uma relação de amizade entre duas pessoas no Neo4j.
     * @param pessoa1Id O ID da primeira pessoa.
     * @param pessoa2Id O ID da segunda pessoa.
     */
    public void removerAmizade(UUID pessoa1Id, UUID pessoa2Id) {
        logger.info("Tentando remover amizade entre ID {} e ID {}", pessoa1Id, pessoa2Id);
        try {
            neo4jRelationshipRepository.removeFriendship(pessoa1Id, pessoa2Id); // Assumindo que este método existe no repositório Neo4j
            mongoLogRepository.saveLog(new Log("INFO", "Amizade removida", "Entre " + pessoa1Id + " e " + pessoa2Id));
            logger.info("Amizade removida entre ID {} e ID {}.", pessoa1Id, pessoa2Id);
        } catch (Exception e) {
            logger.error("Erro inesperado ao remover amizade entre {} e {}: {}", pessoa1Id, pessoa2Id, e.getMessage(), e);
            mongoLogRepository.saveLog(new Log("ERROR", "Erro no serviço ao remover amizade", "Entre " + pessoa1Id + " e " + pessoa2Id + ": " + e.getMessage()));
        }
    }

    /**
     * Fecha o driver da conexão com o Neo4j.
     */
    public void closeNeo4jDriver() {
        if (neo4jRelationshipRepository != null) {
            neo4jRelationshipRepository.closeDriver();
            logger.info("Driver do Neo4j fechado.");
            mongoLogRepository.saveLog(new Log("INFO", "Conexão Neo4j", "Driver fechado."));
        }
    }
}
package com.cadernosegredos.service;

import com.cadernosegredos.model.Log;
import com.cadernosegredos.model.Pessoa;
import com.cadernosegredos.repository.MongoLogRepositoryImpl;
import com.cadernosegredos.repository.PessoaRepository; // Interface (boa prática para o campo)
import com.cadernosegredos.repository.PostgresPessoaRepositoryImpl;
import com.cadernosegredos.repository.RedisPessoaRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID; // Importe UUID

public class PessoaService {
    private static final Logger logger = LoggerFactory.getLogger(PessoaService.class);

    // Mudei o tipo de PostgresPessoaRepositoryImpl para PessoaRepository (a interface),
    // é uma boa prática programar para a interface.
    private final PessoaRepository postgresRepository;
    private final RedisPessoaRepositoryImpl redisRepository;
    private final MongoLogRepositoryImpl logRepository;

    // --- CONSTRUTOR CORRIGIDO PARA INJEÇÃO DE DEPENDÊNCIAS ---
    // Este construtor é CRUCIAL para que o App.java possa passar as dependências
    public PessoaService(PostgresPessoaRepositoryImpl postgresRepository,
                         RedisPessoaRepositoryImpl redisRepository,
                         MongoLogRepositoryImpl logRepository) {
        this.postgresRepository = postgresRepository;
        this.redisRepository = redisRepository;
        this.logRepository = logRepository;
        logger.info("PessoaService inicializado com dependências injetadas.");
    }

    public Pessoa savePessoa(Pessoa pessoa) {
        logger.info("Tentando criar pessoa: {}", pessoa.getNome());
        Pessoa savedPessoa = null;
        try {
            savedPessoa = postgresRepository.save(pessoa); // O repositório deve retornar a Pessoa com o ID

            if (savedPessoa != null && savedPessoa.getId() != null) {
                logRepository.saveLog(new Log("INFO", "Pessoa criada", "ID: " + savedPessoa.getId() + ", Nome: " + savedPessoa.getNome()));
                redisRepository.save(savedPessoa); // Salva no cache Redis
                logger.info("Pessoa criada e cacheada: {} (ID: {})", savedPessoa.getNome(), savedPessoa.getId());
            } else {
                String errorMessage = "Falha ao criar pessoa no PostgreSQL: " + pessoa.getNome() + ". ID não foi gerado.";
                logRepository.saveLog(new Log("ERROR", "Falha ao criar pessoa", errorMessage));
                logger.error(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Erro inesperado ao salvar pessoa: " + pessoa.getNome() + ". Erro: " + e.getMessage();
            logRepository.saveLog(new Log("ERROR", "Erro no serviço de criação de pessoa", errorMessage));
            logger.error(errorMessage, e);
        }
        return savedPessoa;
    }

    // --- MÉTODOS DE BUSCA E DELEÇÃO USANDO UUID CONSISTENTEMENTE ---
    public Optional<Pessoa> findPessoaById(UUID id) { // Alterado de String para UUID
        logger.info("Tentando buscar pessoa por ID: {}", id);
        Optional<Pessoa> pessoaFromCache = redisRepository.findById(id); // Assegure que RedisRepository.findById aceite UUID
        if (pessoaFromCache.isPresent()) {
            logger.info("Pessoa encontrada no cache Redis por ID: {}", id);
            logRepository.saveLog(new Log("INFO", "Pessoa buscada (cache hit)", "ID: " + id));
            return pessoaFromCache;
        }

        Optional<Pessoa> pessoaFromPg = postgresRepository.findById(id); // Assegure que PessoaRepository.findById aceite UUID
        if (pessoaFromPg.isPresent()) {
            logger.info("Pessoa encontrada no PostgreSQL por ID: {}. Adicionando ao cache Redis.", id);
            logRepository.saveLog(new Log("INFO", "Pessoa buscada (cache miss)", "ID: " + id));
            redisRepository.save(pessoaFromPg.get());
            return pessoaFromPg;
        }

        logger.warn("Pessoa com ID {} não encontrada no Redis ou PostgreSQL.", id);
        logRepository.saveLog(new Log("WARN", "Pessoa não encontrada", "ID: " + id));
        return Optional.empty();
    }

    public Optional<Pessoa> findPessoaByCpf(String cpf) {
        logger.info("Tentando buscar pessoa por CPF: {}", cpf);
        Optional<Pessoa> pessoaFromCache = redisRepository.findByCpf(cpf);
        if (pessoaFromCache.isPresent()) {
            logger.info("Pessoa encontrada no cache Redis por CPF: {}", cpf);
            logRepository.saveLog(new Log("INFO", "Pessoa buscada (cache hit)", "CPF: " + cpf));
            return pessoaFromCache;
        }

        Optional<Pessoa> pessoaFromPg = postgresRepository.findByCpf(cpf);
        if (pessoaFromPg.isPresent()) {
            logger.info("Pessoa encontrada no PostgreSQL por CPF: {}. Adicionando ao cache Redis.", cpf);
            logRepository.saveLog(new Log("INFO", "Pessoa buscada (cache miss)", "CPF: " + cpf));
            redisRepository.save(pessoaFromPg.get());
            return pessoaFromPg;
        }

        logger.warn("Pessoa com CPF {} não encontrada no Redis ou PostgreSQL.", cpf);
        logRepository.saveLog(new Log("WARN", "Pessoa não encontrada", "CPF: " + cpf));
        return Optional.empty();
    }

    public Pessoa updatePessoa(Pessoa pessoa) {
        if (pessoa == null || pessoa.getId() == null) {
            String errorMessage = "Não é possível atualizar uma pessoa sem ID.";
            logger.error(errorMessage);
            logRepository.saveLog(new Log("ERROR", "Falha na atualização de pessoa", errorMessage));
            return null;
        }

        logger.info("Tentando atualizar pessoa: {} (ID: {})", pessoa.getNome(), pessoa.getId());
        Pessoa updatedPessoa = null;
        try {
            updatedPessoa = postgresRepository.update(pessoa);
            if (updatedPessoa != null) {
                logRepository.saveLog(new Log("INFO", "Pessoa atualizada", "ID: " + updatedPessoa.getId() + ", Novo Email: " + updatedPessoa.getEmail()));
                redisRepository.save(updatedPessoa); // Atualiza o cache Redis
                logger.info("Pessoa atualizada no PostgreSQL e Redis: {} (ID: {})", updatedPessoa.getNome(), updatedPessoa.getId());
            } else {
                String errorMessage = "Falha ao atualizar pessoa no PostgreSQL. ID: " + pessoa.getId();
                logRepository.saveLog(new Log("ERROR", "Falha ao atualizar pessoa", errorMessage));
                logger.error(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Erro inesperado ao atualizar pessoa: " + pessoa.getNome() + " (ID: " + pessoa.getId() + "). Erro: " + e.getMessage();
            logRepository.saveLog(new Log("ERROR", "Erro no serviço de atualização de pessoa", errorMessage));
            logger.error(errorMessage, e);
        }
        return updatedPessoa;
    }

    public boolean deletePessoa(UUID id) { // Alterado de String para UUID e retorno para boolean
        if (id == null) {
            String errorMessage = "Não é possível deletar uma pessoa com ID nulo.";
            logger.error(errorMessage);
            logRepository.saveLog(new Log("ERROR", "Falha na deleção de pessoa", errorMessage));
            return false;
        }

        logger.info("Tentando deletar pessoa com ID: {}", id);
        boolean deletedFromPg = false;
        try {
            deletedFromPg = postgresRepository.delete(id); // Chamada para o método delete que retorna boolean

            if (deletedFromPg) {
                redisRepository.delete(id); // Remove do cache Redis
                logRepository.saveLog(new Log("INFO", "Pessoa deletada", "ID: " + id));
                logger.info("Pessoa deletada do PostgreSQL e Redis com ID: {}", id);
            } else {
                String warningMessage = "Pessoa com ID {} não encontrada no PostgreSQL para deleção ou falha na deleção.";
                logger.warn(warningMessage, id);
                logRepository.saveLog(new Log("WARN", "Falha na deleção de pessoa", "ID: " + id + ", Motivo: Não encontrada ou erro no PG."));
            }
        } catch (Exception e) {
            String errorMessage = "Erro inesperado ao deletar pessoa com ID: " + id + ". Erro: " + e.getMessage();
            logRepository.saveLog(new Log("ERROR", "Erro no serviço de deleção de pessoa", errorMessage));
            logger.error(errorMessage, e);
        }
        return deletedFromPg;
    }

    public List<Pessoa> findAllPessoas() {
        logger.info("Buscando todas as pessoas no PostgreSQL.");
        List<Pessoa> pessoas = postgresRepository.findAll();
        if (pessoas.isEmpty()) {
            logger.info("Nenhuma pessoa encontrada no PostgreSQL.");
            logRepository.saveLog(new Log("INFO", "Listadas todas as pessoas", "Total: 0"));
        } else {
            logger.info("Encontradas {} pessoas no PostgreSQL.", pessoas.size());
            logRepository.saveLog(new Log("INFO", "Listadas todas as pessoas", "Total: " + pessoas.size()));
        }
        return pessoas;
    }
}
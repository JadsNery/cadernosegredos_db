package com.cadernosegredos.repository;

import com.cadernosegredos.config.RedisConfig;
import com.cadernosegredos.model.Log;
import com.cadernosegredos.model.Pessoa;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Para LocalDate
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.UUID; // Importe UUID

public class RedisPessoaRepositoryImpl { // Não implementa PessoaRepository diretamente aqui
    private static final Logger logger = LoggerFactory.getLogger(RedisPessoaRepositoryImpl.class);
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final MongoLogRepositoryImpl logRepository = new MongoLogRepositoryImpl();

    public RedisPessoaRepositoryImpl() {
        this.jedisPool = RedisConfig.getJedisPool(); // Certifique-se de ter um método para obter JedisPool
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Para serializar LocalDate
    }

    public void save(Pessoa pessoa) {
        if (pessoa == null || pessoa.getId() == null) {
            logger.warn("Tentativa de salvar pessoa nula ou sem ID no Redis.");
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            String pessoaJson = objectMapper.writeValueAsString(pessoa);
            jedis.set(pessoa.getId().toString(), pessoaJson); // Use .toString() para UUID
            jedis.set("cpf:" + pessoa.getCpf(), pessoa.getId().toString()); // Mapeia CPF para ID
            logger.info("Pessoa com ID {} salva no Redis.", pessoa.getId());
            logRepository.saveLog(new Log("INFO", "Pessoa salva no Redis", "ID: " + pessoa.getId()));
        } catch (Exception e) {
            logger.error("Erro ao salvar pessoa no Redis: {}", e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro ao salvar pessoa no Redis", e.getMessage()));
        }
    }

    public Optional<Pessoa> findById(UUID id) { // <--- Mude para UUID
        try (Jedis jedis = jedisPool.getResource()) {
            String pessoaJson = jedis.get(id.toString()); // Use .toString() para UUID
            if (pessoaJson != null) {
                logger.info("Pessoa com ID {} encontrada no Redis.", id);
                return Optional.of(objectMapper.readValue(pessoaJson, Pessoa.class));
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar pessoa por ID {} no Redis: {}", id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro ao buscar pessoa por ID no Redis", "ID: " + id + ", Erro: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public Optional<Pessoa> findByCpf(String cpf) {
        try (Jedis jedis = jedisPool.getResource()) {
            String idString = jedis.get("cpf:" + cpf); // Pega o ID associado ao CPF
            if (idString != null) {
                logger.info("CPF {} encontrado no Redis, buscando pessoa por ID {}.", cpf, idString);
                return findById(UUID.fromString(idString)); // Converte String para UUID
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar pessoa por CPF {} no Redis: {}", cpf, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro ao buscar pessoa por CPF no Redis", "CPF: " + cpf + ", Erro: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public void delete(UUID id) { // <--- Mude para UUID
        try (Jedis jedis = jedisPool.getResource()) {
            Optional<Pessoa> pessoa = findById(id); // Busca a pessoa para obter o CPF
            if (pessoa.isPresent()) {
                jedis.del(pessoa.get().getId().toString()); // Deleta a pessoa pelo ID
                jedis.del("cpf:" + pessoa.get().getCpf()); // Deleta o mapeamento CPF para ID
                logger.info("Pessoa com ID {} deletada do Redis.", id);
                logRepository.saveLog(new Log("INFO", "Pessoa deletada do Redis", "ID: " + id));
            } else {
                logger.warn("Tentativa de deletar pessoa com ID {} que não foi encontrada no Redis.", id);
            }
        } catch (Exception e) {
            logger.error("Erro ao deletar pessoa com ID {} do Redis: {}", id, e.getMessage());
            logRepository.saveLog(new Log("ERROR", "Erro ao deletar pessoa do Redis", "ID: " + id + ", Erro: " + e.getMessage()));
        }
    }
}
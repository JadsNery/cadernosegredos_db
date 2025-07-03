package com.cadernosegredos.repository;

import com.cadernosegredos.model.Pessoa;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Importe UUID

public interface PessoaRepository {
    Pessoa save(Pessoa pessoa);
    Optional<Pessoa> findById(UUID id); // <--- ID agora é UUID
    Optional<Pessoa> findByCpf(String cpf);
    Pessoa update(Pessoa pessoa);
    boolean delete(UUID id); // <--- ID agora é UUID, e o retorno é boolean
    List<Pessoa> findAll();
}
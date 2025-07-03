package com.cadernosegredos.repository;

import com.cadernosegredos.model.Log;

import java.util.List;

public interface LogRepository extends CrudRepository<Log, String> {
    List<Log> findAllLogs(); // Método específico para listar todos os logs
}
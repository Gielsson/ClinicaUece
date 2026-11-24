package repository;

import exception.PersistenciaException;
import java.util.List;
import java.util.Optional;

// Contrato mínimo de persistência. Os services conhecem isto e não sabem que existe SQLite.
public interface Repositorio<T> {
    void salvar(T entidade) throws PersistenciaException;
    Optional<T> buscarPorId(int id) throws PersistenciaException;
    List<T> listarTodos() throws PersistenciaException;
    void atualizar(T entidade) throws PersistenciaException;
}

package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.util.*;
import model.Avaliacao;

public class AvaliacaoRepositorio implements Repositorio<Avaliacao> {
    private static final String BASE = "SELECT a.id, a.consulta_id, a.texto, a.nota FROM avaliacao a ";

    @Override
    public void salvar(Avaliacao a) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar a avaliação.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO avaliacao(consulta_id, texto, nota) VALUES(?, ?, ?)")) {
                ps.setInt(1, a.getConsultaId()); ps.setString(2, a.getTexto()); ps.setInt(3, a.getNota());
                ps.executeUpdate();
                a.setId(BancoDados.ultimoId(c));
            }
        });
    }

    @Override
    public void atualizar(Avaliacao a) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar a avaliação.", c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE avaliacao SET texto = ?, nota = ? WHERE id = ?")) {
                ps.setString(1, a.getTexto()); ps.setInt(2, a.getNota()); ps.setInt(3, a.getId());
                ps.executeUpdate();
            }
        });
    }

    private List<Avaliacao> ler(String complemento, Object... params) throws PersistenciaException {
        List<Avaliacao> r = new ArrayList<>();
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(BASE + complemento)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) r.add(Avaliacao.restaurar(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4)));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler as avaliações.", e);
        }
        return r;
    }

    @Override public Optional<Avaliacao> buscarPorId(int id) throws PersistenciaException {
        return ler("WHERE a.id = ?", id).stream().findFirst();
    }

    @Override public List<Avaliacao> listarTodos() throws PersistenciaException {
        return ler("ORDER BY a.id");
    }

    public boolean existePorConsulta(int consultaId) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM avaliacao WHERE consulta_id = ?", consultaId) > 0;
    }

    // Devolve avaliações "leves" (sem o objeto Consulta) de propósito; o motivo está no comentário de Avaliacao.
    public List<Avaliacao> doMedico(int medicoId) throws PersistenciaException {
        return ler("JOIN consulta c ON c.id = a.consulta_id WHERE c.medico_id = ? ORDER BY a.id DESC", medicoId);
    }

    public List<Avaliacao> recentesDoMedico(int medicoId, int n) throws PersistenciaException {
        return ler("JOIN consulta c ON c.id = a.consulta_id WHERE c.medico_id = ? ORDER BY a.id DESC LIMIT ?", medicoId, n);
    }
}

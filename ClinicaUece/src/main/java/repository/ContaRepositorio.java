package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.util.*;
import model.*;

public class ContaRepositorio implements Repositorio<Conta> {
    private final PacienteRepositorio pacientes = new PacienteRepositorio();
    private final ConsultaRepositorio consultas = new ConsultaRepositorio();

    private record Linha(int id, int pacienteId, int consultaId, double valor, String situacao) {}

    @Override
    public void salvar(Conta conta) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar a conta.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO conta(paciente_id, consulta_id, valor, situacao) VALUES(?,?,?,?)")) {
                ps.setInt(1, conta.getPaciente().getId()); ps.setInt(2, conta.getConsulta().getId());
                ps.setDouble(3, conta.getValor()); ps.setString(4, conta.getSituacao().name());
                ps.executeUpdate();
                conta.setId(BancoDados.ultimoId(c));
            }
        });
    }

    @Override
    public void atualizar(Conta conta) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar a conta.", c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE conta SET situacao = ? WHERE id = ?")) {
                ps.setString(1, conta.getSituacao().name()); ps.setInt(2, conta.getId());
                ps.executeUpdate();
            }
        });
    }

    private List<Conta> ler(String complemento, Object... params) throws PersistenciaException {
        List<Linha> linhas = new ArrayList<>();
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT id, paciente_id, consulta_id, valor, situacao FROM conta " + complemento)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) linhas.add(new Linha(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler as contas.", e);
        }
        List<Conta> r = new ArrayList<>();
        for (Linha l : linhas) {
            Paciente p = pacientes.buscarPorId(l.pacienteId())
                    .orElseThrow(() -> new PersistenciaException("Paciente da conta não encontrado."));
            Consulta c = consultas.buscarPorId(l.consultaId())
                    .orElseThrow(() -> new PersistenciaException("Consulta da conta não encontrada."));
            r.add(Conta.restaurar(l.id(), p, c, l.valor(), SituacaoConta.valueOf(l.situacao())));
        }
        return r;
    }

    @Override public Optional<Conta> buscarPorId(int id) throws PersistenciaException {
        return ler("WHERE id = ?", id).stream().findFirst();
    }

    @Override public List<Conta> listarTodos() throws PersistenciaException {
        return ler("ORDER BY id");
    }

    // filtro null = traz pendentes e pagas
    public List<Conta> porPaciente(int pacienteId, SituacaoConta filtro) throws PersistenciaException {
        return filtro == null ? ler("WHERE paciente_id = ? ORDER BY id DESC", pacienteId)
                              : ler("WHERE paciente_id = ? AND situacao = ? ORDER BY id DESC", pacienteId, filtro.name());
    }

    public boolean existePorConsulta(int consultaId) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM conta WHERE consulta_id = ?", consultaId) > 0;
    }
}

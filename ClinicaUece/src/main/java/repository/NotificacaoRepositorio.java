package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import model.*;

public class NotificacaoRepositorio implements Repositorio<Notificacao> {
    private final PacienteRepositorio pacientes = new PacienteRepositorio();

    private record Linha(int id, int pacienteId, String mensagem, String dataHora, boolean lida) {}

    @Override
    public void salvar(Notificacao n) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar a notificação.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO notificacao(paciente_id, mensagem, data_hora, lida) VALUES(?,?,?,?)")) {
                ps.setInt(1, n.getPaciente().getId()); ps.setString(2, n.getMensagem());
                ps.setString(3, n.getDataHora().toString()); ps.setInt(4, n.isLida() ? 1 : 0);
                ps.executeUpdate();
                n.setId(BancoDados.ultimoId(c));
            }
        });
    }

    @Override
    public void atualizar(Notificacao n) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar a notificação.", c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE notificacao SET lida = ? WHERE id = ?")) {
                ps.setInt(1, n.isLida() ? 1 : 0); ps.setInt(2, n.getId());
                ps.executeUpdate();
            }
        });
    }

    private List<Notificacao> ler(String complemento, Object... params) throws PersistenciaException {
        List<Linha> linhas = new ArrayList<>();
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT id, paciente_id, mensagem, data_hora, lida FROM notificacao " + complemento)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) linhas.add(new Linha(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getInt(5) == 1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler as notificações.", e);
        }
        Map<Integer, Paciente> cache = new HashMap<>();
        List<Notificacao> r = new ArrayList<>();
        for (Linha l : linhas) {
            Paciente p = cache.get(l.pacienteId());
            if (p == null) {
                p = pacientes.buscarPorId(l.pacienteId())
                        .orElseThrow(() -> new PersistenciaException("Paciente da notificação não encontrado."));
                cache.put(l.pacienteId(), p);
            }
            r.add(Notificacao.restaurar(l.id(), p, l.mensagem(), LocalDateTime.parse(l.dataHora()), l.lida()));
        }
        return r;
    }

    @Override public Optional<Notificacao> buscarPorId(int id) throws PersistenciaException {
        return ler("WHERE id = ?", id).stream().findFirst();
    }

    @Override public List<Notificacao> listarTodos() throws PersistenciaException {
        return ler("ORDER BY id DESC");
    }

    public List<Notificacao> porPaciente(int pacienteId) throws PersistenciaException {
        return ler("WHERE paciente_id = ? ORDER BY id DESC", pacienteId);
    }

    public List<Notificacao> naoLidas(int pacienteId) throws PersistenciaException {
        return ler("WHERE paciente_id = ? AND lida = 0 ORDER BY id DESC", pacienteId);
    }
}

package repository;

import especialidade.EspecialidadeFactory;
import exception.PersistenciaException;
import java.sql.*;
import java.util.*;
import model.Medico;
import model.PlanoSaude;

public class MedicoRepositorio implements Repositorio<Medico> {
    private final AvaliacaoRepositorio avaliacoes = new AvaliacaoRepositorio();

    @Override
    public void salvar(Medico m) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar o médico.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO usuario(tipo, nome, login, senha_hash) VALUES('MEDICO', ?, ?, ?)")) {
                ps.setString(1, m.getNome()); ps.setString(2, m.getLogin()); ps.setString(3, m.getSenhaHash());
                ps.executeUpdate();
                m.setId(BancoDados.ultimoId(c));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO medico(usuario_id, especialidade, valor_base) VALUES(?, ?, ?)")) {
                ps.setInt(1, m.getId()); ps.setString(2, m.getEspecialidade().getNome()); ps.setDouble(3, m.getValorBase());
                ps.executeUpdate();
            }
            gravarPlanos(c, m);
        });
    }

    @Override
    public void atualizar(Medico m) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar o médico.", c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET nome = ?, senha_hash = ? WHERE id = ?")) {
                ps.setString(1, m.getNome()); ps.setString(2, m.getSenhaHash()); ps.setInt(3, m.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE medico SET especialidade = ?, valor_base = ? WHERE usuario_id = ?")) {
                ps.setString(1, m.getEspecialidade().getNome()); ps.setDouble(2, m.getValorBase()); ps.setInt(3, m.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM medico_plano WHERE medico_id = ?")) {
                ps.setInt(1, m.getId());
                ps.executeUpdate();
            }
            gravarPlanos(c, m);
        });
    }

    private void gravarPlanos(Connection c, Medico m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO medico_plano(medico_id, plano) VALUES(?, ?)")) {
            for (PlanoSaude p : m.getPlanosAtendidos()) {
                ps.setInt(1, m.getId()); ps.setString(2, p.name());
                ps.executeUpdate();
            }
        }
    }

    private record Dados(String nome, String login, String hash, String especialidade, double valorBase) {}

    @Override
    public Optional<Medico> buscarPorId(int id) throws PersistenciaException {
        Dados d = null;
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT u.nome, u.login, u.senha_hash, m.especialidade, m.valor_base FROM usuario u "
                + "JOIN medico m ON m.usuario_id = u.id WHERE u.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d = new Dados(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler o médico.", e);
        }
        if (d == null) return Optional.empty();
        Medico m = new Medico(d.nome(), d.login(), d.hash(), EspecialidadeFactory.de(d.especialidade()),
                planosDe(id), d.valorBase());
        m.setId(id);
        m.setAvaliacoes(avaliacoes.doMedico(id));
        return Optional.of(m);
    }

    private Set<PlanoSaude> planosDe(int medicoId) throws PersistenciaException {
        Set<PlanoSaude> planos = EnumSet.noneOf(PlanoSaude.class);
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT plano FROM medico_plano WHERE medico_id = ?")) {
            ps.setInt(1, medicoId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) planos.add(PlanoSaude.valueOf(rs.getString(1))); }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler os planos do médico.", e);
        }
        return planos;
    }

    // Termo vazio traz todo mundo. Atenção: o LIKE do SQLite só ignora maiúsculas em letras sem acento
    // ("cardio" acha Cardiologia, mas "CLÍNICA" não acha "Clínica").
    public List<Medico> buscarPorNomeOuEspecialidade(String termo) throws PersistenciaException {
        String t = "%" + (termo == null ? "" : termo.trim()) + "%";
        List<Medico> r = new ArrayList<>();
        for (int id : BancoDados.listarInts("SELECT u.id FROM usuario u JOIN medico m ON m.usuario_id = u.id "
                + "WHERE u.nome LIKE ? OR m.especialidade LIKE ? ORDER BY u.nome", t, t))
            buscarPorId(id).ifPresent(r::add);
        return r;
    }

    @Override
    public List<Medico> listarTodos() throws PersistenciaException {
        return buscarPorNomeOuEspecialidade("");
    }
}

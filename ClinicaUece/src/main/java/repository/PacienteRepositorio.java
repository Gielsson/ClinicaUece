package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.util.*;
import model.Paciente;
import model.PlanoSaude;

public class PacienteRepositorio implements Repositorio<Paciente> {

    @Override
    public void salvar(Paciente p) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar o paciente.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO usuario(tipo, nome, login, senha_hash) VALUES('PACIENTE', ?, ?, ?)")) {
                ps.setString(1, p.getNome()); ps.setString(2, p.getLogin()); ps.setString(3, p.getSenhaHash());
                ps.executeUpdate();
                p.setId(BancoDados.ultimoId(c));
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO paciente(usuario_id, idade, plano) VALUES(?, ?, ?)")) {
                ps.setInt(1, p.getId()); ps.setInt(2, p.getIdade()); ps.setString(3, p.getPlano().name());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void atualizar(Paciente p) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar o paciente.", c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET nome = ?, senha_hash = ? WHERE id = ?")) {
                ps.setString(1, p.getNome()); ps.setString(2, p.getSenhaHash()); ps.setInt(3, p.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE paciente SET idade = ?, plano = ? WHERE usuario_id = ?")) {
                ps.setInt(1, p.getIdade()); ps.setString(2, p.getPlano().name()); ps.setInt(3, p.getId());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Paciente> buscarPorId(int id) throws PersistenciaException {
        Paciente p = null;
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT u.nome, u.login, u.senha_hash, p.idade, p.plano FROM usuario u "
                + "JOIN paciente p ON p.usuario_id = u.id WHERE u.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p = new Paciente(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                            PlanoSaude.valueOf(rs.getString(5)));
                    p.setId(id);
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler o paciente.", e);
        }
        return Optional.ofNullable(p);
    }

    @Override
    public List<Paciente> listarTodos() throws PersistenciaException {
        List<Paciente> r = new ArrayList<>();
        for (int id : BancoDados.listarInts("SELECT usuario_id FROM paciente ORDER BY usuario_id"))
            buscarPorId(id).ifPresent(r::add);
        return r;
    }
}

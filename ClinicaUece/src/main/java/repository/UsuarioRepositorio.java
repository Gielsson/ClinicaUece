package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.util.Optional;
import model.Usuario;

public class UsuarioRepositorio {
    private final MedicoRepositorio medicos = new MedicoRepositorio();
    private final PacienteRepositorio pacientes = new PacienteRepositorio();

    public Optional<Usuario> buscarPorLogin(String login) throws PersistenciaException {
        int id = -1;
        String tipo = null;
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT id, tipo FROM usuario WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { id = rs.getInt(1); tipo = rs.getString(2); }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível consultar o usuário.", e);
        }
        if (tipo == null) return Optional.empty();
        return "MEDICO".equals(tipo) ? medicos.buscarPorId(id).map(m -> (Usuario) m)
                                     : pacientes.buscarPorId(id).map(p -> (Usuario) p);
    }
}

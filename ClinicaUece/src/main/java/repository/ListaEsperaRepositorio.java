package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import model.*;

// A ordem da fila é a ordem do id. Ordenar por horário de inscrição não é seguro: duas pessoas podem cair no mesmo instante.
public class ListaEsperaRepositorio {
    private final MedicoRepositorio medicos = new MedicoRepositorio();
    private final PacienteRepositorio pacientes = new PacienteRepositorio();

    private record Linha(int id, int medicoId, int pacienteId, String data, String inscritoEm) {}

    public void salvar(EntradaListaEspera e) throws PersistenciaException {
        BancoDados.gravar("Não foi possível entrar na lista de espera.", c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lista_espera(medico_id, paciente_id, data, inscrito_em) VALUES(?,?,?,?)")) {
                ps.setInt(1, e.getMedico().getId()); ps.setInt(2, e.getPaciente().getId());
                ps.setString(3, e.getData().toString()); ps.setString(4, e.getInscritoEm().toString());
                ps.executeUpdate();
                e.setId(BancoDados.ultimoId(c));
            }
        });
    }

    public void remover(int id) throws PersistenciaException {
        BancoDados.gravar("Não foi possível sair da lista de espera.", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM lista_espera WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }

    private List<EntradaListaEspera> ler(String complemento, Object... params) throws PersistenciaException {
        List<Linha> linhas = new ArrayList<>();
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT id, medico_id, paciente_id, data, inscrito_em FROM lista_espera " + complemento)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) linhas.add(new Linha(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5)));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("Não foi possível ler a lista de espera.", ex);
        }
        List<EntradaListaEspera> r = new ArrayList<>();
        for (Linha l : linhas) {
            Medico m = medicos.buscarPorId(l.medicoId())
                    .orElseThrow(() -> new PersistenciaException("Médico da fila não encontrado."));
            Paciente p = pacientes.buscarPorId(l.pacienteId())
                    .orElseThrow(() -> new PersistenciaException("Paciente da fila não encontrado."));
            r.add(EntradaListaEspera.restaurar(l.id(), m, p, LocalDate.parse(l.data()), LocalDateTime.parse(l.inscritoEm())));
        }
        return r;
    }

    public Optional<EntradaListaEspera> buscarPorId(int id) throws PersistenciaException {
        return ler("WHERE id = ?", id).stream().findFirst();
    }

    public Optional<EntradaListaEspera> primeiraDaFila(int medicoId, LocalDate data) throws PersistenciaException {
        return ler("WHERE medico_id = ? AND data = ? ORDER BY id LIMIT 1", medicoId, data.toString()).stream().findFirst();
    }

    public boolean existe(int medicoId, int pacienteId, LocalDate data) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM lista_espera WHERE medico_id = ? AND paciente_id = ? AND data = ?",
                medicoId, pacienteId, data.toString()) > 0;
    }

    public int posicao(EntradaListaEspera e) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM lista_espera WHERE medico_id = ? AND data = ? AND id <= ?",
                e.getMedico().getId(), e.getData().toString(), e.getId());
    }

    public List<EntradaListaEspera> porMedicoEData(int medicoId, LocalDate data) throws PersistenciaException {
        return ler("WHERE medico_id = ? AND data = ? ORDER BY id", medicoId, data.toString());
    }

    public List<EntradaListaEspera> porPaciente(int pacienteId) throws PersistenciaException {
        return ler("WHERE paciente_id = ? ORDER BY data, id", pacienteId);
    }
}

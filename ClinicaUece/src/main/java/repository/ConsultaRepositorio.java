package repository;

import exception.PersistenciaException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import model.*;

public class ConsultaRepositorio implements Repositorio<Consulta> {
    private final MedicoRepositorio medicos = new MedicoRepositorio();
    private final PacienteRepositorio pacientes = new PacienteRepositorio();

    private static final String COLUNAS = "id, medico_id, paciente_id, data, estado, sintomas, diagnostico, "
            + "tratamento, medicamentos, exames, observacoes, valor_cobrado";

    private record Linha(int id, int medicoId, int pacienteId, String data, String estado, String sintomas,
                         String diagnostico, String tratamento, String medicamentos, String exames,
                         String observacoes, double valor) {}

    @Override
    public void salvar(Consulta c) throws PersistenciaException {
        BancoDados.gravar("Não foi possível salvar a consulta.", con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO consulta(medico_id, paciente_id, data, estado, sintomas, diagnostico, tratamento, "
                    + "medicamentos, exames, observacoes, valor_cobrado) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, c.getMedico().getId()); ps.setInt(2, c.getPaciente().getId());
                ps.setString(3, c.getData().toString()); ps.setString(4, c.getEstado().name());
                ps.setString(5, c.getSintomas()); ps.setString(6, c.getDiagnostico()); ps.setString(7, c.getTratamento());
                ps.setString(8, c.getMedicamentos()); ps.setString(9, c.getExames()); ps.setString(10, c.getObservacoes());
                ps.setDouble(11, c.getValorCobrado());
                ps.executeUpdate();
                c.setId(BancoDados.ultimoId(con));
            }
        });
    }

    @Override
    public void atualizar(Consulta c) throws PersistenciaException {
        BancoDados.gravar("Não foi possível atualizar a consulta.", con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE consulta SET estado=?, sintomas=?, diagnostico=?, tratamento=?, medicamentos=?, exames=?, "
                    + "observacoes=?, valor_cobrado=? WHERE id=?")) {
                ps.setString(1, c.getEstado().name()); ps.setString(2, c.getSintomas()); ps.setString(3, c.getDiagnostico());
                ps.setString(4, c.getTratamento()); ps.setString(5, c.getMedicamentos()); ps.setString(6, c.getExames());
                ps.setString(7, c.getObservacoes()); ps.setDouble(8, c.getValorCobrado()); ps.setInt(9, c.getId());
                ps.executeUpdate();
            }
        });
    }

    // Primeiro leio todas as linhas e fecho o ResultSet; só depois monto os objetos. Montar uma Consulta
    // dispara outras queries (médico, paciente, avaliações) e eu não quero ResultSet aberto dentro de outro.
    // Os caches evitam buscar o mesmo médico dezenas de vezes numa lista longa.
    private List<Consulta> consultar(String complemento, Object... params) throws PersistenciaException {
        List<Linha> linhas = new ArrayList<>();
        try (PreparedStatement ps = BancoDados.getConexao().prepareStatement(
                "SELECT " + COLUNAS + " FROM consulta " + complemento)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    linhas.add(new Linha(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                            rs.getString(11), rs.getDouble(12)));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível ler as consultas.", e);
        }
        Map<Integer, Medico> cacheM = new HashMap<>();
        Map<Integer, Paciente> cacheP = new HashMap<>();
        List<Consulta> r = new ArrayList<>();
        for (Linha l : linhas) {
            Medico m = cacheM.get(l.medicoId());
            if (m == null) {
                m = medicos.buscarPorId(l.medicoId())
                        .orElseThrow(() -> new PersistenciaException("Médico da consulta não encontrado."));
                cacheM.put(l.medicoId(), m);
            }
            Paciente p = cacheP.get(l.pacienteId());
            if (p == null) {
                p = pacientes.buscarPorId(l.pacienteId())
                        .orElseThrow(() -> new PersistenciaException("Paciente da consulta não encontrado."));
                cacheP.put(l.pacienteId(), p);
            }
            r.add(Consulta.restaurar(l.id(), m, p, LocalDate.parse(l.data()), EstadoConsulta.valueOf(l.estado()),
                    l.sintomas(), l.diagnostico(), l.tratamento(), l.medicamentos(), l.exames(), l.observacoes(), l.valor()));
        }
        return r;
    }

    @Override public Optional<Consulta> buscarPorId(int id) throws PersistenciaException {
        return consultar("WHERE id = ?", id).stream().findFirst();
    }

    @Override public List<Consulta> listarTodos() throws PersistenciaException {
        return consultar("ORDER BY data, id");
    }

    public List<Consulta> porPaciente(int pacienteId) throws PersistenciaException {
        return consultar("WHERE paciente_id = ? ORDER BY data DESC, id DESC", pacienteId);
    }

    public List<Consulta> porMedicoEData(int medicoId, LocalDate data) throws PersistenciaException {
        return consultar("WHERE medico_id = ? AND data = ? ORDER BY id", medicoId, data.toString());
    }

    // Cancelada não conta: é isso que "libera a vaga". Realizada conta, porque ela aconteceu naquele dia.
    public int contarOcupadas(int medicoId, LocalDate data) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM consulta WHERE medico_id = ? AND data = ? AND estado <> 'CANCELADA'",
                medicoId, data.toString());
    }

    public boolean existeAtiva(int medicoId, int pacienteId, LocalDate data) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM consulta WHERE medico_id = ? AND paciente_id = ? "
                + "AND data = ? AND estado <> 'CANCELADA'", medicoId, pacienteId, data.toString()) > 0;
    }

    // Base da regra de acesso ao prontuário: o médico precisa ter (ou ter tido) consulta com o paciente.
    public boolean medicoAtendeuOuAtende(int medicoId, int pacienteId) throws PersistenciaException {
        return BancoDados.contar("SELECT COUNT(*) FROM consulta WHERE medico_id = ? AND paciente_id = ? "
                + "AND estado IN ('AGENDADA','REALIZADA')", medicoId, pacienteId) > 0;
    }
}

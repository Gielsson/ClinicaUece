package model;

import exception.TransicaoInvalidaException;
import java.time.LocalDate;

public class Consulta {
    private int id;
    private final Medico medico;
    private final Paciente paciente;
    private final LocalDate data;
    private EstadoConsulta estado;
    private String sintomas = "", diagnostico = "", tratamento = "", medicamentos = "", exames = "", observacoes = "";
    private double valorCobrado;

    public Consulta(Medico medico, Paciente paciente, LocalDate data) {
        this.medico = medico;
        this.paciente = paciente;
        this.data = data;
        this.estado = EstadoConsulta.AGENDADA;
    }

    private Consulta(int id, Medico medico, Paciente paciente, LocalDate data, EstadoConsulta estado) {
        this(medico, paciente, data);
        this.id = id;
        this.estado = estado;
    }

    // Só o repositório deveria chamar isso. Remonta a consulta exatamente como estava no banco, sem
    // passar pela validação de transição (senão uma REALIZADA nunca conseguiria ser carregada).
    public static Consulta restaurar(int id, Medico medico, Paciente paciente, LocalDate data, EstadoConsulta estado,
                                     String sintomas, String diagnostico, String tratamento, String medicamentos,
                                     String exames, String observacoes, double valorCobrado) {
        Consulta c = new Consulta(id, medico, paciente, data, estado);
        c.sintomas = nv(sintomas); c.diagnostico = nv(diagnostico); c.tratamento = nv(tratamento);
        c.medicamentos = nv(medicamentos); c.exames = nv(exames); c.observacoes = nv(observacoes);
        c.valorCobrado = valorCobrado;
        return c;
    }

    public void cancelar() throws TransicaoInvalidaException {
        mudarPara(EstadoConsulta.CANCELADA);
    }

    public void realizar(DadosAtendimento d, double valorCobrado) throws TransicaoInvalidaException {
        mudarPara(EstadoConsulta.REALIZADA);   // valida ANTES de mexer nos campos: se lançar, a consulta fica intacta
        this.sintomas = nv(d.sintomas());
        this.diagnostico = nv(d.diagnostico());
        this.tratamento = nv(d.tratamento());
        this.medicamentos = nv(d.medicamentos());
        this.exames = nv(d.exames());
        this.observacoes = nv(d.observacoes());
        this.valorCobrado = valorCobrado;
    }

    private void mudarPara(EstadoConsulta novo) throws TransicaoInvalidaException {
        if (!estado.podeIrPara(novo))
            throw new TransicaoInvalidaException("Uma consulta " + estado + " não pode ser "
                    + (novo == EstadoConsulta.CANCELADA ? "cancelada." : "realizada."));
        estado = novo;
    }

    public boolean podeSerAvaliada() { return estado == EstadoConsulta.REALIZADA; }

    // médico ou paciente desta consulta; é quem pode cancelá-la
    public boolean pertenceA(Usuario u) {
        return u != null && (u.getId() == medico.getId() || u.getId() == paciente.getId());
    }

    private static String nv(String s) { return s == null ? "" : s.trim(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Medico getMedico() { return medico; }
    public Paciente getPaciente() { return paciente; }
    public LocalDate getData() { return data; }
    public EstadoConsulta getEstado() { return estado; }
    public String getSintomas() { return sintomas; }
    public String getDiagnostico() { return diagnostico; }
    public String getTratamento() { return tratamento; }
    public String getMedicamentos() { return medicamentos; }
    public String getExames() { return exames; }
    public String getObservacoes() { return observacoes; }
    public double getValorCobrado() { return valorCobrado; }
}

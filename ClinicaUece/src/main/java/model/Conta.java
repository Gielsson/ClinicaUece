package model;

import exception.PagamentoDuplicadoException;

// Só existe para paciente sem plano; quem tem plano não paga a consulta.
public class Conta {
    private int id;
    private final Paciente paciente;
    private final Consulta consulta;
    private final double valor;
    private SituacaoConta situacao;

    public Conta(Paciente paciente, Consulta consulta, double valor) {
        this(0, paciente, consulta, valor, SituacaoConta.PENDENTE);
    }

    private Conta(int id, Paciente paciente, Consulta consulta, double valor, SituacaoConta situacao) {
        this.id = id; this.paciente = paciente; this.consulta = consulta; this.valor = valor; this.situacao = situacao;
    }

    public static Conta restaurar(int id, Paciente paciente, Consulta consulta, double valor, SituacaoConta situacao) {
        return new Conta(id, paciente, consulta, valor, situacao);
    }

    public void pagar() throws PagamentoDuplicadoException {
        if (situacao == SituacaoConta.PAGA) throw new PagamentoDuplicadoException("Esta conta já foi paga.");
        situacao = SituacaoConta.PAGA;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public Consulta getConsulta() { return consulta; }
    public double getValor() { return valor; }
    public SituacaoConta getSituacao() { return situacao; }
}

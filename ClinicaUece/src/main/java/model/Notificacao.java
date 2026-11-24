package model;

import java.time.LocalDateTime;

// É só um aviso que aparece na tela; o enunciado dispensa e-mail e SMS.
public class Notificacao {
    private int id;
    private final Paciente paciente;
    private final String mensagem;
    private final LocalDateTime dataHora;
    private boolean lida;

    public Notificacao(Paciente paciente, String mensagem) {
        this(0, paciente, mensagem, LocalDateTime.now(), false);
    }

    private Notificacao(int id, Paciente paciente, String mensagem, LocalDateTime dataHora, boolean lida) {
        this.id = id; this.paciente = paciente; this.mensagem = mensagem; this.dataHora = dataHora; this.lida = lida;
    }

    public static Notificacao restaurar(int id, Paciente p, String mensagem, LocalDateTime dataHora, boolean lida) {
        return new Notificacao(id, p, mensagem, dataHora, lida);
    }

    public void marcarComoLida() { this.lida = true; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getDataHora() { return dataHora; }
    public boolean isLida() { return lida; }
}

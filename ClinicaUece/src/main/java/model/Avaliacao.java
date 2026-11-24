package model;

/*
 * Guardamos o id da consulta e, só quando a avaliação acabou de ser criada, o objeto Consulta.
 * Se carregássemos a Consulta completa toda vez, cairíamos num ciclo: o Medico carrega as
 * avaliações, que carregam a Consulta, que carrega o Medico, que carrega as avaliações...
 */
public class Avaliacao {
    private int id;
    private final int consultaId;
    private final Consulta consulta;      // null quando a avaliação veio do banco
    private final String texto;
    private final int nota;

    public Avaliacao(Consulta consulta, String texto, int nota) {
        this(0, consulta.getId(), consulta, texto, nota);
    }

    private Avaliacao(int id, int consultaId, Consulta consulta, String texto, int nota) {
        if (nota < 1 || nota > 5) throw new IllegalArgumentException("A nota deve ser de 1 a 5.");
        this.id = id;
        this.consultaId = consultaId;
        this.consulta = consulta;
        this.texto = texto == null ? "" : texto;
        this.nota = nota;
    }

    public static Avaliacao restaurar(int id, int consultaId, String texto, int nota) {
        return new Avaliacao(id, consultaId, null, texto, nota);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConsultaId() { return consultaId; }
    public Consulta getConsulta() { return consulta; }
    public String getTexto() { return texto; }
    public int getNota() { return nota; }
}

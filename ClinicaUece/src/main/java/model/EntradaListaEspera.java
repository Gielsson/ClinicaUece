package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EntradaListaEspera {
    private int id;
    private final Medico medico;
    private final Paciente paciente;
    private final LocalDate data;
    private final LocalDateTime inscritoEm;

    public EntradaListaEspera(Medico medico, Paciente paciente, LocalDate data) {
        this(0, medico, paciente, data, LocalDateTime.now());
    }

    private EntradaListaEspera(int id, Medico medico, Paciente paciente, LocalDate data, LocalDateTime inscritoEm) {
        this.id = id; this.medico = medico; this.paciente = paciente; this.data = data; this.inscritoEm = inscritoEm;
    }

    public static EntradaListaEspera restaurar(int id, Medico m, Paciente p, LocalDate data, LocalDateTime inscritoEm) {
        return new EntradaListaEspera(id, m, p, data, inscritoEm);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Medico getMedico() { return medico; }
    public Paciente getPaciente() { return paciente; }
    public LocalDate getData() { return data; }
    public LocalDateTime getInscritoEm() { return inscritoEm; }
}

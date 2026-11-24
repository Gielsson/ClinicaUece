package model;

import java.util.*;

// Só entram consultas REALIZADAS: uma agendada ainda não tem diagnóstico nenhum pra mostrar.
public class Prontuario {
    private final Paciente paciente;
    private final List<Consulta> entradas = new ArrayList<>();

    public Prontuario(Paciente paciente, List<Consulta> consultas) {
        this.paciente = paciente;
        for (Consulta c : consultas) if (c.getEstado() == EstadoConsulta.REALIZADA) entradas.add(c);
        entradas.sort(Comparator.comparing(Consulta::getData).thenComparingInt(Consulta::getId).reversed());
    }

    public Paciente getPaciente() { return paciente; }
    public List<Consulta> entradas() { return Collections.unmodifiableList(entradas); }
}

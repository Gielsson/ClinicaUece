package app;

import especialidade.*;
import exception.ClinicaException;
import java.time.LocalDate;
import java.util.EnumSet;
import model.*;
import repository.*;
import service.*;
import util.Seguranca;
import static model.PlanoSaude.*;

// Dados só para a demonstração (a senha de todo mundo é 123456).
public final class DadosExemplo {
    private DadosExemplo() {}

    public static void popularSeVazio() throws ClinicaException {
        MedicoRepositorio medicos = new MedicoRepositorio();
        PacienteRepositorio pacientes = new PacienteRepositorio();
        if (!medicos.listarTodos().isEmpty()) return;
        String h = Seguranca.hash("123456");   // um hash só, reaproveitado: BCrypt é lento de propósito
        BancoDados.transacao(() -> {
            Medico caio = new Medico("Dr. Caio Mota", "caio", h, new Cardiologia(), EnumSet.of(UNIMED, BRADESCO), 200);
            Medico lia = new Medico("Dra. Lia Souza", "lia", h, new Cardiologia(), EnumSet.of(UNIMED, HAPVIDA), 180);
            Medico rui = new Medico("Dr. Rui Alves", "rui", h, new ClinicaGeral(), EnumSet.of(UNIMED, HAPVIDA, BRADESCO), 150);
            Medico eva = new Medico("Dra. Eva Nunes", "eva", h, new Pediatria(), EnumSet.of(UNIMED, HAPVIDA), 170);
            Medico davi = new Medico("Dr. Davi Cruz", "davi", h, new Dermatologia(), EnumSet.of(UNIMED, BRADESCO, HAPVIDA), 160);
            for (Medico m : new Medico[]{caio, lia, rui, eva, davi}) medicos.salvar(m);
            Paciente ana = new Paciente("Ana Lima", "ana", h, 34, UNIMED);
            Paciente bruno = new Paciente("Bruno Reis", "bruno", h, 28, NENHUM);
            for (Paciente p : new Paciente[]{ana, bruno,
                    new Paciente("Carla Dias", "carla", h, 41, HAPVIDA),
                    new Paciente("Dani Souza", "dani", h, 22, UNIMED),
                    new Paciente("Edu Pires", "edu", h, 55, NENHUM)}) pacientes.salvar(p);

            // O histórico passa pelos services de verdade (agendar + realizar), não por INSERT direto.
            // Assim a própria demo prova que as regras funcionam e que a conta só nasce para quem não tem plano.
            AgendamentoService agenda = new AgendamentoService();
            ConsultaService consultas = new ConsultaService();
            LocalDate hoje = LocalDate.now();
            consultas.realizar(agenda.agendar(ana, caio, hoje).getId(), caio,
                    new DadosAtendimento("dor no peito", "ansiedade", "reduzir cafeína", "—", "eletrocardiograma", "retorno em 30 dias"));
            consultas.realizar(agenda.agendar(bruno, rui, hoje).getId(), rui,
                    new DadosAtendimento("febre e tosse", "gripe", "repouso e hidratação", "dipirona", "—", ""));
            consultas.realizar(agenda.agendar(bruno, lia, hoje).getId(), lia,
                    new DadosAtendimento("palpitação", "taquicardia leve", "acompanhamento", "—", "holter", ""));
            return null;
        });
    }
}

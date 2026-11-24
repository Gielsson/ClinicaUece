package service;

import exception.*;
import java.util.List;
import model.*;
import repository.*;

public class ConsultaService {
    private final ConsultaRepositorio consultas = new ConsultaRepositorio();
    private final ContaRepositorio contas = new ContaRepositorio();

    // Paciente com plano não paga (valor 0 e nenhuma Conta). Sem plano, nasce uma Conta com o valor
    // particular da especialidade, e é por isso que Cardiologia sai mais cara. Consulta e conta
    // são gravadas juntas: não pode existir consulta realizada sem a cobrança correspondente.
    public void realizar(int consultaId, Medico medico, DadosAtendimento d) throws ClinicaException {
        if (d == null || vazio(d.sintomas()) || vazio(d.diagnostico()))
            throw new ClinicaException("Informe ao menos os sintomas e o diagnóstico.");
        BancoDados.transacao(() -> {
            Consulta c = consultas.buscarPorId(consultaId)
                    .orElseThrow(() -> new ConsultaInexistenteException("Consulta não encontrada."));
            if (c.getMedico().getId() != medico.getId())
                throw new AcessoNegadoException("Esta consulta não pertence a você.");
            Paciente p = c.getPaciente();
            double valor = p.temPlano() ? 0 : c.getMedico().valorParticular();
            c.realizar(d, valor);
            consultas.atualizar(c);
            if (!p.temPlano()) contas.salvar(new Conta(p, c, valor));
            return null;
        });
    }

    // Médico que já atendeu o paciente continua podendo ver o histórico dele; um médico sem vínculo nenhum, não.
    public Prontuario prontuarioDe(Paciente alvo, Usuario solicitante) throws ClinicaException {
        if (solicitante == null) throw new AcessoNegadoException("Faça login para acessar o prontuário.");
        if (solicitante instanceof Paciente) {
            if (solicitante.getId() != alvo.getId())
                throw new AcessoNegadoException("Você só pode ver o seu próprio prontuário.");
        } else if (solicitante instanceof Medico) {
            if (!consultas.medicoAtendeuOuAtende(solicitante.getId(), alvo.getId()))
                throw new AcessoNegadoException("Você não está autorizado a acessar o prontuário deste paciente.");
        }
        return new Prontuario(alvo, consultas.porPaciente(alvo.getId()));
    }

    public List<Consulta> historicoParaMedico(Paciente paciente, Medico medico) throws ClinicaException {
        return prontuarioDe(paciente, medico).entradas();
    }

    private static boolean vazio(String s) { return s == null || s.isBlank(); }
}

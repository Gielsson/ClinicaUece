package service;

import exception.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import model.*;
import repository.*;
import util.Datas;

/*
 * Aqui ficam as regras que mais pesam: limite do dia, duplicidade, fila e promoção.
 * Tudo que grava em mais de uma tabela roda dentro de BancoDados.transacao().
 */
public class AgendamentoService {
    private final ConsultaRepositorio consultas = new ConsultaRepositorio();
    private final ListaEsperaRepositorio espera = new ListaEsperaRepositorio();
    private final NotificacaoRepositorio notificacoes = new NotificacaoRepositorio();

    public int vagasRestantes(Medico m, LocalDate data) throws ClinicaException {
        return Math.max(0, m.limiteDiario() - consultas.contarOcupadas(m.getId(), data));
    }

    public Consulta agendar(Paciente p, Medico m, LocalDate data) throws ClinicaException {
        Datas.validarFutura(data);
        exigirPlanoAceito(p, m);
        return BancoDados.transacao(() -> {
            // Confere de novo aqui dentro: entre a tela mostrar "1 vaga" e o clique, outra pessoa pode ter pegado.
            if (consultas.existeAtiva(m.getId(), p.getId(), data))
                throw new AgendamentoDuplicadoException("Você já tem consulta com " + m.getNome()
                        + " em " + Datas.formatar(data) + ".");
            if (consultas.contarOcupadas(m.getId(), data) >= m.limiteDiario())
                throw new AgendaLotadaException("A agenda de " + m.getNome() + " está lotada em "
                        + Datas.formatar(data) + ". Você pode entrar na lista de espera.");
            Consulta c = new Consulta(m, p, data);
            consultas.salvar(c);
            return c;
        });
    }

    public EntradaListaEspera entrarNaFila(Paciente p, Medico m, LocalDate data) throws ClinicaException {
        Datas.validarFutura(data);
        exigirPlanoAceito(p, m);
        return BancoDados.transacao(() -> {
            if (consultas.existeAtiva(m.getId(), p.getId(), data))
                throw new AgendamentoDuplicadoException("Você já tem consulta com " + m.getNome()
                        + " em " + Datas.formatar(data) + ".");
            // se ainda tem vaga o certo é agendar; fila só existe para agenda cheia
            if (consultas.contarOcupadas(m.getId(), data) < m.limiteDiario())
                throw new ClinicaException("Ainda há vagas nesta data: agende normalmente.");
            if (espera.existe(m.getId(), p.getId(), data))
                throw new ClinicaException("Você já está na lista de espera deste médico para esta data.");
            EntradaListaEspera e = new EntradaListaEspera(m, p, data);
            espera.salvar(e);
            return e;
        });
    }

    public int posicaoNaFila(EntradaListaEspera e) throws ClinicaException {
        return espera.posicao(e);
    }

    public void sairDaFila(int entradaId) throws ClinicaException {
        espera.remover(entradaId);
    }

    // Cancelar, promover o 1º da fila e avisar ele são uma coisa só. Se a notificação falhasse com a
    // consulta já cancelada, a vaga sumiria sem ninguém ocupá-la; por isso tudo na mesma transação.
    public void cancelar(int consultaId, Usuario quem) throws ClinicaException {
        BancoDados.transacao(() -> {
            Consulta c = consultas.buscarPorId(consultaId)
                    .orElseThrow(() -> new ConsultaInexistenteException("Consulta não encontrada."));
            if (!c.pertenceA(quem)) throw new AcessoNegadoException("Você não pode cancelar esta consulta.");
            c.cancelar();
            consultas.atualizar(c);
            promoverDaFila(c);
            return null;
        });
    }

    private void promoverDaFila(Consulta cancelada) throws ClinicaException {
        Medico m = cancelada.getMedico();
        LocalDate data = cancelada.getData();
        Optional<EntradaListaEspera> proximo;
        while ((proximo = espera.primeiraDaFila(m.getId(), data)).isPresent()) {
            EntradaListaEspera e = proximo.get();
            espera.remover(e.getId());
            // quem já marcou por fora enquanto esperava sai da fila e passamos para o próximo
            if (consultas.existeAtiva(m.getId(), e.getPaciente().getId(), data)) continue;
            consultas.salvar(new Consulta(m, e.getPaciente(), data));
            notificacoes.salvar(new Notificacao(e.getPaciente(),
                    "Você foi promovido da lista de espera: " + m.getNome() + " — " + Datas.formatar(data)));
            break;
        }
    }

    public List<Consulta> consultasDoPaciente(Paciente p) throws ClinicaException {
        return consultas.porPaciente(p.getId());
    }

    public List<EntradaListaEspera> filaDoPaciente(Paciente p) throws ClinicaException {
        return espera.porPaciente(p.getId());
    }

    public List<EntradaListaEspera> filaDoMedico(Medico m, LocalDate data) throws ClinicaException {
        return espera.porMedicoEData(m.getId(), data);
    }

    public List<Consulta> consultasDoDia(Medico m, LocalDate data) throws ClinicaException {
        return consultas.porMedicoEData(m.getId(), data);
    }

    // A tela já esconde esses médicos, mas a regra não pode depender só da tela.
    private void exigirPlanoAceito(Paciente p, Medico m) throws ClinicaException {
        if (p.temPlano() && !m.atende(p.getPlano()))
            throw new ClinicaException("Este médico não atende o plano " + p.getPlano().getRotulo() + ".");
    }
}

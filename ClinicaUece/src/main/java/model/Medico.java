package model;

import especialidade.Especialidade;
import java.util.*;

/*
 * Não existe MedicoCardiologista nem MedicoPediatra de propósito. O que muda de uma especialidade
 * pra outra (limite, preço, planos) mora na Especialidade e o médico só repassa a pergunta.
 * Assim, criar uma especialidade nova não obriga a mexer aqui.
 */
public class Medico extends Usuario {
    private final Especialidade especialidade;
    private final Set<PlanoSaude> planosAtendidos = EnumSet.noneOf(PlanoSaude.class);
    private final double valorBase;
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    public Medico(String nome, String login, String senhaHash, Especialidade especialidade,
                  Set<PlanoSaude> planos, double valorBase) {
        super(nome, login, senhaHash);
        if (valorBase < 0) throw new IllegalArgumentException("Valor-base inválido.");
        this.especialidade = especialidade;
        if (planos != null) this.planosAtendidos.addAll(planos);
        this.planosAtendidos.remove(PlanoSaude.NENHUM);   // "sem plano" não é um plano que o médico atende
        this.valorBase = valorBase;
    }

    // as três respostas abaixo vêm da Especialidade (é aqui que o polimorfismo acontece)
    public int limiteDiario() { return especialidade.limiteDiario(); }
    public double valorParticular() { return especialidade.calcularValor(valorBase); }
    public boolean atende(PlanoSaude plano) { return especialidade.aceitaPlano(plano, planosAtendidos); }

    public double mediaEstrelas() {
        return avaliacoes.stream().mapToInt(Avaliacao::getNota).average().orElse(0);
    }
    public void adicionarAvaliacao(Avaliacao a) { avaliacoes.add(a); }

    public Especialidade getEspecialidade() { return especialidade; }
    public Set<PlanoSaude> getPlanosAtendidos() { return Collections.unmodifiableSet(planosAtendidos); }
    public double getValorBase() { return valorBase; }
    public List<Avaliacao> getAvaliacoes() { return Collections.unmodifiableList(avaliacoes); }
    // quem preenche isso é o repositório, na hora de montar o médico a partir do banco
    public void setAvaliacoes(List<Avaliacao> lista) { this.avaliacoes = new ArrayList<>(lista); }
}

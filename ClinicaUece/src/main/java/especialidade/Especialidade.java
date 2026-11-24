package especialidade;

import model.PlanoSaude;
import java.util.Set;

/*
 * Comportamento padrão (3 por dia, preço = valor-base, aceita os planos do médico). Cada
 * especialidade sobrescreve só o que é diferente nela. O resto do sistema pergunta ao médico e
 * nunca precisa de um "if (especialidade == Pediatria)".
 */
public abstract class Especialidade {
    private final String nome;

    protected Especialidade(String nome) { this.nome = nome; }

    public String getNome() { return nome; }
    public int limiteDiario() { return 3; }
    public double calcularValor(double valorBase) { return valorBase; }
    public boolean aceitaPlano(PlanoSaude plano, Set<PlanoSaude> planosDoMedico) {
        return planosDoMedico.contains(plano);
    }

    @Override public String toString() { return nome; }
}

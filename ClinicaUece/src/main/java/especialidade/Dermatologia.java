package especialidade;

import model.PlanoSaude;
import java.util.EnumSet;
import java.util.Set;

// A interseção é de propósito: o plano precisa estar na lista da Dermatologia E o médico também tem que atendê-lo.
public class Dermatologia extends Especialidade {
    private static final Set<PlanoSaude> ACEITOS = EnumSet.of(PlanoSaude.UNIMED, PlanoSaude.BRADESCO);

    public Dermatologia() { super("Dermatologia"); }

    @Override public boolean aceitaPlano(PlanoSaude plano, Set<PlanoSaude> planosDoMedico) {
        return ACEITOS.contains(plano) && planosDoMedico.contains(plano);
    }
}

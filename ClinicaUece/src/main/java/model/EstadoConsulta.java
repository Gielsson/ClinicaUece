package model;

// As transições válidas moram aqui, num lugar só. Se a regra mudar, muda só este método.
public enum EstadoConsulta {
    AGENDADA, CANCELADA, REALIZADA;

    public boolean podeIrPara(EstadoConsulta novo) {
        return this == AGENDADA && (novo == CANCELADA || novo == REALIZADA);
    }
}

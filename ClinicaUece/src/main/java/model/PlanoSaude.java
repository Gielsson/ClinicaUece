package model;

public enum PlanoSaude {
    NENHUM("Não tenho"), UNIMED("Unimed"), HAPVIDA("Hapvida"), BRADESCO("Bradesco Saúde"), AMIL("Amil");

    private final String rotulo;
    PlanoSaude(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
    @Override public String toString() { return rotulo; }
}

package especialidade;

public class Cardiologia extends Especialidade {
    public Cardiologia() { super("Cardiologia"); }
    @Override public double calcularValor(double valorBase) { return valorBase * 1.30; }
}

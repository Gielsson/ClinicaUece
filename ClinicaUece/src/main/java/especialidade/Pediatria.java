package especialidade;

public class Pediatria extends Especialidade {
    public Pediatria() { super("Pediatria"); }
    @Override public int limiteDiario() { return 2; }
}

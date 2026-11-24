package especialidade;

// O banco guarda só o nome. Nome que não reconhecemos cai em ClinicaGeral em vez de quebrar a leitura.
public final class EspecialidadeFactory {
    private EspecialidadeFactory() {}

    public static Especialidade de(String nome) {
        if (nome == null) return new ClinicaGeral();
        return switch (nome.trim().toLowerCase()) {
            case "pediatria" -> new Pediatria();
            case "cardiologia" -> new Cardiologia();
            case "dermatologia" -> new Dermatologia();
            default -> new ClinicaGeral();
        };
    }
}

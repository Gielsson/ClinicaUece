package exception;

// Toda regra de negócio lança uma subclasse desta. A tela captura só ClinicaException e mostra
// getMessage(), então nenhuma regra violada derruba o programa.
public class ClinicaException extends Exception {
    public ClinicaException(String mensagem) { super(mensagem); }
    public ClinicaException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

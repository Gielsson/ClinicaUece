package exception;

public class ConsultaInexistenteException extends ClinicaException {
    public ConsultaInexistenteException(String mensagem) { super(mensagem); }
    public ConsultaInexistenteException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

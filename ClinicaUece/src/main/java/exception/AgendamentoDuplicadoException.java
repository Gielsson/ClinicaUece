package exception;

public class AgendamentoDuplicadoException extends ClinicaException {
    public AgendamentoDuplicadoException(String mensagem) { super(mensagem); }
    public AgendamentoDuplicadoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

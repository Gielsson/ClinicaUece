package exception;

public class PagamentoDuplicadoException extends ClinicaException {
    public PagamentoDuplicadoException(String mensagem) { super(mensagem); }
    public PagamentoDuplicadoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

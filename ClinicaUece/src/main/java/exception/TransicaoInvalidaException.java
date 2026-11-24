package exception;

public class TransicaoInvalidaException extends ClinicaException {
    public TransicaoInvalidaException(String mensagem) { super(mensagem); }
    public TransicaoInvalidaException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

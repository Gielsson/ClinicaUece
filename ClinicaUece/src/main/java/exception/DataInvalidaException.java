package exception;

public class DataInvalidaException extends ClinicaException {
    public DataInvalidaException(String mensagem) { super(mensagem); }
    public DataInvalidaException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

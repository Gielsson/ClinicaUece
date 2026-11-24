package exception;

public class LoginDuplicadoException extends ClinicaException {
    public LoginDuplicadoException(String mensagem) { super(mensagem); }
    public LoginDuplicadoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

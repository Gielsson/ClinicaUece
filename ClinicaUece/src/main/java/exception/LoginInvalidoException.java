package exception;

public class LoginInvalidoException extends ClinicaException {
    public LoginInvalidoException(String mensagem) { super(mensagem); }
    public LoginInvalidoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

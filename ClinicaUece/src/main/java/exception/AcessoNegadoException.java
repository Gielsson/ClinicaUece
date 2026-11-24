package exception;

public class AcessoNegadoException extends ClinicaException {
    public AcessoNegadoException(String mensagem) { super(mensagem); }
    public AcessoNegadoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

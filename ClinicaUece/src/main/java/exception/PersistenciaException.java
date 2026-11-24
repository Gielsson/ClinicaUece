package exception;

// Embrulha SQLException: quem chama não precisa saber que por baixo existe JDBC.
public class PersistenciaException extends ClinicaException {
    public PersistenciaException(String mensagem) { super(mensagem); }
    public PersistenciaException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

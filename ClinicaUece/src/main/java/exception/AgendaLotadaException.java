package exception;

// Exceção própria (e não a genérica) porque a tela usa ela pra oferecer a lista de espera.
public class AgendaLotadaException extends ClinicaException {
    public AgendaLotadaException(String mensagem) { super(mensagem); }
    public AgendaLotadaException(String mensagem, Throwable causa) { super(mensagem, causa); }
}

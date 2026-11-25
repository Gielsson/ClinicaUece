package util;

import exception.DataInvalidaException;
import java.time.LocalDate;
import java.time.format.*;

public final class Datas {
    // STRICT é importante: sem ele, "31/02/2026" viraria 28/02 em silêncio em vez de dar erro.
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private Datas() {}
    public static LocalDate parse(String texto) throws DataInvalidaException {
        try { return LocalDate.parse(texto == null ? "" : texto.trim(), FMT); }
        catch (DateTimeParseException e) {
            throw new DataInvalidaException("Data inválida. Use o formato dd/MM/aaaa (ex.: 02/12/2026).");
        }
    }
    // hoje ainda vale; só o passado é recusado (o nome ficou meio exagerado, mas é o que as telas já usam)
    public static void validarFutura(LocalDate d) throws DataInvalidaException {
        if (d.isBefore(LocalDate.now())) throw new DataInvalidaException("Não é possível agendar para uma data que já passou.");
    }
    public static String formatar(LocalDate d) { return d.format(FMT); }
}

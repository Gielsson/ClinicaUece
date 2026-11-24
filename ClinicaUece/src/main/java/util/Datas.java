package util;

import exception.DataInvalidaException;
import java.time.LocalDate;
import java.time.format.*;

/** Conversão e validação de datas dd/MM/aaaa (o núcleo depende dela). */
public final class Datas {
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private Datas() {}
    public static LocalDate parse(String texto) throws DataInvalidaException {
        try { return LocalDate.parse(texto == null ? "" : texto.trim(), FMT); }
        catch (DateTimeParseException e) {
            throw new DataInvalidaException("Data inválida. Use o formato dd/MM/aaaa (ex.: 02/12/2026).");
        }
    }
    public static void validarFutura(LocalDate d) throws DataInvalidaException {
        if (d.isBefore(LocalDate.now())) throw new DataInvalidaException("Não é possível agendar para uma data que já passou.");
    }
    public static String formatar(LocalDate d) { return d.format(FMT); }
}

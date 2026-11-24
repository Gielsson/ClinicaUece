package util;

import org.mindrot.jbcrypt.BCrypt;

/**  Hash de senha com BCrypt (o núcleo depende dela). */
public final class Seguranca {
    private Seguranca() {}
    public static String hash(String senha) { return BCrypt.hashpw(senha, BCrypt.gensalt(10)); }
    public static boolean confere(String senha, String hash) {
        try { return BCrypt.checkpw(senha, hash); } catch (IllegalArgumentException e) { return false; }
    }
}

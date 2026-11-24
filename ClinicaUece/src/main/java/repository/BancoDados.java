package repository;

import exception.ClinicaException;
import exception.PersistenciaException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Uma conexão só, e toda transação passa por transacao(). O motivo: cancelar uma consulta mexe em
 * três tabelas (consulta, fila de espera, notificação). Se der erro no meio, não pode sobrar
 * nada pela metade, então o commit só acontece no final e qualquer exceção faz rollback de tudo.
 */
public final class BancoDados {
    private static final Logger LOG = Logger.getLogger(BancoDados.class.getName());
    private static Connection conexao;
    private static int nivel = 0;               // profundidade das transações abertas

    private BancoDados() {}

    @FunctionalInterface public interface TrabalhoSql<T> { T executar() throws Exception; }
    @FunctionalInterface public interface Gravacao { void executar(Connection c) throws SQLException; }

    public static void abrir(String url) throws PersistenciaException {
        fechar();
        try {
            conexao = DriverManager.getConnection(url);
            // o SQLite só respeita chave estrangeira se pedirmos; por padrão ele ignora
            try (Statement s = conexao.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
            criarTabelas();
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível abrir o banco de dados.", e);
        }
    }

    public static void fechar() {
        if (conexao != null) {
            try { conexao.close(); } catch (SQLException e) { LOG.log(Level.WARNING, "Erro ao fechar o banco", e); }
            conexao = null;
            nivel = 0;
        }
    }

    public static Connection getConexao() throws PersistenciaException {
        if (conexao == null) throw new PersistenciaException("O banco de dados não foi aberto.");
        return conexao;
    }

    // synchronized porque a conexão é única e o autoCommit é estado dela
    public static synchronized <T> T transacao(TrabalhoSql<T> trabalho) throws ClinicaException {
        Connection c = getConexao();
        if (nivel > 0) {
            // Chamada de dentro de outra transação (ex.: agendar() dentro do cadastro dos dados de exemplo).
            // Aqui NÃO damos commit: quem decide é a transação de fora. Se commitássemos aqui, o
            // rollback da de fora não conseguiria mais desfazer este pedaço.
            try { return trabalho.executar(); }
            catch (ClinicaException e) { throw e; }
            catch (Exception e) { throw new PersistenciaException("Falha ao gravar. Nenhuma alteração foi aplicada.", e); }
        }
        try {
            nivel++;
            c.setAutoCommit(false);
            T resultado = trabalho.executar();
            c.commit();
            return resultado;
        } catch (Exception e) {
            try { c.rollback(); }
            catch (SQLException ex) { LOG.log(Level.SEVERE, "Falha no rollback", ex); }
            if (e instanceof ClinicaException ce) throw ce;
            throw new PersistenciaException("Falha ao gravar. Nenhuma alteração foi aplicada.", e);
        } finally {
            nivel--;
            try { c.setAutoCommit(true); }
            catch (SQLException ex) { LOG.log(Level.WARNING, "Não foi possível restaurar o auto-commit", ex); }
        }
    }

    // Atalho dos repositórios: participa da transação que já estiver aberta ou abre uma só pra essa gravação.
    public static void gravar(String mensagemErro, Gravacao g) throws PersistenciaException {
        try {
            transacao(() -> { g.executar(getConexao()); return null; });
        } catch (PersistenciaException e) {
            throw e;
        } catch (ClinicaException e) {
            throw new PersistenciaException(mensagemErro, e);
        }
    }

    // Não uso getGeneratedKeys(): em algumas versões do driver SQLite ele lança "not implemented".
    // Perguntar direto pro SQLite qual foi o último rowid funciona em todas.
    public static int ultimoId(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static List<Integer> listarInts(String sql, Object... params) throws PersistenciaException {
        List<Integer> r = new ArrayList<>();
        try (PreparedStatement ps = getConexao().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) r.add(rs.getInt(1)); }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha de leitura no banco de dados.", e);
        }
        return r;
    }

    public static int contar(String sql, Object... params) throws PersistenciaException {
        List<Integer> l = listarInts(sql, params);
        return l.isEmpty() ? 0 : l.get(0);
    }

    private static void criarTabelas() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "tipo TEXT NOT NULL CHECK(tipo IN ('MEDICO','PACIENTE')), nome TEXT NOT NULL, "
                + "login TEXT NOT NULL UNIQUE COLLATE NOCASE, senha_hash TEXT NOT NULL)",   // NOCASE: "Ana" e "ana" são o mesmo login
            "CREATE TABLE IF NOT EXISTS medico (usuario_id INTEGER PRIMARY KEY REFERENCES usuario(id), "
                + "especialidade TEXT NOT NULL, valor_base REAL NOT NULL)",
            "CREATE TABLE IF NOT EXISTS medico_plano (medico_id INTEGER NOT NULL REFERENCES medico(usuario_id), "
                + "plano TEXT NOT NULL, PRIMARY KEY(medico_id, plano))",
            "CREATE TABLE IF NOT EXISTS paciente (usuario_id INTEGER PRIMARY KEY REFERENCES usuario(id), "
                + "idade INTEGER NOT NULL, plano TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS consulta (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "medico_id INTEGER NOT NULL REFERENCES medico(usuario_id), "
                + "paciente_id INTEGER NOT NULL REFERENCES paciente(usuario_id), data TEXT NOT NULL, "
                + "estado TEXT NOT NULL CHECK(estado IN ('AGENDADA','CANCELADA','REALIZADA')), "
                + "sintomas TEXT, diagnostico TEXT, tratamento TEXT, medicamentos TEXT, exames TEXT, "
                + "observacoes TEXT, valor_cobrado REAL NOT NULL DEFAULT 0)",
            // Índice único PARCIAL: barra duas consultas ativas do mesmo paciente com o mesmo médico no
            // mesmo dia, mas o WHERE deixa remarcar depois de cancelar (a cancelada não entra na conta).
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_consulta_ativa ON consulta(medico_id, paciente_id, data) "
                + "WHERE estado <> 'CANCELADA'",
            "CREATE TABLE IF NOT EXISTS lista_espera (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "medico_id INTEGER NOT NULL REFERENCES medico(usuario_id), "
                + "paciente_id INTEGER NOT NULL REFERENCES paciente(usuario_id), data TEXT NOT NULL, "
                + "inscrito_em TEXT NOT NULL, UNIQUE(medico_id, paciente_id, data))",
            "CREATE TABLE IF NOT EXISTS avaliacao (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "consulta_id INTEGER NOT NULL UNIQUE REFERENCES consulta(id), texto TEXT, "
                + "nota INTEGER NOT NULL CHECK(nota BETWEEN 1 AND 5))",
            "CREATE TABLE IF NOT EXISTS conta (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "paciente_id INTEGER NOT NULL REFERENCES paciente(usuario_id), "
                + "consulta_id INTEGER NOT NULL UNIQUE REFERENCES consulta(id), valor REAL NOT NULL, "
                + "situacao TEXT NOT NULL CHECK(situacao IN ('PENDENTE','PAGA')))",
            "CREATE TABLE IF NOT EXISTS notificacao (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "paciente_id INTEGER NOT NULL REFERENCES paciente(usuario_id), mensagem TEXT NOT NULL, "
                + "data_hora TEXT NOT NULL, lida INTEGER NOT NULL DEFAULT 0)"
        };
        try (Statement s = conexao.createStatement()) {
            for (String sql : ddl) s.execute(sql);
        }
    }
}

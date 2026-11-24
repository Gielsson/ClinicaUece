package model;

import util.Seguranca;

public abstract class Usuario {
    private int id;
    private String nome;
    private final String login;
    private String senhaHash;        // nunca a senha em si, só o hash

    protected Usuario(String nome, String login, String senhaHash) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
    }

    public boolean autenticar(String senha) {
        return senha != null && Seguranca.confere(senha, senhaHash);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    @Override public String toString() { return nome; }
}

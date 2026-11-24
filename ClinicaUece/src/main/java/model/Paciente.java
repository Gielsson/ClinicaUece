package model;

public class Paciente extends Usuario {
    private int idade;
    private PlanoSaude plano;

    public Paciente(String nome, String login, String senhaHash, int idade, PlanoSaude plano) {
        super(nome, login, senhaHash);
        setIdade(idade);
        setPlano(plano);
    }

    public boolean temPlano() { return plano != PlanoSaude.NENHUM; }

    public int getIdade() { return idade; }
    public void setIdade(int idade) {
        if (idade < 0 || idade > 120) throw new IllegalArgumentException("Idade inválida: " + idade);
        this.idade = idade;
    }
    public PlanoSaude getPlano() { return plano; }
    public void setPlano(PlanoSaude plano) { this.plano = plano == null ? PlanoSaude.NENHUM : plano; }
}

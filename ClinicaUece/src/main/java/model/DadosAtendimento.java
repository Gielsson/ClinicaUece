package model;

// Existe só pra o realizar() não ficar com seis Strings soltas na assinatura.
public record DadosAtendimento(String sintomas, String diagnostico, String tratamento,
                               String medicamentos, String exames, String observacoes) {}

package app;

import javax.swing.*;
import repository.BancoDados;
import exception.ClinicaException;
import util.SwingUtil;

/*
 * Por enquanto abre só a janela do checkpoint. Quando as telas de login estiverem prontas,
 * é só trocar a última linha do main por: new view.LoginFrame().setVisible(true).
 */
public class Main {
    public static final String ARQUIVO_BANCO = "clinica.db";
    public static final String URL_BANCO = "jdbc:sqlite:" + ARQUIVO_BANCO;

    public static void main(String[] args) {
        usarTemaNimbus();
        try {
            BancoDados.abrir(URL_BANCO);
            DadosExemplo.popularSeVazio();
        } catch (ClinicaException e) {
            SwingUtil.erro(null, "Não foi possível iniciar o sistema: " + e.getMessage());
            return;
        }
        SwingUtilities.invokeLater(() -> new JanelaCheckpoint().setVisible(true));
    }

    private static void usarTemaNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); return; }
        } catch (Exception e) {
            System.err.println("Tema Nimbus indisponível, usando o padrão: " + e.getMessage());
        }
    }
}

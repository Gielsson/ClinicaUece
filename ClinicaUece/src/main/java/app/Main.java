package app;

import exception.ClinicaException;
import javax.swing.*;
import repository.BancoDados;
import util.SwingUtil;

/*
 * Por enquanto abre a janela principal de apoio. Quando as telas de login estiverem prontas,
 * é só trocar a última linha do main por: new view.LoginFrame().setVisible(true).
 */
public class Main {
    public static final String ARQUIVO_BANCO = "clinica.db";
    public static final String URL_BANCO = "jdbc:sqlite:" + ARQUIVO_BANCO;

    public static void main(String[] args) {
        aplicarTema();
        try {
            BancoDados.abrir(URL_BANCO);
            DadosExemplo.popularSeVazio();
        } catch (ClinicaException e) {
            SwingUtil.erro(null, "Não foi possível iniciar o sistema: " + e.getMessage());
            return;
        }
        SwingUtilities.invokeLater(() -> new JanelaClinica().setVisible(true));
    }

    // FlatLaf deixa o Swing com cara moderna. Se a biblioteca não estiver no classpath, cai no Nimbus.
    private static void aplicarTema() {
        try {
            Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            return;
        } catch (Exception e) {
            // sem FlatLaf: segue para o Nimbus
        }
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); return; }
        } catch (Exception e) {
            System.err.println("Não foi possível aplicar um tema: " + e.getMessage());
        }
    }
}

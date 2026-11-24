package view;

import exception.ClinicaException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.ConsultaService;
import util.Datas;
import util.SwingUtil;

public class AtendimentoPanel extends JPanel {
    private final ConsultaService servico = new ConsultaService();
    private final Medico medico;
    private final Consulta consulta;
    private final Runnable aoFinalizar;

    private final JTextArea txtSintomas = new JTextArea(3, 20), txtDiagnostico = new JTextArea(3, 20),
            txtTratamento = new JTextArea(3, 20), txtMedicamentos = new JTextArea(3, 20),
            txtExames = new JTextArea(3, 20), txtObservacoes = new JTextArea(3, 20);
    private final DefaultTableModel modeloHistorico = SwingUtil.modelo("Data", "Médico", "Diagnóstico");
    private final JTable tabelaHistorico = SwingUtil.criar(modeloHistorico);
    private final JTextArea detalhes = new JTextArea(6, 24);
    private List<Consulta> historico = new ArrayList<>();

    public AtendimentoPanel(Medico medico, Consulta consulta, Runnable aoFinalizar) {
        this.medico = medico; this.consulta = consulta; this.aoFinalizar = aoFinalizar;
        Paciente p = consulta.getPaciente();
        setLayout(new BorderLayout(10, 10)); setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel cab = new JLabel("Atendimento — " + p.getNome() + " (" + p.getPlano().getRotulo() + "), "
                + p.getIdade() + " anos — " + Datas.formatar(consulta.getData()));
        cab.setFont(cab.getFont().deriveFont(Font.BOLD, 14f));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(campo("Sintomas *", txtSintomas));       form.add(campo("Diagnóstico *", txtDiagnostico));
        form.add(campo("Tratamento sugerido", txtTratamento)); form.add(campo("Medicamentos", txtMedicamentos));
        form.add(campo("Exames solicitados", txtExames)); form.add(campo("Observações gerais", txtObservacoes));

        detalhes.setEditable(false); detalhes.setLineWrap(true); detalhes.setWrapStyleWord(true);
        JPanel lateral = new JPanel(new BorderLayout(4, 4));
        lateral.setBorder(BorderFactory.createTitledBorder("Histórico do paciente"));
        lateral.add(new JScrollPane(tabelaHistorico), BorderLayout.CENTER);
        lateral.add(new JScrollPane(detalhes), BorderLayout.SOUTH);
        lateral.setPreferredSize(new Dimension(340, 0));

        JButton btnFinalizar = new JButton("Finalizar consulta"), btnVoltar = new JButton("Voltar");
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sul.add(btnFinalizar); sul.add(btnVoltar); sul.add(new JLabel("  * campos obrigatórios"));

        add(cab, BorderLayout.NORTH); add(form, BorderLayout.CENTER); add(lateral, BorderLayout.EAST); add(sul, BorderLayout.SOUTH);

        tabelaHistorico.getSelectionModel().addListSelectionListener(e -> {
            int i = tabelaHistorico.getSelectedRow();
            if (!e.getValueIsAdjusting() && i >= 0 && i < historico.size()) detalhes.setText(resumo(historico.get(i)));
        });
        btnFinalizar.addActionListener(e -> finalizar());
        btnVoltar.addActionListener(e -> aoFinalizar.run());
        atualizar();
    }

    private JPanel campo(String titulo, JTextArea area) {
        area.setLineWrap(true); area.setWrapStyleWord(true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        p.add(new JScrollPane(area));
        return p;
    }

    public void atualizar() {
        try {
            historico = servico.historicoParaMedico(consulta.getPaciente(), medico);
            modeloHistorico.setRowCount(0);
            for (Consulta c : historico)
                modeloHistorico.addRow(new Object[]{Datas.formatar(c.getData()), c.getMedico().getNome(), c.getDiagnostico()});
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    private String resumo(Consulta c) {
        return "Sintomas: " + c.getSintomas() + "\nDiagnóstico: " + c.getDiagnostico()
                + "\nTratamento: " + c.getTratamento() + "\nMedicamentos: " + c.getMedicamentos()
                + "\nExames: " + c.getExames() + "\nObservações: " + c.getObservacoes();
    }

    private void finalizar() {
        DadosAtendimento d = new DadosAtendimento(txtSintomas.getText(), txtDiagnostico.getText(), txtTratamento.getText(),
                txtMedicamentos.getText(), txtExames.getText(), txtObservacoes.getText());
        try {
            servico.realizar(consulta.getId(), medico, d);
            Paciente p = consulta.getPaciente();
            // o valor mostrado aqui é só informativo; quem calcula e grava a conta é o ConsultaService
            SwingUtil.info(this, p.temPlano()
                    ? "Consulta realizada e registrada no prontuário. Paciente com plano: sem cobrança."
                    : String.format("Consulta realizada e registrada no prontuário. Conta de R$ %.2f gerada para o paciente.",
                            medico.valorParticular()));
            aoFinalizar.run();
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }
}

package view;

import exception.ClinicaException;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.AgendamentoService;
import service.ConsultaService;
import util.Datas;
import util.SwingUtil;

public class AgendaMedicoPanel extends JPanel {
    private final AgendamentoService agenda = new AgendamentoService();
    private final ConsultaService consultasServico = new ConsultaService();
    private final Medico medico;
    private final JTextField txtData = new JTextField(Datas.formatar(LocalDate.now()), 10);
    private final JLabel lblVagas = new JLabel(" ");
    private final DefaultTableModel modelo = SwingUtil.modelo("#", "Paciente", "Plano", "Estado");
    private final JTable tabela = SwingUtil.criar(modelo);
    private List<Consulta> lista = new ArrayList<>();

    public AgendaMedicoPanel(Medico medico) {
        this.medico = medico;
        setLayout(new BorderLayout(8, 8)); setBorder(new EmptyBorder(12, 12, 12, 12));
        SwingUtil.colorir(tabela, 3);
        JButton btnCarregar = new JButton("Carregar"), btnIniciar = new JButton("Iniciar atendimento"),
                btnHistorico = new JButton("Ver histórico do paciente"), btnCancelar = new JButton("Cancelar consulta");
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Data (dd/MM/aaaa):")); topo.add(txtData); topo.add(btnCarregar); topo.add(lblVagas);
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sul.add(btnIniciar); sul.add(btnHistorico); sul.add(btnCancelar);
        add(topo, BorderLayout.NORTH); add(new JScrollPane(tabela), BorderLayout.CENTER); add(sul, BorderLayout.SOUTH);

        btnCarregar.addActionListener(e -> atualizar());
        txtData.addActionListener(e -> atualizar());
        btnIniciar.addActionListener(e -> iniciarAtendimento());
        btnHistorico.addActionListener(e -> verHistorico());
        btnCancelar.addActionListener(e -> cancelar());
    }

    public void atualizar() {
        try {
            LocalDate data = Datas.parse(txtData.getText());
            lista = agenda.consultasDoDia(medico, data);
            modelo.setRowCount(0);
            int ocupadas = 0, n = 1;
            for (Consulta c : lista) {
                if (c.getEstado() != EstadoConsulta.CANCELADA) ocupadas++;
                modelo.addRow(new Object[]{n++, c.getPaciente().getNome(), c.getPaciente().getPlano().getRotulo(), c.getEstado().name()});
            }
            lblVagas.setText("   Vagas ocupadas: " + ocupadas + "/" + medico.limiteDiario());
            lblVagas.setForeground(ocupadas >= medico.limiteDiario() ? new Color(0xC94040) : new Color(0x2E9E57));
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    private void iniciarAtendimento() {
        Consulta c = SwingUtil.selecionado(this, tabela, lista, "Selecione uma consulta.");
        if (c == null) return;
        if (c.getEstado() != EstadoConsulta.AGENDADA) {
            SwingUtil.erro(this, "Só é possível atender consultas AGENDADAS (esta está " + c.getEstado() + ").");
            return;
        }
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Atendimento", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setContentPane(new AtendimentoPanel(medico, c, dlg::dispose));
        dlg.setSize(960, 560); dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        atualizar();
    }

    private void verHistorico() {
        Consulta c = SwingUtil.selecionado(this, tabela, lista, "Selecione uma consulta.");
        if (c == null) return;
        try {
            List<Consulta> hist = consultasServico.historicoParaMedico(c.getPaciente(), medico);
            DefaultTableModel m = SwingUtil.modelo("Data", "Médico", "Sintomas", "Diagnóstico", "Medicamentos", "Exames");
            for (Consulta h : hist)
                m.addRow(new Object[]{Datas.formatar(h.getData()), h.getMedico().getNome(), h.getSintomas(),
                        h.getDiagnostico(), h.getMedicamentos(), h.getExames()});
            JScrollPane sp = new JScrollPane(SwingUtil.criar(m));
            sp.setPreferredSize(new Dimension(720, 260));
            JOptionPane.showMessageDialog(this, hist.isEmpty() ? new JLabel("Este paciente ainda não tem consultas realizadas.") : sp,
                    "Histórico de " + c.getPaciente().getNome(), JOptionPane.PLAIN_MESSAGE);
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    private void cancelar() {
        Consulta c = SwingUtil.selecionado(this, tabela, lista, "Selecione uma consulta.");
        if (c == null || !SwingUtil.confirmar(this, "Cancelar a consulta de " + c.getPaciente().getNome() + "?")) return;
        try {
            agenda.cancelar(c.getId(), medico);
            SwingUtil.info(this, "Consulta cancelada.");
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
        atualizar();
    }
}

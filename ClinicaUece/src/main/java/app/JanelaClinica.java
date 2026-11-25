package app;

import exception.ClinicaException;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.*;
import repository.*;
import service.*;
import util.Datas;
import util.SwingUtil;
import view.AgendaMedicoPanel;

public class JanelaClinica extends JFrame {
    private final MedicoRepositorio medicosRepo = new MedicoRepositorio();
    private final PacienteRepositorio pacientesRepo = new PacienteRepositorio();
    private final ContaRepositorio contasRepo = new ContaRepositorio();
    private final NotificacaoRepositorio notificacoesRepo = new NotificacaoRepositorio();
    private final AgendamentoService agenda = new AgendamentoService();
    private final ConsultaService consultasServico = new ConsultaService();

    private List<Medico> medicos = new ArrayList<>();
    private List<Paciente> pacientes = new ArrayList<>();
    private boolean carregando = false;   // preencher os combos dispara listener; sem isso a tela recarregava várias vezes

    private final JTabbedPane abas = new JTabbedPane();
    private final JLabel status = new JLabel(" ");

    // médicos
    private final JTextField txtBusca = new JTextField(16);
    private final JComboBox<PlanoSaude> cmbPlano = new JComboBox<>(PlanoSaude.values());
    private final DefaultTableModel mMedicos = SwingUtil.modelo("Nome", "Especialidade", "Consulta particular",
            "Planos aceitos", "Consultas por dia");

    // agendamento
    private final JComboBox<Paciente> cmbPaciente = new JComboBox<>();
    private final JComboBox<Medico> cmbMedico = new JComboBox<>();
    private final JTextField txtData = new JTextField(Datas.formatar(LocalDate.now().plusDays(1)), 10);
    private final JLabel lblVagas = new JLabel(" ");
    private final DefaultTableModel mConsultas = SwingUtil.modelo("Paciente", "Plano", "Estado");
    private final JTable tConsultas = SwingUtil.criar(mConsultas);
    private final DefaultTableModel mFila = SwingUtil.modelo("Posição", "Paciente", "Plano");
    private final DefaultTableModel mAvisos = SwingUtil.modelo("Quando", "Mensagem", "Situação");
    private List<Consulta> consultasDoDia = new ArrayList<>();

    // agenda do médico
    private final JComboBox<Medico> cmbMedicoAgenda = new JComboBox<>();
    private final JPanel centroAgenda = new JPanel(new BorderLayout());

    // prontuário e contas
    private final JComboBox<Paciente> cmbPacienteProntuario = new JComboBox<>();
    private final DefaultTableModel mProntuario = SwingUtil.modelo("Data", "Médico", "Sintomas", "Diagnóstico",
            "Medicamentos", "Exames", "Valor");
    private final DefaultTableModel mContas = SwingUtil.modelo("Consulta", "Médico", "Valor", "Situação");

    public JanelaClinica() {
        super("Clínica UECE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(montarMenu());

        abas.addTab("Médicos", montarMedicos());
        abas.addTab("Agendamento", montarAgendamento());
        abas.addTab("Agenda do médico", montarAgendaMedico());
        abas.addTab("Prontuário e contas", montarProntuario());
        abas.addChangeListener(e -> atualizarAba());

        status.setBorder(new EmptyBorder(4, 10, 6, 10));
        add(abas, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setSize(1050, 620);
        setLocationRelativeTo(null);
        recarregar();
    }

    private JMenuBar montarMenu() {
        JMenuItem recriar = new JMenuItem("Recriar dados de exemplo");
        JMenuItem sair = new JMenuItem("Sair");
        recriar.addActionListener(e -> recriarDados());
        sair.addActionListener(e -> dispose());
        JMenu arquivo = new JMenu("Arquivo");
        arquivo.add(recriar); arquivo.addSeparator(); arquivo.add(sair);
        JMenuBar barra = new JMenuBar();
        barra.add(arquivo);
        return barra;
    }

    // ------------------------------------------------------------ médicos
    private JPanel montarMedicos() {
        JButton btnBuscar = new JButton("Buscar");
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Nome ou especialidade:")); topo.add(txtBusca);
        topo.add(new JLabel("Plano:")); topo.add(cmbPlano); topo.add(btnBuscar);
        btnBuscar.addActionListener(e -> atualizarMedicos());
        txtBusca.addActionListener(e -> atualizarMedicos());
        cmbPlano.addActionListener(e -> atualizarMedicos());
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(4, 4, 4, 4));
        p.add(topo, BorderLayout.NORTH);
        p.add(new JScrollPane(SwingUtil.criar(mMedicos)), BorderLayout.CENTER);
        return p;
    }

    private void atualizarMedicos() {
        PlanoSaude plano = (PlanoSaude) cmbPlano.getSelectedItem();
        mMedicos.setRowCount(0);
        try {
            for (Medico m : medicosRepo.buscarPorNomeOuEspecialidade(txtBusca.getText())) {
                if (plano != PlanoSaude.NENHUM && !m.atende(plano)) continue;
                String planos = m.getPlanosAtendidos().stream().map(PlanoSaude::getRotulo).collect(Collectors.joining(", "));
                mMedicos.addRow(new Object[]{m.getNome(), m.getEspecialidade().getNome(),
                        String.format("R$ %.2f", m.valorParticular()), planos, m.limiteDiario()});
            }
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    // ------------------------------------------------------------ agendamento
    private JPanel montarAgendamento() {
        SwingUtil.colorir(tConsultas, 2);
        JButton btnAgendar = new JButton("Agendar"), btnFila = new JButton("Entrar na fila de espera"),
                btnCancelar = new JButton("Cancelar consulta");
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Paciente:")); topo.add(cmbPaciente);
        topo.add(new JLabel("Médico:")); topo.add(cmbMedico);
        topo.add(new JLabel("Data:")); topo.add(txtData);
        topo.add(btnAgendar); topo.add(btnFila); topo.add(btnCancelar);

        JTable tAvisos = SwingUtil.criar(mAvisos);
        tAvisos.getColumnModel().getColumn(0).setPreferredWidth(70);
        tAvisos.getColumnModel().getColumn(1).setPreferredWidth(320);
        tAvisos.getColumnModel().getColumn(2).setPreferredWidth(60);
        // os avisos têm texto comprido, então ganham mais largura que as outras duas tabelas
        JPanel tabelas = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH; g.weighty = 1; g.insets = new Insets(0, 0, 0, 8);
        g.weightx = 0.27; tabelas.add(titulada("Consultas do dia", new JScrollPane(tConsultas)), g);
        g.weightx = 0.23; tabelas.add(titulada("Fila de espera", new JScrollPane(SwingUtil.criar(mFila))), g);
        g.weightx = 0.50; g.insets = new Insets(0, 0, 0, 0);
        tabelas.add(titulada("Avisos do paciente", new JScrollPane(tAvisos)), g);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(topo, BorderLayout.NORTH);
        lblVagas.setBorder(new EmptyBorder(0, 10, 4, 0));
        norte.add(lblVagas, BorderLayout.SOUTH);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(4, 4, 4, 4));
        p.add(norte, BorderLayout.NORTH); p.add(tabelas, BorderLayout.CENTER);

        cmbPaciente.addActionListener(e -> { if (!carregando) atualizarAgendamento(); });
        cmbMedico.addActionListener(e -> { if (!carregando) atualizarAgendamento(); });
        txtData.addActionListener(e -> atualizarAgendamento());
        txtData.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { atualizarAgendamento(); }
        });

        btnAgendar.addActionListener(e -> acao(() -> {
            Paciente pac = (Paciente) cmbPaciente.getSelectedItem();
            Medico m = (Medico) cmbMedico.getSelectedItem();
            LocalDate d = Datas.parse(txtData.getText());
            agenda.agendar(pac, m, d);
            status.setText("Consulta agendada: " + pac.getNome() + " com " + m.getNome() + " em " + Datas.formatar(d) + ".");
        }));
        btnFila.addActionListener(e -> acao(() -> {
            Paciente pac = (Paciente) cmbPaciente.getSelectedItem();
            Medico m = (Medico) cmbMedico.getSelectedItem();
            EntradaListaEspera entrada = agenda.entrarNaFila(pac, m, Datas.parse(txtData.getText()));
            status.setText(pac.getNome() + " entrou na fila de " + m.getNome() + " (posição " + agenda.posicaoNaFila(entrada) + ").");
        }));
        btnCancelar.addActionListener(e -> {
            Consulta c = SwingUtil.selecionado(this, tConsultas, consultasDoDia, "Selecione uma consulta na lista.");
            if (c == null) return;
            acao(() -> {
                agenda.cancelar(c.getId(), c.getPaciente());
                status.setText("Consulta de " + c.getPaciente().getNome() + " cancelada.");
            });
        });
        return p;
    }

    private JPanel titulada(String titulo, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        p.add(c);
        return p;
    }

    private void atualizarAgendamento() {
        mConsultas.setRowCount(0); mFila.setRowCount(0); mAvisos.setRowCount(0);
        consultasDoDia = new ArrayList<>();
        Medico m = (Medico) cmbMedico.getSelectedItem();
        Paciente pac = (Paciente) cmbPaciente.getSelectedItem();
        if (m == null || pac == null) return;
        try {
            LocalDate d = Datas.parse(txtData.getText());
            consultasDoDia = agenda.consultasDoDia(m, d);
            for (Consulta c : consultasDoDia)
                mConsultas.addRow(new Object[]{c.getPaciente().getNome(), c.getPaciente().getPlano().getRotulo(), c.getEstado().name()});
            for (EntradaListaEspera e : agenda.filaDoMedico(m, d))
                mFila.addRow(new Object[]{agenda.posicaoNaFila(e) + "º", e.getPaciente().getNome(), e.getPaciente().getPlano().getRotulo()});
            int vagas = agenda.vagasRestantes(m, d);
            lblVagas.setText(vagas == 0 ? "Agenda lotada em " + Datas.formatar(d)
                    : "Vagas em " + Datas.formatar(d) + ": " + vagas + " de " + m.limiteDiario());
            lblVagas.setForeground(vagas == 0 ? new Color(0xC94040) : new Color(0x2E9E57));
        } catch (ClinicaException ex) {
            lblVagas.setText(ex.getMessage());
            lblVagas.setForeground(new Color(0xC94040));
        }
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            for (Notificacao n : notificacoesRepo.porPaciente(pac.getId()))
                mAvisos.addRow(new Object[]{n.getDataHora().format(f), n.getMensagem(), n.isLida() ? "Lida" : "Nova"});
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    // ------------------------------------------------------------ agenda do médico
    private JPanel montarAgendaMedico() {
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Médico:")); topo.add(cmbMedicoAgenda);
        cmbMedicoAgenda.addActionListener(e -> { if (!carregando) atualizarAgendaMedico(); });
        JPanel p = new JPanel(new BorderLayout());
        p.add(topo, BorderLayout.NORTH); p.add(centroAgenda, BorderLayout.CENTER);
        return p;
    }

    private void atualizarAgendaMedico() {
        centroAgenda.removeAll();
        Medico m = (Medico) cmbMedicoAgenda.getSelectedItem();
        if (m != null) {
            AgendaMedicoPanel painel = new AgendaMedicoPanel(m);
            centroAgenda.add(painel, BorderLayout.CENTER);
            painel.atualizar();
        }
        centroAgenda.revalidate(); centroAgenda.repaint();
    }

    // ------------------------------------------------------------ prontuário e contas
    private JPanel montarProntuario() {
        JTable tContas = SwingUtil.criar(mContas);
        SwingUtil.colorir(tContas, 3);
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Paciente:")); topo.add(cmbPacienteProntuario);
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 8));
        centro.add(titulada("Prontuário", new JScrollPane(SwingUtil.criar(mProntuario))));
        centro.add(titulada("Contas", new JScrollPane(tContas)));
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(4, 4, 4, 4));
        p.add(topo, BorderLayout.NORTH); p.add(centro, BorderLayout.CENTER);
        cmbPacienteProntuario.addActionListener(e -> { if (!carregando) atualizarProntuario(); });
        return p;
    }

    private void atualizarProntuario() {
        mProntuario.setRowCount(0); mContas.setRowCount(0);
        Paciente p = (Paciente) cmbPacienteProntuario.getSelectedItem();
        if (p == null) return;
        try {
            for (Consulta c : consultasServico.prontuarioDe(p, p).entradas())
                mProntuario.addRow(new Object[]{Datas.formatar(c.getData()), c.getMedico().getNome(), c.getSintomas(),
                        c.getDiagnostico(), c.getMedicamentos(), c.getExames(), String.format("R$ %.2f", c.getValorCobrado())});
            for (Conta c : contasRepo.porPaciente(p.getId(), null))
                mContas.addRow(new Object[]{Datas.formatar(c.getConsulta().getData()), c.getConsulta().getMedico().getNome(),
                        String.format("R$ %.2f", c.getValor()), c.getSituacao().name()});
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
    }

    // ------------------------------------------------------------ geral
    private interface Acao { void executar() throws ClinicaException; }

    // Regra violada vira mensagem na tela; a janela nunca cai por causa disso.
    private void acao(Acao a) {
        try { a.executar(); }
        catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
        atualizarAba();
    }

    private void atualizarAba() {
        switch (abas.getSelectedIndex()) {
            case 0 -> atualizarMedicos();
            case 1 -> atualizarAgendamento();
            case 2 -> atualizarAgendaMedico();
            case 3 -> atualizarProntuario();
            default -> { }
        }
    }

    private void recarregar() {
        carregando = true;
        try {
            medicos = medicosRepo.listarTodos();
            pacientes = pacientesRepo.listarTodos();
        } catch (ClinicaException ex) { SwingUtil.erro(this, ex.getMessage()); }
        preencher(cmbMedico, medicos); preencher(cmbMedicoAgenda, medicos);
        preencher(cmbPaciente, pacientes); preencher(cmbPacienteProntuario, pacientes);
        carregando = false;
        atualizarAba();
    }

    private <T> void preencher(JComboBox<T> combo, List<T> itens) {
        int sel = Math.max(0, combo.getSelectedIndex());
        combo.setModel(new DefaultComboBoxModel<>(new Vector<>(itens)));
        if (!itens.isEmpty()) combo.setSelectedIndex(Math.min(sel, itens.size() - 1));
    }

    private void recriarDados() {
        if (!SwingUtil.confirmar(this, "Apagar o banco e recriar os dados de exemplo?")) return;
        try {
            BancoDados.fechar();
            Files.deleteIfExists(Path.of(Main.ARQUIVO_BANCO));
            BancoDados.abrir(Main.URL_BANCO);
            DadosExemplo.popularSeVazio();
            recarregar();
            status.setText("Dados de exemplo recriados.");
        } catch (Exception ex) { SwingUtil.erro(this, "Não foi possível recriar os dados: " + ex.getMessage()); }
    }
}

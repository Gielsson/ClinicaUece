package app;

import exception.ClinicaException;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

/*
 * Janela só para apresentar o núcleo no checkpoint, sem depender das telas finais. Ela chama os
 * mesmos services que as telas de verdade vão chamar, então o que aparece aqui é a regra real.
 */
public class JanelaCheckpoint extends JFrame {
    private final MedicoRepositorio medicosRepo = new MedicoRepositorio();
    private final PacienteRepositorio pacientesRepo = new PacienteRepositorio();
    private final ContaRepositorio contasRepo = new ContaRepositorio();
    private final NotificacaoRepositorio notificacoesRepo = new NotificacaoRepositorio();
    private final AgendamentoService agenda = new AgendamentoService();
    private final ConsultaService consultasServico = new ConsultaService();

    private List<Medico> medicos = new ArrayList<>();
    private List<Paciente> pacientes = new ArrayList<>();
    private boolean carregando = false;   // evita que preencher os combos dispare os listeners e recarregue tudo várias vezes

    private final JTabbedPane abas = new JTabbedPane();
    private final JTextArea log = new JTextArea(5, 80);

    private final JComboBox<PlanoSaude> cmbPlano = new JComboBox<>(PlanoSaude.values());
    private final DefaultTableModel mEsp = SwingUtil.modelo("Médico", "Especialidade", "Limite/dia", "Valor-base",
            "Valor particular", "Planos atendidos", "Aparece na busca?");
    private final JComboBox<Paciente> cmbPac2 = new JComboBox<>();
    private final JComboBox<Medico> cmbMed2 = new JComboBox<>();
    private final JTextField txtData = new JTextField(Datas.formatar(LocalDate.now().plusDays(1)), 10);
    private final JLabel lblVagas = new JLabel(" ");
    private final DefaultTableModel mCons = SwingUtil.modelo("Paciente", "Plano", "Estado");
    private final JTable tCons = SwingUtil.criar(mCons);
    private final DefaultTableModel mFila = SwingUtil.modelo("Posição", "Paciente", "Plano");
    private final DefaultTableModel mNotif = SwingUtil.modelo("Quando", "Mensagem", "Situação");
    private List<Consulta> consultasLista = new ArrayList<>();
    private final JComboBox<Medico> cmbMed3 = new JComboBox<>();
    private final JPanel centroAba3 = new JPanel(new BorderLayout());
    private final JComboBox<Paciente> cmbPac4 = new JComboBox<>();
    private final DefaultTableModel mPront = SwingUtil.modelo("Data", "Médico", "Sintomas", "Diagnóstico", "Medicamentos", "Exames", "Valor");
    private final DefaultTableModel mContas = SwingUtil.modelo("Consulta", "Médico", "Valor", "Situação");

    public JanelaCheckpoint() {
        super("ClinicaUece — Núcleo do sistema — Checkpoint");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        abas.addTab("1. Especialidades (polimorfismo)", montarEspecialidades());
        abas.addTab("2. Agendamento e lista de espera", montarAgendamento());
        abas.addTab("3. Agenda do médico", montarAgendaMedico());
        abas.addTab("4. Prontuário e contas", montarProntuario());
        abas.addChangeListener(e -> atualizarAba());

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JButton btnRecriar = new JButton("Recriar dados de exemplo");
        btnRecriar.addActionListener(e -> recriarDados());
        JPanel rodape = new JPanel(new BorderLayout(6, 6));
        rodape.setBorder(new EmptyBorder(0, 8, 8, 8));
        rodape.add(new JScrollPane(log), BorderLayout.CENTER);
        rodape.add(btnRecriar, BorderLayout.EAST);
        add(abas, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        recarregar();
        log("Pronto. Use as abas para demonstrar as regras do núcleo (senha dos usuários de exemplo: 123456).");
    }

    private JPanel montarEspecialidades() {
        JTable t = SwingUtil.criar(mEsp);
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Plano do paciente:")); topo.add(cmbPlano);
        topo.add(new JLabel("   (a coluna da direita mostra o filtro de pesquisa; sem plano o paciente vê todos os médicos)"));
        cmbPlano.addActionListener(e -> atualizarEspecialidades());
        JLabel nota = new JLabel("<html>Cada especialidade responde de um jeito ao <b>mesmo método</b>: limiteDiario(), "
                + "calcularValor() e aceitaPlano(). Pediatria: 2 por dia • Cardiologia: valor-base × 1,30 • "
                + "Dermatologia: só UNIMED e BRADESCO.</html>");
        nota.setBorder(new EmptyBorder(6, 8, 6, 8));
        JPanel p = new JPanel(new BorderLayout());
        p.add(topo, BorderLayout.NORTH); p.add(new JScrollPane(t), BorderLayout.CENTER); p.add(nota, BorderLayout.SOUTH);
        return p;
    }

    private void atualizarEspecialidades() {
        PlanoSaude plano = (PlanoSaude) cmbPlano.getSelectedItem();
        mEsp.setRowCount(0);
        for (Medico m : medicos) {
            String planos = m.getPlanosAtendidos().stream().map(PlanoSaude::getRotulo).collect(Collectors.joining(", "));
            String aparece = plano == PlanoSaude.NENHUM ? "Sim (sem plano vê todos)" : (m.atende(plano) ? "Sim" : "Não");
            mEsp.addRow(new Object[]{m.getNome(), m.getEspecialidade().getNome(), m.limiteDiario(),
                    String.format("R$ %.2f", m.getValorBase()), String.format("R$ %.2f", m.valorParticular()), planos, aparece});
        }
    }

    private JPanel montarAgendamento() {
        SwingUtil.colorir(tCons, 2);
        JButton btnVagas = new JButton("Ver vagas"), btnAgendar = new JButton("Agendar"),
                btnFila = new JButton("Entrar na lista de espera"), btnCancelar = new JButton("Cancelar consulta selecionada");
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Paciente:")); topo.add(cmbPac2);
        topo.add(new JLabel("Médico:")); topo.add(cmbMed2);
        topo.add(new JLabel("Data:")); topo.add(txtData);
        topo.add(btnVagas); topo.add(btnAgendar); topo.add(btnFila); topo.add(btnCancelar);

        JPanel tabelas = new JPanel(new GridLayout(1, 3, 8, 0));
        tabelas.add(titulada("Consultas do médico na data", new JScrollPane(tCons)));
        tabelas.add(titulada("Lista de espera (ordem de inscrição)", new JScrollPane(SwingUtil.criar(mFila))));
        JTable tNotif = SwingUtil.criar(mNotif);
        tNotif.getColumnModel().getColumn(0).setPreferredWidth(70);
        tNotif.getColumnModel().getColumn(1).setPreferredWidth(320);
        tNotif.getColumnModel().getColumn(2).setPreferredWidth(60);
        tabelas.add(titulada("Notificações do paciente escolhido", new JScrollPane(tNotif)));
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(4, 4, 4, 4));
        JPanel norte = new JPanel(new BorderLayout()); norte.add(topo, BorderLayout.NORTH); norte.add(lblVagas, BorderLayout.SOUTH);
        p.add(norte, BorderLayout.NORTH); p.add(tabelas, BorderLayout.CENTER);

        cmbPac2.addActionListener(e -> { if (!carregando) atualizarAgendamento(); });
        cmbMed2.addActionListener(e -> { if (!carregando) atualizarAgendamento(); });
        txtData.addActionListener(e -> atualizarAgendamento());
        btnVagas.addActionListener(e -> acao(() -> {
            Medico m = (Medico) cmbMed2.getSelectedItem(); LocalDate d = Datas.parse(txtData.getText());
            log("Vagas de " + m.getNome() + " em " + Datas.formatar(d) + ": " + agenda.vagasRestantes(m, d) + " (limite " + m.limiteDiario() + "/dia)");
        }));
        btnAgendar.addActionListener(e -> acao(() -> {
            Paciente pac = (Paciente) cmbPac2.getSelectedItem(); Medico m = (Medico) cmbMed2.getSelectedItem();
            LocalDate d = Datas.parse(txtData.getText());
            agenda.agendar(pac, m, d);
            log("AGENDADA: " + pac.getNome() + " com " + m.getNome() + " em " + Datas.formatar(d));
        }));
        btnFila.addActionListener(e -> acao(() -> {
            Paciente pac = (Paciente) cmbPac2.getSelectedItem(); Medico m = (Medico) cmbMed2.getSelectedItem();
            EntradaListaEspera en = agenda.entrarNaFila(pac, m, Datas.parse(txtData.getText()));
            log("LISTA DE ESPERA: " + pac.getNome() + " entrou na fila de " + m.getNome() + " — posição " + agenda.posicaoNaFila(en) + "º");
        }));
        btnCancelar.addActionListener(e -> {
            Consulta c = SwingUtil.selecionado(this, tCons, consultasLista, "Selecione uma consulta na tabela da esquerda.");
            if (c == null) return;
            acao(() -> {
                agenda.cancelar(c.getId(), c.getPaciente());
                log("CANCELADA: " + c.getPaciente().getNome() + " com " + c.getMedico().getNome()
                        + ". Se havia fila, o 1º foi promovido e notificado (veja as tabelas).");
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
        mCons.setRowCount(0); mFila.setRowCount(0); mNotif.setRowCount(0); consultasLista = new ArrayList<>();
        Medico m = (Medico) cmbMed2.getSelectedItem(); Paciente pac = (Paciente) cmbPac2.getSelectedItem();
        if (m == null || pac == null) return;
        try {
            LocalDate d = Datas.parse(txtData.getText());
            consultasLista = agenda.consultasDoDia(m, d);
            for (Consulta c : consultasLista)
                mCons.addRow(new Object[]{c.getPaciente().getNome(), c.getPaciente().getPlano().getRotulo(), c.getEstado().name()});
            for (EntradaListaEspera e : agenda.filaDoMedico(m, d))
                mFila.addRow(new Object[]{agenda.posicaoNaFila(e) + "º", e.getPaciente().getNome(), e.getPaciente().getPlano().getRotulo()});
            int vagas = agenda.vagasRestantes(m, d);
            lblVagas.setText("   " + m.getNome() + " (" + m.getEspecialidade().getNome() + ") — limite de " + m.limiteDiario()
                    + " por dia — vagas restantes em " + Datas.formatar(d) + ": " + vagas);
            lblVagas.setForeground(vagas == 0 ? new Color(0xC94040) : new Color(0x2E9E57));
        } catch (ClinicaException ex) {
            lblVagas.setText("   " + ex.getMessage()); lblVagas.setForeground(new Color(0xC94040));
        }
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
            for (Notificacao n : notificacoesRepo.porPaciente(pac.getId()))
                mNotif.addRow(new Object[]{n.getDataHora().format(f), n.getMensagem(), n.isLida() ? "Lida" : "NOVA"});
        } catch (ClinicaException ex) { log("ERRO: " + ex.getMessage()); }
    }

    private JPanel montarAgendaMedico() {
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Entrar como médico:")); topo.add(cmbMed3);
        cmbMed3.addActionListener(e -> { if (!carregando) atualizarAgendaMedico(); });
        JPanel p = new JPanel(new BorderLayout());
        p.add(topo, BorderLayout.NORTH); p.add(centroAba3, BorderLayout.CENTER);
        return p;
    }

    private void atualizarAgendaMedico() {
        centroAba3.removeAll();
        Medico m = (Medico) cmbMed3.getSelectedItem();
        if (m != null) {
            AgendaMedicoPanel painel = new AgendaMedicoPanel(m);
            centroAba3.add(painel, BorderLayout.CENTER);
            painel.atualizar();
        }
        centroAba3.revalidate(); centroAba3.repaint();
    }

    private JPanel montarProntuario() {
        JTable tPront = SwingUtil.criar(mPront), tContas = SwingUtil.criar(mContas);
        SwingUtil.colorir(tContas, 3);
        JButton btnIntruso = new JButton("Testar acesso ao prontuário de outro paciente");
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Paciente:")); topo.add(cmbPac4); topo.add(btnIntruso);
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 8));
        centro.add(titulada("Prontuário (consultas REALIZADAS)", new JScrollPane(tPront)));
        centro.add(titulada("Contas (só pacientes sem plano geram conta)", new JScrollPane(tContas)));
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(4, 4, 4, 4));
        p.add(topo, BorderLayout.NORTH); p.add(centro, BorderLayout.CENTER);
        cmbPac4.addActionListener(e -> { if (!carregando) atualizarProntuario(); });
        btnIntruso.addActionListener(e -> {
            Paciente alvo = (Paciente) cmbPac4.getSelectedItem();
            if (alvo == null || pacientes.size() < 2) return;
            Paciente intruso = pacientes.get((cmbPac4.getSelectedIndex() + 1) % pacientes.size());
            acaoSemAtualizar(() -> consultasServico.prontuarioDe(alvo, intruso),
                    intruso.getNome() + " tentou abrir o prontuário de " + alvo.getNome());
        });
        return p;
    }

    private void atualizarProntuario() {
        mPront.setRowCount(0); mContas.setRowCount(0);
        Paciente p = (Paciente) cmbPac4.getSelectedItem();
        if (p == null) return;
        try {
            for (Consulta c : consultasServico.prontuarioDe(p, p).entradas())
                mPront.addRow(new Object[]{Datas.formatar(c.getData()), c.getMedico().getNome(), c.getSintomas(),
                        c.getDiagnostico(), c.getMedicamentos(), c.getExames(), String.format("R$ %.2f", c.getValorCobrado())});
            for (Conta c : contasRepo.porPaciente(p.getId(), null))
                mContas.addRow(new Object[]{Datas.formatar(c.getConsulta().getData()), c.getConsulta().getMedico().getNome(),
                        String.format("R$ %.2f", c.getValor()), c.getSituacao().name()});
        } catch (ClinicaException ex) { log("ERRO: " + ex.getMessage()); }
    }

    private interface Acao { void executar() throws ClinicaException; }

    // Qualquer ClinicaException vira mensagem na tela e no log; a janela nunca cai por causa de regra de negócio.
    private void acao(Acao a) {
        try { a.executar(); }
        catch (ClinicaException ex) { log("BLOQUEADO (" + ex.getClass().getSimpleName() + "): " + ex.getMessage()); SwingUtil.erro(this, ex.getMessage()); }
        atualizarAba();
    }

    private void acaoSemAtualizar(Acao a, String descricao) {
        try { a.executar(); log("PERMITIDO: " + descricao); }
        catch (ClinicaException ex) { log("BLOQUEADO (" + ex.getClass().getSimpleName() + "): " + descricao + " -> " + ex.getMessage()); SwingUtil.erro(this, ex.getMessage()); }
    }

    private void atualizarAba() {
        switch (abas.getSelectedIndex()) {
            case 0 -> atualizarEspecialidades();
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
        preencher(cmbMed2, medicos); preencher(cmbMed3, medicos);
        preencher(cmbPac2, pacientes); preencher(cmbPac4, pacientes);
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
            log("Dados de exemplo recriados.");
        } catch (Exception ex) { SwingUtil.erro(this, "Falha ao recriar os dados: " + ex.getMessage()); }
    }

    private void log(String msg) {
        log.append(java.time.LocalTime.now().withNano(0) + "  " + msg + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}

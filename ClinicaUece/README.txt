ClinicaUece — Sistema de Clínica Médica (UECE)
==============================================

COMO ABRIR NO NETBEANS
  File > Open Project > pasta ClinicaUece (projeto Maven). Clean and Build baixa as dependências.
  Para rodar: botão direito no projeto > Run  (classe principal: app.Main).
  Abre a janela principal (médicos, agendamento, agenda do médico, prontuário e contas). O banco clinica.db é criado sozinho,
  já com médicos, pacientes e histórico de exemplo (senha de todos: 123456).

PACOTES (src/main/java)
  app            Main, DadosExemplo, JanelaClinica
  model          Usuario, Medico, Paciente, Consulta, DadosAtendimento, Avaliacao, Conta,
                 EntradaListaEspera, Notificacao, Prontuario, EstadoConsulta, SituacaoConta, PlanoSaude
  especialidade  Especialidade (abstract), ClinicaGeral, Pediatria, Cardiologia, Dermatologia,
                 EspecialidadeFactory
  exception      ClinicaException + 11 exceções próprias
  repository     BancoDados (transação), Repositorio, Usuario/Medico/Paciente/Consulta/ListaEspera/
                 Avaliacao/Conta/Notificacao Repositorio
  service        AgendamentoService, ConsultaService
  view           AgendaMedicoPanel, AtendimentoPanel
  util           Seguranca, Datas, SwingUtil

CLASSES CRIADAS, AINDA VAZIAS (a implementar)
  service        SessaoUsuario, Servicos, AutenticacaoService, MedicoService, ContaService,
                 AvaliacaoService, EstatisticaService, NotificacaoService
  view           Tema, Mensagens, Tabelas, TelaBase, LoginFrame, CadastroPacienteDialog, MainFrame,
                 BuscarMedicosPanel, AgendarDialog, MinhasConsultasPanel, AvaliarDialog,
                 ProntuarioPanel, ContasPanel, NotificacoesPanel, MeusDadosPanel, EstatisticasPanel
  util           DadosDemo

OBSERVAÇÕES PARA QUEM IMPLEMENTAR AS CLASSES VAZIAS
  - Os pacotes são curtos: no código enviado antes, apague o prefixo "br.uece.clinica."
    (ex.: import br.uece.clinica.model.*;  ->  import model.*;) e troque
    "model.especialidade" por "especialidade".
  - Quando as telas estiverem prontas, troque a última linha de app.Main.main por:
    SwingUtilities.invokeLater(() -> new view.LoginFrame().setVisible(true));
  - Os services prontos (AgendamentoService, ConsultaService) e todos os repositórios têm construtor
    sem argumentos. Métodos de repositório lançam PersistenciaException (subclasse de ClinicaException).
  - AgendaMedicoPanel e AtendimentoPanel extendem JPanel e têm atualizar(); se quiser, faça-os
    extender view.TelaBase.

REGRAS IMPLEMENTADAS
  - Limite diário vem da Especialidade (padrão 3; Pediatria 2), preço (Cardiologia x1,30) e
    planos aceitos (Dermatologia só UNIMED/BRADESCO): polimorfismo, sem subclasses de Medico.
  - Agendar: data não pode ser passada, plano aceito, sem duplicidade, sem passar do limite.
  - Lista de espera por médico+data, em ordem de inscrição.
  - Cancelar: libera a vaga, promove o 1º da fila e cria Notificacao — tudo em UMA transação.
  - Estados AGENDADA/CANCELADA/REALIZADA com transições validadas em EstadoConsulta/Consulta.
  - Realizar: grava prontuário; paciente sem plano gera Conta com o valor particular.
  - Prontuário: só o próprio paciente ou médico com consulta AGENDADA/REALIZADA com ele.
  - Persistência SQLite (arquivo clinica.db) com restrições (UNIQUE, CHECK, FK, índice parcial
    contra agendamento duplicado). BancoDados.transacao(): commit no fim, rollback em qualquer erro.

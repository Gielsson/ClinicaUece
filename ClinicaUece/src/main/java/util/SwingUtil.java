package util;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class SwingUtil {
    private SwingUtil() {}

    public static void erro(Component pai, String msg) { JOptionPane.showMessageDialog(pai, msg, "Atenção", JOptionPane.ERROR_MESSAGE); }
    public static void info(Component pai, String msg) { JOptionPane.showMessageDialog(pai, msg, "ClinicaUece", JOptionPane.INFORMATION_MESSAGE); }
    public static boolean confirmar(Component pai, String msg) {
        return JOptionPane.showConfirmDialog(pai, msg, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel modelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    public static JTable criar(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    public static void colorir(JTable t, int coluna) {
        t.getColumnModel().getColumn(coluna).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foco, int l, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, foco, l, c);
                if (!sel) comp.setForeground(switch (String.valueOf(v)) {
                    case "AGENDADA", "PENDENTE" -> new Color(0xD9922B);
                    case "REALIZADA", "PAGA" -> new Color(0x2E9E57);
                    case "CANCELADA" -> new Color(0xC94040);
                    default -> Color.BLACK; });
                return comp;
            }
        });
    }

    // Se não tem linha selecionada, já avisa o usuário e devolve null; quem chama só precisa checar o null.
    public static <T> T selecionado(Component pai, JTable t, List<T> dados, String aviso) {
        int i = t.getSelectedRow();
        if (i < 0 || i >= dados.size()) { info(pai, aviso); return null; }
        return dados.get(i);
    }
}

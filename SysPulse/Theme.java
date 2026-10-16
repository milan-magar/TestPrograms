import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

/** Central dark-theme constants and reusable styling helpers. */
public final class Theme {
    private Theme() {}

    /* ── Palette ──────────────────────────────────────────────── */
    public static final Color BG        = new Color(0x1E, 0x1E, 0x1E);
    public static final Color CARD_BG   = new Color(0x25, 0x25, 0x26);
    public static final Color HEADER_BG = new Color(0x2D, 0x2D, 0x30);
    public static final Color FG        = new Color(0xE0, 0xE0, 0xE0);
    public static final Color DIM       = new Color(0x88, 0x88, 0x88);
    public static final Color ACCENT    = new Color(0x00, 0x7A, 0xCC);
    public static final Color ACCENT_H  = new Color(0x1C, 0x97, 0xEA);
    public static final Color GREEN     = new Color(0x10, 0xB9, 0x81);
    public static final Color RED       = new Color(0xF0, 0x64, 0x64);
    public static final Color AMBER     = new Color(0xE6, 0xB4, 0x50);
    public static final Color BORDER    = new Color(0x3E, 0x3E, 0x44);
    public static final Color ALT_ROW   = new Color(0x2C, 0x2C, 0x30);
    public static final Color SEL_BG    = new Color(0x3C, 0x5A, 0x82);

    /* ── Typography ──────────────────────────────────────────── */
    public static final String SANS = "Segoe UI";
    public static final String MONO = "Consolas";

    public static final Font UI       = new Font(SANS, Font.PLAIN, 12);
    public static final Font UI_BOLD  = new Font(SANS, Font.BOLD, 12);
    public static final Font TITLE    = new Font(SANS, Font.BOLD, 16);
    public static final Font H1       = new Font(SANS, Font.BOLD, 24);
    public static final Font H2       = new Font(SANS, Font.BOLD, 14);
    public static final Font MONO_12  = new Font(MONO, Font.PLAIN, 12);

    /* ── Styling helpers ─────────────────────────────────────── */

    /** Flat accent-filled button that paints itself, immune to LAF overrides. */
    public static void styleButton(AbstractButton b) {
        b.setFont(UI_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));

        b.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                AbstractButton btn = (AbstractButton) c;
                Color base = !btn.isEnabled() ? new Color(90,90,95)
                           : btn.getModel().isPressed() ? ACCENT.darker()
                           : btn.getModel().isRollover() ? ACCENT_H
                           : ACCENT;
                g.setColor(base);
                g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);
                super.paint(g, c);
            }
        });
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.repaint(); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.repaint(); }
        });
    }

    /** Neutral dark button for secondary actions. */
    public static void styleSecondaryButton(AbstractButton b) {
        styleButton(b);
        b.setBackground(CARD_BG);
        b.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                AbstractButton btn = (AbstractButton) c;
                Color base = btn.getModel().isRollover() ? new Color(55,55,60) : CARD_BG;
                g.setColor(base);
                g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 6, 6);
                super.paint(g, c);
            }
        });
    }

    /** Dark toggle button whose color reflects selected state. */
    public static void styleToggle(AbstractButton b) {
        b.setFont(UI_BOLD);
        b.setForeground(FG);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                AbstractButton btn = (AbstractButton) c;
                boolean sel = btn.isSelected();
                Color base = sel ? (btn.getModel().isRollover() ? ACCENT_H : ACCENT)
                                 : (btn.getModel().isRollover() ? new Color(55,55,60) : CARD_BG);
                g.setColor(base);
                g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 6, 6);
                super.paint(g, c);
            }
        });
        b.addActionListener(e -> b.repaint());
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.repaint(); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.repaint(); }
        });
    }

    /** A themed, sortable JTable with alternating rows and dark headers. */
    public static JTable createTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(CARD_BG);
        t.setForeground(FG);
        t.setGridColor(BORDER);
        t.setFont(MONO_12);
        t.setRowHeight(24);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(SEL_BG);
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JTableHeader h = t.getTableHeader();
        h.setBackground(HEADER_BG);
        h.setForeground(FG);
        h.setFont(UI_BOLD);
        h.setPreferredSize(new Dimension(0, 30));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setReorderingAllowed(false);
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) h.getDefaultRenderer();
        hr.setHorizontalAlignment(SwingConstants.LEFT);
        hr.setBackground(HEADER_BG);
        hr.setForeground(FG);
        hr.setBorder(new EmptyBorder(0, 10, 0, 10));

        DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tab, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tab, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    int mr = tab.convertRowIndexToModel(row);
                    setBackground(mr % 2 == 0 ? CARD_BG : ALT_ROW);
                    setForeground(FG);
                } else {
                    setBackground(SEL_BG);
                    setForeground(Color.WHITE);
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, rend);
        return t;
    }

    public static JScrollPane wrapTable(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(CARD_BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Themed dark tabbed pane; used by MainFrame. */
    public static void styleTabbedPane(JTabbedPane pane) {
        pane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected void paintTabBackground(Graphics g, int placement,
                    int index, int x, int y, int w, int h, boolean selected) {
                g.setColor(selected ? BG : HEADER_BG);
                g.fillRect(x, y, w, h);
                if (selected) {
                    g.setColor(ACCENT);
                    g.fillRect(x, y, w, 2);
                }
            }
            @Override protected void paintTabBorder(Graphics g, int placement,
                    int index, int x, int y, int w, int h, boolean selected) {
                g.setColor(BORDER);
                g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
                g.drawLine(x, y, x, y + h - 1);
                g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
            }
            @Override protected void paintFocusIndicator(Graphics g, int placement,
                    Rectangle[] rects, int index, Rectangle iconRect,
                    Rectangle textRect, boolean selected) { /* none */ }
        });
        pane.setBackground(HEADER_BG);
        pane.setForeground(FG);
        pane.setFont(UI);
    }
}
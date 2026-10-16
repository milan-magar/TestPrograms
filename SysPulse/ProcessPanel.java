import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessPanel extends JPanel implements MainFrame.Refreshable {

    private final MainFrame frame;
    private final DefaultTableModel model;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JTextField searchField = new JTextField();
    private final JButton refreshBtn = new JButton("Refresh");
    private final JButton killBtn = new JButton("Terminate Process");
    private final JLabel statusText = new JLabel("Idle");
    private final JLabel statusDot  = new JLabel("\u25CF");

    private SwingWorker<String, Void> worker;

    public ProcessPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        /* ── Header ─────────────────────────────────────── */
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Theme.HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 14, 8, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        statusDot.setForeground(Theme.DIM);
        statusDot.setFont(new Font(Theme.SANS, Font.PLAIN, 14));
        statusText.setFont(Theme.UI); statusText.setForeground(Theme.DIM);
        left.add(statusDot); left.add(statusText);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        Theme.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());
        Theme.styleButton(killBtn);
        killBtn.addActionListener(e -> terminateSelected());
        right.add(killBtn); right.add(refreshBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        /* ── Search bar ─────────────────────────────────── */
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(Theme.BG);
        searchBar.setBorder(new EmptyBorder(12, 14, 6, 14));

        JLabel sl = new JLabel("Filter:");
        sl.setFont(Theme.UI); sl.setForeground(Theme.DIM);
        searchField.setFont(Theme.UI);
        searchField.setBackground(Theme.CARD_BG);
        searchField.setForeground(Theme.FG);
        searchField.setCaretColor(Theme.FG);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        searchField.setToolTipText("Type to filter processes by image name or PID");

        searchBar.add(sl, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);

        /* ── Table ─────────────────────────────────────── */
        model = new DefaultTableModel(
                new Object[]{"Image Name", "PID", "Session Name", "Session#", "Memory"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = Theme.createTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(1, (a, b) -> Long.compare(Long.parseLong(a.toString()),
                                                       Long.parseLong(b.toString())));
        sorter.setComparator(4, (a, b) -> Long.compare(parseMem(a.toString()),
                                                       parseMem(b.toString())));
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applyFilter(sorter); }
            public void removeUpdate(DocumentEvent e)  { applyFilter(sorter); }
            public void changedUpdate(DocumentEvent e) { applyFilter(sorter); }
        });

        scrollPane = Theme.wrapTable(table);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(searchBar, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void applyFilter(TableRowSorter<DefaultTableModel> sorter) {
        String q = searchField.getText().trim();
        if (q.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
    }

    @Override public void setBusy(boolean b) { refreshBtn.setEnabled(!b); }

    @Override public void refresh() {
        if (worker != null && !worker.isDone()) return;
        setBusy(true);
        statusDot.setForeground(Theme.AMBER);
        statusText.setText("Loading…");
        statusText.setForeground(Theme.AMBER);
        frame.setStatus("Processes: loading…");

        worker = CommandExecutor.runAsync("tasklist", this::render, err -> {
            setBusy(false);
            statusDot.setForeground(Theme.RED);
            statusText.setText("Error");
            statusText.setForeground(Theme.RED);
            frame.setStatus("Processes: " + err.getMessage());
        });
    }

    private void render(String raw) {
        int scroll = scrollPane.getVerticalScrollBar().getValue();
        model.setRowCount(0);
        Pattern p = Pattern.compile(
                "^(.+?)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+([\\d,]+\\s*K)\\s*$");
        for (String line : raw.split("\\r?\\n")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                model.addRow(new Object[]{
                        m.group(1).trim(), m.group(2), m.group(3), m.group(4),
                        m.group(5).replaceAll("\\s+", " ").trim()
                });
            }
        }
        setBusy(false);
        statusDot.setForeground(Theme.GREEN);
        statusText.setText("Ready");
        statusText.setForeground(Theme.GREEN);
        frame.setStatus("Processes: " + model.getRowCount() + " entries");
        SwingUtilities.invokeLater(() ->
                scrollPane.getVerticalScrollBar().setValue(
                        Math.min(scroll, scrollPane.getVerticalScrollBar().getMaximum())));
    }

    private void terminateSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select a process in the table first.",
                    "Terminate Process", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = String.valueOf(model.getValueAt(modelRow, 0));
        String pid  = String.valueOf(model.getValueAt(modelRow, 1));

        int r = JOptionPane.showConfirmDialog(this,
                "Terminate process \"" + name + "\" (PID " + pid + ")?\n\n"
              + "This issues:  taskkill /f /pid " + pid,
                "Confirm Termination", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        frame.setStatus("Terminating " + name + " (" + pid + ")…");
        CommandExecutor.runAsync("taskkill /f /pid " + pid,
                out -> {
                    frame.setStatus("Terminated: " + name);
                    refresh();
                },
                err -> {
                    frame.setStatus("taskkill error: " + err.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Could not terminate process:\n" + err.getMessage()
                          + "\n\n(Administrator rights may be required.)",
                            "taskkill", JOptionPane.ERROR_MESSAGE);
                });
    }

    private static long parseMem(String s) {
        try { return Long.parseLong(s.replace(",", "").replace("K", "").trim()); }
        catch (Exception e) { return 0; }
    }
}
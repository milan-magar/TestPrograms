import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkPanel extends JPanel implements MainFrame.Refreshable {

    private final MainFrame frame;

    private final DefaultTableModel adaptersModel = new DefaultTableModel(
            new Object[]{"Adapter", "Type", "IPv4 Address", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel connModel = new DefaultTableModel(
            new Object[]{"Protocol", "Local Address", "Foreign Address", "State"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable connTable;
    private final JScrollPane connScroll;
    private final JLabel statusText = new JLabel("Idle");
    private final JLabel statusDot  = new JLabel("\u25CF");
    private final JButton refreshBtn = new JButton("Refresh");
    private final JLabel countLbl = new JLabel("0 connections");

    private SwingWorker<String, Void> worker;

    public NetworkPanel(MainFrame frame) {
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
        countLbl.setFont(Theme.UI);  countLbl.setForeground(Theme.DIM);
        left.add(statusDot); left.add(statusText);
        left.add(Box.createHorizontalStrut(14)); left.add(countLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        Theme.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());
        right.add(refreshBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        /* ── Body ───────────────────────────────────────── */
        JTable adaptersTable = Theme.createTable(adaptersModel);
        adaptersTable.getColumnModel().getColumn(0).setPreferredWidth(320);
        adaptersTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        adaptersTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        adaptersTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        JScrollPane adaptersScroll = Theme.wrapTable(adaptersTable);

        connTable = Theme.createTable(connModel);
        connTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        connTable.getColumnModel().getColumn(1).setPreferredWidth(310);
        connTable.getColumnModel().getColumn(2).setPreferredWidth(310);
        connTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(connModel);
        connTable.setRowSorter(sorter);
        connScroll = Theme.wrapTable(connTable);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, adaptersScroll, connScroll);
        split.setResizeWeight(0.35);
        split.setBorder(new EmptyBorder(12, 12, 12, 12));
        split.setBackground(Theme.BG);
        split.setDividerLocation(200);
        add(split, BorderLayout.CENTER);
    }

    @Override public void setBusy(boolean b) { refreshBtn.setEnabled(!b); }

    @Override public void refresh() {
        if (worker != null && !worker.isDone()) return;
        setBusy(true);
        statusDot.setForeground(Theme.AMBER);
        statusText.setText("Scanning…"); statusText.setForeground(Theme.AMBER);
        frame.setStatus("Network: scanning…");

        String cmd = "ipconfig /all & echo ===NETSTAT=== & netstat -an";

        worker = CommandExecutor.runAsync(cmd, this::render, err -> {
            setBusy(false);
            statusDot.setForeground(Theme.RED);
            statusText.setText("Error"); statusText.setForeground(Theme.RED);
            frame.setStatus("Network: " + err.getMessage());
        });
    }

    private void render(String raw) {
        int split = raw.indexOf("===NETSTAT===");
        String ipPart = split >= 0 ? raw.substring(0, split) : raw;
        String nsPart = split >= 0 ? raw.substring(split + "===NETSTAT===".length()) : "";

        renderAdapters(ipPart);
        renderConnections(nsPart);

        setBusy(false);
        statusDot.setForeground(Theme.GREEN);
        statusText.setText("Ready"); statusText.setForeground(Theme.GREEN);
        countLbl.setText(connModel.getRowCount() + " connections");
        frame.setStatus("Network: " + adaptersModel.getRowCount() + " adapters, "
                + connModel.getRowCount() + " connections");
    }

    private void renderAdapters(String ipPart) {
        adaptersModel.setRowCount(0);
        String section = null;
        String type = "";
        String ipv4 = "";
        String status = "—";

        java.util.List<String[]> out = new java.util.ArrayList<>();

        for (String line : ipPart.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            if (!Character.isWhitespace(line.charAt(0))) {
                // flush previous section
                if (section != null) {
                    out.add(new String[]{section, type, ipv4, status});
                }
                section = line.replaceAll(":\\s*$", "").trim();
                type = section.toLowerCase().contains("wireless") || section.toLowerCase().contains("wi-fi")
                        ? "Wireless"
                        : section.toLowerCase().contains("ethernet") ? "Ethernet"
                        : section.toLowerCase().contains("tunnel") ? "Tunnel"
                        : section.toLowerCase().contains("loopback") ? "Loopback"
                        : "Other";
                ipv4 = "—";
                status = "Disconnected";
                continue;
            }
            String t = line.trim();
            if (t.startsWith("IPv4 Address") || t.startsWith("IPv4-Adresse")) {
                int c = t.indexOf(':');
                if (c > 0) ipv4 = t.substring(c + 1).replace("(Preferred)", "").trim();
            } else if (t.startsWith("Media State") || t.contains("Media disconnected")) {
                status = "Disconnected";
            } else if (t.startsWith("Default Gateway")) {
                int c = t.indexOf(':');
                if (c > 0 && !t.substring(c + 1).isBlank()) status = "Connected";
            }
        }
        if (section != null) out.add(new String[]{section, type, ipv4, status});
        for (String[] r : out) adaptersModel.addRow(r);
    }

    private void renderConnections(String nsPart) {
        int scroll = connScroll.getVerticalScrollBar().getValue();
        connModel.setRowCount(0);
        Pattern p = Pattern.compile("^\\s*(TCP|UDP)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\S+))?\\s*$");
        for (String line : nsPart.split("\\r?\\n")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                connModel.addRow(new Object[]{
                        m.group(1), m.group(2), m.group(3),
                        m.group(4) == null ? "" : m.group(4)
                });
            }
        }
        SwingUtilities.invokeLater(() ->
                connScroll.getVerticalScrollBar().setValue(
                        Math.min(scroll, connScroll.getVerticalScrollBar().getMaximum())));
    }
}
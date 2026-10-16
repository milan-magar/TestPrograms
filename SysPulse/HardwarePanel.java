import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardwarePanel extends JPanel implements MainFrame.Refreshable {

    private final MainFrame frame;
    private final File reportFile =
            new File(System.getProperty("java.io.tmpdir"), "syspulse-battery-report.html");

    private final DefaultTableModel batteryModel = new DefaultTableModel(
            new Object[]{"Property", "Value"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel diskModel = new DefaultTableModel(
            new Object[]{"Drive", "Label", "Type", "Total", "Free", "Free %"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JEditorPane htmlPane = new JEditorPane();
    private final JButton genBtn = new JButton("Generate Report");
    private final JButton openBtn = new JButton("Open in Browser");
    private final JLabel statusText = new JLabel("Idle");
    private final JLabel statusDot  = new JLabel("\u25CF");

    private SwingWorker<String, Void> worker;

    public HardwarePanel(MainFrame frame) {
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
        Theme.styleButton(genBtn);
        genBtn.addActionListener(e -> generateReport());
        Theme.styleSecondaryButton(openBtn);
        openBtn.addActionListener(e -> openReport());
        right.add(openBtn); right.add(genBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        /* ── Body: split pane — battery summary + HTML report / disks ── */
        JTable batTable = Theme.createTable(batteryModel);
        batTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        batTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        JScrollPane batScroll = Theme.wrapTable(batTable);

        htmlPane.setEditable(false);
        htmlPane.setContentType("text/html");
        htmlPane.setBackground(Theme.CARD_BG);
        htmlPane.setForeground(Theme.FG);
        JScrollPane htmlScroll = new JScrollPane(htmlPane);
        htmlScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        htmlScroll.getViewport().setBackground(Theme.CARD_BG);

        JTabbedPane subTabs = new JTabbedPane();
        Theme.styleTabbedPane(subTabs);
        subTabs.addTab("Battery Summary", batScroll);
        subTabs.addTab("Full Report (HTML)", htmlScroll);
        for (int i = 0; i < subTabs.getTabCount(); i++)
            subTabs.setForegroundAt(i, Theme.FG);

        JTable diskTable = Theme.createTable(diskModel);
        JScrollPane diskScroll = Theme.wrapTable(diskTable);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, subTabs, diskScroll);
        split.setResizeWeight(0.62);
        split.setBorder(new EmptyBorder(12, 12, 12, 12));
        split.setBackground(Theme.BG);
        split.setDividerLocation(360);
        add(split, BorderLayout.CENTER);

        htmlPane.setText("<html><body style='background:#252526;color:#888;"
                + "font-family:Segoe UI;padding:20px;'>"
                + "<h2 style='color:#e0e0e0;'>Full Battery Report</h2>"
                + "<p>Click <b>Generate Report</b> to create <code>powercfg /batteryreport</code> "
                + "output and embed it here.</p></body></html>");
    }

    @Override public void setBusy(boolean b) { genBtn.setEnabled(!b); }

    @Override public void refresh() {
        if (worker != null && !worker.isDone()) return;
        setBusy(true);
        statusDot.setForeground(Theme.AMBER);
        statusText.setText("Loading…"); statusText.setForeground(Theme.AMBER);
        frame.setStatus("Hardware: loading…");

        String cmd =
              "wmic PATH Win32_Battery get Name,EstimatedChargeRemaining,DesignCapacity,FullChargeCapacity /format:list"
            + " & wmic logicaldisk get DeviceID,VolumeName,DriveType,Size,FreeSpace /format:list";

        worker = CommandExecutor.runAsync(cmd, this::render, err -> {
            setBusy(false);
            statusDot.setForeground(Theme.RED);
            statusText.setText("Error"); statusText.setForeground(Theme.RED);
            frame.setStatus("Hardware: " + err.getMessage());
        });
    }

    private void render(String raw) {
        batteryModel.setRowCount(0);
        diskModel.setRowCount(0);

        // Split raw output at the first "DeviceID=" to separate battery from disks
        int diskIdx = raw.indexOf("DeviceID=");
        String batPart  = diskIdx > 0 ? raw.substring(0, diskIdx) : raw;
        String diskPart = diskIdx > 0 ? raw.substring(diskIdx) : "";

        // Battery
        for (String line : batPart.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            String v = line.substring(eq + 1).trim();
            if (v.isEmpty()) continue;
            batteryModel.addRow(new Object[]{k, v});
        }

        // Disks — grouped by DeviceID
        Pattern blockPat = Pattern.compile(
                "DeviceID=([^\\r\\n]+)[\\s\\S]*?DriveType=(\\d+)[\\s\\S]*?FreeSpace=(\\d+)[\\s\\S]*?Size=(\\d+)(?:[\\s\\S]*?VolumeName=([^\\r\\n]*))?");
        Matcher m = blockPat.matcher(diskPart);
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        while (m.find()) {
            String drive = m.group(1).trim();
            int type = Integer.parseInt(m.group(2));
            long free = parseLong(m.group(3));
            long size = parseLong(m.group(4));
            String label = m.group(5) == null ? "" : m.group(5).trim();
            String typeStr = type == 2 ? "Removable" : type == 3 ? "Fixed"
                           : type == 4 ? "Network" : type == 5 ? "CD-ROM" : "Other";
            int pct = size > 0 ? (int) Math.round(100.0 * free / size) : 0;
            rows.add(new String[]{ drive, label, typeStr,
                    fmtGB(size), fmtGB(free), pct + " %" });
        }
        for (String[] r : rows) diskModel.addRow(r);

        setBusy(false);
        statusDot.setForeground(Theme.GREEN);
        statusText.setText("Ready"); statusText.setForeground(Theme.GREEN);
        frame.setStatus("Hardware: ready");
    }

    private void generateReport() {
        frame.setStatus("Hardware: generating battery report…");
        setBusy(true);
        CommandExecutor.runAsync(
                "powercfg /batteryreport /output \"" + reportFile.getAbsolutePath() + "\"",
                out -> {
                    setBusy(false);
                    if (!reportFile.exists()) {
                        frame.setStatus("Battery report not created.");
                        JOptionPane.showMessageDialog(this,
                                "Battery report was not created.\n" + out,
                                "Battery Report", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    try {
                        htmlPane.setPage(reportFile.toURI().toURL());
                    } catch (IOException ex) {
                        htmlPane.setText("<html><body style='background:#252526;color:#e0e0e0;'>"
                                + "Report generated at:<br><code>"
                                + reportFile.getAbsolutePath() + "</code>"
                                + "<br><br>" + ex.getMessage() + "</body></html>");
                    }
                    frame.setStatus("Battery report generated: " + reportFile.getAbsolutePath());
                },
                err -> {
                    setBusy(false);
                    frame.setStatus("powercfg error: " + err.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Failed to generate report:\n" + err.getMessage(),
                            "powercfg", JOptionPane.ERROR_MESSAGE);
                });
    }

    private void openReport() {
        if (!reportFile.exists()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Report doesn't exist yet. Generate it now?",
                    "Battery Report", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) generateReport();
            return;
        }
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(reportFile.toURI());
                frame.setStatus("Battery report opened in browser");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Browse not supported.\nPath: " + reportFile.getAbsolutePath(),
                        "Open Report", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open report:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static long parseLong(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String fmtGB(long bytes) {
        if (bytes <= 0) return "\u2014";
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        return String.format("%.1f GB", gb);
    }
}
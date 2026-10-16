import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    public interface Refreshable { void refresh(); void setBusy(boolean b); }

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JToggleButton autoToggle = new JToggleButton("Auto-Refresh", true);
    private final JComboBox<String> intervalBox =
            new JComboBox<>(new String[]{"3 s", "5 s", "10 s", "30 s", "60 s"});
    private final JButton refreshNowBtn = new JButton("Refresh Now");

    private Timer autoTimer;
    private final Refreshable[] panels;

    public MainFrame() {
        super("SysPulse  •  System & Network Health Suite");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        OverviewPanel overview = new OverviewPanel(this);
        ProcessPanel  process  = new ProcessPanel(this);
        HardwarePanel hardware = new HardwarePanel(this);
        NetworkPanel  network  = new NetworkPanel(this);
        panels = new Refreshable[]{ overview, process, hardware, network };

        buildUI(overview, process, hardware, network);

        setSize(1280, 820);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        tabs.addChangeListener(e -> refreshVisible());
        intervalBox.setSelectedItem("10 s");
        startAutoTimer();
        SwingUtilities.invokeLater(this::refreshVisible);
    }

    private void buildUI(OverviewPanel overview, ProcessPanel process,
                         HardwarePanel hardware, NetworkPanel network) {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        /* ── Top bar ────────────────────────────────────────── */
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.HEADER_BG);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(10, 16, 10, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("SysPulse");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.FG);
        JLabel sub = new JLabel("   system & network health suite");
        sub.setFont(Theme.UI);
        sub.setForeground(Theme.DIM);
        left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel il = new JLabel("Interval:");
        il.setFont(Theme.UI); il.setForeground(Theme.DIM);

        intervalBox.setFont(Theme.UI);
        intervalBox.setPreferredSize(new Dimension(80, 26));
        intervalBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                setBackground(s ? Theme.ACCENT : Theme.CARD_BG);
                setForeground(s ? Color.WHITE : Theme.FG);
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        intervalBox.addActionListener(e -> { if (autoToggle.isSelected()) startAutoTimer(); });

        Theme.styleToggle(autoToggle);
        autoToggle.addActionListener(e -> {
            if (autoToggle.isSelected()) startAutoTimer(); else stopAutoTimer();
        });

        Theme.styleButton(refreshNowBtn);
        refreshNowBtn.addActionListener(e -> refreshAll());

        right.add(il); right.add(intervalBox); right.add(autoToggle); right.add(refreshNowBtn);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        /* ── Tabs ───────────────────────────────────────────── */
        Theme.styleTabbedPane(tabs);
        tabs.addTab("  Overview  ",  overview);
        tabs.addTab("  Processes  ", process);
        tabs.addTab("  Hardware  ",  hardware);
        tabs.addTab("  Network  ",   network);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setForegroundAt(i, Theme.FG);
            tabs.setBackgroundAt(i, Theme.HEADER_BG);
        }
        add(tabs, BorderLayout.CENTER);

        /* ── Status bar ─────────────────────────────────────── */
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Theme.ACCENT);
        bottom.setBorder(new EmptyBorder(6, 14, 6, 14));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(Theme.UI);
        bottom.add(statusLabel, BorderLayout.WEST);
        JLabel hint = new JLabel("SysPulse  •  cmd.exe /c runner");
        hint.setForeground(new Color(220, 235, 250));
        hint.setFont(Theme.UI);
        bottom.add(hint, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    /* ── Status ─────────────────────────────────────────── */
    public void setStatus(String s) { statusLabel.setText("  " + s); }

    /* ── Refresh orchestration ──────────────────────────── */
    private void refreshVisible() {
        int i = tabs.getSelectedIndex();
        if (i >= 0) panels[i].refresh();
    }

    private void refreshAll() {
        setStatus("Refreshing all panels…");
        for (Refreshable r : panels) r.refresh();
    }

    private int getIntervalSeconds() {
        String s = (String) intervalBox.getSelectedItem();
        try { return Integer.parseInt(s.replace(" s", "").trim()); }
        catch (Exception e) { return 10; }
    }

    private void startAutoTimer() {
        if (autoTimer != null) autoTimer.stop();
        int sec = getIntervalSeconds();
        autoTimer = new Timer(sec * 1000, e -> refreshVisible());
        autoTimer.setInitialDelay(sec * 1000);
        autoTimer.start();
        setStatus("Auto-refresh every " + sec + "s");
    }

    private void stopAutoTimer() {
        if (autoTimer != null) autoTimer.stop();
        setStatus("Auto-refresh paused");
    }
}
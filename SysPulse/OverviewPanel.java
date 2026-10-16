import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/** Dashboard tab: OS, RAM, CPU, Battery, Network — as health cards. */
public class OverviewPanel extends JPanel implements MainFrame.Refreshable {

    private final MainFrame frame;

    private final HealthCard osCard    = new HealthCard("Operating System", "\u2014");
    private final GaugeCard  cpuCard   = new GaugeCard("CPU Load",   "0 %", Theme.ACCENT);
    private final GaugeCard  ramCard   = new GaugeCard("RAM Usage",  "0 %", Theme.GREEN);
    private final GaugeCard  batCard   = new GaugeCard("Battery",    "N/A", Theme.AMBER);
    private final BadgeCard  netCard   = new BadgeCard("Network", "Checking…", Theme.DIM);
    private final HealthCard uptimeCard = new HealthCard("Uptime", "\u2014");

    private SwingWorker<String, Void> worker;

    public OverviewPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 14, 8, 14)));
        JLabel t = new JLabel("System Telemetry");
        t.setFont(Theme.H2); t.setForeground(Theme.FG);
        header.add(t, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setBackground(Theme.BG);
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));
        grid.add(osCard);
        grid.add(cpuCard);
        grid.add(ramCard);
        grid.add(batCard);
        grid.add(netCard);
        grid.add(uptimeCard);
        add(grid, BorderLayout.CENTER);
    }

    @Override public void setBusy(boolean b) { /* handled by tab header, no-op */ }

    @Override public void refresh() {
        if (worker != null && !worker.isDone()) return;
        frame.setStatus("Overview: loading…");

        String cmd =
              "wmic OS get Caption,BuildNumber,FreePhysicalMemory,TotalVisibleMemorySize,LastBootUpTime /format:list"
            + " & wmic CPU get LoadPercentage /format:list"
            + " & wmic PATH Win32_Battery get EstimatedChargeRemaining /format:list"
            + " & wmic NIC where NetEnabled=true get Name /format:list";

        worker = CommandExecutor.runAsync(cmd, this::render, err -> {
            frame.setStatus("Overview: error — " + err.getMessage());
        });
    }

    private void render(String raw) {
        Map<String, String> kv = new HashMap<>();
        boolean anyNic = false;
        for (String line : raw.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq > 0) {
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if (!v.isEmpty() && !kv.containsKey(k)) kv.put(k, v);
            } else if (line.startsWith("Name=")) {
                anyNic = true;
            }
        }

        // OS
        String osName = kv.getOrDefault("Caption", "Unknown OS");
        String build  = kv.getOrDefault("BuildNumber", "");
        osCard.setValue(osName);
        osCard.setSub("Build " + build);

        // RAM
        long total = parseLong(kv.get("TotalVisibleMemorySize"));
        long free  = parseLong(kv.get("FreePhysicalMemory"));
        int ramPct = total > 0 ? (int) Math.round(100.0 * (total - free) / total) : 0;
        ramCard.set(ramPct, fmtGB(total - free) + " / " + fmtGB(total));

        // CPU
        int cpu = (int) parseLong(kv.getOrDefault("LoadPercentage", "0"));
        cpuCard.set(cpu, cpu + " %");

        // Battery
        String batS = kv.get("EstimatedChargeRemaining");
        if (batS != null) batCard.set((int) parseLong(batS), batS + " %");
        else batCard.set(-1, "Desktop / N/A");

        // Network
        if (anyNic) netCard.set("Online", Theme.GREEN);
        else        netCard.set("Offline", Theme.RED);

        // Uptime
        String boot = kv.get("LastBootUpTime");
        if (boot != null && boot.length() >= 14) {
            try {
                int y = Integer.parseInt(boot.substring(0, 4));
                int mo = Integer.parseInt(boot.substring(4, 6));
                int d = Integer.parseInt(boot.substring(6, 8));
                int h = Integer.parseInt(boot.substring(8, 10));
                int mi = Integer.parseInt(boot.substring(10, 12));
                java.time.LocalDateTime bt = java.time.LocalDateTime.of(y, mo, d, h, mi);
                java.time.Duration dur = java.time.Duration.between(bt, java.time.LocalDateTime.now());
                uptimeCard.setValue(dur.toDays() + "d " + (dur.toHours() % 24) + "h "
                        + (dur.toMinutes() % 60) + "m");
                uptimeCard.setSub("since " + String.format("%04d-%02d-%02d %02d:%02d", y, mo, d, h, mi));
            } catch (Exception ignored) { }
        }

        frame.setStatus("Overview: ready");
    }

    private static long parseLong(String s) {
        if (s == null) return 0;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String fmtGB(long kb) {
        double gb = kb / 1024.0 / 1024.0;
        return String.format("%.1f GB", gb);
    }

    /* ── Card widgets ─────────────────────────────────────── */

    private static class Card extends JPanel {
        final JLabel titleLbl = new JLabel();
        final JLabel valueLbl = new JLabel("\u2014");
        final JLabel subLbl   = new JLabel(" ");

        Card(String title) {
            setLayout(new BorderLayout(0, 6));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    new EmptyBorder(14, 16, 14, 16)));

            titleLbl.setText(title);
            titleLbl.setFont(Theme.UI_BOLD);
            titleLbl.setForeground(Theme.DIM);

            valueLbl.setFont(Theme.H1);
            valueLbl.setForeground(Theme.FG);

            subLbl.setFont(Theme.UI);
            subLbl.setForeground(Theme.DIM);

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(titleLbl, BorderLayout.NORTH);
            top.add(valueLbl, BorderLayout.CENTER);
            top.add(subLbl,   BorderLayout.SOUTH);
            add(top, BorderLayout.CENTER);
        }
    }

    private static class HealthCard extends Card {
        HealthCard(String title, String initial) {
            super(title);
            valueLbl.setFont(Theme.TITLE);
            valueLbl.setText(initial);
        }
        void setValue(String v) { valueLbl.setText(v); }
        void setSub(String v)   { subLbl.setText(v); }
    }

    private static class BadgeCard extends Card {
        BadgeCard(String title, String initial, Color c) {
            super(title);
            valueLbl.setFont(Theme.TITLE);
            valueLbl.setText(initial);
            valueLbl.setForeground(c);
        }
        void set(String text, Color color) {
            valueLbl.setText(text);
            valueLbl.setForeground(color);
        }
    }

    private static class GaugeCard extends Card {
        private final JProgressBar bar;
        GaugeCard(String title, String initial, Color color) {
            super(title);
            valueLbl.setFont(Theme.H1);
            valueLbl.setText(initial);

            bar = new JProgressBar(0, 100);
            bar.setValue(0);
            bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(0, 8));
            bar.setBackground(new Color(50,50,55));
            bar.setForeground(color);

            add(bar, BorderLayout.SOUTH);
        }
        void set(int pct, String label) {
            if (pct < 0) { bar.setValue(0); bar.setForeground(Theme.DIM); }
            else {
                bar.setValue(Math.max(0, Math.min(100, pct)));
                Color c = pct >= 85 ? Theme.RED : pct >= 65 ? Theme.AMBER : bar.getForeground();
                bar.setForeground(c);
            }
            valueLbl.setText(label);
        }
    }
}
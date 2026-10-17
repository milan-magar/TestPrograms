import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.PrintWriter;

public class BrailleArtCanvas extends JFrame {

    // ---------- Configuration ----------
    private int rows = 16;
    private int cols = 32;
    private static final char BLANK = '\u2800'; // ⠀ braille blank

    private static final char[] PALETTE = {
            '\u28FF', '\u28FE', '\u2877', '\u2866', '\u2804', '\u2870', '\u284F', '\u2807',
            '\u281B', '\u281F', '\u281E', '\u2827', '\u2824', '\u2802', '\u2801', BLANK
    };

    // ---------- State ----------
    private char[][] grid;
    private char activeBrush = '\u28FF';

    // ---------- UI Components ----------
    private final CanvasPanel canvasPanel = new CanvasPanel();
    private final JLabel statusLabel = new JLabel(" Ready");
    private final JScrollPane scrollPane;
    private final ButtonGroup paletteButtonGroup = new ButtonGroup();
    
    private JSpinner rowSpinner;
    private JSpinner colSpinner;

    public BrailleArtCanvas() {
        super("Braille & ASCII Art Drawing Canvas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initGrid(rows, cols);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(22, 22, 24));

        // Wrap canvas in a scroll pane
        scrollPane = new JScrollPane(canvasPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80), 1));
        scrollPane.getViewport().setBackground(new Color(18, 18, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildPalette(), BorderLayout.WEST);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(buildBottomControls(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
    }

    private void initGrid(int r, int c) {
        grid = new char[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                grid[i][j] = BLANK;
            }
        }
    }

    // ====================== TOP PANEL (Dimensions) ======================
    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(new Color(22, 22, 24));

        rowSpinner = new JSpinner(new SpinnerNumberModel(rows, 4, 200, 1));
        colSpinner = new JSpinner(new SpinnerNumberModel(cols, 4, 200, 1));
        styleSpinner(rowSpinner);
        styleSpinner(colSpinner);

        JButton applyBtn = new JButton("Apply Dimensions");
        styleButton(applyBtn);
        applyBtn.addActionListener(e -> {
            int newRows = (int) rowSpinner.getValue();
            int newCols = (int) colSpinner.getValue();
            resizeGrid(newRows, newCols);
        });

        JLabel lblRows = new JLabel("Rows:");
        lblRows.setForeground(Color.LIGHT_GRAY);
        JLabel lblCols = new JLabel("Columns:");
        lblCols.setForeground(Color.LIGHT_GRAY);

        panel.add(lblRows);
        panel.add(rowSpinner);
        panel.add(lblCols);
        panel.add(colSpinner);
        panel.add(applyBtn);
        
        return panel;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(new Color(45, 45, 52));
            tf.setForeground(Color.WHITE);
            tf.setCaretColor(Color.WHITE);
        }
    }

    private void resizeGrid(int newRows, int newCols) {
        char[][] newGrid = new char[newRows][newCols];
        for (int r = 0; r < newRows; r++) {
            for (int c = 0; c < newCols; c++) {
                // Preserve existing art where possible
                newGrid[r][c] = (r < rows && c < cols) ? grid[r][c] : BLANK;
            }
        }
        grid = newGrid;
        rows = newRows;
        cols = newCols;
        
        rowSpinner.setValue(rows);
        colSpinner.setValue(cols);
        
        canvasPanel.refreshSize();
        statusLabel.setText("Canvas resized to " + rows + " rows × " + cols + " cols.");
    }

    // ====================== PALETTE ======================
    private JPanel buildPalette() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(new Color(22, 22, 24));
        outer.setBorder(new EmptyBorder(0, 0, 0, 10));

        JLabel title = new JLabel("Symbols");
        title.setForeground(new Color(220, 220, 230));
        title.setFont(pickFont(Font.BOLD, 16));
        outer.add(title, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(0, 4, 6, 6));
        gridPanel.setBackground(new Color(22, 22, 24));

        for (char sym : PALETTE) {
            JToggleButton btn = new JToggleButton(String.valueOf(sym));
            btn.setFont(pickFont(Font.PLAIN, 24));
            btn.setPreferredSize(new Dimension(46, 46));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(40, 40, 45));
            btn.setForeground(new Color(235, 235, 240));
            btn.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
            
            btn.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    AbstractButton b = (AbstractButton) c;
                    g.setColor(b.isSelected() ? new Color(0, 120, 215) : b.getBackground());
                    g.fillRect(0, 0, c.getWidth(), c.getHeight());
                    super.paint(g, c);
                }
            });

            if (sym == activeBrush) btn.setSelected(true);
            btn.addActionListener(e -> {
                activeBrush = sym;
                updateStatus();
            });
            paletteButtonGroup.add(btn);
            gridPanel.add(btn);
        }
        outer.add(gridPanel, BorderLayout.CENTER);
        return outer;
    }

    // ====================== BOTTOM CONTROLS ======================
    private JPanel buildBottomControls() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(new Color(22, 22, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(new Color(22, 22, 24));

        JButton clearBtn = new JButton("Clear");
        styleButton(clearBtn);
        clearBtn.addActionListener(e -> {
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    grid[r][c] = BLANK;
            canvasPanel.repaint();
            statusLabel.setText("Canvas cleared.");
        });

        JButton eraserBtn = new JButton("Eraser");
        styleButton(eraserBtn);
        eraserBtn.addActionListener(e -> {
            activeBrush = BLANK;
            paletteButtonGroup.clearSelection(); 
            updateStatus();
        });

        JButton copyBtn = new JButton("Copy");
        styleButton(copyBtn);
        copyBtn.addActionListener(e -> copyToClipboard());

        JButton pasteBtn = new JButton("Paste");
        styleButton(pasteBtn);
        pasteBtn.addActionListener(e -> pasteFromClipboard());

        JButton saveBtn = new JButton("Save .txt");
        styleButton(saveBtn);
        saveBtn.addActionListener(e -> saveToFile());

        left.add(clearBtn);
        left.add(eraserBtn);
        left.add(copyBtn);
        left.add(pasteBtn);
        left.add(saveBtn);

        statusLabel.setForeground(new Color(0, 229, 255));
        statusLabel.setFont(pickFont(Font.PLAIN, 14));

        bar.add(left, BorderLayout.WEST);
        bar.add(statusLabel, BorderLayout.CENTER);
        updateStatus();
        return bar;
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setBackground(new Color(45, 45, 52));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 80), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    private void updateStatus() {
        statusLabel.setText(String.format("Brush: '%s' (U+%04X) | Canvas: %d × %d", 
                activeBrush, (int) activeBrush, rows, cols));
    }

    // ====================== FILE & CLIPBOARD OPERATIONS ======================
    private String exportText() {
        StringBuilder sb = new StringBuilder(rows * (cols + 1));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) sb.append(grid[r][c]);
            if (r < rows - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private void copyToClipboard() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(exportText()), null);
        statusLabel.setText("Copied canvas to clipboard.");
    }

    private void pasteFromClipboard() {
        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data == null || data.isEmpty()) {
                statusLabel.setText("Clipboard is empty.");
                return;
            }

            String[] lines = data.split("\\r?\\n");
            int newRows = lines.length;
            int newCols = 0;
            for (String line : lines) {
                newCols = Math.max(newCols, line.length());
            }

            newRows = Math.max(4, newRows);
            newCols = Math.max(4, newCols);

            resizeGrid(newRows, newCols);
            for (int r = 0; r < newRows; r++) {
                String line = lines[r];
                for (int c = 0; c < newCols; c++) {
                    if (c < line.length()) {
                        grid[r][c] = line.charAt(c);
                    } else {
                        grid[r][c] = BLANK;
                    }
                }
            }
            canvasPanel.refreshSize();
            statusLabel.setText("Pasted " + newRows + " rows from clipboard.");
        } catch (Exception ex) {
            statusLabel.setText("Failed to paste: " + ex.getMessage());
        }
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Canvas as Text File");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        chooser.setSelectedFile(new File("braille_art.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
                out.print(exportText());
                statusLabel.setText("Saved to: " + file.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ====================== CANVAS ======================
    private class CanvasPanel extends JPanel {
        // --- Tighter Dimensions ---
        private final int cellW = 14; // Narrowed from 20
        private final int cellH = 24; // Reduced from 30
        private final int padLeft = 35; 
        private final int padTop = 24;  
        private final int padRight = 10;
        private final int padBottom = 10;

        private boolean dragging = false;
        private int hoverRow = -1, hoverCol = -1;

        CanvasPanel() {
            setBackground(new Color(18, 18, 20));
            setFont(pickFont(Font.PLAIN, 16)); // Smaller font to fit tighter cells
            refreshSize();

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragging = true;
                    paintAt(e);
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragging) paintAt(e);
                    updateHover(e);
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragging = false;
                }
                @Override public void mouseMoved(MouseEvent e) {
                    updateHover(e);
                }
                @Override public void mouseExited(MouseEvent e) {
                    hoverRow = -1; hoverCol = -1;
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        public void refreshSize() {
            int w = padLeft + cols * cellW + padRight;
            int h = padTop + rows * cellH + padBottom;
            setPreferredSize(new Dimension(w, h));
            revalidate();
            repaint();
        }

        private void updateHover(MouseEvent e) {
            int c = (e.getX() - padLeft) / cellW;
            int r = (e.getY() - padTop) / cellH;
            if (r >= 0 && r < rows && c >= 0 && c < cols) {
                if (r != hoverRow || c != hoverCol) {
                    hoverRow = r; hoverCol = c;
                    repaint();
                }
            } else {
                if (hoverRow != -1 || hoverCol != -1) {
                    hoverRow = -1; hoverCol = -1;
                    repaint();
                }
            }
        }

        private void paintAt(MouseEvent e) {
            int c = (e.getX() - padLeft) / cellW;
            int r = (e.getY() - padTop) / cellH;
            if (r < 0 || r >= rows || c < 0 || c >= cols) return;
            if (grid[r][c] == activeBrush) return;
            grid[r][c] = activeBrush;
            repaint(cellRect(c, r));
        }

        private Rectangle cellRect(int c, int r) {
            return new Rectangle(padLeft + c * cellW, padTop + r * cellH, cellW, cellH);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw grid lines
            g.setColor(new Color(45, 45, 55));
            for (int r = 0; r <= rows; r++) {
                int y = padTop + r * cellH;
                g.drawLine(padLeft, y, padLeft + cols * cellW, y);
            }
            for (int c = 0; c <= cols; c++) {
                int x = padLeft + c * cellW;
                g.drawLine(x, padTop, x, padTop + rows * cellH);
            }

            // Draw Axes Text
            g.setFont(new Font("SansSerif", Font.PLAIN, 10)); // Smaller axis font
            FontMetrics fm = g.getFontMetrics();
            
            // X-axis (Columns)
            for (int c = 0; c < cols; c++) {
                if (c % 5 != 0 && cols > 40) continue; 
                
                g.setColor(c % 5 == 0 ? new Color(180, 180, 190) : new Color(100, 100, 110));
                String num = String.valueOf(c);
                int x = padLeft + c * cellW + (cellW - fm.stringWidth(num)) / 2;
                g.drawString(num, x, padTop - 6); // Tighter padding
            }
            
            // Y-axis (Rows)
            for (int r = 0; r < rows; r++) {
                if (r % 5 != 0 && rows > 40) continue;

                g.setColor(r % 5 == 0 ? new Color(180, 180, 190) : new Color(100, 100, 110));
                String num = String.valueOf(r);
                int y = padTop + r * cellH + (cellH + fm.getAscent()) / 2 - 2;
                g.drawString(num, padLeft - 8 - fm.stringWidth(num), y); // Tighter padding
            }

            // Draw hover highlight (opacity increases when dragging)
            if (hoverRow >= 0 && hoverCol >= 0) {
                g.setColor(new Color(0, 150, 255, dragging ? 80 : 40));
                g.fillRect(padLeft + hoverCol * cellW, padTop + hoverRow * cellH, cellW, cellH);
            }

            // Draw symbols
            g.setFont(getFont());
            g.setColor(new Color(0, 229, 255)); // Cyan
            
            FontMetrics symFm = g.getFontMetrics();
            int ascent = symFm.getAscent();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    char ch = grid[r][c];
                    if (ch == BLANK || ch == '\0') continue;
                    String s = String.valueOf(ch);
                    int w = symFm.stringWidth(s);
                    int x = padLeft + c * cellW + (cellW - w) / 2;
                    int y = padTop + r * cellH + (cellH - symFm.getHeight()) / 2 + ascent;
                    g.drawString(s, x, y);
                }
            }
            g.dispose();
        }
    }

    // ====================== FONT UTILITY ======================
    private static Font pickFont(int style, int size) {
        String[] candidates = {
                "Segoe UI Symbol", "Segoe UI", "DejaVu Sans", 
                "Noto Sans Symbols 2", "Noto Sans Symbols", "Dialog", "SansSerif"
        };
        String probe = "\u28FF\u2801\u2800";
        for (String name : candidates) {
            Font f = new Font(name, style, size);
            if (f.canDisplay('\u28FF') && f.canDisplay('\u2801') && f.canDisplay('\u2800')) {
                if (f.canDisplayUpTo(probe) == -1) return f;
            }
        }
        return new Font("SansSerif", style, size);
    }

    // ====================== MAIN ======================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new BrailleArtCanvas().setVisible(true);
        });
    }
}
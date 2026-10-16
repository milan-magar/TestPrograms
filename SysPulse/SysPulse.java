import javax.swing.*;
import java.awt.*;

/** Application entry point. */
public class SysPulse {
    public static void main(String[] args) {
        // Use Metal LAF (respects custom colors) rather than Windows LAF.
        try { UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); }
        catch (Exception ignored) { }

        installDarkUIDefaults();

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    /** Sets every relevant Swing UI default to a dark palette. */
    private static void installDarkUIDefaults() {
        UIManager.put("control", Theme.CARD_BG);
        UIManager.put("info",    Theme.CARD_BG);
        UIManager.put("text",    Theme.FG);

        UIManager.put("Panel.background",  Theme.BG);
        UIManager.put("Label.foreground",  Theme.FG);
        UIManager.put("Label.background",  Theme.BG);

        UIManager.put("Button.background",  Theme.ACCENT);
        UIManager.put("Button.foreground",  Color.WHITE);
        UIManager.put("Button.disabledText", new Color(150,150,150));

        UIManager.put("TextField.background", Theme.CARD_BG);
        UIManager.put("TextField.foreground", Theme.FG);
        UIManager.put("TextField.caretForeground", Theme.FG);
        UIManager.put("TextField.border",
                BorderFactory.createLineBorder(Theme.BORDER));

        UIManager.put("ComboBox.background", Theme.CARD_BG);
        UIManager.put("ComboBox.foreground", Theme.FG);
        UIManager.put("ComboBox.selectionBackground", Theme.ACCENT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.buttonBackground", Theme.CARD_BG);
        UIManager.put("ComboBox.buttonShadow",     Theme.BORDER);
        UIManager.put("ComboBox.buttonDarkShadow", Theme.BORDER);

        UIManager.put("TabbedPane.background", Theme.HEADER_BG);
        UIManager.put("TabbedPane.foreground", Theme.FG);
        UIManager.put("TabbedPane.selected",   Theme.BG);
        UIManager.put("TabbedPane.selectedForeground", Theme.FG);

        UIManager.put("Table.background", Theme.CARD_BG);
        UIManager.put("Table.foreground", Theme.FG);
        UIManager.put("Table.selectionBackground", Theme.SEL_BG);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", Theme.BORDER);
        UIManager.put("TableHeader.background", Theme.HEADER_BG);
        UIManager.put("TableHeader.foreground", Theme.FG);

        UIManager.put("ScrollPane.background", Theme.BG);
        UIManager.put("Viewport.background",   Theme.CARD_BG);
        UIManager.put("ScrollBar.thumb",       new Color(80,80,88));
        UIManager.put("ScrollBar.track",       Theme.BG);
        UIManager.put("ScrollBar.background",  Theme.BG);
        UIManager.put("ScrollBar.foreground",  Theme.FG);

        UIManager.put("ToolTip.background", Theme.HEADER_BG);
        UIManager.put("ToolTip.foreground", Theme.FG);

        UIManager.put("OptionPane.background",        Theme.HEADER_BG);
        UIManager.put("OptionPane.messageForeground", Theme.FG);
        UIManager.put("OptionPane.messageBackground", Theme.HEADER_BG);

        UIManager.put("List.background", Theme.CARD_BG);
        UIManager.put("List.foreground", Theme.FG);
        UIManager.put("List.selectionBackground", Theme.ACCENT);
        UIManager.put("List.selectionForeground", Color.WHITE);

        UIManager.put("SplitPane.background", Theme.BG);
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
    }
}
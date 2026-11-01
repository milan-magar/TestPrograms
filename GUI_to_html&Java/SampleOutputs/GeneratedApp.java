import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GeneratedApp extends JFrame {
    public GeneratedApp() {
        setTitle("Generated App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);

    JPanel panel_1 = new JPanel();
    JLabel panel_1_c_0 = new JLabel("Heading");
    JLabel panel_1_c_1 = new JLabel("Label text");
    JButton panel_1_c_2 = new JButton("Button");
    JTextField panel_1_c_3 = new JTextField();
    JTextField panel_1_c_4 = new JTextField();
    JTextArea panel_1_c_5 = new JTextArea();
    JLabel panel_1_c_6 = new JLabel("Custom Widget");

    panel_1.setLayout(null);
    panel_1.setBackground(new Color(255, 255, 255));
    panel_1_c_0.setBounds(281, 36, 300, 44);
    panel_1_c_0.setBackground(new Color(255, 255, 255));
    panel_1_c_0.setOpaque(true);
    panel_1_c_0.setForeground(new Color(17, 17, 17));
    panel_1_c_0.setFont(panel_1_c_0.getFont().deriveFont(Font.BOLD, 28f));
    panel_1.add(panel_1_c_0);
    panel_1_c_1.setBounds(281, 90, 160, 24);
    panel_1_c_1.setBackground(new Color(255, 255, 255));
    panel_1_c_1.setOpaque(true);
    panel_1_c_1.setForeground(new Color(17, 17, 17));
    panel_1_c_1.setFont(panel_1_c_1.getFont().deriveFont(Font.PLAIN, 14f));
    panel_1.add(panel_1_c_1);
    panel_1_c_2.setBounds(281, 126, 120, 40);
    panel_1_c_2.setBackground(new Color(37, 99, 235));
    panel_1_c_2.setOpaque(true);
    panel_1_c_2.setForeground(new Color(255, 255, 255));
    panel_1_c_2.setFont(panel_1_c_2.getFont().deriveFont(Font.BOLD, 14f));
    panel_1_c_2.setBorderPainted(false);
    panel_1_c_2.setFocusPainted(false);
    panel_1.add(panel_1_c_2);
    panel_1_c_3.setBounds(281, 180, 220, 36);
    panel_1_c_3.setBackground(new Color(255, 255, 255));
    panel_1_c_3.setOpaque(true);
    panel_1_c_3.setForeground(new Color(255, 255, 255));
    panel_1_c_3.setFont(panel_1_c_3.getFont().deriveFont(Font.PLAIN, 14f));
    panel_1.add(panel_1_c_3);
    panel_1_c_4.setBounds(281, 234, 220, 57);
    panel_1_c_4.setBackground(new Color(255, 255, 255));
    panel_1_c_4.setOpaque(true);
    panel_1_c_4.setForeground(new Color(255, 255, 255));
    panel_1_c_4.setFont(panel_1_c_4.getFont().deriveFont(Font.PLAIN, 14f));
    panel_1.add(panel_1_c_4);
    panel_1_c_5.setBounds(281, 306, 260, 100);
    panel_1_c_5.setBackground(new Color(255, 255, 255));
    panel_1_c_5.setOpaque(true);
    panel_1_c_5.setForeground(new Color(255, 255, 255));
    panel_1_c_5.setFont(panel_1_c_5.getFont().deriveFont(Font.PLAIN, 14f));
    panel_1.add(panel_1_c_5);
    panel_1_c_6.setBounds(281, 432, 200, 60);
    panel_1_c_6.setBackground(new Color(255, 251, 235));
    panel_1_c_6.setOpaque(true);
    panel_1_c_6.setForeground(new Color(146, 64, 14));
    panel_1_c_6.setFont(panel_1_c_6.getFont().deriveFont(Font.PLAIN, 16f));
    panel_1.add(panel_1_c_6);

    mainPanel.add(panel_1, "page_1793531799283");

        add(mainPanel);
        setVisible(true);
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(GeneratedApp::new); }
}
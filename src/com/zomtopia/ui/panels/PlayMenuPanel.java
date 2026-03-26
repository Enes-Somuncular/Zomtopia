package com.zomtopia.ui.panels;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.main.GameApp;
import com.zomtopia.utils.ResourceManager;
import com.zomtopia.utils.SettingsManager;
import com.zomtopia.audio.AudioManager;
import com.zomtopia.ui.components.MenuButton;

public class PlayMenuPanel extends JPanel {
    private Image backgroundImage;

    public PlayMenuPanel(GameApp gameApp) {
        setLayout(new BorderLayout());

        backgroundImage = ResourceManager.loadImage("play_menu_bg.png");

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Karakterinizi Oluşturun");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        JLabel nameLabel = new JLabel("İsim:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);
        JTextField nameField = new JTextField(SettingsManager.getPlayerName(), 15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 20));
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton nextButton = createButton("İlerle");
        JButton backButton = createButton("Geri");

        nextButton.addActionListener(e -> {
            AudioManager.getInstance().playMenuClick();
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lütfen bir isim giriniz!", "Hata", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Save the name persistently
            SettingsManager.setPlayerName(name);
            // Launch the actual game world
            JFrame gameFrame = new JFrame("Zomtopia - " + name);
            com.zomtopia.game.GamePanel gp = new com.zomtopia.game.GamePanel();
            gameFrame.setContentPane(gp);
            gameFrame.pack();
            gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setResizable(false);
            gameFrame.setVisible(true);
            gp.requestFocusInWindow();
        });
        backButton.addActionListener(e -> { AudioManager.getInstance().playMenuClick(); gameApp.showPanel("MainMenu"); });

        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);

        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(inputPanel);
        centerPanel.add(Box.createVerticalStrut(40));
        centerPanel.add(buttonPanel);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String text) {
        MenuButton button = new MenuButton(text);
        Dimension preferredSize = new Dimension(160, 45);
        button.setPreferredSize(preferredSize);
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

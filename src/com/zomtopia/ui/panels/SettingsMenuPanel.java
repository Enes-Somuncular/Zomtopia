package com.zomtopia.ui.panels;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.main.GameApp;
import com.zomtopia.utils.ResourceManager;
import com.zomtopia.ui.components.AudioSettingRow;
import com.zomtopia.audio.AudioManager;

public class SettingsMenuPanel extends JPanel {
    private Image backgroundImage;

    public SettingsMenuPanel(GameApp gameApp) {
        setLayout(new BorderLayout());

        backgroundImage = ResourceManager.loadImage("settings_menu_bg.png");

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Ayarlar");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        AudioSettingRow soundRow = new AudioSettingRow("Genel Ses", "sfx");
        AudioSettingRow musicRow = new AudioSettingRow("Müzik", "music");
        AudioSettingRow menuSoundRow = new AudioSettingRow("Menü Sesi", "menu");

        JButton backButton = new JButton("Geri");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(20, 20, 20, 160));
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 40, 10, 40)));
        backButton.setOpaque(true);
        Dimension fixedSize = new Dimension(250, 50);
        backButton.setPreferredSize(fixedSize);
        backButton.setMinimumSize(fixedSize);
        backButton.setMaximumSize(fixedSize);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        backButton.addActionListener(e -> gameApp.showPanel("MainMenu"));

        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(soundRow);
        centerPanel.add(musicRow);
        centerPanel.add(menuSoundRow);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(backButton);

        add(centerPanel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

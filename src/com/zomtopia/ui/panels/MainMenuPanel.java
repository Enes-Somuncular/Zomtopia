package com.zomtopia.ui.panels;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.main.GameApp;
import com.zomtopia.utils.ResourceManager;
import com.zomtopia.audio.AudioManager;

public class MainMenuPanel extends JPanel {
    private GameApp gameApp;
    private Image backgroundImage;

    public MainMenuPanel(GameApp gameApp) {
        this.gameApp = gameApp;
        setLayout(new BorderLayout());

        backgroundImage = ResourceManager.loadImage("background.png");

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JButton playButton = createMenuButton("Oyna");
        JButton settingsButton = createMenuButton("Ayarlar");
        JButton exitButton = createMenuButton("Çıkış");

        playButton.addActionListener(e -> { AudioManager.getInstance().playMenuClick(); gameApp.showPanel("PlayMenu"); });
        settingsButton.addActionListener(e -> { AudioManager.getInstance().playMenuClick(); gameApp.showPanel("SettingsMenu"); });
        exitButton.addActionListener(e -> { AudioManager.getInstance().playMenuClick(); System.exit(0); });

        centerPanel.add(Box.createVerticalStrut(200));
        centerPanel.add(playButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(settingsButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(exitButton);
        centerPanel.add(Box.createVerticalGlue());

        add(centerPanel, BorderLayout.CENTER);

        JLabel aboutLabel = new JLabel("Hakkında: Zomtopia Demo v2.0");
        aboutLabel.setForeground(Color.WHITE);
        aboutLabel.setFont(new Font("Arial", Font.BOLD, 14));
        aboutLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(aboutLabel, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(20, 20, 20, 160)); 
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 40, 10, 40)));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(220, 55);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
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

package com.zomtopia.menu;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsMenuPanel extends JPanel {

    private GameMenu gameMenu;
    private Image backgroundImage;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    public SettingsMenuPanel(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
        setLayout(new BorderLayout());

        try {
            String basePath = new File("").getAbsolutePath();
            backgroundImage = new ImageIcon(basePath + "/res/background.png").getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Ayarlar");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton soundButton = createButton("Ses: AÇIK");
        JButton musicButton = createButton("Müzik: AÇIK");
        JButton backButton = createButton("Geri");

        soundButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        musicButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        soundButton.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            soundButton.setText("Ses: " + (soundEnabled ? "AÇIK" : "KAPALI"));
        });

        musicButton.addActionListener(e -> {
            musicEnabled = !musicEnabled;
            musicButton.setText("Müzik: " + (musicEnabled ? "AÇIK" : "KAPALI"));
        });

        backButton.addActionListener(e -> gameMenu.showPanel("MainMenu"));

        centerPanel.add(Box.createVerticalStrut(100));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(soundButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(musicButton);
        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(backButton);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(40, 40, 40, 200));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 40, 10, 40)));
        button.setOpaque(true);
        button.setMaximumSize(new Dimension(250, 50));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
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

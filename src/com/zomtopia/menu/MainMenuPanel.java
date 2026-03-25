package com.zomtopia.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MainMenuPanel extends JPanel {

    private GameMenu gameMenu;
    private Image backgroundImage;

    public MainMenuPanel(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
        setLayout(new BorderLayout());

        // Load Background Image
        try {
            // Find absolute path to res folder
            String basePath = new File("").getAbsolutePath();
            String bgPath = basePath + "/res/background.png";
            backgroundImage = new ImageIcon(bgPath).getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Center Buttons Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false); // Transparent so background shows
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Create Buttons
        JButton playButton = createMenuButton("Oyna");
        JButton settingsButton = createMenuButton("Ayarlar");
        JButton exitButton = createMenuButton("Çıkış");

        // Add action listeners
        playButton.addActionListener(e -> gameMenu.showPanel("PlayMenu"));
        settingsButton.addActionListener(e -> gameMenu.showPanel("SettingsMenu"));
        exitButton.addActionListener(e -> System.exit(0));

        // Adding components to center panel with some vertical glue/spacing
        centerPanel.add(Box.createVerticalStrut(200));
        centerPanel.add(playButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(settingsButton);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(exitButton);
        centerPanel.add(Box.createVerticalGlue());

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Left About text
        JLabel aboutLabel = new JLabel("Hakkında: Zomtopia Demo v1.0");
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
        button.setBackground(new Color(40, 40, 40, 200)); // Semi-transparent dark background
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 40, 10, 40)));
        button.setOpaque(true);
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

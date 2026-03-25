package com.zomtopia.menu;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PlayMenuPanel extends JPanel {

    private GameMenu gameMenu;
    private Image backgroundImage;
    private Image characterImage;

    public PlayMenuPanel(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
        setLayout(new BorderLayout());

        // Load Background Image
        try {
            String bgPath = new File("res/play_menu_bg.png").exists() ? "res/play_menu_bg.png" : "../res/play_menu_bg.png";
            String charPath = new File("res/character.png").exists() ? "res/character.png" : "../res/character.png";
            backgroundImage = new ImageIcon(bgPath).getImage();
            characterImage = new ImageIcon(charPath).getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Title
        JLabel titleLabel = new JLabel("Karakterinizi Oluşturun");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Name input
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        JLabel nameLabel = new JLabel("İsim:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);
        JTextField nameField = new JTextField(15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 20));
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);

        // Character preview
        JLabel characterPreview = new JLabel();
        if (characterImage != null) {
            // Resize character image for preview
            Image scaledImage = characterImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            characterPreview.setIcon(new ImageIcon(scaledImage));
        }
        characterPreview.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton nextButton = createButton("İlerle");
        JButton backButton = createButton("Geri");

        nextButton.addActionListener(e -> {
            String name = nameField.getText();
            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lütfen bir isim giriniz!", "Hata", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Hoşgeldin, " + name + "!\nOyun yükleniyor...", "Zomtopia", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        backButton.addActionListener(e -> gameMenu.showPanel("MainMenu"));

        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);

        // Layout components
        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(inputPanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(characterPreview);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(buttonPanel);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(20, 20, 20, 160));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)));
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

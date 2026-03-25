package com.zomtopia.ui.panels;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.main.GameApp;
import com.zomtopia.utils.ResourceManager;
import com.zomtopia.audio.AudioManager;

public class PlayMenuPanel extends JPanel {
    private GameApp gameApp;
    private Image backgroundImage;
    private Image characterImage;

    public PlayMenuPanel(GameApp gameApp) {
        this.gameApp = gameApp;
        setLayout(new BorderLayout());

        backgroundImage = ResourceManager.loadImage("play_menu_bg.png");
        characterImage = ResourceManager.loadImage("character.png");

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
        JTextField nameField = new JTextField(15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 20));
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);

        JLabel characterPreview = new JLabel();
        if (characterImage != null) {
            Image scaledImage = characterImage.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            characterPreview.setIcon(new ImageIcon(scaledImage));
        }
        characterPreview.setAlignmentX(Component.CENTER_ALIGNMENT);

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

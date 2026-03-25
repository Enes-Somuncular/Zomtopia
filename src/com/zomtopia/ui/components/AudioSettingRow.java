package com.zomtopia.ui.components;

import javax.swing.*;
import java.awt.*;

public class AudioSettingRow extends JPanel {
    private JButton toggleButton;
    private JSlider volumeSlider;
    private boolean isEnabled = true;

    public AudioSettingRow(String title) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        setOpaque(false);
        setMaximumSize(new Dimension(600, 60)); // Ensure it doesn't stretch vertically in BoxLayout

        toggleButton = new JButton(title + ": AÇIK");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 18));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setBackground(new Color(20, 20, 20, 160));
        toggleButton.setFocusPainted(false);
        toggleButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        toggleButton.setOpaque(true);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Dimension btnSize = new Dimension(220, 40);
        toggleButton.setPreferredSize(btnSize);

        toggleButton.addActionListener(e -> {
            isEnabled = !isEnabled;
            toggleButton.setText(title + (isEnabled ? ": AÇIK" : ": KAPALI"));
        });

        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPreferredSize(new Dimension(200, 40)); 

        add(toggleButton);
        add(volumeSlider);
    }
}

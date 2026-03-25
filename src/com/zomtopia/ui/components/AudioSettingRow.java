package com.zomtopia.ui.components;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.audio.AudioManager;

public class AudioSettingRow extends JPanel {
    private boolean isEnabled = true;
    private JLabel statusLabel;
    private JSlider volumeSlider;
    private final String channelKey;

    public AudioSettingRow(String title, String channelKey) {
        this.channelKey = channelKey;
        setOpaque(false);
        setMaximumSize(new Dimension(700, 65));

        // Layout: [Title Label] [ACIK/KAPALI Label] [Slider]
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Title button - fixed size, text NEVER changes
        JButton toggleButton = new JButton(title);
        toggleButton.setFont(new Font("Arial", Font.BOLD, 17));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setBackground(new Color(20, 20, 20, 160));
        toggleButton.setFocusPainted(false);
        toggleButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        toggleButton.setOpaque(true);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setPreferredSize(new Dimension(170, 40));
        toggleButton.setMinimumSize(new Dimension(170, 40));
        toggleButton.setMaximumSize(new Dimension(170, 40));

        // Status label - fixed size, text changes here instead of button
        statusLabel = new JLabel("AÇIK");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 15));
        statusLabel.setForeground(new Color(100, 220, 100));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(70, 40));
        statusLabel.setMinimumSize(new Dimension(70, 40));
        statusLabel.setMaximumSize(new Dimension(70, 40));

        toggleButton.addActionListener(e -> {
            AudioManager.getInstance().playMenuClick();
            isEnabled = !isEnabled;
            if (isEnabled) {
                statusLabel.setText("AÇIK");
                statusLabel.setForeground(new Color(100, 220, 100));
                AudioManager.getInstance().setChannelEnabled(channelKey, true);
                volumeSlider.setEnabled(true);
            } else {
                statusLabel.setText("KAPALI");
                statusLabel.setForeground(new Color(220, 80, 80));
                AudioManager.getInstance().setChannelEnabled(channelKey, false);
                volumeSlider.setEnabled(false);
            }
        });

        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPreferredSize(new Dimension(200, 40));
        volumeSlider.addChangeListener(e -> {
            float vol = volumeSlider.getValue() / 100f;
            AudioManager.getInstance().setChannelVolume(channelKey, vol);
        });

        gbc.gridx = 0; gbc.gridy = 0;
        add(toggleButton, gbc);
        gbc.gridx = 1;
        add(statusLabel, gbc);
        gbc.gridx = 2;
        add(volumeSlider, gbc);
    }
}

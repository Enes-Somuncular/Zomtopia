package com.zomtopia.ui.components;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.audio.AudioManager;
import com.zomtopia.utils.SettingsManager;

public class AudioSettingRow extends JPanel {
    private boolean isEnabled = true;
    private JLabel statusLabel;
    private JSlider volumeSlider;
    private final String channelKey;

    public AudioSettingRow(String title, String channelKey) {
        this.channelKey = channelKey;
        setOpaque(false);
        setMaximumSize(new Dimension(700, 65));

        AudioManager audio = AudioManager.getInstance();
        isEnabled = audio.isChannelEnabled(channelKey);

        // Layout: [Title Label] [ACIK/KAPALI Label] [Slider]
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Title button - fixed size, text NEVER changes
        MenuButton toggleButton = new MenuButton(title);
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

        float initialVol = audio.getChannelVolume(channelKey);
        int initialSliderVal = Math.max(0, Math.min(100, Math.round(initialVol * 100f)));

        toggleButton.addActionListener(e -> {
            AudioManager.getInstance().playMenuClick();
            isEnabled = !isEnabled;
            if (isEnabled) {
                statusLabel.setText("AÇIK");
                statusLabel.setForeground(new Color(100, 220, 100));
                audio.setChannelEnabled(channelKey, true);
                volumeSlider.setEnabled(true);
                SettingsManager.setChannelEnabled(channelKey, true);
            } else {
                statusLabel.setText("KAPALI");
                statusLabel.setForeground(new Color(220, 80, 80));
                audio.setChannelEnabled(channelKey, false);
                volumeSlider.setEnabled(false);
                SettingsManager.setChannelEnabled(channelKey, false);
            }
        });

        volumeSlider = new JSlider(0, 100, initialSliderVal);
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setEnabled(isEnabled);

        // Reflect initial enabled state in UI
        if (!isEnabled) {
            statusLabel.setText("KAPALI");
            statusLabel.setForeground(new Color(220, 80, 80));
        }

        volumeSlider.setPreferredSize(new Dimension(200, 40));
        volumeSlider.addChangeListener(e -> {
            float vol = volumeSlider.getValue() / 100f;
            audio.setChannelVolume(channelKey, vol);
            // Avoid writing to disk continuously while dragging.
            if (!volumeSlider.getValueIsAdjusting()) {
                SettingsManager.setChannelVolume(channelKey, vol);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        add(toggleButton, gbc);
        gbc.gridx = 1;
        add(statusLabel, gbc);
        gbc.gridx = 2;
        add(volumeSlider, gbc);
    }
}

package com.zomtopia.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * A custom JButton that handles semi-transparent backgrounds correctly
 * to avoid rendering artifacts (shadows/smears) in Swing.
 */
public class MenuButton extends JButton {

    private static final Color DEFAULT_BG = new Color(20, 20, 20, 160);
    private static final Color HOVER_BG = new Color(40, 40, 40, 180);

    public MenuButton(String text) {
        super(text);
        
        // Essential for semi-transparent backgrounds
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        
        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 22));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint background manually
        if (getModel().isPressed()) {
            g2.setColor(HOVER_BG.darker());
        } else if (getModel().isRollover()) {
            g2.setColor(HOVER_BG);
        } else {
            g2.setColor(DEFAULT_BG);
        }

        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.dispose();
        
        // Paint text and other standard components
        super.paintComponent(g);
    }
}

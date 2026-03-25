package com.zomtopia.menu;

import javax.swing.*;
import java.awt.*;

public class GameMenu extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    public GameMenu() {
        setTitle("Zomtopia");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setResizable(false);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create the individual menu panels
        MainMenuPanel mainMenuPanel = new MainMenuPanel(this);
        PlayMenuPanel playMenuPanel = new PlayMenuPanel(this);
        SettingsMenuPanel settingsMenuPanel = new SettingsMenuPanel(this);
        
        // Add them to the main panel with string identifiers
        mainPanel.add(mainMenuPanel, "MainMenu");
        mainPanel.add(playMenuPanel, "PlayMenu");
        mainPanel.add(settingsMenuPanel, "SettingsMenu");
        
        add(mainPanel);
        
        // Show the main menu first
        showPanel("MainMenu");
    }
    
    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    public static void main(String[] args) {
        // Run GUI construction on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GameMenu gameMenu = new GameMenu();
            gameMenu.setVisible(true);
        });
    }
}

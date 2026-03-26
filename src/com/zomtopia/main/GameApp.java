package com.zomtopia.main;

import javax.swing.*;
import java.awt.*;
import com.zomtopia.ui.panels.MainMenuPanel;
import com.zomtopia.ui.panels.PlayMenuPanel;
import com.zomtopia.ui.panels.SettingsMenuPanel;

public class GameApp extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    public GameApp() {
        setTitle("Zomtopia");
        setSize(800, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        MainMenuPanel mainMenuPanel = new MainMenuPanel(this);
        PlayMenuPanel playMenuPanel = new PlayMenuPanel(this);
        SettingsMenuPanel settingsMenuPanel = new SettingsMenuPanel(this);
        
        mainPanel.add(mainMenuPanel, "MainMenu");
        mainPanel.add(playMenuPanel, "PlayMenu");
        mainPanel.add(settingsMenuPanel, "SettingsMenu");
        
        add(mainPanel);
        showPanel("MainMenu");
    }
    
    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameApp app = new GameApp();
            app.setVisible(true);
        });
    }
}

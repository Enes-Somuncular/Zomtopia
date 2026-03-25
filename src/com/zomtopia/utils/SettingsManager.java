package com.zomtopia.utils;

import java.io.*;
import java.util.Properties;

public class SettingsManager {
    private static final String SETTINGS_FILE = System.getProperty("user.dir") + File.separator + "settings.properties";
    private static Properties properties = new Properties();

    static {
        load();
    }

    public static void load() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Zomtopia Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getPlayerName() {
        return properties.getProperty("playerName", "");
    }

    public static void setPlayerName(String name) {
        properties.setProperty("playerName", name);
        save();
    }
}

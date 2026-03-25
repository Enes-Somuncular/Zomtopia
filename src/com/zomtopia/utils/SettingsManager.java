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

    // ---- Audio settings (persistent) ----
    private static String audioEnabledKey(String channel) {
        return "audio." + channel + ".enabled";
    }

    private static String audioVolumeKey(String channel) {
        return "audio." + channel + ".volume";
    }

    public static boolean getChannelEnabled(String channel, boolean defaultValue) {
        String v = properties.getProperty(audioEnabledKey(channel));
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v);
    }

    public static float getChannelVolume(String channel, float defaultValue) {
        String v = properties.getProperty(audioVolumeKey(channel));
        if (v == null) return defaultValue;
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void setChannelEnabled(String channel, boolean enabled) {
        properties.setProperty(audioEnabledKey(channel), String.valueOf(enabled));
        save();
    }

    public static void setChannelVolume(String channel, float volume) {
        properties.setProperty(audioVolumeKey(channel), String.valueOf(volume));
        save();
    }
}

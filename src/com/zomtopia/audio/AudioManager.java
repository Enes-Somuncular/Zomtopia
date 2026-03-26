package com.zomtopia.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.zomtopia.utils.SettingsManager;

/**
 * Singleton AudioManager handles all game audio channels.
 * Channels: "sfx", "music", "menu"
 */
public class AudioManager {
    private static AudioManager instance;

    private final Map<String, Boolean> channelEnabled = new HashMap<>();
    private final Map<String, Float> channelVolume = new HashMap<>();

    // Cached clips
    private Clip menuClickClip;
    private Clip damageClip;

    private AudioManager() {
        // Load audio settings from persistent SettingsManager
        channelEnabled.put("sfx", SettingsManager.getChannelEnabled("sfx", true));
        channelEnabled.put("music", SettingsManager.getChannelEnabled("music", true));
        channelEnabled.put("menu", SettingsManager.getChannelEnabled("menu", true));

        channelVolume.put("sfx", SettingsManager.getChannelVolume("sfx", 0.8f));
        channelVolume.put("music", SettingsManager.getChannelVolume("music", 0.8f));
        channelVolume.put("menu", SettingsManager.getChannelVolume("menu", 0.8f));

        loadMenuClick();
        loadDamageSound();
    }

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    /** Loads the menu click sound from res/sounds/menu_click.wav */
    private void loadMenuClick() {
        try {
            File f = new File("res/sounds/menu_click.wav");
            if (!f.exists()) f = new File("../res/sounds/menu_click.wav");
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                menuClickClip = AudioSystem.getClip();
                menuClickClip.open(ais);
                setClipVolume(menuClickClip, channelVolume.get("menu"));
            }
        } catch (Exception e) {
            System.err.println("AudioManager: Could not load menu click sound: " + e.getMessage());
        }
    }

    /** Loads the damage sound from res/sounds/damage.wav */
    private void loadDamageSound() {
        try {
            File f = new File("res/sounds/damage.wav");
            if (!f.exists()) f = new File("../res/sounds/damage.wav");
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                damageClip = AudioSystem.getClip();
                damageClip.open(ais);
                setClipVolume(damageClip, channelVolume.get("sfx"));
            }
        } catch (Exception e) {
            System.err.println("AudioManager: Could not load damage sound: " + e.getMessage());
        }
    }

    /** Plays the menu click sound if menu channel is enabled */
    public void playMenuClick() {
        if (!Boolean.TRUE.equals(channelEnabled.get("menu"))) return;
        if (menuClickClip != null) {
            menuClickClip.stop();
            menuClickClip.setFramePosition(0);
            setClipVolume(menuClickClip, channelVolume.get("menu"));
            menuClickClip.start();
        }
    }

    /** Plays the damage sound if sfx channel is enabled */
    public void playDamageSound() {
        if (!Boolean.TRUE.equals(channelEnabled.get("sfx"))) return;
        if (damageClip != null) {
            damageClip.stop();
            damageClip.setFramePosition(0);
            setClipVolume(damageClip, channelVolume.get("sfx"));
            damageClip.start();
        }
    }

    public void setChannelEnabled(String channel, boolean enabled) {
        channelEnabled.put(channel, enabled);
    }

    public void setChannelVolume(String channel, float volume) {
        channelVolume.put(channel, volume);
        if ("menu".equals(channel) && menuClickClip != null) {
            setClipVolume(menuClickClip, volume);
        }
        if ("sfx".equals(channel) && damageClip != null) {
            setClipVolume(damageClip, volume);
        }
    }

    public boolean isChannelEnabled(String channel) {
        return Boolean.TRUE.equals(channelEnabled.get(channel));
    }

    public float getChannelVolume(String channel) {
        Float v = channelVolume.get(channel);
        return (v != null) ? v : 0.8f;
    }

    private void setClipVolume(Clip clip, float volume) {
        try {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = control.getMinimum();
            float max = control.getMaximum();
            float gain = min + (max - min) * Math.max(0f, Math.min(1f, volume));
            control.setValue(gain);
        } catch (Exception ignored) {}
    }
}

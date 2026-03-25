package com.zomtopia.utils;

import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;

public class ResourceManager {
    public static Image loadImage(String fileName) {
        try {
            File f = new File("res/" + fileName);
            if (!f.exists()) f = new File("../res/" + fileName);
            if (f.exists()) {
                return new ImageIcon(f.getAbsolutePath()).getImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

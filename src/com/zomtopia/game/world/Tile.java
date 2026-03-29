package com.zomtopia.game.world;

import java.awt.Color;

public enum Tile {
    AIR     (0, new Color(0,0,0,0),           false, Category.BLOCK),
    DIRT    (1, new Color(139, 90, 43),        true,  Category.BLOCK),
    GRASS   (2, new Color(56, 150, 50),        true,  Category.BLOCK),
    ROCK    (3, new Color(110, 110, 110),       true,  Category.BLOCK),
    WOOD    (4, new Color(120, 75, 30),        true,  Category.BLOCK),
    LEAVES  (5, new Color(40, 120, 40, 200),   true,  Category.BLOCK),
    BEDROCK (6, new Color(50, 50, 50),         true,  Category.BLOCK),
    IRON_ORE(7, new Color(130, 110, 100),      true,  Category.BLOCK),
    
    // Wearables
    RED_SHIRT(10, new Color(200, 50, 50),     false, Category.SHIRT),
    BLUE_HAT (11, new Color(50, 50, 200),     false, Category.HAT),
    WINGS    (12, new Color(230, 230, 250),    false, Category.BACK),
    DARK_MASK(13, new Color(30, 30, 30),       false, Category.MASK),
    JEANS    (14, new Color(50, 80, 150),      false, Category.PANTS),
    SNEAKERS (15, new Color(220, 220, 220),    false, Category.SHOES),
    IRON_INGOT(16, new Color(210, 210, 210),   false, Category.MATERIAL),

    // Tools
    WOODEN_PICKAXE(20, new Color(139, 90, 43), false, Category.TOOL),
    WOODEN_AXE    (21, new Color(139, 90, 43), false, Category.TOOL),
    WOODEN_SWORD  (22, new Color(139, 90, 43), false, Category.TOOL),
    WOODEN_SHOVEL (23, new Color(139, 90, 43), false, Category.TOOL),
    IRON_PICKAXE  (24, new Color(180, 180, 180), false, Category.TOOL);

    public enum Category { BLOCK, HAT, MASK, SHIRT, PANTS, SHOES, BACK, TOOL, MATERIAL }

    public final int id;
    public final Color color;
    public final boolean solid;
    public final Category category;

    Tile(int id, Color color, boolean solid, Category category) {
        this.id = id;
        this.color = color;
        this.solid = solid;
        this.category = category;
    }

    public static Tile fromId(int id) {
        for (Tile t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return AIR;
    }
}

package com.zomtopia.game.world;

import java.awt.Color;

public enum Tile {
    AIR     (0, new Color(0,0,0,0),           false),
    DIRT    (1, new Color(139, 90, 43),        true),
    GRASS   (2, new Color(56, 150, 50),        true),
    ROCK    (3, new Color(110, 110, 110),       true),
    WOOD    (4, new Color(120, 75, 30),        true),
    LEAVES  (5, new Color(40, 120, 40, 200),   true),
    BEDROCK (6, new Color(50, 50, 50),         true);

    public final int id;
    public final Color color;
    public final boolean solid;

    Tile(int id, Color color, boolean solid) {
        this.id = id;
        this.color = color;
        this.solid = solid;
    }

    public static Tile fromId(int id) {
        for (Tile t : values()) if (t.id == id) return t;
        return AIR;
    }
}

package com.zomtopia.game.entity;

import com.zomtopia.game.world.Tile;
import com.zomtopia.game.world.World;

public class ItemEntity {
    public double x, y;
    public double vx, vy;
    public final Tile tile;
    public final boolean isBackground;
    
    public static final int SIZE = 16;
    private static final double GRAVITY  = 0.3;
    private static final double FRICTION = 0.9;

    public ItemEntity(double x, double y, Tile tile, boolean isBackground) {
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.isBackground = isBackground;
        // Small random initial pop
        this.vx = (Math.random() - 0.5) * 4;
        this.vy = -Math.random() * 4;
    }

    public void update(World world) {
        vy += GRAVITY;
        vx *= FRICTION;

        x += vx;
        if (world.isRectBlocked(x, y, SIZE, SIZE)) {
            x -= vx;
            vx = -vx * 0.5;
        }

        y += vy;
        if (world.isRectBlocked(x, y, SIZE, SIZE)) {
            y -= vy;
            vy = -vy * 0.3;
            if (Math.abs(vy) < 1) vy = 0;
        }
    }
}

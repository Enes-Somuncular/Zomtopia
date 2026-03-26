package com.zomtopia.game.entity;

import com.zomtopia.game.world.Tile;
import com.zomtopia.game.world.World;

public class ItemEntity {
    public double x, y;
    public double vx, vy;
    public final Tile tile;
    public final boolean isBackground;
    public int amount;
    public int pickupCooldown = 25; // Prevent immediate pickup
    
    public static final int SIZE = 16;
    private static final double GRAVITY  = 0.3;
    private static final double FRICTION = 0.9;

    public ItemEntity(double x, double y, Tile tile, boolean isBackground, int amount) {
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.isBackground = isBackground;
        this.amount = amount;
        // Small random initial pop
        this.vx = (Math.random() - 0.5) * 4;
        this.vy = -Math.random() * 4;
    }

    // Convenience: default amount=1
    public ItemEntity(double x, double y, Tile tile, boolean isBackground) {
        this(x, y, tile, isBackground, 1);
    }

    public void update(World world) {
        vy += GRAVITY;
        vx *= FRICTION;
        if (pickupCooldown > 0) pickupCooldown--;

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

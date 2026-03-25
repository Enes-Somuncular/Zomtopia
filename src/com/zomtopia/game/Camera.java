package com.zomtopia.game;

import com.zomtopia.game.world.World;

public class Camera {
    public double x, y;  // top-left of viewport in world-pixels

    private final int viewW, viewH;

    public Camera(int viewW, int viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
    }

    /** Center camera on the player's position */
    public void follow(double playerPx, double playerPy) {
        x = playerPx - viewW / 2.0;
        y = playerPy - viewH / 2.0;

        // Clamp to world bounds
        int worldPixW = World.WIDTH  * World.TILE_SIZE;
        int worldPixH = World.HEIGHT * World.TILE_SIZE;
        x = Math.max(0, Math.min(x, worldPixW  - viewW));
        y = Math.max(0, Math.min(y, worldPixH - viewH));
    }

    /** World-pixel → screen X */
    public int toScreenX(double wx) { return (int)(wx - x); }
    /** World-pixel → screen Y */
    public int toScreenY(double wy) { return (int)(wy - y); }
    /** Screen X → world tile X */
    public int toTileX(int sx) { return (int)((sx + x) / World.TILE_SIZE); }
    /** Screen Y → world tile Y */
    public int toTileY(int sy) { return (int)((sy + y) / World.TILE_SIZE); }
}

package com.zomtopia.game.world;

public class World {
    public static final int WIDTH     = 100;
    public static final int HEIGHT    = 100;
    public static final int TILE_SIZE = 32;

    // Foreground layer – solid, collidable
    private final int[][] fg = new int[WIDTH][HEIGHT];
    // Background layer – decorative, walkthrough
    private final int[][] bg = new int[WIDTH][HEIGHT];

    public World() {
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++) {
                fg[x][y] = Tile.AIR.id;
                bg[x][y] = Tile.AIR.id;
            }
    }

    // ---- Foreground ----
    public Tile getFg(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return Tile.BEDROCK;
        return Tile.fromId(fg[x][y]);
    }
    public void setFg(int x, int y, Tile tile) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        fg[x][y] = tile.id;
    }

    // ---- Background ----
    public Tile getBg(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return Tile.AIR;
        return Tile.fromId(bg[x][y]);
    }
    public void setBg(int x, int y, Tile tile) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        bg[x][y] = tile.id;
    }

    // ---- Collision (only foreground is solid) ----
    public boolean isSolid(int x, int y) {
        return getFg(x, y).solid;
    }

    public boolean isRectBlocked(double px, double py, double pw, double ph) {
        int x0 = (int) Math.floor(px / TILE_SIZE);
        int y0 = (int) Math.floor(py / TILE_SIZE);
        int x1 = (int) Math.floor((px + pw - 1) / TILE_SIZE);
        int y1 = (int) Math.floor((py + ph - 1) / TILE_SIZE);
        for (int tx = x0; tx <= x1; tx++)
            for (int ty = y0; ty <= y1; ty++)
                if (isSolid(tx, ty)) return true;
        return false;
    }
}

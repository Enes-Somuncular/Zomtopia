package com.zomtopia.game.world;

public class World {
    public static final int WIDTH  = 100;
    public static final int HEIGHT = 100;
    public static final int TILE_SIZE = 32;

    private final int[][] tiles = new int[WIDTH][HEIGHT];

    public World() {
        // Fill with air by default
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                tiles[x][y] = Tile.AIR.id;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return Tile.BEDROCK;
        return Tile.fromId(tiles[x][y]);
    }

    public void setTile(int x, int y, Tile tile) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        tiles[x][y] = tile.id;
    }

    public boolean isSolid(int x, int y) {
        return getTile(x, y).solid;
    }

    /** Pixel-based bounds check for player/entity collision */
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

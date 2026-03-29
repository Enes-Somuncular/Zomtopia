package com.zomtopia.game.world;

import java.util.Random;

public class WorldGenerator {
    private final Random rng;

    public WorldGenerator(long seed) {
        this.rng = new Random(seed);
    }

    public void generate(World world) {
        int W = World.WIDTH;
        int H = World.HEIGHT;

        // ---- Terrain Heights (Flat) ----
        int surf = H - 30; // Constant surface level

        // ---- Fill World ----
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (y == H - 1) {
                    world.setFg(x, y, Tile.BEDROCK);
                    world.setBg(x, y, Tile.BEDROCK);
                } else if (y == surf) {
                    world.setFg(x, y, Tile.GRASS);
                    world.setBg(x, y, Tile.DIRT);
                } else if (y > surf && y < surf + 5) {
                    world.setFg(x, y, Tile.DIRT);
                    world.setBg(x, y, Tile.DIRT);
                } else if (y >= surf + 5) {
                    Tile stone = rng.nextInt(7) == 0 ? Tile.DIRT : Tile.ROCK;
                    world.setFg(x, y, stone);
                    world.setBg(x, y, stone);
                }
                // Sky: FG stays AIR, BG stays AIR
            }
        }

        // ---- Caves (remove foreground blocks, keep background) ----
        for (int iter = 0; iter < 12; iter++) {
            int cx = rng.nextInt(W);
            int cy = surf + 5 + rng.nextInt(20);  // below surface
            int cw = 4 + rng.nextInt(6);
            int ch = 3 + rng.nextInt(4);
            for (int tx = cx; tx < cx + cw && tx < W; tx++)
                for (int ty = cy; ty < cy + ch && ty < H - 1; ty++)
                    world.setFg(tx, ty, Tile.AIR);  // BG stays → visible cave wall
        }

        // ---- Iron Ore Patches ----
        for (int i = 0; i < 20; i++) {
            int ix = rng.nextInt(W);
            int iy = surf + 8 + rng.nextInt(15);
            // Simple 2x2 or 1x1 patches
            int size = 1 + rng.nextInt(2);
            for (int ox = 0; ox < size && ix + ox < W; ox++) {
                for (int oy = 0; oy < size && iy + oy < H - 1; oy++) {
                    if (world.getFg(ix + ox, iy + oy) == Tile.ROCK) {
                        world.setFg(ix + ox, iy + oy, Tile.IRON_ORE);
                        world.setBg(ix + ox, iy + oy, Tile.IRON_ORE);
                    }
                }
            }
        }
    }
}

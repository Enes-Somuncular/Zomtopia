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

        // ---- Terrain Heights ----
        int[] heights = new int[W];
        int base = H - 30;
        int h    = base;
        for (int x = 0; x < W; x++) {
            h += rng.nextInt(3) - 1;
            h = Math.max(base - 8, Math.min(base + 8, h));
            heights[x] = h;
        }

        // ---- Fill World ----
        for (int x = 0; x < W; x++) {
            int surf = heights[x];
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
        for (int iter = 0; iter < 8; iter++) {
            int cx = rng.nextInt(W);
            int cy = heights[cx] + 5 + rng.nextInt(20);  // below surface
            int cw = 4 + rng.nextInt(6);
            int ch = 3 + rng.nextInt(4);
            for (int tx = cx; tx < cx + cw && tx < W; tx++)
                for (int ty = cy; ty < cy + ch && ty < H - 1; ty++)
                    world.setFg(tx, ty, Tile.AIR);  // BG stays → visible cave wall
        }

        // ---- Trees ----
        for (int x = 3; x < W - 3; x++) {
            if (rng.nextInt(10) == 0) {
                int surf = heights[x];
                int treeH = 4 + rng.nextInt(3);
                for (int y = surf - treeH; y < surf; y++) {
                    if (y >= 0) {
                        world.setFg(x, y, Tile.WOOD);
                        world.setBg(x, y, Tile.WOOD);
                    }
                }
                // Leaves
                int topY = surf - treeH;
                for (int lx = x - 2; lx <= x + 2; lx++)
                    for (int ly = topY - 2; ly <= topY + 1; ly++)
                        if (ly >= 0 && world.getFg(lx, ly) == Tile.AIR) {
                            world.setFg(lx, ly, Tile.LEAVES);
                            world.setBg(lx, ly, Tile.LEAVES);
                        }
            }
        }
    }
}

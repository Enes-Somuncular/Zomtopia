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

        // --- Terrain Heights (simple noise) ---
        int[] heights = new int[W];
        int base = H - 30;           // ground level around y=70
        int h = base;
        for (int x = 0; x < W; x++) {
            h += rng.nextInt(3) - 1;  // walk: -1, 0, +1
            h = Math.max(base - 10, Math.min(base + 10, h));
            heights[x] = h;
        }

        // --- Fill world ---
        for (int x = 0; x < W; x++) {
            int surfaceY = heights[x];
            for (int y = 0; y < H; y++) {
                if (y == H - 1) {
                    world.setTile(x, y, Tile.BEDROCK); // unbreakable bottom
                } else if (y == surfaceY) {
                    world.setTile(x, y, Tile.GRASS);
                } else if (y > surfaceY && y < surfaceY + 5) {
                    world.setTile(x, y, Tile.DIRT);
                } else if (y >= surfaceY + 5) {
                    // Rock with occasional dirt pocket
                    world.setTile(x, y, rng.nextInt(7) == 0 ? Tile.DIRT : Tile.ROCK);
                }
            }
        }

        // --- Trees ---
        for (int x = 3; x < W - 3; x++) {
            if (rng.nextInt(10) == 0) {
                int surfaceY = heights[x];
                int treeH = 4 + rng.nextInt(3);
                // Trunk
                for (int y = surfaceY - treeH; y < surfaceY; y++) {
                    if (y >= 0) world.setTile(x, y, Tile.WOOD);
                }
                // Leaves
                int topY = surfaceY - treeH;
                for (int lx = x - 2; lx <= x + 2; lx++)
                    for (int ly = topY - 2; ly <= topY + 2; ly++)
                        if (ly >= 0 && world.getTile(lx, ly) == Tile.AIR)
                            world.setTile(lx, ly, Tile.LEAVES);
            }
        }
    }
}

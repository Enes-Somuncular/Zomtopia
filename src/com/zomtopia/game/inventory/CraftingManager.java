package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;
import java.util.HashMap;
import java.util.Map;

public class CraftingManager {
    
    private static final Map<String, Recipe> recipes = new HashMap<>();

    static {
        // --- 2x2 Recipes (Inventory Crafting) ---
        // Note: These will be stored as 9-tile strings, with indices 0,1,3,4 used for 2x2.
        
        // 1 Log -> 4 Wood Planks
        addRecipe2x2(new Tile[]{Tile.LOG, null, null, null}, Tile.WOOD_PLANKS, 4);
        
        // 4 Planks -> 1 Crafting Table
        addRecipe2x2(new Tile[]{Tile.WOOD_PLANKS, Tile.WOOD_PLANKS, Tile.WOOD_PLANKS, Tile.WOOD_PLANKS}, Tile.CRAFTING_TABLE, 1);
        
        // 4 Rock -> 1 Bedrock
        addRecipe2x2(new Tile[]{Tile.ROCK, Tile.ROCK, Tile.ROCK, Tile.ROCK}, Tile.BEDROCK, 1);
        
        // 1 Grass -> 1 Leaves
        addRecipe2x2(new Tile[]{Tile.GRASS, null, null, null}, Tile.LEAVES, 1);
        
        // --- 3x3 Recipes (Crafting Table) ---
        
        // Iron Ore Hub: 4 Iron Ore -> 1 Iron Ingot
        addRecipe2x2(new Tile[]{Tile.IRON_ORE, Tile.IRON_ORE, Tile.IRON_ORE, Tile.IRON_ORE}, Tile.IRON_INGOT, 1);
        
        // Iron Tool: 4 Iron Ingot -> 1 Iron Pickaxe
        addRecipe2x2(new Tile[]{Tile.IRON_INGOT, Tile.IRON_INGOT, Tile.IRON_INGOT, Tile.IRON_INGOT}, Tile.IRON_PICKAXE, 1);
    }

    private static void addRecipe2x2(Tile[] grid2x2, Tile result, int amount) {
        Tile[] grid3x3 = new Tile[9];
        // Map 2x2 to a 3x3 grid (top left 2x2)
        grid3x3[0] = grid2x2[0];
        grid3x3[1] = grid2x2[1];
        grid3x3[3] = grid2x2[2];
        grid3x3[4] = grid2x2[3];
        addRecipe3x3(grid3x3, result, amount);
    }

    private static void addRecipe3x3(Tile[] grid, Tile result, int amount) {
        StringBuilder sb = new StringBuilder();
        for (Tile t : grid) {
            sb.append(t == null ? "AIR" : t.name()).append(",");
        }
        recipes.put(sb.toString(), new Recipe(result, amount));
    }

    public static ItemStack checkRecipe(ItemStack[] grid, boolean is3x3) {
        StringBuilder sb = new StringBuilder();
        boolean empty = true;
        
        ItemStack[] finalGrid = grid;
        // If not in 3x3 mode, we map the first 4 slots (0,1,2,3) to 3x3 (0,1,3,4)
        if (!is3x3) {
            ItemStack[] temp = new ItemStack[9];
            temp[0] = grid[0]; temp[1] = grid[1];
            temp[3] = grid[2]; temp[4] = grid[3];
            finalGrid = temp;
        }

        for (ItemStack s : finalGrid) {
            Tile t = (s == null || s.amount <= 0) ? Tile.AIR : s.tile;
            if (t != Tile.AIR) empty = false;
            sb.append(t.name()).append(",");
        }
        
        if (empty) return null;

        Recipe r = recipes.get(sb.toString());
        if (r != null) {
            return new ItemStack(r.result, r.amount, false);
        }
        return null;
    }

    private static class Recipe {
        final Tile result;
        final int amount;
        Recipe(Tile result, int amount) {
            this.result = result;
            this.amount = amount;
        }
    }
}

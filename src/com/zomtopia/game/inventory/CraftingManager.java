package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;
import java.util.HashMap;
import java.util.Map;

public class CraftingManager {
    
    private static final Map<String, Recipe> recipes = new HashMap<>();

    static {
        // Simple recipes for the 2x2 grid
        // Format: "tile1,tile2,tile3,tile4" where null is "AIR"
        
        // Example: 1 Wood -> 4 Dirt (Placeholder for Planks)
        addRecipe(new Tile[]{Tile.WOOD, null, null, null}, Tile.DIRT, 4);
        
        // Example: 4 Rock -> 1 Bedrock
        addRecipe(new Tile[]{Tile.ROCK, Tile.ROCK, Tile.ROCK, Tile.ROCK}, Tile.BEDROCK, 1);
        
        // Example: 1 Grass -> 1 Leaves
        addRecipe(new Tile[]{Tile.GRASS, null, null, null}, Tile.LEAVES, 1);
    }

    private static void addRecipe(Tile[] grid, Tile result, int amount) {
        StringBuilder sb = new StringBuilder();
        for (Tile t : grid) {
            sb.append(t == null ? "AIR" : t.name()).append(",");
        }
        recipes.put(sb.toString(), new Recipe(result, amount));
    }

    public static ItemStack checkRecipe(ItemStack[] grid) {
        StringBuilder sb = new StringBuilder();
        boolean empty = true;
        for (ItemStack s : grid) {
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

import com.zomtopia.game.inventory.CraftingManager;
import com.zomtopia.game.inventory.ItemStack;
import com.zomtopia.game.world.Tile;

public class CraftingTester {
    public static void main(String[] args) {
        // Mock a 9-slot grid containing 1 LOG in the first slot (2x2 mode)
        ItemStack[] grid2x2 = new ItemStack[9];
        grid2x2[0] = new ItemStack(Tile.LOG, 1, false);
        
        System.out.println("Testing LOG -> PLANKS (2x2)...");
        ItemStack result = CraftingManager.checkRecipe(grid2x2, false);
        if (result != null) {
            System.out.println("Result: " + result.tile.name() + " x" + result.amount);
        } else {
            System.out.println("No recipe found!");
        }

        // Mock a 9-slot grid containing 4 PLANKS in 2x2 pattern
        ItemStack[] gridPlanks = new ItemStack[9];
        gridPlanks[0] = new ItemStack(Tile.WOOD_PLANKS, 1, false);
        gridPlanks[1] = new ItemStack(Tile.WOOD_PLANKS, 1, false);
        gridPlanks[2] = new ItemStack(Tile.WOOD_PLANKS, 1, false);
        gridPlanks[3] = new ItemStack(Tile.WOOD_PLANKS, 1, false);

        System.out.println("\nTesting PLANKS -> TABLE (2x2)...");
        result = CraftingManager.checkRecipe(gridPlanks, false);
        if (result != null) {
            System.out.println("Result: " + result.tile.name() + " x" + result.amount);
        } else {
            System.out.println("No recipe found!");
        }
    }
}

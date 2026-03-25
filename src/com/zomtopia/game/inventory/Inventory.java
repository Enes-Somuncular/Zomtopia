package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;

public class Inventory {
    private final ItemStack[] slots;
    public static final int SIZE = 40;

    public Inventory() {
        slots = new ItemStack[SIZE];
        // Starting items for testing/demo (all FG)
        addItem(Tile.DIRT, 20, false);
        addItem(Tile.GRASS, 10, false);
        addItem(Tile.ROCK, 5, false);
        addItem(Tile.WOOD, 5, false);
        addItem(Tile.LEAVES, 5, false);
    }

    public boolean addItem(Tile tile, int count, boolean isBackground) {
        if (tile == Tile.AIR || tile == Tile.BEDROCK) return false;

        // Try to add to existing stack (must match tile AND layer type)
        for (ItemStack stack : slots) {
            if (stack != null && stack.tile == tile && stack.isBackground == isBackground) {
                stack.amount += count;
                return true;
            }
        }

        // Try to find empty slot
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = new ItemStack(tile, count, isBackground);
                return true;
            }
        }

        return false; // Inventory full
    }

    public boolean hasItem(Tile tile) {
        for (ItemStack stack : slots) {
            if (stack != null && stack.tile == tile && stack.amount > 0) {
                return true;
            }
        }
        return false;
    }

    public void removeItem(Tile tile, boolean isBackground) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].tile == tile && slots[i].isBackground == isBackground) {
                slots[i].amount--;
                if (slots[i].amount <= 0) {
                    slots[i] = null;
                }
                return;
            }
        }
    }

    public ItemStack getStack(int index) {
        if (index < 0 || index >= SIZE) return null;
        return slots[index];
    }

    public void setStack(int index, ItemStack stack) {
        if (index >= 0 && index < SIZE) {
            slots[index] = stack;
        }
    }
}

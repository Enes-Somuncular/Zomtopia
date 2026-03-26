package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;

public class Inventory {
    private final ItemStack[] slots;
    private final ItemStack[] equipment;
    public static final int SIZE = 40;

    public Inventory() {
        this.slots = new ItemStack[40];
        this.equipment = new ItemStack[6];
        
        // Demo items for testing
        addItem(Tile.DIRT, 64, false);
        addItem(Tile.RED_SHIRT, 1, false);
        addItem(Tile.BLUE_HAT, 1, false);
        addItem(Tile.WINGS, 1, false);
        addItem(Tile.DARK_MASK, 1, false);
        addItem(Tile.JEANS, 1, false);
        addItem(Tile.SNEAKERS, 1, false);
        
        // Initial tools
        addItem(Tile.WOODEN_PICKAXE, 1, false);
        addItem(Tile.WOODEN_AXE, 1, false);
        addItem(Tile.WOODEN_SWORD, 1, false);
        addItem(Tile.WOODEN_SHOVEL, 1, false);
    }

    public ItemStack[] getSlots() { return slots; }
    public ItemStack[] getEquipment() { return equipment; }

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

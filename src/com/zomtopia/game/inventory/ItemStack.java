package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;

public class ItemStack {
    public Tile tile;
    public int amount;
    public boolean isBackground;

    public ItemStack(Tile tile, int amount, boolean isBackground) {
        this.tile = tile;
        this.amount = amount;
        this.isBackground = isBackground;
    }
}

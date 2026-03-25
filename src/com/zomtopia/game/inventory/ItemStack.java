package com.zomtopia.game.inventory;

import com.zomtopia.game.world.Tile;

public class ItemStack {
    public Tile tile;
    public int amount;

    public ItemStack(Tile tile, int amount) {
        this.tile = tile;
        this.amount = amount;
    }
}

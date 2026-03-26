package com.zomtopia.utils;

import com.zomtopia.game.world.World;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.entity.Player;
import com.zomtopia.game.inventory.Inventory;
import com.zomtopia.game.inventory.ItemStack;

import java.io.*;

public class SaveManager {

    private static final String SAVE_FILE = System.getProperty("user.dir") + File.separator + "savegame.dat";

    public static void saveGame(World world, Player player) {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(SAVE_FILE))) {
            // 1. Player Data
            out.writeDouble(player.x);
            out.writeDouble(player.y);
            out.writeInt(player.healthHalf);
            out.writeFloat(player.stamina);
            out.writeInt(player.foodHalf);
            // We need access to GamePanel's worldTimeSeconds or pass it as param. 
            // For now, let's just save player state.

            // 2. Inventory Data
            Inventory inv = player.getInventory();
            ItemStack[] slots = inv.getSlots();
            out.writeInt(slots.length);
            for (ItemStack stack : slots) {
                if (stack == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(stack.tile.id);
                    out.writeInt(stack.amount);
                    out.writeBoolean(stack.isBackground);
                }
            }

            ItemStack[] equip = inv.getEquipment();
            out.writeInt(equip.length);
            for (ItemStack stack : equip) {
                if (stack == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(stack.tile.id);
                    out.writeInt(stack.amount);
                    out.writeBoolean(stack.isBackground);
                }
            }

            // 3. World Data
            out.writeInt(World.WIDTH);
            out.writeInt(World.HEIGHT);
            for (int x = 0; x < World.WIDTH; x++) {
                for (int y = 0; y < World.HEIGHT; y++) {
                    out.writeInt(world.getFg(x, y).id);
                    out.writeInt(world.getBg(x, y).id);
                }
            }

            System.out.println("Oyun kaydedildi: " + SAVE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadGame(World world, Player player) {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return false;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            // 1. Player Data
            player.x = in.readDouble();
            player.y = in.readDouble();
            int loadedHealth = in.readInt();
            // Backward compatibility: older saves stored full hearts (0..10).
            player.healthHalf = loadedHealth <= 10 ? loadedHealth * 2 : loadedHealth;
            player.stamina = in.readFloat();
            try {
                player.foodHalf = in.readInt();
            } catch (EOFException e) {
                player.foodHalf = player.maxFoodHalf;
            }

            // 2. Inventory Data
            Inventory inv = player.getInventory();
            int slotCount = in.readInt();
            for (int i = 0; i < slotCount; i++) {
                int id = in.readInt();
                if (id == -1) {
                    inv.getSlots()[i] = null;
                } else {
                    int amount = in.readInt();
                    boolean isBg = in.readBoolean();
                    inv.getSlots()[i] = new ItemStack(Tile.fromId(id), amount, isBg);
                }
            }

            int equipCount = in.readInt();
            for (int i = 0; i < equipCount; i++) {
                int id = in.readInt();
                if (id == -1) {
                    inv.getEquipment()[i] = null;
                } else {
                    int amount = in.readInt();
                    boolean isBg = in.readBoolean();
                    inv.getEquipment()[i] = new ItemStack(Tile.fromId(id), amount, isBg);
                }
            }

            // 3. World Data
            int w = in.readInt();
            int h = in.readInt();
            // Basic validation
            if (w == World.WIDTH && h == World.HEIGHT) {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        world.setFg(x, y, Tile.fromId(in.readInt()));
                        world.setBg(x, y, Tile.fromId(in.readInt()));
                    }
                }
            }
            
            System.out.println("Oyun yüklendi: " + SAVE_FILE);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

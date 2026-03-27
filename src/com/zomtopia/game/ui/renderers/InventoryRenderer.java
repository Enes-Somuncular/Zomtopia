package com.zomtopia.game.ui.renderers;

import com.zomtopia.game.inventory.Inventory;
import com.zomtopia.game.inventory.ItemStack;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.entity.Player;

import java.awt.*;

public class InventoryRenderer {

    // Layout configuration
    public int invX, invY, invW, invH;
    public int gridX, gridY, slotSize, slotPadding;
    public int eqX, eqY, eqSlotSize, eqPadding;
    public int craftX, craftY, craftSlotSize;
    public int previewX, previewY, previewW, previewH;
    public int detailX, detailY, detailW, detailH;

    private static final Color GLASS_BG = new Color(20, 20, 25, 230);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 30);
    private static final Color SLOT_BG = new Color(255, 255, 255, 15);
    private static final Color SLOT_BORDER = new Color(255, 255, 255, 25);
    private static final Color ACCENT_COLOR = new Color(100, 150, 255);

    public void updateLayout(int screenW, int screenH) {
        invW = 760;
        invH = 480;
        invX = (screenW - invW) / 2;
        invY = (screenH - invH) / 2;

        previewX = invX + 30;
        previewY = invY + 60;
        previewW = 200;
        previewH = 260;

        craftX = invX + 30;
        craftY = invY + 340;
        craftSlotSize = 44;

        gridX = invX + 260;
        gridY = invY + 70;
        slotSize = 44;
        slotPadding = 8;

        detailX = invX + 25;
        detailY = invY + invH - 60;
        detailW = invW - 50;
        detailH = 40;
    }

    public void drawInventory(Graphics2D g2, Inventory inv, int mouseX, int mouseY, Player player) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(invX + 5, invY + 5, invW, invH, 24, 24);

        // Glass Background
        g2.setColor(GLASS_BG);
        g2.fillRoundRect(invX, invY, invW, invH, 24, 24);
        
        // Glass Border
        g2.setColor(GLASS_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(invX, invY, invW, invH, 24, 24);
        
        // Title
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("ENVANTER & ÜRETİM", invX + 25, invY + 45);

        // 1. Draw Character Preview & Equipment Slots
        drawPreview(g2, player);
        drawEquipmentSlots(g2, inv.getEquipment(), mouseX, mouseY);
        
        // 2. Draw Crafting Grid (Below Preview)
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("ÜRETİM MASASI", craftX, craftY - 10);
        drawSlots(g2, inv.getCraftingGrid(), craftX, craftY, 2, 2, mouseX, mouseY, 200);

        // Arrow and Result
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("→", craftX + (craftSlotSize + slotPadding) * 2 + 5, craftY + craftSlotSize);
        drawSlot(g2, inv.getCraftingResult(), craftX + (craftSlotSize + slotPadding) * 2 + 35, craftY + craftSlotSize/2, craftSlotSize, mouseX, mouseY, 300);

        // 3. Draw Main Inventory (Right Side, 8x5)
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("HAZİNE", gridX, gridY - 10);
        drawSlots(g2, inv.getSlots(), gridX, gridY, 8, 5, mouseX, mouseY);

        // 4. Detail Panel
        int hoveredIdx = getSlotAt(mouseX, mouseY);
        if (hoveredIdx != -1) {
            ItemStack hovered = null;
            if (hoveredIdx < 100) hovered = inv.getSlots()[hoveredIdx];
            else if (hoveredIdx < 200) hovered = inv.getEquipment()[hoveredIdx - 100];
            else if (hoveredIdx < 300) hovered = inv.getCraftingGrid()[hoveredIdx - 200];
            else if (hoveredIdx == 300) hovered = inv.getCraftingResult();
            if (hovered != null && hovered.tile != Tile.AIR) drawDetailPanel(g2, hovered);
        }
    }

    private void drawEquipmentSlots(Graphics2D g2, ItemStack[] equip, int mx, int my) {
        int[][] pos = getEquipPositions();
        for (int i = 0; i < 6; i++) {
            drawSlot(g2, equip[i], pos[i][0], pos[i][1], slotSize, mx, my, 100 + i);
        }
    }

    private int[][] getEquipPositions() {
        return new int[][]{
            {previewX + 70, previewY + 10},  // 100: Hat
            {previewX + 115, previewY + 45}, // 101: Mask
            {previewX + 70, previewY + 80},  // 102: Shirt
            {previewX + 70, previewY + 140}, // 103: Pants
            {previewX + 70, previewY + 200}, // 104: Shoes
            {previewX + 20, previewY + 80}   // 105: Back
        };
    }

    private void drawSlots(Graphics2D g2, ItemStack[] stacks, int x, int y, int cols, int rows, int mx, int my) {
        drawSlots(g2, stacks, x, y, cols, rows, mx, my, 0);
    }

    private void drawSlots(Graphics2D g2, ItemStack[] stacks, int x, int y, int cols, int rows, int mx, int my, int offset) {
        for (int i = 0; i < stacks.length; i++) {
            int r = i / cols;
            int c = i % cols;
            int sx = x + c * (slotSize + slotPadding);
            int sy = y + r * (slotSize + slotPadding);
            drawSlot(g2, stacks[i], sx, sy, slotSize, mx, my, offset + i);
        }
    }

    private void drawSlot(Graphics2D g2, ItemStack stack, int x, int y, int size, int mx, int my, int id) {
        boolean hovered = (mx >= x && mx <= x + size && my >= y && my <= y + size);
        
        // Slot background
        g2.setColor(hovered ? new Color(255, 255, 255, 40) : SLOT_BG);
        g2.fillRoundRect(x, y, size, size, 12, 12);
        
        // Border
        g2.setColor(hovered ? ACCENT_COLOR : SLOT_BORDER);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
        g2.drawRoundRect(x, y, size, size, 12, 12);

        if (stack != null && stack.tile != Tile.AIR) {
            drawItemIcon(g2, stack, x + 6, y + 6, size - 12);
            
            // Amount
            if (stack.amount > 1) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                String amt = String.valueOf(stack.amount);
                int tw = g2.getFontMetrics().stringWidth(amt);
                g2.drawString(amt, x + size - tw - 6, y + size - 6);
            }
        }
    }

    public void drawItemIcon(Graphics2D g2, ItemStack stack, int x, int y, int size) {
        Tile tile = stack.tile;
        Color base = stack.isBackground ? tile.color.darker().darker() : tile.color;
        
        if (tile.category == Tile.Category.BLOCK) {
            g2.setColor(base);
            g2.fillRoundRect(x, y, size, size, 4, 4);
            g2.setColor(base.darker());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, size, size, 4, 4);
        } else {
            // Draw a simplified icon for items/tools
            g2.setColor(base);
            g2.fillOval(x + 2, y + 2, size - 4, size - 4);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, size/2));
            String initial = tile.name().substring(0, 1);
            g2.drawString(initial, x + size/3, y + 2*size/3);
        }
    }

    /**
     * Helper for stickman/player rendering.
     * x, y is the center point (except for some categories where it's the anchor).
     */
    public void drawEquippedItem(Graphics2D g2, ItemStack stack, int x, int y, int size, boolean isCircle) {
        if (stack == null || stack.tile == Tile.AIR) return;
        g2.setColor(stack.tile.color.brighter());
        if (isCircle) {
            g2.fillOval(x - size/2 - 2, y - 2, size + 4, size + 4);
        } else {
            if (stack.tile.category == Tile.Category.SHIRT) {
                g2.setStroke(new BasicStroke(4.5f));
                g2.drawLine(x, y, x, y + size);
            } else if (stack.tile.category == Tile.Category.BACK) {
                g2.fillOval(x - 22, y - 5, 18, 25);
                g2.fillOval(x + 4, y - 5, 18, 25);
            } else if (stack.tile.category == Tile.Category.PANTS) {
                g2.setStroke(new BasicStroke(4.5f));
                g2.drawLine(x, y, x - 5, y + size);
                g2.drawLine(x, y, x + 5, y + size);
            } else if (stack.tile.category == Tile.Category.SHOES) {
                g2.fillRoundRect(x - 5, y - 2, 10, 5, 2, 2);
            }
        }
    }

    private void drawPreview(Graphics2D g2, Player player) {
        // Semi-transparent box for stickman
        g2.setColor(new Color(255, 255, 255, 10));
        g2.fillRoundRect(previewX, previewY - 5, previewW, previewH + 5, 20, 20);
        g2.setColor(SLOT_BORDER);
        g2.drawRoundRect(previewX, previewY - 5, previewW, previewH + 5, 20, 20);
        
        // Centers for drawing stickman segments to align with slots
        int cx = previewX + 70 + slotSize/2;
        int cyHat = previewY + 10 + slotSize/2;
        int cyShirt = previewY + 80 + slotSize/2;
        int cyPants = previewY + 140 + slotSize/2;
        int cyShoes = previewY + 200 + slotSize/2;
        
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(3f));
        
        // Head circle
        g2.drawOval(cx - 15, cyHat - 15, 30, 30);
        // Spine
        g2.drawLine(cx, cyHat + 15, cx, cyShirt + 15);
        // Arms
        g2.drawLine(cx, cyHat + 25, cx - 25, cyShirt);
        g2.drawLine(cx, cyHat + 25, cx + 25, cyShirt);
        // Legs
        g2.drawLine(cx, cyShirt + 15, cx - 15, cyPants + 15);
        g2.drawLine(cx, cyShirt + 15, cx + 15, cyPants + 15);
        // Feet
        g2.drawLine(cx - 15, cyPants + 15, cx - 15, cyShoes);
        g2.drawLine(cx + 15, cyPants + 15, cx + 15, cyShoes);

        g2.setStroke(new BasicStroke(1f));
        
        // The actual equipment visual representation (not just slots)
        // is handled in drawEquippedItem called from GamePanel, 
        // but for preview we want the items drawn on the stickman.
        ItemStack[] equip = player.getInventory().getEquipment();
        drawEquippedItem(g2, equip[5], cx, cyShirt, 0, false); // Back
        drawEquippedItem(g2, equip[0], cx, cyHat - 10, 14, true); // Hat
        drawEquippedItem(g2, equip[1], cx, cyHat, 14, true); // Mask
        drawEquippedItem(g2, equip[2], cx, cyShirt - 15, 30, false); // Shirt
        drawEquippedItem(g2, equip[3], cx, cyShirt + 15, 30, false); // Pants
        drawEquippedItem(g2, equip[4], cx - 15, cyShoes, 10, false); // Shoes L
        drawEquippedItem(g2, equip[4], cx + 15, cyShoes, 10, false); // Shoes R
    }

    private void drawDetailPanel(Graphics2D g2, ItemStack stack) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(detailX, detailY, detailW, detailH, 15, 15);
        g2.setColor(ACCENT_COLOR);
        g2.drawRoundRect(detailX, detailY, detailW, detailH, 15, 15);
        
        int tx = detailX + 15;
        int ty = detailY + 25;
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(stack.tile.name().replace("_", " "), tx, ty);
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString("Kategori: " + stack.tile.category, tx, ty + 20);
        
        if (stack.tile.solid) {
            g2.setColor(new Color(150, 255, 150));
            g2.drawString("• Katı Blok", tx, ty + 40);
        }
    }

    public int getSlotAt(int mx, int my) {
        // 1. Main Inventory (0-39) -> 8 cols x 5 rows
        for (int i = 0; i < 40; i++) {
            int r = i / 8;
            int c = i % 8;
            int sx = gridX + c * (slotSize + slotPadding);
            int sy = gridY + r * (slotSize + slotPadding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return i;
        }
        
        // 2. Equipment (100-105)
        int[][] equipPos = getEquipPositions();
        for (int i = 0; i < 6; i++) {
            int sx = equipPos[i][0];
            int sy = equipPos[i][1];
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return 100 + i;
        }

        // 3. Crafting Grid (200-203)
        for (int i = 0; i < 4; i++) {
            int r = i / 2;
            int c = i % 2;
            int sx = craftX + c * (slotSize + slotPadding);
            int sy = craftY + r * (slotSize + slotPadding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return 200 + i;
        }

        // 4. Crafting Result (300)
        int resX = craftX + (craftSlotSize + slotPadding) * 2 + 35;
        int resY = craftY + craftSlotSize / 2;
        if (mx >= resX && mx <= resX + slotSize && my >= resY && my <= resY + slotSize) return 300;

        return -1;
    }

    public void drawHotbar(Graphics2D g2, Inventory inv, int screenW, int screenH, int selectedSlot) {
        int hbSlotSize = 42;
        int hbPadding = 5;
        int totalW = 10 * (hbSlotSize + hbPadding) - hbPadding;
        int x0 = (screenW - totalW) / 2;
        int y = screenH - hbSlotSize - 18;

        // Hotbar Glass
        g2.setColor(new Color(10, 10, 15, 170));
        g2.fillRoundRect(x0 - 8, y - 8, totalW + 16, hbSlotSize + 16, 12, 12);
        g2.setColor(GLASS_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x0 - 8, y - 8, totalW + 16, hbSlotSize + 16, 12, 12);

        for (int i = 0; i < 10; i++) {
            int sx = x0 + i * (hbSlotSize + hbPadding);
            boolean sel = (i == selectedSlot);
            
            ItemStack stack = inv.getSlots()[i];
            
            // Background
            g2.setColor(sel ? new Color(255, 255, 255, 50) : SLOT_BG);
            g2.fillRoundRect(sx, y, hbSlotSize, hbSlotSize, 10, 10);
            
            // Border
            g2.setColor(sel ? ACCENT_COLOR : SLOT_BORDER);
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
            g2.drawRoundRect(sx, y, hbSlotSize, hbSlotSize, 10, 10);
            
            if (stack != null && stack.tile != Tile.AIR) {
                drawItemIcon(g2, stack, sx + 7, y + 7, hbSlotSize - 14);
                if (stack.amount > 1) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 11));
                    String amt = String.valueOf(stack.amount);
                    g2.drawString(amt, sx + hbSlotSize - 14, y + hbSlotSize - 6);
                }
            }
        }
    }
}

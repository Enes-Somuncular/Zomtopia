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
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 40);
    private static final Color SLOT_BG = new Color(255, 255, 255, 15);
    private static final Color SLOT_BORDER = new Color(255, 255, 255, 25);
    private static final Color ACCENT_COLOR = new Color(100, 150, 255);

    public void updateLayout(int screenW, int screenH) {
        invW = 760;
        invH = 480;
        invX = (screenW - invW) / 2;
        invY = (screenH - invH) / 2;

        gridX = invX + 30;
        gridY = invY + 80;
        slotSize = 44;
        slotPadding = 6;

        previewX = invX + 540;
        previewY = invY + 80;
        previewW = 180;
        previewH = 160;

        eqX = previewX;
        eqY = previewY + previewH + 40;
        eqSlotSize = 40;
        eqPadding = 8;
        
        craftX = invX + 540;
        craftY = invY + 80;
        craftSlotSize = 44;
        
        // Let's reposition: Grid on left. Mid-right: Crafting & Equipment. Far right: Details.
        // Revised layout:
        gridX = invX + 25;
        gridY = invY + 70;
        
        // Crafting Grid (2x2)
        craftX = invX + 535;
        craftY = invY + 70;
        
        // Equipment (2 columns)
        eqX = invX + 535;
        eqY = craftY + (craftSlotSize + slotPadding) * 2 + 30;
        
        // Preview Box (Moved to far right or center)
        previewX = invX + 380;
        previewY = invY + 70;
        previewW = 140;
        previewH = 200;
        
        // Detail panel at bottom or top
        detailX = invX + 25;
        detailY = invY + invH - 100;
        detailW = invW - 50;
        detailH = 80;
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

        // 1. Draw Main Slots
        drawSlots(g2, inv.getSlots(), gridX, gridY, 10, 4, mouseX, mouseY);
        
        // 2. Draw Crafting Grid (2x2)
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("ÜRETİM", craftX, craftY - 10);
        drawSlots(g2, inv.getCraftingGrid(), craftX, craftY, 2, 2, mouseX, mouseY, 200);

        // Arrow for crafting
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("→", craftX + (craftSlotSize + slotPadding) * 2 + 5, craftY + craftSlotSize);
        
        // Result Slot
        int resX = craftX + (craftSlotSize + slotPadding) * 2 + 35;
        int resY = craftY + craftSlotSize / 2;
        drawSlot(g2, inv.getCraftingResult(), resX, resY, craftSlotSize, mouseX, mouseY, 300);

        // 3. Draw Equipment
        g2.drawString("EKİPMAN", eqX, eqY - 10);
        drawSlots(g2, inv.getEquipment(), eqX, eqY, 2, 3, mouseX, mouseY, 100);

        // 4. Draw Character Preview
        drawPreview(g2, player);

        // 5. Draw Info Panel for hovered item
        int hoveredIdx = getSlotAt(mouseX, mouseY);
        if (hoveredIdx != -1) {
            ItemStack hovered = null;
            if (hoveredIdx < 100) hovered = inv.getSlots()[hoveredIdx];
            else if (hoveredIdx < 200) hovered = inv.getEquipment()[hoveredIdx - 100];
            else if (hoveredIdx < 300) hovered = inv.getCraftingGrid()[hoveredIdx - 200];
            else if (hoveredIdx == 300) hovered = inv.getCraftingResult();
            
            if (hovered != null && hovered.tile != Tile.AIR) {
                drawDetailPanel(g2, hovered);
            }
        }
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
        g2.setColor(SLOT_BG);
        g2.fillRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        g2.setColor(SLOT_BORDER);
        g2.drawRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("KARAKTER", previewX + 10, previewY + 20);
        
        // Detailed Stickman Preview
        int cx = previewX + previewW / 2;
        int cy = previewY + previewH / 2 + 15;
        int headSize = 24;
        int spineLen = 60;
        
        ItemStack[] equip = player.getInventory().getEquipment();
        
        // Back
        drawEquippedItem(g2, equip[5], cx, cy - spineLen + 20, 0, false);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4f));
        // Head
        g2.drawOval(cx - headSize/2, cy - spineLen - headSize, headSize, headSize);
        // Gear
        drawEquippedItem(g2, equip[0], cx, cy - spineLen - headSize, headSize, true); // Hat
        drawEquippedItem(g2, equip[1], cx, cy - spineLen - headSize, headSize, true); // Mask
        
        // Spine
        g2.setColor(Color.WHITE);
        g2.drawLine(cx, cy - spineLen, cx, cy);
        drawEquippedItem(g2, equip[2], cx, cy - spineLen, spineLen, false); // Shirt
        
        // Arms
        g2.drawLine(cx, cy - spineLen + 10, cx - 25, cy - spineLen + 30);
        g2.drawLine(cx, cy - spineLen + 10, cx + 25, cy - spineLen + 30);
        
        // Legs
        g2.drawLine(cx, cy, cx - 20, cy + 40);
        g2.drawLine(cx, cy, cx + 20, cy + 40);
        drawEquippedItem(g2, equip[3], cx, cy, 30, false); // Pants
        drawEquippedItem(g2, equip[4], cx - 20, cy + 40, 0, false);
        drawEquippedItem(g2, equip[4], cx + 20, cy + 40, 0, false);
        
        g2.setStroke(new BasicStroke(1f));
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
        // Main Grid (0-39)
        for (int i = 0; i < 40; i++) {
            int r = i / 10;
            int c = i % 10;
            int sx = gridX + c * (slotSize + slotPadding);
            int sy = gridY + r * (slotSize + slotPadding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return i;
        }
        
        // Equipment (100-105)
        for (int i = 0; i < 6; i++) {
            int r = i / 2;
            int c = i % 2;
            int sx = eqX + c * (slotSize + slotPadding);
            int sy = eqY + r * (slotSize + slotPadding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return 100 + i;
        }

        // Crafting Grid (200-203)
        for (int i = 0; i < 4; i++) {
            int r = i / 2;
            int c = i % 2;
            int sx = craftX + c * (slotSize + slotPadding);
            int sy = craftY + r * (slotSize + slotPadding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return 200 + i;
        }

        // Crafting Result (300)
        int resX = craftX + (craftSlotSize + slotPadding) * 2 + 35;
        int resY = craftY + craftSlotSize / 2;
        if (mx >= resX && mx <= resX + slotSize && my >= resY && my <= resY + slotSize) return 300;

        return -1;
    }

    public void drawHotbar(Graphics2D g2, Inventory inv, int screenW, int screenH, int selectedSlot) {
        int hbSlotSize = 48;
        int hbPadding = 8;
        int totalW = 10 * (hbSlotSize + hbPadding) - hbPadding;
        int x0 = (screenW - totalW) / 2;
        int y = screenH - hbSlotSize - 20;

        // Hotbar Glass
        g2.setColor(new Color(10, 10, 15, 180));
        g2.fillRoundRect(x0 - 10, y - 10, totalW + 20, hbSlotSize + 20, 15, 15);
        g2.setColor(GLASS_BORDER);
        g2.drawRoundRect(x0 - 10, y - 10, totalW + 20, hbSlotSize + 20, 15, 15);

        for (int i = 0; i < 10; i++) {
            int sx = x0 + i * (hbSlotSize + hbPadding);
            boolean sel = (i == selectedSlot);
            
            ItemStack stack = inv.getSlots()[i];
            
            // Background
            g2.setColor(sel ? new Color(255, 255, 255, 60) : SLOT_BG);
            g2.fillRoundRect(sx, y, hbSlotSize, hbSlotSize, 12, 12);
            
            // Border
            g2.setColor(sel ? ACCENT_COLOR : SLOT_BORDER);
            g2.setStroke(new BasicStroke(sel ? 3f : 1.5f));
            g2.drawRoundRect(sx, y, hbSlotSize, hbSlotSize, 12, 12);
            
            if (stack != null && stack.tile != Tile.AIR) {
                drawItemIcon(g2, stack, sx + 8, y + 8, hbSlotSize - 16);
                if (stack.amount > 1) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 12));
                    String amt = String.valueOf(stack.amount);
                    g2.drawString(amt, sx + hbSlotSize - 16, y + hbSlotSize - 8);
                }
            }
        }
    }
}

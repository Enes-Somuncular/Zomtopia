package com.zomtopia.game;

import com.zomtopia.game.entity.ItemEntity;
import com.zomtopia.game.entity.Player;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.world.Tile.Category;
import com.zomtopia.game.world.World;
import com.zomtopia.game.world.WorldGenerator;
import com.zomtopia.game.inventory.Inventory;
import com.zomtopia.game.inventory.ItemStack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.zomtopia.utils.SaveManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private final World world;
    private final Player player;
    private final Camera camera;
    private Timer gameTimer;

    // Gameplay State
    private int selectedSlot = 0;
    private int breakX = -1, breakY = -1;
    private float breakProgress = 0;
    private static final float BREAK_SPEED = 0.065f;

    // Items and Inventory UI
    private final List<ItemEntity> droppedItems = new CopyOnWriteArrayList<>();
    private boolean inventoryOpen = false;
    private boolean escapeMenuOpen = false;
    private int draggedSourceIdx = -1;
    private ItemStack draggedStack = null;

    // Input state
    private int mouseScreenX, mouseScreenY;
    private boolean leftHeld, rightHeld;
    private long lastPlacementTime = 0;
    private static final long PLACEMENT_COOLDOWN = 150;
    private static final int T = World.TILE_SIZE;

    // Snapshot of mouse target for sync
    private int targetTX, targetTY;
    private int targetSX, targetSY; // Screen snapshot
    private boolean isInRangeBreak, isInRangePlace;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));

        world = new World();
        new WorldGenerator(System.currentTimeMillis()).generate(world);

        player = new Player();
        // Spawn above surface at world middle
        int mx = World.WIDTH / 2;
        player.x = mx * World.TILE_SIZE;
        for (int y = 0; y < World.HEIGHT; y++) {
            if (world.getFg(mx, y) != Tile.AIR) {
                player.y = (y - 2) * World.TILE_SIZE;
                break;
            }
        }

        camera = new Camera(800, 600);
        camera.follow(player.x, player.y);

        addKeyListener(this);
        addMouseListener(this);
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { /* reset keys if needed */ }
            @Override public void focusLost(FocusEvent e) { 
                leftHeld = false; rightHeld = false; 
                if (player != null) {
                    player.left = player.right = player.jump = player.sprinting = player.crouchInput = false;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { mouseScreenX = e.getX(); mouseScreenY = e.getY(); }
            @Override public void mouseDragged(MouseEvent e) { mouseScreenX = e.getX(); mouseScreenY = e.getY(); }
        });
        addMouseWheelListener(e -> {
            selectedSlot = (selectedSlot + (e.getWheelRotation() > 0 ? 1 : -1) + com.zomtopia.game.inventory.Inventory.SIZE) % com.zomtopia.game.inventory.Inventory.SIZE;
        });
        setFocusable(true);
        startGameLoop();
    }

    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            if (!escapeMenuOpen) {
                player.update(world);
                camera.follow(player.x + Player.W / 2.0, player.y + Player.H / 2.0);
                updateItems();
                handleBlockInteraction();
                
                // Continuous placement with cooldown
                if (rightHeld && !inventoryOpen) {
                    long now = System.currentTimeMillis();
                    if (now - lastPlacementTime >= PLACEMENT_COOLDOWN) {
                        if (isInRangePlace) {
                            handleRightClickPlacement(targetTX, targetTY);
                            lastPlacementTime = now;
                        }
                    }
                }
            }
            repaint();
        });
        gameTimer.start();
    }

    private void updateItems() {
        for (ItemEntity item : droppedItems) {
            item.update(world);

            Rectangle itemRect   = new Rectangle((int) item.x, (int) item.y, ItemEntity.SIZE, ItemEntity.SIZE);
            Rectangle playerRect = new Rectangle((int) player.x, (int) player.y, Player.W, Player.H);

            if (itemRect.intersects(playerRect)) {
                if (player.getInventory().addItem(item.tile, 1, item.isBackground)) {
                    droppedItems.remove(item);
                }
            }
        }
    }

    /** Called every tick: handle held mouse buttons */
    private void handleBlockInteraction() {
        // Direct polling for mouse position (more robust on Mac during key repeats)
        Point p = getMousePosition();
        if (p != null) {
            mouseScreenX = p.x;
            mouseScreenY = p.y;
        }

        targetTX = camera.toTileX(mouseScreenX);
        targetTY = camera.toTileY(mouseScreenY);

        // SNAPSHOT the screen position RIGHT NOW for the renderer
        targetSX = camera.toScreenX(targetTX * T);
        targetSY = camera.toScreenY(targetTY * T);

        double px = player.x + Player.W / 2.0;
        double py = player.y + Player.H / 2.0;
        double txC = targetTX * T + T / 2.0;
        double tyC = targetTY * T + T / 2.0;
        double dist = Math.sqrt(Math.pow(txC - px, 2) + Math.pow(tyC - py, 2)) / T;
        
        // Increased ranges for smoother movement interaction
        isInRangeBreak = (dist <= 4.5);
        isInRangePlace = (dist <= 5.5);

        if (leftHeld && !inventoryOpen) {
            player.startPunch();
            
            if (!isInRangeBreak) {
                resetBreak();
                return;
            }

            Tile fgTile = world.getFg(targetTX, targetTY);
            Tile bgTile = world.getBg(targetTX, targetTY);

            boolean hasFg = fgTile != Tile.AIR && fgTile != Tile.BEDROCK;
            boolean hasBg = bgTile != Tile.AIR && bgTile != Tile.BEDROCK;

            if (!hasFg && !hasBg) {
                resetBreak();
                return;
            }

            // Reset if moved to different tile
            if (targetTX != breakX || targetTY != breakY) {
                breakX = targetTX; breakY = targetTY; breakProgress = 0;
            }

            breakProgress += BREAK_SPEED;

            if (breakProgress >= 1.0f) {
                // Break FG first, then BG on next hold
                Tile brokenTile;
                if (hasFg) {
                    brokenTile = fgTile;
                    world.setFg(targetTX, targetTY, Tile.AIR);
                } else {
                    brokenTile = bgTile;
                    world.setBg(targetTX, targetTY, Tile.AIR);
                }
                // Spawn dropped item with layer info
                droppedItems.add(new ItemEntity(targetTX * World.TILE_SIZE + 8, targetTY * World.TILE_SIZE + 8, brokenTile, !hasFg));
                resetBreak();
            }
        } else {
            // Not holding – slowly decay break progress
            if (breakProgress > 0) breakProgress = Math.max(0, breakProgress - 0.02f);
            if (breakProgress == 0) resetBreak();
        }

        // Note: placement is now handled in mousePressed to avoid multi-placement bug
    }

    private void resetBreak() { breakX = -1; breakY = -1; breakProgress = 0; }

    // ────────────────── RENDERING ──────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 180, 240), 0, getHeight(), new Color(170, 220, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int T = World.TILE_SIZE;
        int startX = Math.max(0, (int)(camera.x / T) - 1);
        int startY = Math.max(0, (int)(camera.y / T) - 1);
        int endX   = Math.min(World.WIDTH  - 1, startX + getWidth()  / T + 2);
        int endY   = Math.min(World.HEIGHT - 1, startY + getHeight() / T + 2);

        for (int tx = startX; tx <= endX; tx++) {
            for (int ty = startY; ty <= endY; ty++) {
                int sx = camera.toScreenX(tx * T);
                int sy = camera.toScreenY(ty * T);

                Tile bgTile = world.getBg(tx, ty);
                Tile fgTile = world.getFg(tx, ty);

                // --- Background layer (darker, no top border, walkthrough) ---
                if (bgTile != Tile.AIR) {
                    Color bgColor = bgTile.color.darker().darker();
                    g2.setColor(bgColor);
                    g2.fillRect(sx, sy, T, T);
                    g2.setColor(bgColor.darker());
                    g2.drawRect(sx, sy, T - 1, T - 1);
                }

                // --- Foreground layer (full bright, solid) ---
                if (fgTile != Tile.AIR) {
                    g2.setColor(fgTile.color);
                    g2.fillRect(sx, sy, T, T);

                    // Depth border
                    if (fgTile != Tile.LEAVES) {
                        g2.setColor(fgTile.color.darker());
                        g2.drawRect(sx, sy, T - 1, T - 1);
                    }
                    // Grass top highlight
                    if (fgTile == Tile.GRASS) {
                        g2.setColor(new Color(80, 200, 60));
                        g2.fillRect(sx, sy, T, 4);
                    }
                }

                // --- Break progress overlay ---
                if (tx == breakX && ty == breakY && breakProgress > 0) {
                    int alpha = (int)(200 * breakProgress);
                    g2.setColor(new Color(0, 0, 0, Math.min(200, alpha)));
                    g2.fillRect(sx, sy, T, T);
                    // Crack lines proportional to progress
                    g2.setColor(new Color(255, 255, 255, 100));
                    int cracks = (int)(breakProgress * 5);
                    for (int c = 0; c < cracks; c++) {
                        g2.drawLine(sx + T/2, sy + T/2,
                                sx + (c % 2 == 0 ? T - 4 : 4),
                                sy + (c < 2 ? 4 : T - 4));
                    }
                }
            }
        }

        // --- Hover outline (Always visible for clarity, color-coded by range) ---
        if (!inventoryOpen && !escapeMenuOpen) {
            Tile fg = world.getFg(targetTX, targetTY);
            boolean inRange = (fg != Tile.AIR) ? isInRangeBreak : isInRangePlace;

            // Use the SNAPSHOTTED screen coordinates for perfect sync
            int hsx = targetSX;
            int hsy = targetSY;

            if (inRange) {
                g2.setColor(new Color(255, 255, 255, 80)); // White if in range
            } else {
                g2.setColor(new Color(255, 0, 0, 40));     // Faint red if out of range
            }
            g2.fillRect(hsx, hsy, T, T);
            
            g2.setColor(inRange ? Color.WHITE : new Color(255, 100, 100, 100));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(hsx, hsy, T, T);
        }
        g2.setStroke(new BasicStroke(1f));
        // --- Stickman Player sprite ---
        int spx = camera.toScreenX((int) player.x);
        int spy = camera.toScreenY((int) player.y);
        int w = Player.W;
        int h = player.crouching ? 30 : 60;
        boolean facingLeft = player.facingLeft;
        
        Inventory inv = player.getInventory();
        ItemStack[] equip = inv.getEquipment();
        
        g2.setStroke(new BasicStroke(2.5f));
        
        int centerX = spx + w/2;
        int headSize = 16;
        int spineBottomY = spy + h - 14;
        int armY = spy + headSize + 6;

        // Back items (Wings etc) - Behind spine but in front of back arm maybe? No, behind everything for wings.
        drawEquip(g2, equip[5], centerX, armY, 0, false); // BACK

        g2.setColor(Color.BLACK);
        
        // Head
        g2.drawOval(centerX - headSize/2, spy, headSize, headSize);
        // Hat
        drawEquip(g2, equip[0], centerX, spy, headSize, true); // HAT
        // Mask
        drawEquip(g2, equip[1], centerX, spy, headSize, true); // MASK

        // Body (Spine)
        g2.setColor(Color.BLACK);
        g2.drawLine(centerX, spy + headSize, centerX, spineBottomY);
        // Shirt
        drawEquip(g2, equip[2], centerX, spy + headSize, (spineBottomY - (spy + headSize)), false); // SHIRT

        // --- Arms & Held Item ---
        int armX = centerX;
        float p = player.punchAnim;
        
        // Back arm
        g2.setColor(Color.BLACK);
        g2.drawLine(armX, armY, armX + (facingLeft ? 8 : -8), armY + 10);
        
        // Front arm (animated & holding item)
        double swingAngle = (p > 0) ? (facingLeft ? -1.0 : 1.0) * Math.sin(p * Math.PI) * 1.2 : 0;
        int handX = armX + (int)((facingLeft ? -12 : 12) * Math.cos(swingAngle) - 10 * Math.sin(swingAngle));
        int handY = armY + (int)((facingLeft ? -12 : 12) * Math.sin(swingAngle) + 10 * Math.cos(swingAngle));
        
        g2.setColor(Color.BLACK);
        g2.drawLine(armX, armY, handX, handY);
        
        // Held Item
        ItemStack held = inv.getStack(selectedSlot);
        if (held != null && held.tile != com.zomtopia.game.world.Tile.AIR) {
            int itemSize = 12;
            boolean isBg = held.isBackground;
            g2.setColor(isBg ? held.tile.color.darker() : held.tile.color);
            g2.fillRoundRect(handX - 6, handY - 6, itemSize, itemSize, 3, 3);
        }
        
        // Legs & Pants/Shoes
        g2.setColor(Color.BLACK);
        if (player.crouching) {
            g2.drawLine(centerX, spineBottomY, centerX - 8, spy + h); // Left
            g2.drawLine(centerX, spineBottomY, centerX + 8, spy + h); // Right
            drawEquip(g2, equip[3], centerX, spineBottomY, 14, false); // PANTS
        } else {
            double walkCycle = (System.currentTimeMillis() % 400) / 400.0;
            int legOffset = (Math.abs(player.vx) > 0.5) ? (int)(Math.sin(walkCycle * Math.PI * 2) * 10) : 0;
            
            g2.drawLine(centerX, spineBottomY, centerX - 6 + legOffset, spy + h); // Left
            g2.drawLine(centerX, spineBottomY, centerX + 6 - legOffset, spy + h); // Right
            drawEquip(g2, equip[3], centerX, spineBottomY, 14, false); // PANTS
            drawEquip(g2, equip[4], centerX - 6 + legOffset, spy + h, 0, false); // SHOES L
            drawEquip(g2, equip[4], centerX + 6 - legOffset, spy + h, 0, false); // SHOES R
        }
        
        g2.setStroke(new BasicStroke(1f));
        
        drawDroppedItems(g2);
        drawHUD(g2);
        if (inventoryOpen) {
            drawExpandedInventory(g2);
            drawDraggedItem(g2);
        }
        if (escapeMenuOpen) {
            drawEscapeMenu(g2);
        }
    }

    private void drawEscapeMenu(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        // Dim background
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);

        int menuW = 260, menuH = 320;
        int menuX = (w - menuW) / 2;
        int menuY = (h - menuH) / 2;

        g2.setColor(new Color(30, 30, 35, 240));
        g2.fillRoundRect(menuX, menuY, menuW, menuH, 20, 20);
        g2.setColor(new Color(150, 150, 200, 100));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(menuX, menuY, menuW, menuH, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        String title = "MENÜ";
        g2.drawString(title, menuX + (menuW - g2.getFontMetrics().stringWidth(title))/2, menuY + 45);

        String[] buttons = {"DEVAM ET", "KAYDET", "YÜKLE", "KAYDET VE ÇIK"};
        for (int i = 0; i < buttons.length; i++) {
            int bx = menuX + 30;
            int by = menuY + 70 + i * 60;
            int bw = menuW - 60;
            int bh = 45;

            g2.setColor(new Color(50, 50, 65));
            g2.fillRoundRect(bx, by, bw, bh, 10, 10);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawRoundRect(bx, by, bw, bh, 10, 10);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            int tw = g2.getFontMetrics().stringWidth(buttons[i]);
            g2.drawString(buttons[i], bx + (bw - tw)/2, by + 28);
        }
    }

    private void drawStickmanPreview(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(Color.WHITE);
        
        int headSize = 24;
        int spineLen = 60;
        
        Inventory inv = player.getInventory();
        ItemStack[] equip = inv.getEquipment();
        
        // Back
        drawEquip(g2, equip[5], cx, cy - spineLen + 20, 0, false);
        
        g2.setColor(Color.WHITE);
        // Head
        g2.drawOval(cx - headSize/2, cy - spineLen - headSize, headSize, headSize);
        // Gear
        drawEquip(g2, equip[0], cx, cy - spineLen - headSize, headSize, true); // Hat
        drawEquip(g2, equip[1], cx, cy - spineLen - headSize, headSize, true); // Mask
        
        // Spine
        g2.setColor(Color.WHITE);
        g2.drawLine(cx, cy - spineLen, cx, cy);
        drawEquip(g2, equip[2], cx, cy - spineLen, spineLen, false); // Shirt
        
        // Arms
        g2.drawLine(cx, cy - spineLen + 10, cx - 25, cy - spineLen + 30);
        g2.drawLine(cx, cy - spineLen + 10, cx + 25, cy - spineLen + 30);
        
        // Legs
        g2.drawLine(cx, cy, cx - 20, cy + 40);
        g2.drawLine(cx, cy, cx + 20, cy + 40);
        drawEquip(g2, equip[3], cx, cy, 30, false); // Pants
        drawEquip(g2, equip[4], cx - 20, cy + 40, 0, false);
        drawEquip(g2, equip[4], cx + 20, cy + 40, 0, false);
        
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawDraggedItem(Graphics2D g2) {
        if (draggedStack != null && draggedStack.tile != Tile.AIR) {
            int slotSize = 44;
            g2.setColor(draggedStack.tile.color);
            g2.fillRoundRect(mouseScreenX - slotSize/2 + 8, mouseScreenY - slotSize/2 + 8, slotSize - 16, slotSize - 16, 4, 4);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString(String.valueOf(draggedStack.amount), mouseScreenX - slotSize/2 + 4, mouseScreenY - slotSize/2 + 14);
        }
    }

    private void drawDroppedItems(Graphics2D g2) {
        for (ItemEntity item : droppedItems) {
            int sx = camera.toScreenX((int) item.x);
            int sy = camera.toScreenY((int) item.y);
            boolean bg = item.isBackground;
            int renderSize = bg ? ItemEntity.SIZE - 4 : ItemEntity.SIZE;
            int offset = bg ? 2 : 0;

            g2.setColor(item.tile.color.darker());
            g2.fillRoundRect(sx + offset, sy + offset, renderSize, renderSize, 4, 4);
            g2.setColor(bg ? item.tile.color.darker() : item.tile.color);
            g2.fillRoundRect(sx + offset + 2, sy + offset + 2, renderSize - 4, renderSize - 4, 2, 2);
        }
    }

    private void drawHUD(Graphics2D g2) {
        int slotSize = 44;
        int padding  = 6;
        int hotbarSize = 10;
        int totalW   = hotbarSize * (slotSize + padding) - padding;
        int sx0      = (getWidth() - totalW) / 2;
        int y        = getHeight() - slotSize - 16;

        // --- Status Bars (Health & Stamina) ---
        int barWidth = totalW;
        int barX = sx0;
        // Health: 10 hearts
        int heartSize = 16;
        int heartPadding = 4;
        for (int i = 0; i < player.maxHealth; i++) {
            int hx = barX + i * (heartSize + heartPadding);
            if (i < player.health) {
                g2.setColor(new Color(220, 40, 40)); // Filled heart red
            } else {
                g2.setColor(new Color(60, 20, 20, 150)); // Empty heart dark
            }
            // Simplification: just a small rounded rect for now for pixel-look
            g2.fillRoundRect(hx, y - 38, heartSize, heartSize, 4, 4);
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawRoundRect(hx, y - 38, heartSize, heartSize, 4, 4);
        }

        // Stamina bar
        int sBarH = 8;
        int sBarY = y - 18;
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(barX, sBarY, barWidth, sBarH, 4, 4);
        float staminaRatio = player.stamina / player.maxStamina;
        g2.setColor(staminaRatio > 0.2 ? new Color(40, 200, 40) : new Color(200, 150, 40));
        g2.fillRoundRect(barX, sBarY, (int)(barWidth * staminaRatio), sBarH, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(barX, sBarY, barWidth, sBarH, 4, 4);

        // --- Hotbar ---
        for (int i = 0; i < hotbarSize; i++) {
            int sx = sx0 + i * (slotSize + padding);
            boolean sel = (i == selectedSlot);
            com.zomtopia.game.inventory.ItemStack stack = player.getInventory().getStack(i);

            Color slotBg = sel ? new Color(255, 255, 255, 220) : (inventoryOpen ? new Color(100, 100, 100, 240) : new Color(0, 0, 0, 140));
            g2.setColor(slotBg);
            g2.fillRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setColor(sel ? Color.YELLOW : (inventoryOpen ? Color.WHITE : Color.GRAY));
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.drawRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            if (stack != null && stack.tile != Tile.AIR) {
                boolean isBg = stack.isBackground;
                int iconSize = isBg ? slotSize - 22 : slotSize - 16;
                int iconOff  = isBg ? 11 : 8;

                g2.setColor(isBg ? stack.tile.color.darker() : stack.tile.color);
                g2.fillRoundRect(sx + iconOff, y + iconOff, iconSize, iconSize, 4, 4);
                
                if (isBg) {
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.drawRoundRect(sx + iconOff, y + iconOff, iconSize, iconSize, 4, 4);
                }
                
                // Miktar
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString(String.valueOf(stack.amount), sx + 4, y + 14);
                
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String name = stack.tile.name().substring(0, Math.min(3, stack.tile.name().length()));
                if (isBg) name += "B";
                g2.drawString(name, sx + 4, y + slotSize - 4);
            }
        }

        // Controls
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(8, 8, 240, 95, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("WASD / ← →   Hareket & Zıpla", 16, 26);
        g2.drawString("SHIFT: Koşma (Stamina)", 16, 44);
        g2.drawString("Sol Tık (Basılı): Kır (Menzil: 3)", 16, 62);
        g2.drawString("Sağ Tık: Blok Yerleştir (Menzil: 4)", 16, 80);
        g2.drawString("Scroll / 1-5: Blok Seç", 16, 98);
    }

    private void drawEquip(Graphics2D g2, ItemStack stack, int x, int y, int size, boolean isCircle) {
        if (stack == null || stack.tile == Tile.AIR) return;
        g2.setColor(stack.tile.color.brighter());
        if (isCircle) {
            g2.fillOval(x - size/2 - 2, y - 2, size + 4, size + 4);
        } else {
            if (stack.tile.category == Tile.Category.SHIRT) {
                g2.setStroke(new BasicStroke(4.5f));
                g2.drawLine(x, y, x, y + size);
                g2.setStroke(new BasicStroke(2.5f));
            } else if (stack.tile.category == Tile.Category.BACK) {
                g2.fillOval(x - 22, y - 5, 18, 25);
                g2.fillOval(x + 4, y - 5, 18, 25);
            } else if (stack.tile.category == Tile.Category.PANTS) {
                g2.setStroke(new BasicStroke(4.5f));
                g2.drawLine(x, y, x - 5, y + size);
                g2.drawLine(x, y, x + 5, y + size);
                g2.setStroke(new BasicStroke(2.5f));
            } else if (stack.tile.category == Tile.Category.SHOES) {
                g2.fillRoundRect(x - 5, y - 2, 10, 5, 2, 2);
            }
        }
    }

    private void drawExpandedInventory(Graphics2D g2) {
        int w = 800, h = 600;
        int invW = 740, invH = 480;
        int invX = (w - invW) / 2;
        int invY = (h - invH) / 2;
        
        // Background
        g2.setColor(new Color(30, 30, 30, 248));
        g2.fillRoundRect(invX, invY, invW, invH, 20, 20);
        g2.setColor(new Color(150, 150, 200, 150));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(invX, invY, invW, invH, 20, 20);
        
        int slotSize = 44;
        int padding = 8;
        Inventory inv = player.getInventory();
        
        // --- Left Section: Main Slots (4x10) ---
        int gridX = invX + 20;
        int gridY = invY + 70;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("ENVANTER", gridX, gridY - 20);
        
        for (int i = 0; i < 40; i++) {
            int row = i / 10;
            int col = i % 10;
            int sx = gridX + col * (slotSize + padding);
            int sy = gridY + row * (slotSize + padding);
            
            ItemStack stack = inv.getSlots()[i];
            
            g2.setColor(new Color(45, 45, 50, 220));
            g2.fillRoundRect(sx, sy, slotSize, slotSize, 10, 10);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sx, sy, slotSize, slotSize, 10, 10);
            
            if (stack != null && stack.tile != Tile.AIR) {
                g2.setColor(stack.isBackground ? stack.tile.color.darker() : stack.tile.color);
                g2.fillRoundRect(sx + 6, sy + 6, slotSize - 12, slotSize - 12, 5, 5);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString(String.valueOf(stack.amount), sx + 4, sy + 14);
            }
        }
        
        // --- Right Section: Character Preview Box ---
        int previewX = invX + 575;
        int previewY = invY + 70;
        int previewW = 140;
        int previewH = 260;
        
        g2.setColor(new Color(45, 45, 45));
        g2.fillRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        g2.setColor(new Color(80, 80, 100));
        g2.drawRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        
        // Draw Large Stickman in Preview
        drawStickmanPreview(g2, previewX + previewW / 2, previewY + 120);
        
        // --- Equipment Slots (Around Character) ---
        int[][] eqCoords = {
            {previewX + previewW/2 - 20, previewY - 55}, // HAT
            {previewX - 48, previewY + 10},              // MASK
            {previewX - 48, previewY + 70},              // SHIRT
            {previewX - 48, previewY + 130},             // PANTS
            {previewX + previewW/2 - 20, previewY + previewH + 10}, // SHOE
            {previewX + previewW + 8, previewY + 70}     // BACK
        };
        String[] eqLabels = {"BAŞLIK", "MASKE", "GÖVDE", "BACAK", "AYAK", "SIRT"};
        
        for (int i = 0; i < 6; i++) {
            int sx = eqCoords[i][0];
            int sy = eqCoords[i][1];
            ItemStack stack = inv.getEquipment()[i];
            
            g2.setColor(new Color(60, 60, 70, 220));
            g2.fillRoundRect(sx, sy, slotSize, slotSize, 12, 12);
            g2.setColor(new Color(180, 180, 220, 120));
            g2.drawRoundRect(sx, sy, slotSize, slotSize, 12, 12);
            
            g2.setColor(new Color(200, 200, 200, 150));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString(eqLabels[i], sx, sy - 4);
            
            if (stack != null && stack.tile != Tile.AIR) {
                g2.setColor(stack.tile.color);
                g2.fillRoundRect(sx + 10, sy + 10, slotSize - 20, slotSize - 20, 6, 6);
            }
        }
    }

    // ──────────── INPUT ────────────
    private int getSlotAt(int mx, int my) {
        if (!inventoryOpen) return -1;
        
        int w = 800, h = 600;
        int invW = 740, invH = 480;
        int invX = (w - invW) / 2;
        int invY = (h - invH) / 2;
        int slotSize = 44, padding = 8;
        
        // Main inventory slots
        int gridX = invX + 20;
        int gridY = invY + 70;
        for (int i = 0; i < 40; i++) {
            int col = i % 10;
            int row = i / 10;
            int sx = gridX + col * (slotSize + padding);
            int sy = gridY + row * (slotSize + padding);
            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) return i;
        }
        
        // Character preview equipment slots
        int previewX = invX + 575;
        int previewY = invY + 70;
        int previewW = 140;
        int previewH = 260;
        int[][] eqCoords = {
            {previewX + previewW/2 - 20, previewY - 55}, // HAT
            {previewX - 48, previewY + 10},              // MASK
            {previewX - 48, previewY + 70},              // SHIRT
            {previewX - 48, previewY + 130},             // PANTS
            {previewX + previewW/2 - 20, previewY + previewH + 10}, // SHOE
            {previewX + previewW + 8, previewY + 70}     // BACK
        };
        for (int i = 0; i < 6; i++) {
            int sx = eqCoords[i][0];
            int sy = eqCoords[i][1];
            // Using 40x40 hit area for equipment slots
            if (mx >= sx && mx <= sx + 40 && my >= sy && my <= sy + 40) return 100 + i;
        }
        
        return -1;
    }

    @Override public void mousePressed(MouseEvent e) {
        if (escapeMenuOpen) {
            int mx = e.getX(), my = e.getY();
            int w = 800, h = 600;
            int menuW = 260, menuH = 320;
            int menuX = (w - menuW) / 2;
            int menuY = (h - menuH) / 2;

            for (int i = 0; i < 4; i++) {
                int bx = menuX + 30;
                int by = menuY + 70 + i * 60;
                int bw = menuW - 60;
                int bh = 45;
                if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                    if (i == 0) escapeMenuOpen = false;
                    if (i == 1) SaveManager.saveGame(world, player);
                    if (i == 2) SaveManager.loadGame(world, player);
                    if (i == 3) {
                        SaveManager.saveGame(world, player);
                        System.exit(0);
                    }
                    repaint();
                    return;
                }
            }
            return;
        }

        if (inventoryOpen) {
            int slot = getSlotAt(e.getX(), e.getY());
            if (slot != -1) {
                Inventory inv = player.getInventory();
                if (slot < 100) { // Standard inventory slot
                    draggedStack = inv.getSlots()[slot];
                    inv.getSlots()[slot] = null;
                } else { // Equipment slot
                    int eqIdx = slot - 100;
                    draggedStack = inv.getEquipment()[eqIdx];
                    inv.getEquipment()[eqIdx] = null;
                }
                draggedSourceIdx = slot;
                return;
            }
        }
        
        if (SwingUtilities.isLeftMouseButton(e)) {
            leftHeld = true;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            rightHeld = true;
            // Immediate placement on first press if in range
            if (isInRangePlace && !inventoryOpen) {
                handleRightClickPlacement(targetTX, targetTY);
                lastPlacementTime = System.currentTimeMillis();
            }
        }
        requestFocusInWindow();
    }

    private void handleRightClickPlacement(int tx, int ty) { // Modified to take tx, ty
        double px = player.x + Player.W / 2.0;
        double py = player.y + Player.H / 2.0;
        double txC = tx * World.TILE_SIZE + World.TILE_SIZE / 2.0;
        double tyC = ty * World.TILE_SIZE + World.TILE_SIZE / 2.0;
        double distPlace = Math.sqrt(Math.pow(px - txC, 2) + Math.pow(py - tyC, 2));

        if (distPlace <= 4.5 * World.TILE_SIZE) { 
            com.zomtopia.game.inventory.ItemStack selStack = player.getInventory().getStack(selectedSlot);
            if (selStack != null && selStack.amount > 0) {
                // Animasyon tetikle
                player.startPunch();

                Rectangle playerRect = new Rectangle((int) player.x, (int) player.y, Player.W, Player.H);
                Rectangle tileRect   = new Rectangle(tx * World.TILE_SIZE, ty * World.TILE_SIZE,
                                                      World.TILE_SIZE, World.TILE_SIZE);
                if (!playerRect.intersects(tileRect)) {
                    if (selStack.isBackground) {
                        if (world.getBg(tx, ty) == Tile.AIR) {
                            world.setBg(tx, ty, selStack.tile);
                            player.getInventory().removeItem(selStack.tile, true);
                        }
                    } else {
                        // Foreground blocks ONLY place on Foreground AIR
                        if (world.getFg(tx, ty) == Tile.AIR) {
                            world.setFg(tx, ty, selStack.tile);
                            player.getInventory().removeItem(selStack.tile, false);
                        }
                    }
                }
            }
        }
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            leftHeld = false;
            resetBreak();
        }
        
        // Strict button check to avoid Mac modifier interference
        // ONLY release if it's explicitly the Right Button (BUTTON3)
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightHeld = false;
        } else if (SwingUtilities.isRightMouseButton(e) && e.getButton() != 0) {
            // Some Mac mice/trackpads use BUTTON1 + Meta, handle with care
            rightHeld = false;
        }

        if (draggedStack != null) {
            int targetIndex = getSlotAt(e.getX(), e.getY());
            Inventory inv = player.getInventory();
            
            if (targetIndex >= 0) {
                if (targetIndex < 100) {
                    // Standard slot
                    ItemStack temp = inv.getSlots()[targetIndex];
                    inv.getSlots()[targetIndex] = draggedStack;
                    if (draggedSourceIdx < 100) { // Was from standard slot
                        inv.getSlots()[draggedSourceIdx] = temp;
                    } else { // Was from equipment slot
                        inv.getEquipment()[draggedSourceIdx - 100] = temp;
                    }
                } else {
                    // Equipment slot
                    int eqIdx = targetIndex - 100;
                    Tile.Category[] cats = {Tile.Category.HAT, Tile.Category.MASK, Tile.Category.SHIRT, Tile.Category.PANTS, Tile.Category.SHOES, Tile.Category.BACK};
                    if (draggedStack.tile.category == cats[eqIdx]) {
                        ItemStack temp = inv.getEquipment()[eqIdx];
                        inv.getEquipment()[eqIdx] = draggedStack;
                        if (draggedSourceIdx < 100) { // Was from standard slot
                            inv.getSlots()[draggedSourceIdx] = temp;
                        } else { // Was from equipment slot
                            inv.getEquipment()[draggedSourceIdx - 100] = temp;
                        }
                    } else {
                        // Return to source if not compatible
                        if (draggedSourceIdx < 100) {
                            inv.getSlots()[draggedSourceIdx] = draggedStack;
                        } else {
                            inv.getEquipment()[draggedSourceIdx - 100] = draggedStack;
                        }
                    }
                }
            } else {
                // Drop item
                droppedItems.add(new ItemEntity(player.x, player.y, draggedStack.tile, draggedStack.isBackground));
            }
            draggedStack = null;
            draggedSourceIdx = -1;
        }

        if (SwingUtilities.isLeftMouseButton(e))  { leftHeld  = false; resetBreak(); }
        if (SwingUtilities.isRightMouseButton(e)) { /* No longer needed for placement */ }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escapeMenuOpen = !escapeMenuOpen;
            if (escapeMenuOpen) inventoryOpen = false;
            repaint();
            return;
        }
        if (escapeMenuOpen) return;

        player.handleKeyPress(e.getKeyCode());
        int k = e.getKeyCode() - KeyEvent.VK_1;
        if (k >= 0 && k < com.zomtopia.game.inventory.Inventory.SIZE) selectedSlot = k;
        if (e.getKeyCode() == KeyEvent.VK_E) {
            if (inventoryOpen && draggedStack != null) {
                player.getInventory().setStack(draggedSourceIdx, draggedStack);
                draggedStack = null;
                draggedSourceIdx = -1;
            }
            inventoryOpen = !inventoryOpen;
        }
    }
    @Override public void keyReleased(KeyEvent e) { player.handleKeyRelease(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}
}

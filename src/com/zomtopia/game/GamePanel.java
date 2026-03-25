package com.zomtopia.game;

import com.zomtopia.game.entity.ItemEntity;
import com.zomtopia.game.entity.Player;
import com.zomtopia.game.world.Tile;
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
    private boolean deathMenuOpen = false;
    private int draggedSourceIdx = -1;
    private ItemStack draggedStack = null;
    private boolean draggingFromHotbar = false;
    private int draggedHotbarIdx = -1;

    // Input state
    private int mouseScreenX, mouseScreenY;
    private boolean leftHeld, rightHeld;
    private long lastPlacementTime = 0;
    private static final long PLACEMENT_COOLDOWN = 150;
    private static final int T = World.TILE_SIZE;
    private static final double BREAK_RANGE_TILES = 3.0;
    private static final double PLACE_RANGE_TILES = 4.0;

    private double distanceFromPlayerRectToTileCenter(int tx, int ty) {
        // Better "reach" estimation than center-to-center distance:
        // measure from the nearest point on the player's rect to the tile center.
        int h = player.crouching ? 30 : 60;
        double rectLeft = player.x;
        double rectRight = player.x + Player.W;
        double rectTop = player.y;
        double rectBottom = player.y + h;

        double tileCX = tx * T + T / 2.0;
        double tileCY = ty * T + T / 2.0;

        double closestX = Math.max(rectLeft, Math.min(tileCX, rectRight));
        double closestY = Math.max(rectTop, Math.min(tileCY, rectBottom));

        double dx = tileCX - closestX;
        double dy = tileCY - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

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
            if (!escapeMenuOpen && !deathMenuOpen) {
                player.update(world);
                camera.follow(player.x + Player.W / 2.0, player.y + Player.H / 2.0);
                updateItems();
                handleBlockInteraction();
                
                // Open death menu when health reaches zero.
                if (!deathMenuOpen && player.healthHalf <= 0) {
                    openDeathMenu();
                }
                
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

    private void openDeathMenu() {
        deathMenuOpen = true;
        escapeMenuOpen = false;
        inventoryOpen = false;
        leftHeld = false;
        rightHeld = false;
        draggedStack = null;
        draggedSourceIdx = -1;
        resetBreak();
        
        if (player != null) {
            player.left = player.right = player.jump = player.sprinting = player.crouchInput = false;
        }
    }

    private void respawnPlayer() {
        // Reset player health/state
        player.resetForRespawn();

        // Respawn above surface at world middle (same logic as constructor).
        int mx = World.WIDTH / 2;
        player.x = mx * World.TILE_SIZE;
        for (int y = 0; y < World.HEIGHT; y++) {
            if (world.getFg(mx, y) != Tile.AIR) {
                player.y = (y - 2) * World.TILE_SIZE;
                break;
            }
        }

        camera.follow(player.x + Player.W / 2.0, player.y + Player.H / 2.0);
        
        deathMenuOpen = false;
        repaint();
    }

    private void updateItems() {
        // 1) Update positions
        for (ItemEntity item : droppedItems) {
            item.update(world);
        }

        // 2) Merge nearby items (same tile + same layer) into one stack.
        // Since ItemEntity doesn't have automatic stacking, we merge if centers are close.
        final double mergeRadius = ItemEntity.SIZE * 1.2;
        java.util.List<ItemEntity> merged = new java.util.ArrayList<>();
        for (ItemEntity item : droppedItems) {
            boolean didMerge = false;
            double itemCX = item.x + ItemEntity.SIZE / 2.0;
            double itemCY = item.y + ItemEntity.SIZE / 2.0;

            for (ItemEntity kept : merged) {
                if (kept.tile == item.tile && kept.isBackground == item.isBackground) {
                    double keptCX = kept.x + ItemEntity.SIZE / 2.0;
                    double keptCY = kept.y + ItemEntity.SIZE / 2.0;
                    double dx = keptCX - itemCX;
                    double dy = keptCY - itemCY;
                    if ((dx * dx + dy * dy) <= (mergeRadius * mergeRadius)) {
                        kept.amount += item.amount;
                        didMerge = true;
                        break;
                    }
                }
            }
            if (!didMerge) {
                merged.add(item);
            }
        }

        droppedItems.clear();
        droppedItems.addAll(merged);

        // 3) Pick up by player (amount-aware)
        for (ItemEntity item : new java.util.ArrayList<>(droppedItems)) {
            Rectangle itemRect = new Rectangle((int) item.x, (int) item.y, ItemEntity.SIZE, ItemEntity.SIZE);
            Rectangle playerRect = new Rectangle((int) player.x, (int) player.y, Player.W, player.crouching ? 30 : Player.H);

            if (itemRect.intersects(playerRect)) {
                if (player.getInventory().addItem(item.tile, item.amount, item.isBackground)) {
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

        double reachDistPx = distanceFromPlayerRectToTileCenter(targetTX, targetTY);

        // Interaction ranges: break=3 tiles, place=4 tiles (matches HUD)
        isInRangeBreak = (reachDistPx <= BREAK_RANGE_TILES * T);
        isInRangePlace = (reachDistPx <= PLACE_RANGE_TILES * T);

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
        if (deathMenuOpen) {
            drawDeathMenu(g2);
        } else if (escapeMenuOpen) {
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

    private void drawDeathMenu(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        // Dim background
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, w, h);

        int menuW = 300, menuH = 320;
        int menuX = (w - menuW) / 2;
        int menuY = (h - menuH) / 2;

        g2.setColor(new Color(60, 20, 20, 240));
        g2.fillRoundRect(menuX, menuY, menuW, menuH, 20, 20);
        g2.setColor(new Color(180, 100, 100, 120));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(menuX, menuY, menuW, menuH, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        String title = "ÖLDÜN";
        g2.drawString(title, menuX + (menuW - g2.getFontMetrics().stringWidth(title)) / 2, menuY + 45);

        String[] buttons = {"YENİDEN DOĞ", "KAYDET", "KAYDET VE ÇIK"};
        for (int i = 0; i < buttons.length; i++) {
            int bx = menuX + 30;
            int by = menuY + 90 + i * 70;
            int bw = menuW - 60;
            int bh = 45;

            g2.setColor(new Color(50, 30, 35));
            g2.fillRoundRect(bx, by, bw, bh, 10, 10);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawRoundRect(bx, by, bw, bh, 10, 10);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            int tw = g2.getFontMetrics().stringWidth(buttons[i]);
            g2.drawString(buttons[i], bx + (bw - tw) / 2, by + 28);
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

            // Show amount for stack-like behavior
            if (item.amount > 1) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString(String.valueOf(item.amount), sx + offset + 2, sy + offset + renderSize - 3);
            }
        }
    }

    private int getDropX() {
        // Place just outside player rect, in the direction the player is facing.
        return (int) (player.x + (player.facingLeft ? -ItemEntity.SIZE - 4 : Player.W + 4));
    }

    private int getDropY() {
        int h = player.crouching ? 30 : Player.H;
        return (int) (player.y + h / 2.0 - ItemEntity.SIZE / 2.0);
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
        // Health: 10 hearts (tracked as 20 half-hearts)
        int heartSize = 16;
        int heartPadding = 4;
        int totalHearts = Math.max(1, player.maxHealthHalf / 2);
        int fullHearts = player.healthHalf / 2;
        int hasHalf = player.healthHalf % 2;

        // Damage flash affects filled hearts only.
        float flash = Math.max(0f, Math.min(1f, player.damageFlash));
        int filledAlpha = (int) Math.min(255, 200 + flash * 55);

        int heartY = y - 38;
        for (int i = 0; i < totalHearts; i++) {
            int hx = barX + i * (heartSize + heartPadding);

            boolean isFull = i < fullHearts;
            boolean isHalf = (i == fullHearts && hasHalf == 1);

            // Base heart background.
            // Empty/half hearts should be clearly "not full" -> make them mostly transparent.
            if (isFull) {
                g2.setColor(new Color(220, 40, 40, filledAlpha));
            } else {
                g2.setColor(new Color(60, 20, 20, 25));
            }

            g2.fillRoundRect(hx, heartY, heartSize, heartSize, 4, 4);

            // Half-heart overlay: clip to left half.
            if (isHalf) {
                Shape oldClip = g2.getClip();
                g2.setClip(new Rectangle(hx, heartY, heartSize / 2, heartSize));
                g2.setColor(new Color(220, 40, 40, filledAlpha));
                g2.fillRoundRect(hx, heartY, heartSize, heartSize, 4, 4);
                g2.setClip(oldClip);
            }

            // Outline (keep it visible but subtle)
            g2.setColor(new Color(255, 255, 255, isFull ? 90 : 55));
            g2.drawRoundRect(hx, heartY, heartSize, heartSize, 4, 4);
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
                int iconSize = Math.max(14, slotSize - 24);
                int iconX = sx + (slotSize - iconSize) / 2;
                int iconY = y + 6;

                // Demo icon (same as inventory) + readable name under it.
                drawInventoryDemoIcon(g2, stack, iconX, iconY, iconSize);

                // Amount
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString(String.valueOf(stack.amount), sx + 4, y + 16);

                // Name (fit to slot)
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String name = getTileDisplayName(stack.tile);
                if (stack.isBackground) name += " (ARKA)";

                FontMetrics fm = g2.getFontMetrics();
                while (name.length() > 1 && fm.stringWidth(name) > slotSize - 8) {
                    name = name.substring(0, name.length() - 1);
                }
                g2.drawString(name, sx + 4, y + slotSize - 6);
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

    // Shared layout for the expanded inventory screen (render + hit testing).
    private static class InventoryLayout {
        int invX, invY, invW, invH;

        int gridX, gridY;
        int gridCols, gridRows;
        int slotSize, slotPadding;

        int rightX, rightW;
        int previewX, previewY, previewW, previewH;

        int eqSlotSize, eqPadding;
        int eqX, eqY;
    }

    private InventoryLayout computeInventoryLayout() {
        InventoryLayout l = new InventoryLayout();

        int screenW = getWidth();
        int screenH = getHeight();

        int margin = 20;
        l.invW = Math.max(520, Math.min(780, screenW - margin * 2));
        l.invH = Math.max(360, Math.min(520, screenH - margin * 2));
        l.invX = (screenW - l.invW) / 2;
        l.invY = (screenH - l.invH) / 2;

        // Layout tuning: left grid vs right equipment panel.
        l.rightW = Math.max(190, Math.min(260, l.invW / 3));
        int gap = 18;
        int leftX = l.invX + 30;
        l.rightX = l.invX + l.invW - l.rightW - 20;

        int leftW = l.rightX - leftX - gap;

        l.gridCols = 10;
        l.gridRows = 4;
        l.slotPadding = 6;

        int gridAvailableW = leftW;
        int maxSlotFromW = (gridAvailableW - l.slotPadding * (l.gridCols - 1)) / l.gridCols;

        // Vertically reserve space for title + right panel preview/equipment.
        int gridYTop = l.invY + 88;
        int bottomSpace = 20;
        int maxH = (l.invY + l.invH) - gridYTop - bottomSpace;
        int maxSlotFromH = (maxH - l.slotPadding * (l.gridRows - 1)) / l.gridRows;

        l.slotSize = Math.max(28, Math.min(48, Math.min(maxSlotFromW, maxSlotFromH)));

        l.gridX = leftX;
        l.gridY = gridYTop;

        // Right panel: preview box on top, 2x3 equipment grid below.
        l.previewX = l.rightX;
        l.previewY = l.invY + 88;
        l.previewW = l.rightW;

        l.eqPadding = 10;
        int eqCols = 2;
        int eqRows = 3;
        int eqGridW = l.rightW - l.eqPadding * 2;
        int eqSlotCandidate = eqGridW / eqCols - l.eqPadding;
        l.eqSlotSize = Math.max(28, Math.min(l.slotSize, Math.min(46, eqSlotCandidate)));

        int eqGridH = l.eqSlotSize * eqRows + l.eqPadding * (eqRows - 1);

        int eqYTop = l.previewY + 20; // preview starts at previewY, but we add 20 for header breathing room
        int previewBottomBudget = (l.invY + l.invH) - eqGridH - 40; // bottom margin
        l.previewH = Math.max(140, previewBottomBudget - eqYTop);

        l.eqX = l.rightX + (l.rightW - (eqCols * l.eqSlotSize + (eqCols - 1) * l.eqPadding)) / 2;
        l.eqY = l.previewY + l.previewH + 12;

        return l;
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

    private String getTileDisplayName(Tile tile) {
        if (tile == null) return "";
        switch (tile) {
            case DIRT: return "TOPRAK";
            case GRASS: return "OT";
            case ROCK: return "TAŞ";
            case WOOD: return "AĞAÇ";
            case LEAVES: return "YAPRAK";
            case BEDROCK: return "ZIRHLI";
            case RED_SHIRT: return "KIRMIZI GÖMLEK";
            case BLUE_HAT: return "MAVİ ŞAPKA";
            case WINGS: return "KANATLAR";
            case DARK_MASK: return "SİYAH MASKE";
            case JEANS: return "JEAN";
            case SNEAKERS: return "AYAKKABI";
            default: return tile.name();
        }
    }

    private void drawInventoryDemoIcon(Graphics2D g2, ItemStack stack, int x, int y, int size) {
        if (stack == null || stack.tile == Tile.AIR) return;
        Tile tile = stack.tile;

        Color base = stack.isBackground ? tile.color.darker().darker() : tile.color;
        Color outline = base.darker();

        int padding = Math.max(2, size / 10);
        int inner = size - padding * 2;

        // Background tiles (walkthrough) -> more faded demo icons
        int alpha = stack.isBackground ? 140 : 220;
        base = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

        g2.setStroke(new BasicStroke(Math.max(1f, size / 14f)));

        if (tile.category == Tile.Category.BLOCK) {
            // Simple isometric-ish cube
            g2.setColor(base);
            g2.fillRect(x + padding, y + padding + inner/6, inner, inner - inner/6);
            g2.setColor(base.darker());
            g2.drawRect(x + padding, y + padding + inner/6, inner, inner - inner/6);
            g2.setColor(base);
            g2.fillRoundRect(x + padding, y + padding, inner, inner/3, 4, 4);
            g2.setColor(outline);
            g2.drawRoundRect(x + padding, y + padding, inner, inner/3, 4, 4);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, size / 6)));
            g2.drawString(String.valueOf(getTileDisplayName(tile).charAt(0)), x + padding + inner/3, y + padding + inner/2);
            return;
        }

        // Wearables: simple category-specific glyphs
        g2.setColor(base);
        g2.setPaintMode();
        switch (tile.category) {
            case HAT:
                // brim
                g2.drawLine(x + padding, y + padding + inner/3, x + size - padding, y + padding + inner/3);
                g2.setColor(base.brighter());
                g2.fillRoundRect(x + padding, y + padding, inner, inner/4, 3, 3);
                break;
            case MASK:
                g2.fillOval(x + padding, y + padding, inner, inner);
                g2.setColor(Color.BLACK);
                int eyeR = Math.max(2, inner / 10);
                g2.fillOval(x + padding + inner/4, y + padding + inner/3, eyeR, eyeR);
                g2.fillOval(x + padding + 3*inner/4 - eyeR, y + padding + inner/3, eyeR, eyeR);
                break;
            case SHIRT:
                g2.fillRect(x + padding + inner/3, y + padding, inner/3, inner);
                g2.fillRoundRect(x + padding, y + padding + inner/2, inner, inner/2, 6, 6);
                break;
            case PANTS:
                g2.fillRoundRect(x + padding + inner/4, y + padding + inner/4, inner/2, inner/2 + inner/4, 6, 6);
                // belt
                g2.fillRect(x + padding + inner/4, y + padding + inner/4 - inner/10, inner/2, Math.max(2, inner/10));
                break;
            case SHOES:
                g2.fillRoundRect(x + padding, y + padding + inner/2, inner/2, inner/3, 4, 4);
                g2.fillRoundRect(x + padding + inner/2 - inner/8, y + padding + inner/2, inner/2, inner/3, 4, 4);
                break;
            case BACK:
                int wingW = inner/2;
                g2.setColor(base.brighter());
                g2.fillPolygon(
                    new int[]{x + padding, x + padding + wingW, x + padding + wingW/2},
                    new int[]{y + padding + inner/2, y + padding + padding, y + padding + inner/2},
                    3
                );
                g2.fillPolygon(
                    new int[]{x + size - padding, x + size - padding - wingW, x + size - padding - wingW/2},
                    new int[]{y + padding + inner/2, y + padding + padding, y + padding + inner/2},
                    3
                );
                break;
            default:
                // Fallback
                g2.drawRect(x + padding, y + padding, inner, inner);
                break;
        }

        g2.setColor(outline);
        g2.drawRoundRect(x + padding, y + padding, inner, inner, 6, 6);
    }

    private void drawExpandedInventory(Graphics2D g2) {
        InventoryLayout l = computeInventoryLayout();
        
        // Background
        g2.setColor(new Color(30, 30, 30, 248));
        g2.fillRoundRect(l.invX, l.invY, l.invW, l.invH, 20, 20);
        g2.setColor(new Color(150, 150, 200, 150));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(l.invX, l.invY, l.invW, l.invH, 20, 20);
        
        Inventory inv = player.getInventory();
        
        // --- Left Section: Main Slots ---
        int gridX = l.gridX;
        int gridY = l.gridY;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("ENVANTER", gridX, l.invY + 60);
        
        for (int i = 0; i < 40; i++) {
            int row = i / l.gridCols;
            int col = i % l.gridCols;
            int sx = gridX + col * (l.slotSize + l.slotPadding);
            int sy = gridY + row * (l.slotSize + l.slotPadding);
            
            ItemStack stack = inv.getSlots()[i];
            
            g2.setColor(new Color(40, 40, 45, 230));
            g2.fillRoundRect(sx, sy, l.slotSize, l.slotSize, 10, 10);
            g2.setColor(new Color(255, 255, 255, 20));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(sx, sy, l.slotSize, l.slotSize, 10, 10);
            
            if (stack != null && stack.tile != Tile.AIR) {
                int iconSize = Math.max(16, l.slotSize - 30);
                int iconX = sx + (l.slotSize - iconSize) / 2;
                int iconY = sy + 6;

                drawInventoryDemoIcon(g2, stack, iconX, iconY, iconSize);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString(String.valueOf(stack.amount), sx + 6, sy + 14);

                // Name under icon (inside the slot).
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String name = getTileDisplayName(stack.tile);
                if (stack.isBackground) name += " (ARKA)";
                // Fit: simple truncation based on slot width.
                int maxWidth = l.slotSize - 10;
                FontMetrics fm = g2.getFontMetrics();
                while (name.length() > 1 && fm.stringWidth(name) > maxWidth) {
                    name = name.substring(0, name.length() - 1);
                }
                g2.drawString(name, sx + 5, sy + l.slotSize - 6);
            }
        }
        
        // --- Right Section: Character Preview Box ---
        int previewX = l.previewX;
        int previewY = l.previewY;
        int previewW = l.previewW;
        int previewH = l.previewH;

        g2.setColor(new Color(45, 45, 45));
        g2.fillRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        g2.setColor(new Color(80, 80, 100));
        g2.drawRoundRect(previewX, previewY, previewW, previewH, 15, 15);
        
        // Draw Large Stickman in Preview
        drawStickmanPreview(g2, previewX + previewW / 2, previewY + previewH / 2 - 10);
        
        // --- Equipment Slots (2 columns x 3 rows) ---
        String[] eqLabels = {"BAŞLIK", "MASKE", "GÖVDE", "BACAK", "AYAK", "SIRT"};
        
        for (int i = 0; i < 6; i++) {
            int row = i / 2;
            int col = i % 2;
            int sx = l.eqX + col * (l.eqSlotSize + l.eqPadding);
            int sy = l.eqY + row * (l.eqSlotSize + l.eqPadding);
            ItemStack stack = inv.getEquipment()[i];
            
            g2.setColor(new Color(60, 60, 70, 220));
            g2.fillRoundRect(sx, sy, l.eqSlotSize, l.eqSlotSize, 12, 12);
            g2.setColor(new Color(180, 180, 220, 120));
            g2.drawRoundRect(sx, sy, l.eqSlotSize, l.eqSlotSize, 12, 12);
            
            g2.setColor(new Color(200, 200, 200, 150));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            // Slot role label (when empty) lives near top-left.
            if (stack == null || stack.tile == Tile.AIR) {
                g2.drawString(eqLabels[i], sx + 6, sy + 14);
            }
            
            if (stack != null && stack.tile != Tile.AIR) {
                int iconSize = Math.max(14, l.eqSlotSize - 30);
                int iconX = sx + (l.eqSlotSize - iconSize) / 2;
                int iconY = sy + 6;

                drawInventoryDemoIcon(g2, stack, iconX, iconY, iconSize);

                // Name under icon inside the slot.
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String name = getTileDisplayName(stack.tile);
                int maxWidth = l.eqSlotSize - 10;
                FontMetrics fm = g2.getFontMetrics();
                while (name.length() > 1 && fm.stringWidth(name) > maxWidth) {
                    name = name.substring(0, name.length() - 1);
                }
                g2.drawString(name, sx + 5, sy + l.eqSlotSize - 6);
            }
        }
    }

    // ──────────── INPUT ────────────
    private int getSlotAt(int mx, int my) {
        if (!inventoryOpen) return -1;

        InventoryLayout l = computeInventoryLayout();

        // Main inventory slots
        for (int i = 0; i < 40; i++) {
            int row = i / l.gridCols;
            int col = i % l.gridCols;
            int sx = l.gridX + col * (l.slotSize + l.slotPadding);
            int sy = l.gridY + row * (l.slotSize + l.slotPadding);
            if (mx >= sx && mx <= sx + l.slotSize && my >= sy && my <= sy + l.slotSize) return i;
        }

        // Equipment slots (2 columns x 3 rows)
        for (int i = 0; i < 6; i++) {
            int row = i / 2;
            int col = i % 2;
            int sx = l.eqX + col * (l.eqSlotSize + l.eqPadding);
            int sy = l.eqY + row * (l.eqSlotSize + l.eqPadding);
            if (mx >= sx && mx <= sx + l.eqSlotSize && my >= sy && my <= sy + l.eqSlotSize) return 100 + i;
        }

        return -1;
    }

    private int getHotbarSlotAt(int mx, int my) {
        int slotSize = 44;
        int padding = 6;
        int hotbarSize = 10;
        int totalW = hotbarSize * (slotSize + padding) - padding;
        int sx0 = (getWidth() - totalW) / 2;
        int y = getHeight() - slotSize - 16;

        for (int i = 0; i < hotbarSize; i++) {
            int sx = sx0 + i * (slotSize + padding);
            if (mx >= sx && mx <= sx + slotSize && my >= y && my <= y + slotSize) return i;
        }
        return -1;
    }

    @Override public void mousePressed(MouseEvent e) {
        if (deathMenuOpen) {
            int mx = e.getX(), my = e.getY();
            int w = 800, h = 600;
            int menuW = 300, menuH = 320;
            int menuX = (w - menuW) / 2;
            int menuY = (h - menuH) / 2;

            for (int i = 0; i < 3; i++) {
                int bx = menuX + 30;
                int by = menuY + 90 + i * 70;
                int bw = menuW - 60;
                int bh = 45;
                if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                    if (i == 0) {
                        respawnPlayer();
                    } else if (i == 1) {
                        SaveManager.saveGame(world, player);
                    } else if (i == 2) {
                        SaveManager.saveGame(world, player);
                        System.exit(0);
                    }
                    repaint();
                    return;
                }
            }
            return;
        }

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

        // Drag/drop from hotbar (outside expanded inventory):
        // - Click + drag an item from hotbar
        // - Drop outside the hotbar => spawn item in the world
        // - Drop onto another hotbar slot => swap
        if (SwingUtilities.isLeftMouseButton(e) && !inventoryOpen && !deathMenuOpen) {
            int hb = getHotbarSlotAt(e.getX(), e.getY());
            if (hb != -1) {
                Inventory inv = player.getInventory();
                ItemStack stack = inv.getSlots()[hb];
                if (stack != null && stack.tile != Tile.AIR) {
                    // Drop 1 item from the stack (not the whole stack).
                    draggedStack = new ItemStack(stack.tile, 1, stack.isBackground);
                    stack.amount -= 1;
                    if (stack.amount <= 0) inv.getSlots()[hb] = null;
                    draggedHotbarIdx = hb;
                    draggingFromHotbar = true;
                    // Do not start breaking/placing while dragging from hotbar.
                    return;
                }
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
        double reachDistPx = distanceFromPlayerRectToTileCenter(tx, ty);

        if (reachDistPx <= PLACE_RANGE_TILES * World.TILE_SIZE) { 
            com.zomtopia.game.inventory.ItemStack selStack = player.getInventory().getStack(selectedSlot);
            if (selStack != null && selStack.amount > 0) {
                // Equippable items should not be placed into the world as blocks.
                if (selStack.tile.category != Tile.Category.BLOCK) return;

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

    private void returnDraggedStackToSource() {
        if (draggedStack == null || draggedSourceIdx < 0) return;
        Inventory inv = player.getInventory();
        if (draggedSourceIdx < 100) {
            inv.getSlots()[draggedSourceIdx] = draggedStack;
        } else {
            inv.getEquipment()[draggedSourceIdx - 100] = draggedStack;
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
            Inventory inv = player.getInventory();
            if (inventoryOpen) {
                int targetIndex = getSlotAt(e.getX(), e.getY());
                
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
                    // Drop item (outside expanded inventory area)
                    if (draggedStack.tile.category != Tile.Category.BLOCK) {
                        returnDraggedStackToSource();
                    } else {
                droppedItems.add(new ItemEntity(getDropX(), getDropY(), draggedStack.tile, draggedStack.isBackground, draggedStack.amount));
                    }
                }
            } else if (draggingFromHotbar) {
                // Hotbar drag released outside expanded inventory.
                int hbTarget = getHotbarSlotAt(e.getX(), e.getY());
                if (hbTarget != -1) {
                    // Merge into compatible hotbar slot; otherwise keep stacks unchanged.
                    ItemStack target = inv.getSlots()[hbTarget];
                    if (target == null) {
                        inv.getSlots()[hbTarget] = draggedStack;
                    } else if (target.tile == draggedStack.tile && target.isBackground == draggedStack.isBackground) {
                        target.amount += draggedStack.amount;
                    } else {
                        // Return the 1 item back to source stack.
                        ItemStack source = inv.getSlots()[draggedHotbarIdx];
                        if (source == null) {
                            inv.getSlots()[draggedHotbarIdx] = draggedStack;
                        } else {
                            source.amount += draggedStack.amount;
                        }
                    }
                } else {
                    // Drop to world.
                    if (draggedStack.tile.category != Tile.Category.BLOCK) {
                        // Equippables cannot be dropped.
                        ItemStack source = inv.getSlots()[draggedHotbarIdx];
                        if (source == null) {
                            inv.getSlots()[draggedHotbarIdx] = draggedStack;
                        } else {
                            source.amount += draggedStack.amount;
                        }
                    } else {
                        droppedItems.add(new ItemEntity(getDropX(), getDropY(), draggedStack.tile, draggedStack.isBackground, draggedStack.amount));
                    }
                }
            } else {
                // Fallback: drop to world (shouldn't happen often).
                droppedItems.add(new ItemEntity(getDropX(), getDropY(), draggedStack.tile, draggedStack.isBackground, draggedStack.amount));
            }

            draggedStack = null;
            draggedSourceIdx = -1;
            draggingFromHotbar = false;
            draggedHotbarIdx = -1;
        }

        if (SwingUtilities.isLeftMouseButton(e))  { leftHeld  = false; resetBreak(); }
        if (SwingUtilities.isRightMouseButton(e)) { /* No longer needed for placement */ }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (deathMenuOpen) return;
            escapeMenuOpen = !escapeMenuOpen;
            if (escapeMenuOpen) inventoryOpen = false;
            repaint();
            return;
        }
        if (escapeMenuOpen) return;
        if (deathMenuOpen) return;

        player.handleKeyPress(e.getKeyCode());
        // Hotbar selection: only map numeric keys 1-5 to slots 0-4.
        // Otherwise, non-numeric keys (e.g. A/D/W) can accidentally fall into range
        // due to keyCode ordering and switch the selected item.
        if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_5) {
            selectedSlot = e.getKeyCode() - KeyEvent.VK_1;
        }
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

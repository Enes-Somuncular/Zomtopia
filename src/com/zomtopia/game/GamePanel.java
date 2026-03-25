package com.zomtopia.game;

import com.zomtopia.game.entity.ItemEntity;
import com.zomtopia.game.entity.Player;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.world.World;
import com.zomtopia.game.world.WorldGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private final World world;
    private final Player player;
    private final Camera camera;
    private Timer gameTimer;

    // Hotbar selection
    private int selectedSlot = 0;

    // Block breaking state
    private int breakX = -1, breakY = -1;
    private float breakProgress = 0;        // 0.0 – 1.0
    private static final float BREAK_SPEED = 0.065f;  // per tick

    // Items and Inventory UI
    private final List<ItemEntity> droppedItems = new CopyOnWriteArrayList<>();
    private boolean inventoryOpen = false;
    private com.zomtopia.game.inventory.ItemStack draggedStack = null;
    private int draggedSourceIdx = -1;

    // Mouse state
    private int mouseScreenX, mouseScreenY;
    private boolean leftHeld, rightHeld;

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
            handleBlockInteraction();
            player.update(world);
            updateItems();
            camera.follow(player.x + Player.W / 2.0, player.y + Player.H / 2.0);
            repaint();
        });
        gameTimer.start();
    }

    private void updateItems() {
        double px = player.x + Player.W / 2.0;
        double py = player.y + Player.H / 2.0;

        for (ItemEntity item : droppedItems) {
            item.update(world);

            double dx = px - (item.x + ItemEntity.SIZE / 2.0);
            double dy = py - (item.y + ItemEntity.SIZE / 2.0);
            double distSq = dx * dx + dy * dy;

            if (distSq < Math.pow(World.TILE_SIZE * 0.9, 2)) {
                if (player.getInventory().addItem(item.tile, 1)) {
                    droppedItems.remove(item);
                }
            }
        }
    }

    /** Called every tick: handle held mouse buttons */
    private void handleBlockInteraction() {
        int tx = camera.toTileX(mouseScreenX);
        int ty = camera.toTileY(mouseScreenY);

        if (leftHeld) {
            // Distance check (3 blocks)
            double px = player.x + Player.W / 2.0;
            double py = player.y + Player.H / 2.0;
            double txC = tx * World.TILE_SIZE + World.TILE_SIZE / 2.0;
            double tyC = ty * World.TILE_SIZE + World.TILE_SIZE / 2.0;
            double distBreak = Math.sqrt(Math.pow(px - txC, 2) + Math.pow(py - tyC, 2));

            if (distBreak > 3.5 * World.TILE_SIZE) { // Allow up to 3 blocks + small buffer
                resetBreak();
                return;
            }

            Tile fgTile = world.getFg(tx, ty);
            Tile bgTile = world.getBg(tx, ty);

            boolean hasFg = fgTile != Tile.AIR && fgTile != Tile.BEDROCK;
            boolean hasBg = bgTile != Tile.AIR && bgTile != Tile.BEDROCK;

            if (!hasFg && !hasBg) {
                resetBreak();
                return;
            }

            // Reset if moved to different tile
            if (tx != breakX || ty != breakY) {
                breakX = tx; breakY = ty; breakProgress = 0;
            }

            breakProgress += BREAK_SPEED;

            if (breakProgress >= 1.0f) {
                // Break FG first, then BG on next hold
                Tile brokenTile;
                if (hasFg) {
                    brokenTile = fgTile;
                    world.setFg(tx, ty, Tile.AIR);
                } else {
                    brokenTile = bgTile;
                    world.setBg(tx, ty, Tile.AIR);
                }
                // Spawn dropped item instead of direct add
                droppedItems.add(new ItemEntity(tx * World.TILE_SIZE + 8, ty * World.TILE_SIZE + 8, brokenTile));
                resetBreak();
            }
        } else {
            // Not holding – slowly decay break progress
            if (breakProgress > 0) breakProgress = Math.max(0, breakProgress - 0.02f);
            if (breakProgress == 0) resetBreak();
        }

        if (rightHeld) {
            // Distance check (4 blocks)
            double px = player.x + Player.W / 2.0;
            double py = player.y + Player.H / 2.0;
            double txC = tx * World.TILE_SIZE + World.TILE_SIZE / 2.0;
            double tyC = ty * World.TILE_SIZE + World.TILE_SIZE / 2.0;
            double distPlace = Math.sqrt(Math.pow(px - txC, 2) + Math.pow(py - tyC, 2));

            if (distPlace <= 4.5 * World.TILE_SIZE) { 
                com.zomtopia.game.inventory.ItemStack selStack = player.getInventory().getStack(selectedSlot);
                if (selStack != null && selStack.amount > 0) {
                    Rectangle playerRect = new Rectangle((int) player.x, (int) player.y, Player.W, Player.H);
                    Rectangle tileRect   = new Rectangle(tx * World.TILE_SIZE, ty * World.TILE_SIZE,
                                                          World.TILE_SIZE, World.TILE_SIZE);
                    if (!playerRect.intersects(tileRect)) {
                        if (world.getFg(tx, ty) == Tile.AIR) {
                            world.setFg(tx, ty, selStack.tile);
                            player.getInventory().removeItem(selStack.tile);
                        } else if (world.getBg(tx, ty) == Tile.AIR) {
                            world.setBg(tx, ty, selStack.tile);
                            player.getInventory().removeItem(selStack.tile);
                        }
                    }
                }
            }
        }
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

        // --- Hover outline (Only if in range) ---
        int hoverTX = camera.toTileX(mouseScreenX);
        int hoverTY = camera.toTileY(mouseScreenY);
        double px = player.x + Player.W / 2.0;
        double py = player.y + Player.H / 2.0;
        double txC = hoverTX * T + T / 2.0;
        double tyC = hoverTY * T + T / 2.0;
        double distHover = Math.sqrt(Math.pow(px - txC, 2) + Math.pow(py - tyC, 2));

        if (distHover <= 4.5 * T) {
            int hsx = camera.toScreenX(hoverTX * T);
            int hsy = camera.toScreenY(hoverTY * T);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(hsx, hsy, T, T);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(hsx, hsy, T, T);
            g2.setStroke(new BasicStroke(1f));
        }
        // --- Player sprite ---
        int spx = camera.toScreenX((int) player.x);
        int spy = camera.toScreenY((int) player.y);
        g2.setColor(new Color(60, 120, 200));
        g2.fillRoundRect(spx + 4, spy, Player.W - 8, Player.H, 6, 6);
        g2.setColor(new Color(230, 185, 140));
        g2.fillOval(spx + 4, spy - 16, 16, 16);
        g2.setColor(new Color(50, 50, 80));
        g2.fillOval(spx + 7, spy - 12, 3, 3);
        g2.fillOval(spx + 13, spy - 12, 3, 3);

        drawDroppedItems(g2);
        drawHUD(g2);
        if (inventoryOpen) {
            drawExpandedInventory(g2);
            drawDraggedItem(g2);
        }
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
            g2.setColor(item.tile.color.darker());
            g2.fillRoundRect(sx, sy, ItemEntity.SIZE, ItemEntity.SIZE, 4, 4);
            g2.setColor(item.tile.color);
            g2.fillRoundRect(sx + 2, sy + 2, ItemEntity.SIZE - 4, ItemEntity.SIZE - 4, 2, 2);
        }
    }

    private void drawExpandedInventory(Graphics2D g2) {
        int slotSize = 44;
        int padding  = 6;
        int cols     = 10;
        int rows     = 3;
        int totalW   = cols * (slotSize + padding) - padding;
        int totalH   = rows * (slotSize + padding) - padding;
        int x0       = (getWidth() - totalW) / 2;
        int y0       = (getHeight() - totalH) / 2 - 50;

        // Dim background
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("ENVANTER", x0, y0 - 20);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slotIdx = 10 + (r * cols + c); // Starts after hotbar (0-9)
                int sx = x0 + c * (slotSize + padding);
                int sy = y0 + r * (slotSize + padding);
                
                com.zomtopia.game.inventory.ItemStack stack = player.getInventory().getStack(slotIdx);
                
                g2.setColor(new Color(40, 40, 40, 220));
                g2.fillRoundRect(sx, sy, slotSize, slotSize, 8, 8);
                g2.setColor(Color.GRAY);
                g2.drawRoundRect(sx, sy, slotSize, slotSize, 8, 8);
                
                if (stack != null && stack.tile != Tile.AIR) {
                    g2.setColor(stack.tile.color);
                    g2.fillRoundRect(sx + 8, sy + 8, slotSize - 16, slotSize - 16, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 11));
                    g2.drawString(String.valueOf(stack.amount), sx + 4, sy + 14);
                }
            }
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

            g2.setColor(sel ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 140));
            g2.fillRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setColor(sel ? Color.YELLOW : Color.GRAY);
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.drawRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            if (stack != null && stack.tile != Tile.AIR) {
                g2.setColor(stack.tile.color);
                g2.fillRoundRect(sx + 8, y + 8, slotSize - 16, slotSize - 16, 4, 4);
                
                // Miktar
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString(String.valueOf(stack.amount), sx + 4, y + 14);
                
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                g2.drawString(stack.tile.name().substring(0, Math.min(3, stack.tile.name().length())), sx + 4, y + slotSize - 4);
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

    // ──────────── INPUT ────────────
    private int getSlotAt(int mx, int my) {
        int slotSize = 44;
        int padding = 6;
        
        // Check hotbar
        int hotbarSize = 10;
        int totalW_h = hotbarSize * (slotSize + padding) - padding;
        int sx0_h = (getWidth() - totalW_h) / 2;
        int y_h = getHeight() - slotSize - 16;
        
        if (my >= y_h && my <= y_h + slotSize) {
            for (int i = 0; i < hotbarSize; i++) {
                int sx = sx0_h + i * (slotSize + padding);
                if (mx >= sx && mx <= sx + slotSize) return i;
            }
        }
        
        // Check expanded inventory
        if (inventoryOpen) {
            int cols = 10;
            int rows = 3;
            int totalW_e = cols * (slotSize + padding) - padding;
            int totalH_e = rows * (slotSize + padding) - padding;
            int x0_e = (getWidth() - totalW_e) / 2;
            int y0_e = (getHeight() - totalH_e) / 2 - 50;
            
            if (mx >= x0_e && mx <= x0_e + totalW_e && my >= y0_e && my <= y0_e + totalH_e) {
                int col = (mx - x0_e) / (slotSize + padding);
                int row = (my - y0_e) / (slotSize + padding);
                if (col >= 0 && col < cols && row >= 0 && row < rows) {
                    return 10 + (row * cols + col);
                }
            }
        }
        
        return -1;
    }

    @Override public void mousePressed(MouseEvent e) {
        if (inventoryOpen) {
            int slot = getSlotAt(e.getX(), e.getY());
            if (slot != -1) {
                draggedStack = player.getInventory().getStack(slot);
                draggedSourceIdx = slot;
                player.getInventory().setStack(slot, null);
                return;
            }
        }
        
        if (SwingUtilities.isLeftMouseButton(e))  leftHeld  = true;
        if (SwingUtilities.isRightMouseButton(e)) rightHeld = true;
        requestFocusInWindow();
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (draggedStack != null) {
            int targetSlot = getSlotAt(e.getX(), e.getY());
            if (targetSlot != -1) {
                // Swap
                com.zomtopia.game.inventory.ItemStack targetStack = player.getInventory().getStack(targetSlot);
                player.getInventory().setStack(targetSlot, draggedStack);
                player.getInventory().setStack(draggedSourceIdx, targetStack);
            } else {
                // Return to source
                player.getInventory().setStack(draggedSourceIdx, draggedStack);
            }
            draggedStack = null;
            draggedSourceIdx = -1;
        }

        if (SwingUtilities.isLeftMouseButton(e))  { leftHeld  = false; resetBreak(); }
        if (SwingUtilities.isRightMouseButton(e)) rightHeld = false;
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override public void keyPressed(KeyEvent e) {
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
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameTimer.stop();
            JFrame f = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (f != null) f.dispose();
        }
    }
    @Override public void keyReleased(KeyEvent e) { player.handleKeyRelease(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}
}

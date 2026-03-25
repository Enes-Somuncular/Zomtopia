package com.zomtopia.game;

import com.zomtopia.game.entity.Player;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.world.World;
import com.zomtopia.game.world.WorldGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private final World world;
    private final Player player;
    private final Camera camera;
    private Timer gameTimer;

    // Toolbar – which tile the player is holding
    private final Tile[] hotbar = {Tile.DIRT, Tile.GRASS, Tile.ROCK, Tile.WOOD, Tile.LEAVES};
    private int selectedSlot = 0;

    // Block break helper
    private int breakX = -1, breakY = -1;
    private int breakTick = 0;
    private static final int BREAK_TICKS = 12;

    // Mouse screen position
    private int mouseScreenX, mouseScreenY;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(100, 180, 240)); // sky color

        world = new World();
        new WorldGenerator(System.currentTimeMillis()).generate(world);

        player = new Player();
        // Spawn player above the ground around the middle
        player.x = (World.WIDTH / 2) * World.TILE_SIZE;
        for (int y = 0; y < World.HEIGHT; y++) {
            if (world.getTile(World.WIDTH / 2, y) != Tile.AIR) {
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
            selectedSlot = (selectedSlot + (e.getWheelRotation() > 0 ? 1 : -1) + hotbar.length) % hotbar.length;
            repaint();
        });
        setFocusable(true);

        startGameLoop();
    }

    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            player.update(world);
            camera.follow(player.x + Player.W / 2.0, player.y + Player.H / 2.0);
            repaint();
        });
        gameTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int T = World.TILE_SIZE;

        // --- Tile range to draw (only visible tiles) ---
        int startX = Math.max(0, (int)(camera.x / T) - 1);
        int startY = Math.max(0, (int)(camera.y / T) - 1);
        int endX   = Math.min(World.WIDTH  - 1, startX + getWidth()  / T + 2);
        int endY   = Math.min(World.HEIGHT - 1, startY + getHeight() / T + 2);

        // --- Draw tiles ---
        for (int tx = startX; tx <= endX; tx++) {
            for (int ty = startY; ty <= endY; ty++) {
                Tile tile = world.getTile(tx, ty);
                if (tile == Tile.AIR) continue;

                int sx = camera.toScreenX(tx * T);
                int sy = camera.toScreenY(ty * T);

                // Main tile fill
                g2.setColor(tile.color);
                g2.fillRect(sx, sy, T, T);

                // Darker top edge for depth (except leaves)
                if (tile != Tile.LEAVES) {
                    g2.setColor(tile.color.darker());
                    g2.drawRect(sx, sy, T - 1, T - 1);
                }

                // Grass special top highlight
                if (tile == Tile.GRASS) {
                    g2.setColor(new Color(80, 200, 60));
                    g2.fillRect(sx, sy, T, 4);
                }

                // Breaking overlay
                if (tx == breakX && ty == breakY && breakTick > 0) {
                    int alpha = (int)(200.0 * breakTick / BREAK_TICKS);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRect(sx, sy, T, T);
                    // Crack lines
                    g2.setColor(new Color(255, 255, 255, alpha / 2));
                    int cracks = breakTick * 4 / BREAK_TICKS;
                    for (int c = 0; c < cracks; c++) {
                        g2.drawLine(sx + T/2, sy + T/2, sx + (c % 2 == 0 ? T - 5 : 5), sy + (c < 2 ? 5 : T - 5));
                    }
                }
            }
        }

        // --- Hover tile highlight ---
        int hoverTX = camera.toTileX(mouseScreenX);
        int hoverTY = camera.toTileY(mouseScreenY);
        int hsx = camera.toScreenX(hoverTX * T);
        int hsy = camera.toScreenY(hoverTY * T);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillRect(hsx, hsy, T, T);
        g2.setColor(Color.WHITE);
        g2.drawRect(hsx, hsy, T, T);

        // --- Player ---
        int px = camera.toScreenX((int) player.x);
        int py = camera.toScreenY((int) player.y);
        // Body
        g2.setColor(new Color(60, 120, 200));
        g2.fillRoundRect(px + 4, py, Player.W - 8, Player.H, 6, 6);
        // Head
        g2.setColor(new Color(230, 185, 140));
        g2.fillOval(px + 4, py - 16, 16, 16);
        // Eyes
        g2.setColor(new Color(50, 50, 80));
        g2.fillOval(px + 7, py - 12, 3, 3);
        g2.fillOval(px + 13, py - 12, 3, 3);

        drawHUD(g2);
    }

    private void drawHUD(Graphics2D g2) {
        int slotSize = 44;
        int padding  = 6;
        int total = hotbar.length * (slotSize + padding) - padding;
        int startX = (getWidth() - total) / 2;
        int y = getHeight() - slotSize - 16;

        for (int i = 0; i < hotbar.length; i++) {
            int sx = startX + i * (slotSize + padding);
            boolean sel = (i == selectedSlot);

            // Slot background
            g2.setColor(sel ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 140));
            g2.fillRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setColor(sel ? Color.YELLOW : Color.GRAY);
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.drawRoundRect(sx, y, slotSize, slotSize, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Tile color swatch
            g2.setColor(hotbar[i].color);
            g2.fillRoundRect(sx + 8, y + 8, slotSize - 16, slotSize - 16, 4, 4);

            // Label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            String name = hotbar[i].name().substring(0, Math.min(3, hotbar[i].name().length()));
            g2.drawString(name, sx + 4, y + slotSize - 4);
        }

        // Controls hint
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(8, 8, 230, 70, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("WASD / ← → ↑  Hareket & Zıpla", 16, 26);
        g2.drawString("Sol Tık: Blok Kır", 16, 44);
        g2.drawString("Sağ Tık: Blok Yerleştir", 16, 62);
        g2.drawString("Scroll: Blok Seç", 16, 78);
    }

    // ============ Mouse events ============
    @Override
    public void mousePressed(MouseEvent e) {
        int tx = camera.toTileX(e.getX());
        int ty = camera.toTileY(e.getY());

        if (SwingUtilities.isLeftMouseButton(e)) {
            // Start breaking
            if (tx != breakX || ty != breakY) { breakX = tx; breakY = ty; breakTick = 0; }
            Tile t = world.getTile(tx, ty);
            if (t != Tile.AIR && t != Tile.BEDROCK) {
                breakTick++;
                if (breakTick >= BREAK_TICKS) {
                    world.setTile(tx, ty, Tile.AIR);
                    breakX = -1; breakY = -1; breakTick = 0;
                }
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            // Place block (not on player)
            Rectangle playerRect = new Rectangle((int)player.x, (int)player.y, Player.W, Player.H);
            Rectangle tileRect   = new Rectangle(tx * World.TILE_SIZE, ty * World.TILE_SIZE, World.TILE_SIZE, World.TILE_SIZE);
            if (world.getTile(tx, ty) == Tile.AIR && !playerRect.intersects(tileRect)) {
                world.setTile(tx, ty, hotbar[selectedSlot]);
            }
        }
        requestFocusInWindow();
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) { breakX = -1; breakY = -1; breakTick = 0; }
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // ============ Key events ============
    @Override
    public void keyPressed(KeyEvent e) {
        player.handleKeyPress(e.getKeyCode());
        // Number keys for hotbar
        int k = e.getKeyCode() - KeyEvent.VK_1;
        if (k >= 0 && k < hotbar.length) selectedSlot = k;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameTimer.stop();
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (frame != null) frame.dispose();
        }
    }
    @Override public void keyReleased(KeyEvent e) { player.handleKeyRelease(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}
}

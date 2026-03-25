package com.zomtopia.game.entity;

import com.zomtopia.game.world.World;
import com.zomtopia.game.world.Tile;
import com.zomtopia.game.inventory.Inventory;
import java.awt.event.KeyEvent;

public class Player {
    // Position (top-left pixel of player rect)
    public double x, y;
    public double vx, vy;

    private final Inventory inventory;

    public Player() {
        this.inventory = new Inventory();
    }

    public Inventory getInventory() { return inventory; }

    public static final int W = 24;
    public static final int H = 60;

    private static final double GRAVITY    = 0.5;
    private static final double MAX_VY     = 14;
    private static final double MOVE_SPEED = 2.2;
    private static final double SPRINT_SPEED = 3.8;
    private static final double JUMP_VY    = -11.0;
    private static final double FRICTION   = 0.75;

    public int health = 10;
    public int maxHealth = 10;
    public float stamina = 100f;
    public float maxStamina = 100f;

    public boolean onGround = false;

    // Input flags (set by GamePanel)
    public boolean left, right, jump, sprinting;

    public void update(World world) {
        // Horizontal
        double currentSpeed = MOVE_SPEED;
        boolean isMoving = left || right;

        if (sprinting && isMoving && stamina > 0) {
            currentSpeed = SPRINT_SPEED;
            stamina -= 1.0f; // Consume stamina
            if (stamina < 0) {
                stamina = 0;
                sprinting = false;
            }
        } else {
            // Recharge stamina
            if (stamina < maxStamina) {
                stamina += 0.5f;
                if (stamina > maxStamina) stamina = maxStamina;
            }
        }

        if (left)  vx -= currentSpeed;
        if (right) vx += currentSpeed;
        vx *= FRICTION;
        if (Math.abs(vx) < 0.1) vx = 0;

        // Jump
        if (jump && onGround) {
            vy = JUMP_VY;
            onGround = false;
        }

        // Gravity
        vy += GRAVITY;
        if (vy > MAX_VY) vy = MAX_VY;

        // Move X with collision
        x += vx;
        if (world.isRectBlocked(x, y, W, H)) {
            x -= vx;
            vx = 0;
        }

        // Move Y with collision
        y += vy;
        if (world.isRectBlocked(x, y, W, H)) {
            if (vy > 0) onGround = true;
            y -= vy;
            vy = 0;
        } else {
            onGround = false;
        }

        // Clamp to world
        double maxX = World.WIDTH  * World.TILE_SIZE - W;
        double maxY = World.HEIGHT * World.TILE_SIZE - H;
        if (x < 0) { x = 0; vx = 0; }
        if (x > maxX) { x = maxX; vx = 0; }
        if (y < 0) { y = 0; vy = 0; }
        if (y > maxY) { y = maxY; vy = 0; onGround = true; }
    }

    public void handleKeyPress(int key) {
        if (key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_A) left  = true;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = true;
        if (key == KeyEvent.VK_UP    || key == KeyEvent.VK_W || key == KeyEvent.VK_SPACE) jump = true;
        if (key == KeyEvent.VK_SHIFT) sprinting = true;
    }

    public void handleKeyRelease(int key) {
        if (key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_A) left  = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = false;
        if (key == KeyEvent.VK_UP    || key == KeyEvent.VK_W || key == KeyEvent.VK_SPACE) jump = false;
        if (key == KeyEvent.VK_SHIFT) sprinting = false;
    }
}

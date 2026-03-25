package com.zomtopia.game.entity;

import com.zomtopia.game.world.World;
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

    /**
     * Health is tracked in half-hearts to support half-heart damage.
     * UI displays 10 full hearts (20 half-hearts) by default.
     */
    public int healthHalf = 20;
    public int maxHealthHalf = 20;
    public float stamina = 100f;
    public float maxStamina = 100f;

    public boolean onGround = false;

    // Small UI flash when damage happens.
    public float damageFlash = 0f; // 0..1

    // Fall tracking for landing damage.
    private boolean trackingFall = false;
    private double fallStartY = 0; // top-left Y when leaving the ground

    // Fall damage tuning:
    // We use "dBlocks" (vertical distance in tiles) and convert it into damage (half-hearts).
    // Desired points (hearts):
    // d=5  -> 1.0
    // d=10 -> 2.5
    // d=15 -> 6.0
    // d=20 -> 8.5
    private static final double FALL_START_BLOCKS = 5.0;

    // Input flags (set by GamePanel)
    public boolean left, right, jump, sprinting, crouchInput;
    public boolean crouching;
    public boolean facingLeft = false;
    public float punchAnim = 0f; // 0.0 to 1.0

    public void startPunch() {
        if (punchAnim <= 0) punchAnim = 0.01f;
    }

    public void update(World world) {
        boolean wasOnGround = onGround;
        double yBefore = y;

        // Horizontal
        double currentSpeed = MOVE_SPEED;
        
        // Update orientation
        if (left) facingLeft = true;
        if (right) facingLeft = false;
        
        // --- Animation Progress ---
        if (punchAnim > 0) {
            punchAnim += 0.15f; 
            if (punchAnim >= 1.0f) punchAnim = 0;
        }
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

        // Start fall tracking when we leave ground.
        if (wasOnGround && !onGround && !trackingFall) {
            trackingFall = true;
            fallStartY = yBefore;
        }

        // --- Crouching Logic ---
        int targetH = crouchInput ? 30 : 60;
        
        // If trying to stand up, check if there's space
        if (!crouchInput && crouching) {
            if (world.isRectBlocked(x, y - (60 - 30), W, 60)) {
                targetH = 30; // Force stay crouched
            }
        }
        
        int oldH = crouching ? 30 : 60;
        int newH = targetH;
        
        // Adjust y to keep feet on ground when changing height
        if (newH != oldH) {
            y += (oldH - newH);
        }
        crouching = (newH == 30);
        int h = newH;

        // Gravity
        vy += GRAVITY;
        if (vy > MAX_VY) vy = MAX_VY;

        // Move X with collision
        x += vx;
        if (world.isRectBlocked(x, y, W, h)) {
            x -= vx;
            vx = 0;
        }

        // Move Y with collision
        y += vy;
        if (world.isRectBlocked(x, y, W, h)) {
            if (vy > 0) onGround = true;
            y -= vy;
            vy = 0;
        } else {
            onGround = false;
        }

        // Apply fall damage on landing.
        if (trackingFall && !wasOnGround && onGround) {
            // fallStartY - y is the approximate vertical distance traveled.
            double fallDist = fallStartY - y;
            if (fallDist > 0) {
                int damageHalf = computeMappedFallDamageHalfHearts(fallDist);
                if (damageHalf > 0) applyDamageHalf(damageHalf);
            }
            trackingFall = false;
        }

        // Start tracking when we leave the ground without jumping too.
        // (e.g. walking off an edge: wasOnGround==true, onGround becomes false after collision checks)
        if (!trackingFall && wasOnGround && !onGround) {
            trackingFall = true;
            fallStartY = yBefore;
        }

        // Clamp to world
        double maxX = World.WIDTH  * World.TILE_SIZE - W;
        double maxY = World.HEIGHT * World.TILE_SIZE - h;
        if (x < 0) { x = 0; vx = 0; }
        if (x > maxX) { x = maxX; vx = 0; }
        if (y < 0) { y = 0; vy = 0; }
        if (y > maxY) { y = maxY; vy = 0; onGround = true; }

        if (damageFlash > 0f) {
            damageFlash = Math.max(0f, damageFlash - 0.06f);
        }
    }

    private void applyDamageHalf(int damageHalf) {
        int before = healthHalf;
        healthHalf = Math.max(0, healthHalf - damageHalf);
        if (healthHalf < before) {
            damageFlash = 1f;
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private int computeMappedFallDamageHalfHearts(double fallDistPx) {
        double tile = World.TILE_SIZE;
        int dBlocks = (int) Math.floor(fallDistPx / tile);

        if (dBlocks <= (int) FALL_START_BLOCKS) {
            // d=5 -> 1 heart => 2 half-hearts
            if (dBlocks < (int) FALL_START_BLOCKS) return 0;
        }
        if (dBlocks < (int) FALL_START_BLOCKS) return 0;

        // Convert dBlocks to "hearts" using piecewise-linear interpolation between the points.
        // Then convert hearts -> half-hearts.
        double hearts;
        if (dBlocks <= 10) {
            hearts = lerp(1.0, 2.5, (dBlocks - 5) / 5.0);
        } else if (dBlocks <= 15) {
            hearts = lerp(2.5, 6.0, (dBlocks - 10) / 5.0);
        } else if (dBlocks <= 20) {
            hearts = lerp(6.0, 8.5, (dBlocks - 15) / 5.0);
        } else {
            // Extend using the last slope: 8.5 at 20 and slope = (8.5-6.0)/5 = 0.5 hearts per block.
            hearts = 8.5 + 0.5 * (dBlocks - 20);
        }

        // Convert to half-hearts.
        // Keep odd/even half-hearts so the HUD can display half-filled hearts.
        int damageHalfRaw = (int) Math.floor(hearts * 2.0 + 1e-6);
        return damageHalfRaw;
    }

    public void handleKeyPress(int key) {
        if (key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_A) left  = true;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = true;
        if (key == KeyEvent.VK_UP    || key == KeyEvent.VK_W || key == KeyEvent.VK_SPACE) jump = true;
        if (key == KeyEvent.VK_SHIFT) sprinting = true;
        if (key == KeyEvent.VK_CONTROL) crouchInput = true;
    }

    public void handleKeyRelease(int key) {
        if (key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_A) left  = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = false;
        if (key == KeyEvent.VK_UP    || key == KeyEvent.VK_W || key == KeyEvent.VK_SPACE) jump = false;
        if (key == KeyEvent.VK_SHIFT) sprinting = false;
        if (key == KeyEvent.VK_CONTROL) crouchInput = false;
    }
}

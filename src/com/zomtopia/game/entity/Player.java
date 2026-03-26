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
    public int foodHalf = 20;
    public int maxFoodHalf = 20;
    private float foodExhaustion = 0f; // Internal counter for sub-unit depletion

    public boolean onGround = false;

    // Small UI flash when damage happens.
    public float damageFlash = 0f; // 0..1

    // Fall tracking for landing damage.
    private boolean trackingFall = false;
    private double fallStartCenterY = 0; // player center Y when leaving the ground

    // Fall damage tuning:
    // We use "dBlocks" (vertical distance in tiles) and convert it into damage (half-hearts).
    // Desired points (hearts):
    // d=5  -> 1.0
    // d=10 -> 2.5
    // d=15 -> 6.0
    // d=20 -> 8.5

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

        // Hunger depletion
        float foodDrain = 0.0006f; 
        if (sprinting && Math.abs(vx) > 0.5) {
            foodDrain += 0.002f; 
        }
        if (jump && onGround) { 
            foodDrain += 0.05f; 
        }
        
        foodExhaustion += foodDrain;
        if (foodExhaustion >= 1.0f) {
            foodHalf = Math.max(0, foodHalf - 1);
            foodExhaustion -= 1.0f;
        }

        // Starvation damage
        if (foodHalf <= 0) {
            if (System.currentTimeMillis() % 4000 < 20) {
                applyDamageHalf(1);
            }
        } 
        // Regeneration: if food is high (>= 18 half-units / 9 full icons), heal slowly.
        else if (foodHalf >= 18 && healthHalf < maxHealthHalf) {
            if (System.currentTimeMillis() % 3000 < 20) {
                healthHalf = Math.min(maxHealthHalf, healthHalf + 1);
            }
        }

        // Jump
        if (jump && onGround) {
            vy = JUMP_VY;
            onGround = false;
        }

        // Start fall tracking when we leave ground.
        if (wasOnGround && !onGround && !trackingFall) {
            trackingFall = true;
            fallStartCenterY = yBefore + H / 2.0;
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
            // Measure fall distance using player center-to-center.
            // y coordinate increases downward, so falling makes (y + H/2) larger.
            // We want a positive "distance fallen" number.
            double fallDist = (y + H / 2.0) - fallStartCenterY;
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
            fallStartCenterY = yBefore + H / 2.0;
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

    public void resetForRespawn() {
        // Reset gameplay state after death menu "respawn".
        healthHalf = maxHealthHalf;
        foodHalf = maxFoodHalf;
        damageFlash = 0f;
        vx = 0;
        vy = 0;
        onGround = false;

        // Private fall tracking fields live inside this class.
        trackingFall = false;
        fallStartCenterY = 0;
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

        // More realistic curve:
        // 0-3 blocks: 0
        // 4 blocks: 1 half-heart
        // 5 blocks: 2 half-hearts (1 full heart)
        // 12 blocks: 10 half-hearts (5 hearts)
        // 20 blocks: 20 half-hearts (Fatal)
        
        if (dBlocks <= 3) return 0;
        
        double halfHearts;
        if (dBlocks <= 5) {
            halfHearts = lerp(1.0, 2.0, (dBlocks - 4) / 1.0);
        } else if (dBlocks <= 12) {
            halfHearts = lerp(2.0, 10.0, (dBlocks - 5) / 7.0);
        } else if (dBlocks <= 20) {
            halfHearts = lerp(10.0, 20.0, (dBlocks - 12) / 8.0);
        } else {
            halfHearts = 20.0; // Fatal
        }

        return (int) Math.floor(halfHearts + 1e-6);
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

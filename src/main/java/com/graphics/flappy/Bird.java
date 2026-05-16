package com.graphics.flappy;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Bird {
    public float x, y;
    public float velY;
    public boolean alive = true;
    public int score = 0;
    
    private final float width = 0.1f;
    private final float height = 0.1f;
    private final float r, g, b;

    public Bird(float x, float y, float r, float g, float b) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void jump(float force) {
        if (alive) velY = force;
    }

    public void update(float dt, float gravity) {
        if (!alive) return;
        velY += gravity * dt;
        y += velY * dt;

        if (y + height/2 >= 1.0f || y - height/2 <= -1.0f) {
            alive = false;
        }
    }

    public void render(int uOffsetLoc, int uScaleLoc, int uColorLoc) {
        GL20.glUniform2f(uOffsetLoc, x, y);
        GL20.glUniform2f(uScaleLoc, width, height);
        GL20.glUniform3f(uColorLoc, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public boolean collidesWith(float px, float py, float pw, float ph) {
        if (!alive) return false;
        float bL = x - width/2, bR = x + width/2, bT = y + height/2, bB = y - height/2;
        float pL = px - pw/2, pR = px + pw/2;
        if (bR > pL && bL < pR) {
            float gT = py + ph/2, gB = py - ph/2;
            return bT > gT || bB < gB;
        }
        return false;
    }
}

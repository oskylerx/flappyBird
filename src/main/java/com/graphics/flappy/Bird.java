package com.graphics.flappy;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Bird {
    public float x, y;
    public float velY;
    public boolean alive = true;
    public int score = 0;
    
    private final float width = 0.12f;
    private final float height = 0.10f;
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

        if (y + height/2 >= 1.0f || y - height/2 <= -1.0f) alive = false;
    }

    public void render(int uOff, int uSca, int uCol) {
        float pxS = 0.012f; // Tamaño del "pixel" geométrico

        // Sprite compacto: 1:Borde, 2:Cuerpo(Amarillo), 3:Blanco, 4:Rojo, 5:Naranja, 6:Pupila
        int[][] sprite = {
            {0,0,0,0,1,1,1,1,1,0,0,0},
            {0,0,1,1,2,2,2,3,3,1,0,0},
            {0,1,2,2,2,2,3,3,3,3,1,0},
            {1,2,2,2,2,2,3,3,1,3,3,1},
            {1,2,2,2,2,2,3,1,3,1,3,1},
            {1,2,2,2,2,2,3,1,6,1,3,1},
            {1,2,2,2,2,2,3,3,1,3,3,1},
            {1,2,2,2,2,2,2,3,3,3,1,0},
            {0,1,2,2,2,2,4,4,4,4,4,1},
            {0,0,1,1,5,5,4,4,4,4,4,1},
            {0,0,0,0,1,1,1,1,1,1,1,0}
        };

        float[][] colors = {
            {0,0,0}, {0.1f,0.05f,0.05f}, {r,g,b}, {1f,1f,1f}, {0.9f,0.15f,0.1f}, {0.95f,0.4f,0f}, {0f,0f,0f}
        };

        for (int row = 0; row < sprite.length; row++) {
            for (int col = 0; col < sprite[row].length; col++) {
                int colorIdx = sprite[row][col];
                if (colorIdx == 0) continue;

                // Dibujamos cada pieza relativa al centro x, y del pájaro
                float ox = (col - sprite[row].length / 2f) * pxS;
                float oy = (sprite.length / 2f - row) * pxS;

                float[] c = colors[colorIdx];
                GL20.glUniform2f(uOff, x + ox, y + oy);
                GL20.glUniform2f(uSca, pxS, pxS);
                GL20.glUniform3f(uCol, c[0], c[1], c[2]);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            }
        }
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

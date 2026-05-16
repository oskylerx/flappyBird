package com.graphics.flappy;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Bird {
    public float x, y;
    public float velY;
    public boolean alive = true;
    public int score = 0;
    
    private final float width = 0.12f;
    private final float height = 0.10f;
    private final float r, g, b;
    private float wingTimer = 0;

    public Bird(float x, float y, float r, float g, float b) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void jump(float force) {
        if (alive) {
            velY = force;
            wingTimer = 0.25f;
        }
    }

    public void update(float dt, float gravity) {
        // Siempre aplicamos gravedad para que caiga incluso muerto
        velY += gravity * dt;
        y += velY * dt;
        
        if (alive) {
            if (wingTimer > 0) wingTimer -= dt;
            if (y + height/2 >= 1.0f || y - height/2 <= -1.0f) alive = false;
        }
    }

    public void render(int uOff, int uSca, int uCol, int uTilt, int vaoQuad, int vaoTri) {
        // Si cae por debajo de la pantalla visible, no lo dibujamos (desaparece)
        if (y < -1.2f) return;

        float pxS = 0.012f; 
        
        // Tilt calculado y enviado al shader para que aplique un "shear" 
        // perfecto a todos los vértices, asegurando que el pico no se despegue.
        float tiltFactor = Math.max(-0.4f, Math.min(0.4f, velY * 0.4f));
        GL20.glUniform1f(uTilt, tiltFactor);

        int[][] sprite = {
            {0,0,0,0,0,0,1,1,1,1,1,0,0,0},
            {0,0,0,0,1,1,2,2,2,3,3,1,0,0},
            {0,0,0,1,2,2,2,2,3,3,3,3,1,0},
            {8,8,8,8,8,1,2,2,3,3,1,3,3,1},
            {8,7,7,7,7,8,2,2,3,1,3,1,3,1},
            {8,7,7,7,7,8,2,2,3,1,6,1,3,1},
            {8,8,8,8,8,1,2,2,3,3,1,3,3,1},
            {0,0,0,1,2,2,2,2,2,3,3,3,1,0},
            {0,0,0,0,1,2,2,2,2,1,0,0,0,0}, // Cara cerrada sin huecos
            {0,0,0,0,0,1,1,5,5,1,0,0,0,0}, // Cara cerrada sin huecos
            {0,0,0,0,0,0,0,1,1,1,0,0,0,0}
        };

        float[][] colors = {
            {0,0,0}, {0.1f,0.05f,0.05f}, {r,g,b}, {1,1,1}, {0.9f,0.15f,0.1f}, {0.95f,0.4f,0}, {0,0,0}, {1,1,1}, {0.1f,0.05f,0.05f}
        };

        float wingOffY = (wingTimer > 0) ? (float) Math.sin(wingTimer * 40) * 0.015f : 0;

        // DIBUJAR PICO (Primero para que quede detrás de la cara y no queden huecos)
        GL30.glBindVertexArray(vaoTri);
        float beakOx = 0.055f; // Un poco más atrás para que se incruste en la cara
        float beakOy = -0.035f; // Alineado con la fila 8 y 9
        float finalBeakOy = beakOy + (beakOx * tiltFactor);
        
        // Borde pico
        GL20.glUniform2f(uOff, x + beakOx, y + finalBeakOy);
        GL20.glUniform2f(uSca, 0.08f, 0.055f);
        GL20.glUniform3f(uCol, 0.1f, 0.05f, 0.05f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        
        // Interior rojo
        GL20.glUniform2f(uOff, x + beakOx, y + finalBeakOy);
        GL20.glUniform2f(uSca, 0.06f, 0.04f);
        GL20.glUniform3f(uCol, 0.9f, 0.2f, 0.1f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        // DIBUJAR CUERPO Y OJOS
        GL30.glBindVertexArray(vaoQuad);
        for (int row = 0; row < sprite.length; row++) {
            for (int col = 0; col < sprite[row].length; col++) {
                int colorIdx = sprite[row][col];
                if (colorIdx == 0) continue;

                float ox = (col - sprite[row].length / 2f) * pxS;
                float oy = (sprite.length / 2f - row) * pxS;

                if (colorIdx == 7 || colorIdx == 8) oy += wingOffY;
                
                oy += ox * tiltFactor;

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

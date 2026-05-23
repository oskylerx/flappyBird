package com.graphics.flappy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class PipeManager {
    public static class Pipe {
        float x, gapY;
        boolean p1Passed = false, p2Passed = false, p3Passed = false;

        Pipe(float x, float gapY) {
            this.x = x;
            this.gapY = gapY;
        }
    }

    private final List<Pipe> pipes = new ArrayList<>();
    private final Random rand = new Random();
    private float timer = 0;

    public float baseSpeed = 0.6f;
    public float baseInterval = 1.6f;
    public final float width = 0.18f;
    public final float gapHeight = 0.45f;

    private float currentSpeed = 0.6f;
    private float currentInterval = 1.6f;

    public void update(float dt, List<Bird> birds) {
        int maxScore = 0;
        for (Bird b : birds)
            if (b.score > maxScore)
                maxScore = b.score;

        // Dificultad: aumenta velocidad y frecuencia cada 5 puntos (cap maximo jugable)
        currentSpeed = Math.min(1.8f, baseSpeed + (maxScore / 5) * 0.15f);
        currentInterval = Math.max(0.8f, baseInterval - (maxScore / 5) * 0.1f);

        timer += dt;
        if (timer >= currentInterval) {
            timer = 0;
            spawn();
        }

        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= currentSpeed * dt;

            for (int i = 0; i < birds.size(); i++) {
                Bird b = birds.get(i);
                if (b.collidesWith(p.x, p.gapY, width, gapHeight))
                    b.alive = false;
                if (p.x + width / 2 < b.x) {
                    if (i == 0 && !p.p1Passed) {
                        p.p1Passed = true;
                        if (b.alive) b.score++;
                    }
                    if (i == 1 && !p.p2Passed) {
                        p.p2Passed = true;
                        if (b.alive) b.score++;
                    }
                    if (i == 2 && !p.p3Passed) {
                        p.p3Passed = true;
                        if (b.alive) b.score++;
                    }
                }
            }
            if (p.x < -1.5f)
                it.remove();
        }
    }

    public int getLevel(List<Bird> birds) {
        int maxScore = 0;
        for (Bird b : birds)
            if (b.score > maxScore)
                maxScore = b.score;
        return (maxScore / 5) + 1;
    }

    private void spawn() {
        pipes.add(new Pipe(1.2f, -0.4f + rand.nextFloat() * 0.8f));
    }

    public void render(int uOff, int uSca, int uCol, int uTilt) {
        for (Pipe p : pipes) {
            float tH = 1.0f - (p.gapY + gapHeight / 2);
            float tY = (p.gapY + gapHeight / 2) + tH / 2;
            drawPipe(uOff, uSca, uCol, uTilt, p.x, tY, width, tH, true);
            
            float bH = (p.gapY - gapHeight / 2) - (-1.0f);
            float bY = -1.0f + bH / 2;
            drawPipe(uOff, uSca, uCol, uTilt, p.x, bY, width, bH, false);
        }
    }

    private void drawPipe(int uOff, int uSca, int uCol, int uTilt, float x, float y, float w, float h, boolean isTopPipe) {
        GL20.glUniform1f(uTilt, 0); // No tilt for pipes
        
        float capHeight = 0.08f;
        float capWidth = w + 0.04f;
        
        float pipeTopY = y + h/2;
        float pipeBotY = y - h/2;
        
        float shaftH = h - capHeight;
        float shaftY;
        float capY;
        
        if (isTopPipe) {
            // Cap at the bottom
            capY = pipeBotY + capHeight/2;
            shaftY = pipeBotY + capHeight + shaftH/2;
        } else {
            // Cap at the top
            capY = pipeTopY - capHeight/2;
            shaftY = pipeTopY - capHeight - shaftH/2;
        }
        
        // --- DRAW SHAFT ---
        // Borde oscuro (Grosor extra en H también para borde superior/inferior)
        GL20.glUniform2f(uOff, x, shaftY);
        GL20.glUniform2f(uSca, w + 0.02f, shaftH + 0.02f);
        GL20.glUniform3f(uCol, 0.0f, 0.0f, 0.0f); // Negro
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        // Cuerpo verde
        GL20.glUniform2f(uOff, x, shaftY);
        GL20.glUniform2f(uSca, w, shaftH);
        GL20.glUniform3f(uCol, 0.45f, 0.75f, 0.25f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        // Brillo izquierdo
        GL20.glUniform2f(uOff, x - w/3, shaftY);
        GL20.glUniform2f(uSca, w/4, shaftH);
        GL20.glUniform3f(uCol, 0.6f, 0.9f, 0.4f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        
        // --- DRAW CAP ---
        // Borde oscuro (Grosor extra en H también)
        GL20.glUniform2f(uOff, x, capY);
        GL20.glUniform2f(uSca, capWidth + 0.02f, capHeight + 0.02f);
        GL20.glUniform3f(uCol, 0.0f, 0.0f, 0.0f); // Negro
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        // Cuerpo verde
        GL20.glUniform2f(uOff, x, capY);
        GL20.glUniform2f(uSca, capWidth, capHeight);
        GL20.glUniform3f(uCol, 0.45f, 0.75f, 0.25f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        // Brillo izquierdo
        GL20.glUniform2f(uOff, x - capWidth/3, capY);
        GL20.glUniform2f(uSca, capWidth/4, capHeight);
        GL20.glUniform3f(uCol, 0.6f, 0.9f, 0.4f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public void reset() {
        pipes.clear();
        timer = 0;
    }
}

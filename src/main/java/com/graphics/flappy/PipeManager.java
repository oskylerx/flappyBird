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
        boolean p1Passed = false, p2Passed = false;

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

        // Dificultad: aumenta velocidad y frecuencia cada 5 puntos
        currentSpeed = baseSpeed + (maxScore / 5) * 0.15f;
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
                        b.score++;
                    }
                    if (i == 1 && !p.p2Passed) {
                        p.p2Passed = true;
                        b.score++;
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

    public void render(int uOff, int uSca, int uCol) {
        for (Pipe p : pipes) {
            float tH = 1.0f - (p.gapY + gapHeight / 2);
            draw(uOff, uSca, uCol, p.x, (p.gapY + gapHeight / 2) + tH / 2, width, tH);
            float bH = (p.gapY - gapHeight / 2) - (-1.0f);
            draw(uOff, uSca, uCol, p.x, -1.0f + bH / 2, width, bH);
        }
    }

    private void draw(int uOff, int uSca, int uCol, float x, float y, float w, float h) {
        // Borde oscuro
        GL20.glUniform2f(uOff, x, y);
        GL20.glUniform2f(uSca, w + 0.015f, h);
        GL20.glUniform3f(uCol, 0.15f, 0.1f, 0.1f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        // Cuerpo verde
        GL20.glUniform2f(uOff, x, y);
        GL20.glUniform2f(uSca, w, h);
        GL20.glUniform3f(uCol, 0.3f, 0.8f, 0.3f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public void reset() {
        pipes.clear();
        timer = 0;
    }
}

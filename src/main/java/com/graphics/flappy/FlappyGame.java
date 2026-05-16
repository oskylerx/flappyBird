package com.graphics.flappy;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class FlappyGame {
    private enum State {
        START, PLAYING, GAMEOVER
    }

    private State state = State.START;

    private long window;
    private Shader shader;
    private int vao, vbo, vaoTri, vboTri;
    private int uOff, uSca, uCol, uTilt;

    // Control de teclas
    private boolean spacePressed = false;
    private boolean wPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean enterPressed = false;
    private int menuSelection = 1;
    private int gameOverSelection = 1;

    // Fuente de píxeles para números 0-9 y algunas letras
    private final int[][][] FONT = {
        {{1,1,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1}}, // 0
        {{0,1,0},{1,1,0},{0,1,0},{0,1,0},{1,1,1}}, // 1
        {{1,1,1},{0,0,1},{1,1,1},{1,0,0},{1,1,1}}, // 2
        {{1,1,1},{0,0,1},{1,1,1},{0,0,1},{1,1,1}}, // 3
        {{1,0,1},{1,0,1},{1,1,1},{0,0,1},{0,0,1}}, // 4
        {{1,1,1},{1,0,0},{1,1,1},{0,0,1},{1,1,1}}, // 5
        {{1,1,1},{1,0,0},{1,1,1},{1,0,1},{1,1,1}}, // 6
        {{1,1,1},{0,0,1},{0,1,0},{0,1,0},{0,1,0}}, // 7
        {{1,1,1},{1,0,1},{1,1,1},{1,0,1},{1,1,1}}, // 8
        {{1,1,1},{1,0,1},{1,1,1},{0,0,1},{1,1,1}}, // 9
        {{0,1,0},{1,0,1},{1,1,1},{1,0,1},{1,0,1}}, // 10: A
        {{1,1,0},{1,0,1},{1,1,0},{1,0,1},{1,1,0}}, // 11: B
        {{1,1,0},{1,0,1},{1,0,1},{1,0,1},{1,1,0}}, // 12: D
        {{1,1,1},{1,0,0},{1,1,0},{1,0,0},{1,0,0}}, // 13: F
        {{1,1,1},{0,1,0},{0,1,0},{0,1,0},{1,1,1}}, // 14: I
        {{1,0,0},{1,0,0},{1,0,0},{1,0,0},{1,1,1}}, // 15: L
        {{1,1,1},{1,0,1},{1,1,1},{1,0,0},{1,0,0}}, // 16: P
        {{1,1,0},{1,0,1},{1,1,0},{1,0,1},{1,0,1}}, // 17: R
        {{1,0,1},{1,0,1},{0,1,0},{0,1,0},{0,1,0}}, // 18: Y
        {{0,0,1},{0,0,1},{0,0,1},{1,0,1},{0,1,0}}, // 19: J
        {{1,0,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1}}, // 20: U
        {{0,1,1},{1,0,0},{1,0,1},{1,0,1},{0,1,1}}, // 21: G
        {{0,1,0},{1,0,1},{1,0,1},{1,0,1},{0,1,0}}, // 22: O
        {{1,1,1},{1,0,0},{1,1,0},{1,0,0},{1,1,1}}, // 23: E
        {{0,1,1},{1,0,0},{0,1,0},{0,0,1},{1,1,0}}, // 24: S
        {{1,0,1},{1,1,1},{1,0,1},{1,0,1},{1,0,1}}, // 25: M
        {{1,0,1},{1,0,1},{1,0,1},{1,0,1},{0,1,0}}, // 26: V
        {{1,1,1},{1,0,1},{1,0,1},{1,0,1},{1,0,1}}, // 27: N
        {{0,1,1},{1,0,0},{1,0,0},{1,0,0},{0,1,1}}  // 28: C
    };

    private final List<Bird> birds = new ArrayList<>();
    private final PipeManager pipeManager = new PipeManager();
    private SoundEngine sound;

    // Fondo: Nubes aleatorias
    private float[] cloudX = {0.5f, -0.8f, 1.2f, -0.2f, 1.8f};
    private float[] cloudY = {0.6f, 0.8f, 0.4f, 0.3f, 0.7f};
    private float[] cloudS = {1.0f, 0.6f, 0.8f, 1.2f, 0.7f};

    // Fondo: Montañas con Parallax
    private float[] mountX = {-0.5f, 0.8f, 0.1f, -0.9f, 1.5f, 2.2f};
    private float[] mountW = {0.8f, 1.0f, 0.9f, 0.6f, 0.7f, 1.1f};
    private float[] mountH = {0.6f, 0.7f, 0.5f, 0.4f, 0.55f, 0.65f};
    private int[] mountLayer = {0, 0, 1, 1, 1, 0}; // 0 = fondo lento, 1 = frente rápido

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit())
            throw new IllegalStateException("GLFW Fail");
        window = GLFW.glfwCreateWindow(900, 700, "Flappy Bird Parcial - 2 Players", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();

        shader = new Shader(
                "#version 330 core\nlayout(location=0) in vec3 p; uniform vec2 uO, uS; uniform float uT; void main(){gl_Position=vec4(p.x*uS.x+uO.x, p.y*uS.y+uO.y + p.x*uS.x*uT, p.z, 1);}",
                "#version 330 core\nuniform vec3 uC; out vec4 f; void main(){f=vec4(uC,1);}");
        uOff = shader.getUniformLocation("uO");
        uSca = shader.getUniformLocation("uS");
        uCol = shader.getUniformLocation("uC");
        uTilt = shader.getUniformLocation("uT");

        float[] v = { -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0 };
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer b = BufferUtils.createFloatBuffer(v.length);
        b.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, b, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        // --- VAO PARA TRIÁNGULOS (Pico) ---
        float[] vt = { 0.5f, 0.0f, 0, -0.5f, 0.5f, 0, -0.5f, -0.5f, 0 }; 
        vaoTri = GL30.glGenVertexArrays();
        vboTri = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoTri);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTri);
        FloatBuffer bt = BufferUtils.createFloatBuffer(vt.length);
        bt.put(vt).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bt, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        reset();
        sound = new SoundEngine();
        sound.init();
    }

    private void reset() {
        birds.clear();
        pipeManager.reset();
        state = State.START;
    }

    private void startGame(int numPlayers) {
        birds.clear();
        birds.add(new Bird(-0.5f, 0, 0.97f, 0.71f, 0.0f)); // P1
        if (numPlayers == 2) {
            birds.add(new Bird(-0.3f, 0, 0.1f, 0.8f, 0.95f)); // P2
        }
        pipeManager.reset();
        state = State.PLAYING;
    }

    private void loop() {
        float last = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt = Math.min(now - last, 0.05f);
            last = now;

            // Mover nubes siempre para dar vida al fondo
            for (int i = 0; i < cloudX.length; i++) {
                cloudX[i] -= 0.15f * dt * cloudS[i];
                if (cloudX[i] < -1.5f) cloudX[i] = 1.5f;
            }

            // Mover montañas (Parallax)
            for (int i = 0; i < mountX.length; i++) {
                float speed = mountLayer[i] == 0 ? 0.04f : 0.08f;
                mountX[i] -= speed * dt;
                if (mountX[i] < -2.0f) mountX[i] += 4.5f;
            }

            input();
            if (state == State.PLAYING) {
                // Rastreamos puntajes Y estado vivo ANTES de actualizar
                int[] prevScores = new int[birds.size()];
                boolean[] wasAlive = new boolean[birds.size()];
                int prevLevel = pipeManager.getLevel(birds);
                for (int i = 0; i < birds.size(); i++) {
                    prevScores[i] = birds.get(i).score;
                    wasAlive[i] = birds.get(i).alive;
                }

                pipeManager.update(dt, birds);

                boolean anyAlive = false;
                for (int i = 0; i < birds.size(); i++) {
                    Bird b = birds.get(i);
                    b.update(dt, -1.8f);
                    // Deteccion: por colision de tubo (pipeManager) O suelo/techo (b.update)
                    if (!b.alive && wasAlive[i]) sound.playDie(i);
                    if (b.alive) anyAlive = true;
                }

                // Detectar si algun score subio
                for (int i = 0; i < birds.size(); i++) {
                    if (birds.get(i).score > prevScores[i]) sound.playScore();
                }

                // Detectar subida de nivel
                int newLevel = pipeManager.getLevel(birds);
                if (newLevel > prevLevel) sound.playLevelUp();

                if (!anyAlive)
                    state = State.GAMEOVER;
            }

            render();

            int level = pipeManager.getLevel(birds);
            String t = "Flappy 2P";
            if (birds.size() > 0) {
                t += " | NIVEL " + level + " | P1: " + birds.get(0).score;
                if (birds.size() > 1) t += " | P2: " + birds.get(1).score;
            }
            if (state == State.START)
                t = "MENU PRINCIPAL";
            else if (state == State.GAMEOVER)
                t = "GAME OVER";
            GLFW.glfwSetWindowTitle(window, t);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void render() {
        GL11.glClearColor(0.4f, 0.7f, 0.9f, 1.0f); // Cielo azul claro
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        shader.use();
        GL30.glBindVertexArray(vao);

        // --- DIBUJAR FONDO ---
        // Montañas
        for (int layer = 0; layer <= 1; layer++) {
            for (int i = 0; i < mountX.length; i++) {
                if (mountLayer[i] == layer) {
                    float r = layer == 0 ? 0.3f : 0.4f;
                    float g = layer == 0 ? 0.5f : 0.6f;
                    float b = layer == 0 ? 0.3f : 0.35f;
                    drawPixelMountain(mountX[i], -0.9f, mountW[i], mountH[i], r, g, b);
                }
            }
        }

        // Nubes
        for (int i = 0; i < cloudX.length; i++) {
            drawCloud(cloudX[i], cloudY[i], cloudS[i]);
        }

        // Suelo
        drawRect(0, -0.95f, 2.0f, 0.1f, 0.3f, 0.5f, 0.2f);
        drawRect(0, -0.9f, 2.0f, 0.02f, 0.2f, 0.4f, 0.1f); // Borde del suelo

        if (state == State.START) {
            // Pantalla de carga / Menú
            GL30.glBindVertexArray(vao);
            drawText("FLAPPY BIRD", 0.0f, 0.5f, 0.04f, 1.0f, 0.8f, 0.0f); // Título amarillo
            
            drawText("1 JUGADOR", 0.0f, 0.0f, 0.025f, 0.97f, 0.71f, 0.0f); // Opcion 1P
            drawText("2 JUGADORES", 0.0f, -0.2f, 0.025f, 0.1f, 0.8f, 0.95f); // Opcion 2P

            // Dibujar cursor como triángulo
            GL30.glBindVertexArray(vaoTri);
            float cursorY = menuSelection == 1 ? (0.0f - 0.06f) : (-0.2f - 0.06f);
            GL20.glUniform1f(uTilt, 0);
            GL20.glUniform2f(uOff, -0.6f, cursorY);
            GL20.glUniform2f(uSca, 0.06f, 0.06f);
            GL20.glUniform3f(uCol, 1.0f, 1.0f, 1.0f); // Blanco
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } else {
            pipeManager.render(uOff, uSca, uCol, uTilt);
            for (Bird b : birds)
                b.render(uOff, uSca, uCol, uTilt, vao, vaoTri);

            // DIBUJAR UI (Marcadores y Nivel)
            GL30.glBindVertexArray(vao);
            drawText(String.valueOf(pipeManager.getLevel(birds)), 0.0f, 0.8f, 0.025f, 1.0f, 1.0f, 1.0f);
            
            if(birds.size() > 0)
                drawText(String.valueOf(birds.get(0).score), -0.6f, 0.8f, 0.025f, 0.97f, 0.71f, 0.0f);
                
            if(birds.size() > 1)
                drawText(String.valueOf(birds.get(1).score), 0.6f, 0.8f, 0.025f, 0.1f, 0.8f, 0.95f);

            if (state == State.GAMEOVER) {
                // Menú Game Over
                GL30.glBindVertexArray(vao);
                drawText("GAME OVER", 0.0f, 0.4f, 0.05f, 1.0f, 0.2f, 0.2f); // Rojo oscuro

                drawText("REINICIAR", 0.0f, 0.0f, 0.025f, 1.0f, 1.0f, 1.0f);
                drawText("MENU", 0.0f, -0.2f, 0.025f, 1.0f, 1.0f, 1.0f);

                // Cursor Game Over
                GL30.glBindVertexArray(vaoTri);
                float cursorY = gameOverSelection == 1 ? (0.0f - 0.06f) : (-0.2f - 0.06f);
                GL20.glUniform1f(uTilt, 0);
                GL20.glUniform2f(uOff, -0.55f, cursorY);
                GL20.glUniform2f(uSca, 0.06f, 0.06f);
                GL20.glUniform3f(uCol, 1.0f, 0.2f, 0.2f); // Triángulo rojo
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            }
        }
    }

    private void drawText(String text, float x, float y, float size, float r, float g, float b) {
        text = text.toUpperCase();
        float startX = x - (text.length() * size * 4) / 2f; 
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') continue;
            
            int idx = -1;
            if (c >= '0' && c <= '9') idx = c - '0';
            else if (c == 'A') idx = 10;
            else if (c == 'B') idx = 11;
            else if (c == 'D') idx = 12;
            else if (c == 'F') idx = 13;
            else if (c == 'I') idx = 14;
            else if (c == 'L') idx = 15;
            else if (c == 'P') idx = 16;
            else if (c == 'R') idx = 17;
            else if (c == 'Y') idx = 18;
            else if (c == 'J') idx = 19;
            else if (c == 'U') idx = 20;
            else if (c == 'G') idx = 21;
            else if (c == 'O') idx = 22;
            else if (c == 'E') idx = 23;
            else if (c == 'S') idx = 24;
            else if (c == 'M') idx = 25;
            else if (c == 'V') idx = 26;
            else if (c == 'N') idx = 27;
            else if (c == 'C') idx = 28;
            
            if (idx >= 0) {
                int[][] matrix = FONT[idx];
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 3; col++) {
                        if (matrix[row][col] == 1) {
                            drawRect(startX + (i * 4 + col) * size, y - row * size, size, size, r, g, b);
                        }
                    }
                }
            }
        }
    }

    private void drawRect(float x, float y, float w, float h, float cr, float cg, float cb) {
        GL20.glUniform1f(uTilt, 0);
        GL20.glUniform2f(uOff, x, y);
        GL20.glUniform2f(uSca, w, h);
        GL20.glUniform3f(uCol, cr, cg, cb);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private void drawPixelMountain(float cx, float cy, float width, float height, float r, float g, float b) {
        int steps = 14;
        float stepH = height / steps;
        for (int i = 0; i < steps; i++) {
            float stepW = width * (1.0f - (float)i / steps);
            
            // Sombra/borde perimetral (Grosor extra para delinear)
            drawRect(cx, cy + i * stepH + stepH/2, stepW + 0.03f, stepH, 0.1f, 0.15f, 0.1f);
            
            // Textura: Nieve en la cima (los últimos 4 escalones)
            if (i >= steps - 4) {
                drawRect(cx, cy + i * stepH + stepH/2, stepW, stepH, 0.9f, 0.95f, 1.0f);
            } else {
                // Interior verde
                drawRect(cx, cy + i * stepH + stepH/2, stepW, stepH, r, g, b);
                // Textura: Raya oscura en el medio simulando relieve
                drawRect(cx + stepW/6, cy + i * stepH + stepH/2, stepW/2, stepH, r*0.75f, g*0.75f, b*0.75f);
            }
        }
    }

    private void drawCloud(float cx, float cy, float scale) {
        // Formamos una nube uniendo varios bloques para dar aspecto pixelado
        drawRect(cx, cy, 0.4f * scale, 0.15f * scale, 1, 1, 1);
        drawRect(cx - 0.15f * scale, cy + 0.05f * scale, 0.2f * scale, 0.15f * scale, 1, 1, 1);
        drawRect(cx + 0.1f * scale, cy + 0.08f * scale, 0.25f * scale, 0.2f * scale, 1, 1, 1);
        drawRect(cx + 0.2f * scale, cy + 0.02f * scale, 0.15f * scale, 0.1f * scale, 1, 1, 1);
    }

    private void input() {
        boolean isUp = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean isDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
        boolean isSpace = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean isEnter = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;

        if (state == State.START) {
            if (isUp && !upPressed) { menuSelection = 1; sound.playSelect(); }
            if (isDown && !downPressed) { menuSelection = 2; sound.playSelect(); }
            if (isEnter && !enterPressed) {
                sound.playConfirm();
                startGame(menuSelection);
            }
        } else if (state == State.GAMEOVER) {
            if (isUp && !upPressed) { gameOverSelection = 1; sound.playSelect(); }
            if (isDown && !downPressed) { gameOverSelection = 2; sound.playSelect(); }
            if (isEnter && !enterPressed) {
                sound.playConfirm();
                if (gameOverSelection == 1) {
                    startGame(birds.size()); // Reiniciar partida actual
                } else {
                    reset(); // Ir al menu principal
                }
            }
        } else {
            if (isSpace && !spacePressed) {
                if (state == State.PLAYING && birds.size() > 0) {
                    birds.get(0).jump(0.8f);
                    sound.playJump(0);
                }
            }

            boolean isWDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
            if ((isWDown || isUp) && !wPressed) {
                if (state == State.PLAYING && birds.size() > 1) {
                    birds.get(1).jump(0.8f);
                    sound.playJump(1);
                }
            }
            wPressed = isWDown || isUp;
        }

        upPressed = isUp;
        downPressed = isDown;
        spacePressed = isSpace;
        enterPressed = isEnter;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }
    }

    private void cleanup() {
        if (sound != null) sound.cleanup();
        shader.cleanup();
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new FlappyGame().run();
    }
}

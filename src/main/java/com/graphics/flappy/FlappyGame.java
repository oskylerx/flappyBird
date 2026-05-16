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
    private long window;
    private Shader shader;
    private int vao, vbo;
    private int uOff, uSca, uCol;

    private final List<Bird> birds = new ArrayList<>();
    private final PipeManager pipeManager = new PipeManager();
    private boolean started = false, gameOver = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW Fail");
        window = GLFW.glfwCreateWindow(900, 700, "Flappy Bird Parcial", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();

        shader = new Shader(
            "#version 330 core\nlayout(location=0) in vec3 p; uniform vec2 uO, uS; void main(){gl_Position=vec4(p.xy*uS+uO,p.z,1);}",
            "#version 330 core\nuniform vec3 uC; out vec4 f; void main(){f=vec4(uC,1);}"
        );
        uOff = shader.getUniformLocation("uO");
        uSca = shader.getUniformLocation("uS");
        uCol = shader.getUniformLocation("uC");

        float[] v = {-0.5f,-0.5f,0, 0.5f,-0.5f,0, 0.5f,0.5f,0, -0.5f,-0.5f,0, 0.5f,0.5f,0, -0.5f,0.5f,0};
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer b = BufferUtils.createFloatBuffer(v.length);
        b.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, b, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        reset();
    }

    private void reset() {
        birds.clear();
        birds.add(new Bird(-0.5f, 0, 0.9f, 0.8f, 0.1f)); // P1
        birds.add(new Bird(-0.3f, 0, 0.1f, 0.8f, 0.9f)); // P2
        pipeManager.reset();
        started = gameOver = false;
    }

    private void loop() {
        float last = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt = Math.min(now - last, 0.05f);
            last = now;

            input();
            if (started && !gameOver) {
                pipeManager.update(dt, birds);
                boolean anyAlive = false;
                for (Bird b : birds) {
                    b.update(dt, -1.8f);
                    if (b.alive) anyAlive = true;
                }
                if (!anyAlive) gameOver = true;
            }

            GL11.glClearColor(0.5f, 0.7f, 0.9f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            shader.use();
            GL30.glBindVertexArray(vao);
            pipeManager.render(uOff, uSca, uCol);
            for (Bird b : birds) b.render(uOff, uSca, uCol);

            String t = "Flappy 2P | P1: " + birds.get(0).score + " | P2: " + birds.get(1).score;
            if (gameOver) t += " - GAME OVER (SPACE to restart)";
            GLFW.glfwSetWindowTitle(window, t);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void input() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            if (gameOver) reset(); else { started = true; birds.get(0).jump(0.8f); }
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            if (!gameOver) { started = true; birds.get(1).jump(0.8f); }
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }
    }

    private void cleanup() {
        shader.cleanup();
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) { new FlappyGame().run(); }
}

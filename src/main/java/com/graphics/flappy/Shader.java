package com.graphics.flappy;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Shader {
    private final int id;

    public Shader(String vertexSrc, String fragmentSrc) {
        int vertexShader = compile(vertexSrc, GL20.GL_VERTEX_SHADER);
        int fragmentShader = compile(fragmentSrc, GL20.GL_FRAGMENT_SHADER);

        id = GL20.glCreateProgram();
        GL20.glAttachShader(id, vertexShader);
        GL20.glAttachShader(id, fragmentShader);
        GL20.glLinkProgram(id);

        if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error linkeando shader: " + GL20.glGetProgramInfoLog(id));
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private int compile(String src, int type) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, src);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error compilando shader (" + type + "): " + GL20.glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    public void use() {
        GL20.glUseProgram(id);
    }

    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(id, name);
    }

    public void cleanup() {
        GL20.glDeleteProgram(id);
    }
}

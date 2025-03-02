package com.vnd.superbible7.common.shapes;

import android.opengl.GLES32;

import com.vnd.superbible7.common.Attributes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {
    private static final float[] vertices = {
            // Front face
            -0.25f, 0.25f, -0.25f,
            -0.25f, -0.25f, -0.25f,
            0.25f, -0.25f, -0.25f,
            0.25f, -0.25f, -0.25f,
            0.25f, 0.25f, -0.25f,
            -0.25f, 0.25f, -0.25f,

            // Right face
            0.25f, -0.25f, -0.25f,
            0.25f, -0.25f, 0.25f,
            0.25f, 0.25f, -0.25f,
            0.25f, -0.25f, 0.25f,
            0.25f, 0.25f, 0.25f,
            0.25f, 0.25f, -0.25f,

            // Back face
            0.25f, -0.25f, 0.25f,
            -0.25f, -0.25f, 0.25f,
            0.25f, 0.25f, 0.25f,
            -0.25f, -0.25f, 0.25f,
            -0.25f, 0.25f, 0.25f,
            0.25f, 0.25f, 0.25f,

            // Left face
            -0.25f, -0.25f, 0.25f,
            -0.25f, -0.25f, -0.25f,
            -0.25f, 0.25f, 0.25f,
            -0.25f, -0.25f, -0.25f,
            -0.25f, 0.25f, -0.25f,
            -0.25f, 0.25f, 0.25f,

            // Bottom face
            -0.25f, -0.25f, 0.25f,
            0.25f, -0.25f, 0.25f,
            0.25f, -0.25f, -0.25f,
            0.25f, -0.25f, -0.25f,
            -0.25f, -0.25f, -0.25f,
            -0.25f, -0.25f, 0.25f,

            // Top face
            -0.25f, 0.25f, -0.25f,
            0.25f, 0.25f, -0.25f,
            0.25f, 0.25f, 0.25f,
            0.25f, 0.25f, 0.25f,
            -0.25f, 0.25f, 0.25f,
            -0.25f, 0.25f, -0.25f
    };

    private static final float[] normals = {
            // Front face
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,

            // Right face
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,

            // Back face
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,

            // Left face
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,

            // Bottom face
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,

            // Top face
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f
    };

    private static final float[] textureCoordinates = {
            // Front face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            // Right face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            // Back face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            // Left face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            // Top face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,

            // Bottom face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
    };

    private final int[] vao = new int[1];
    private final int[] vbos = new int[Attributes.MAX];

    public Cube() {
        GLES32.glGenVertexArrays(1, vao, 0);
        GLES32.glGenBuffers(Attributes.MAX, vbos, 0);
        GLES32.glBindVertexArray(vao[0]);

        // Vertex buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.VERTEX]);
        FloatBuffer vertexBuffer = getFloatBuffer(vertices);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Normal buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.NORMAL]);
        FloatBuffer normalBuffer = getFloatBuffer(normals);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, normals.length * 4, normalBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.NORMAL, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.NORMAL);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Texture coordinates buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.TEXCOORD]);
        FloatBuffer textureCoordinateBuffer = getFloatBuffer(textureCoordinates);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, textureCoordinates.length * 4, textureCoordinateBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.TEXCOORD, 2, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.TEXCOORD);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Unbind VAO and buffers
        GLES32.glBindVertexArray(0);
    }

    private FloatBuffer getFloatBuffer(float[] data) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);
        return floatBuffer;
    }

    public void draw() {
        GLES32.glBindVertexArray(vao[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertices.length / 3);
        GLES32.glBindVertexArray(0);
    }

    public void cleanup() {
        // Delete VBOs for vertex attributes
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] > 0) {
                GLES32.glDeleteBuffers(1, vbos, i);
                vbos[i] = 0;
            }
        }

        // Delete the VAO
        if (vao[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao, 0);
            vao[0] = 0;
        }
    }
}

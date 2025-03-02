package com.vnd.superbible7.common.shapes;

import android.opengl.GLES32;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.Attributes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class IndexedCube {
    //  8 unique corners of the cube
    private static final float[] vertices = {
            -0.25f, -0.25f, -0.25f,
            -0.25f, 0.25f, -0.25f,
            0.25f, -0.25f, -0.25f,
            0.25f, 0.25f, -0.25f,
            0.25f, -0.25f, 0.25f,
            0.25f, 0.25f, 0.25f,
            -0.25f, -0.25f, 0.25f,
            -0.25f, 0.25f, 0.25f
    };

    // Normals for each face
    // 24 (6 faces × 4 vertices per face)
    private static final float[] normals = {
            // -Z face
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
            // +X face
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // +Z face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            // -X face
            -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            // -Y face
            0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
            // +Y face
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
    };

    // Textures for each face
    // 24 (6 faces × 4 vertices per face)
    private static final float[] textureCoordinates = {
            // -Z face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // +X face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // +Z face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // -X face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // -Y face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // +Y face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f
    };

    // how vertices are connected to form triangles
    // 36 (6 faces × 6 indices per face)
    private static final short[] indices = {
            0, 1, 2, 2, 1, 3,
            2, 3, 4, 4, 3, 5,
            4, 5, 6, 6, 5, 7,
            6, 7, 0, 0, 7, 1,
            6, 0, 2, 2, 4, 6,
            7, 5, 3, 7, 3, 1
    };

    private final int[] vao = new int[1];
    private final int[] vbos = new int[Attributes.MAX];
    private final int[] ebo = new int[1];

    public IndexedCube() {
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

        // Index buffer
        GLES32.glGenBuffers(1, ebo, 0);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
        ShortBuffer shortBuffer = getShortBuffer();
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, shortBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Unbind VAO and buffers
        GLES32.glBindVertexArray(0);
    }

    @NonNull
    private static ShortBuffer getShortBuffer() {
        ByteBuffer byteBufferIndices = ByteBuffer.allocateDirect(indices.length * 2);
        byteBufferIndices.order(ByteOrder.nativeOrder());
        ShortBuffer shortBufferIndices = byteBufferIndices.asShortBuffer();
        shortBufferIndices.put(indices);
        shortBufferIndices.position(0);
        return shortBufferIndices;
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
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, indices.length, GLES32.GL_UNSIGNED_SHORT, 0);
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

        // Delete the EBO
        if (ebo[0] > 0) {
            GLES32.glDeleteVertexArrays(1, ebo, 0);
            ebo[0] = 0;
        }

        // Delete the VAO
        if (vao[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao, 0);
            vao[0] = 0;
        }
    }
}

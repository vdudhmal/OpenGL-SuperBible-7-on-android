package com.vnd.superbible7.common.shapes;

import android.opengl.GLES32;

import com.vnd.superbible7.common.Attributes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class Torus {
    private final ArrayList<Float> vertices = new ArrayList<>();

    private final int[] vao = new int[1];
    private final int[] vbos = new int[Attributes.MAX];

    public Torus() {
        // Generate the torus data
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Float> texture_coordinates = new ArrayList<>();
        generateTorus(vertices, normals, texture_coordinates);

        // Convert vertices List<Float> to float array
        float[] positionArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            positionArray[i] = vertices.get(i);
        }

        // Convert normals List<Float> to float array
        float[] normalArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normalArray[i] = normals.get(i);
        }

        // Convert texCoordinates List<Float> to float array
        float[] textureArray = new float[texture_coordinates.size()];
        for (int i = 0; i < texture_coordinates.size(); i++) {
            textureArray[i] = texture_coordinates.get(i);
        }

        // Log the sizes of the vertex data
        System.out.println("VND: TorusData vertices = " + vertices.size() + ", normals = " + normals.size() + ", texture_coordinates = " + texture_coordinates.size());

        // torus
        GLES32.glGenVertexArrays(1, vao, 0);
        GLES32.glGenBuffers(Attributes.MAX, vbos, 0);
        GLES32.glBindVertexArray(vao[0]);

        // Vertex buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.VERTEX]);
        FloatBuffer vertexBuffer = getFloatBuffer(positionArray);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Normal buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.NORMAL]);
        FloatBuffer normalBuffer = getFloatBuffer(normalArray);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.NORMAL, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.NORMAL);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Texture coordinates buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.TEXCOORD]);
        FloatBuffer textureCoordinateBuffer = getFloatBuffer(textureArray);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, textureCoordinateBuffer.capacity() * 4, textureCoordinateBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.TEXCOORD, 2, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.TEXCOORD);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);
    }

    // Function to generate torus data
    private void generateTorus(ArrayList<Float> vertices, ArrayList<Float> normals, ArrayList<Float> texture_coordinates) {
        int numMajor = 64;
        for (int i = 0; i < numMajor; ++i) {
            float a0 = (float) i / numMajor * 2.0f * (float) Math.PI;
            float a1 = (float) (i + 1) / numMajor * 2.0f * (float) Math.PI;
            float x0 = (float) Math.cos(a0), y0 = (float) Math.sin(a0);
            float x1 = (float) Math.cos(a1), y1 = (float) Math.sin(a1);

            int numMinor = 32;
            for (int j = 0; j <= numMinor; ++j) {
                float b = (float) j / numMinor * 2.0f * (float) Math.PI;
                float majorRadius = 2.0f;
                float minorRadius = 1.5f;
                float c = (float) Math.cos(b), r = minorRadius * c + majorRadius;
                float z = minorRadius * (float) Math.sin(b);

                // First vertex
                vertices.add(x0 * r);
                vertices.add(y0 * r);
                vertices.add(z);

                normals.add(x0 * c);
                normals.add(y0 * c);
                normals.add((float) Math.sin(b));

                texture_coordinates.add((float) i / numMajor);
                texture_coordinates.add((float) j / numMinor);

                // Second vertex
                vertices.add(x1 * r);
                vertices.add(y1 * r);
                vertices.add(z);

                normals.add(x1 * c);
                normals.add(y1 * c);
                normals.add((float) Math.sin(b));

                texture_coordinates.add((float) (i + 1) / numMajor);
                texture_coordinates.add((float) j / numMinor);
            }
        }
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
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, vertices.size() / 3);
        GLES32.glBindVertexArray(0);
    }

    public void draw(int instances) {
        GLES32.glBindVertexArray(vao[0]);
        GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLE_STRIP, 0, vertices.size() / 3, instances);
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

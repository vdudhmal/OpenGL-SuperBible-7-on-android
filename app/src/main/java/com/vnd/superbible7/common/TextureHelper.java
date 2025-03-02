package com.vnd.superbible7.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class TextureHelper {
    private final Context context;

    public TextureHelper(Context context) {
        this.context = context;
    }

    public int generateColorPattern() {
        final int width = 256;
        final int height = 256;
        final float[] floatData = new float[width * height * 4];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Set the RGBA values
                floatData[(i * width + j) * 4] = (float) ((j & i) & 0xFF) / 255.0f; // Red
                floatData[(i * width + j) * 4 + 1] = (float) ((j | i) & 0xFF) / 255.0f; // Green
                floatData[(i * width + j) * 4 + 2] = (float) ((j ^ i) & 0xFF) / 255.0f; // Blue
                floatData[(i * width + j) * 4 + 3] = 1.0f; // Alpha
            }
        }

        // Create texture
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);

        // Texture Filtering
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        // Create texture image
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(floatData.length * 4); // 4 bytes for float
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(floatData);
        floatBuffer.position(0);

        // Specify the texture image
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA, width, height, 0,
                GLES32.GL_RGBA, GLES32.GL_FLOAT, floatBuffer);

        // Generate mipmaps
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    public int generateCheckerBoard() {
        final int width = 64;
        final int height = 64;
        final byte[] byteData = new byte[width * height * 4];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int c = ((i & 8) ^ (j & 8)) * 255;
                byteData[(i * width + j) * 4] = (byte) c;
                byteData[(i * width + j) * 4 + 1] = (byte) c;
                byteData[(i * width + j) * 4 + 2] = (byte) c;
                byteData[(i * width + j) * 4 + 3] = (byte) 0xff;
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(byteData);
        byteBuffer.position(0);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);

        // create image texture
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Texture Filtering
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);

        // create multiple MIPMAP images
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    public int generateToonTexture() {
        int[] texture = new int[1];

        // Generate texture ID
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);

        // Texture data (1D texture represented as 2D with height of 1)
        byte[] toonTexData = new byte[]{
                (byte) 0x44, 0x00, 0x00, (byte) 0xFF, // Red-ish
                (byte) 0x88, 0x00, 0x00, (byte) 0xFF, // Darker Red
                (byte) 0xCC, 0x00, 0x00, (byte) 0xFF, // Even Darker Red
                (byte) 0xFF, 0x00, 0x00, (byte) 0xFF  // Bright Red
        };

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(toonTexData.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(toonTexData);
        byteBuffer.position(0);

        // Create 2D texture with height 1 to mimic 1D texture behavior
        GLES32.glTexImage2D(
                GLES32.GL_TEXTURE_2D,
                0,
                GLES32.GL_RGBA,
                toonTexData.length / 4,
                1, // simulating 1D texture as 2D
                0,
                GLES32.GL_RGBA,
                GLES32.GL_UNSIGNED_BYTE,
                byteBuffer);

        // Set texture parameters for filtering and wrapping
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    public int generateJuliaTexture() {
        int[] texture = new int[1];

        // Generate texture ID
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);

        // Texture data (1D texture represented as 2D with height of 1)
        byte[] palette = new byte[]{
                (byte) 0xFF, 0x00, 0x00, (byte) 0xFF, 0x0E, 0x03, (byte) 0xFF, 0x1C,
                0x07, (byte) 0xFF, 0x2A, 0x0A, (byte) 0xFF, 0x38, 0x0E, (byte) 0xFF,
                0x46, 0x12, (byte) 0xFF, 0x54, 0x15, (byte) 0xFF, 0x62, 0x19,
                (byte) 0xFF, 0x71, 0x1D, (byte) 0xFF, 0x7F, 0x20, (byte) 0xFF, (byte) 0x88,
                0x22, (byte) 0xFF, (byte) 0x92, 0x25, (byte) 0xFF, (byte) 0x9C, 0x27, (byte) 0xFF,
                (byte) 0xA6, 0x2A, (byte) 0xFF, (byte) 0xB0, 0x2C, (byte) 0xFF, (byte) 0xBA, 0x2F,
                (byte) 0xFF, (byte) 0xC4, 0x31, (byte) 0xFF, (byte) 0xCE, 0x34, (byte) 0xFF, (byte) 0xD8,
                0x36, (byte) 0xFF, (byte) 0xE2, 0x39, (byte) 0xFF, (byte) 0xEC, 0x3B, (byte) 0xFF,
                (byte) 0xF6, 0x3E, (byte) 0xFF, (byte) 0xFF, 0x40, (byte) 0xF8, (byte) 0xFE, 0x40,
                (byte) 0xF1, (byte) 0xFE, 0x40, (byte) 0xEA, (byte) 0xFE, 0x41, (byte) 0xE3, (byte) 0xFD,
                0x41, (byte) 0xDC, (byte) 0xFD, 0x41, (byte) 0xD6, (byte) 0xFD, 0x42, (byte) 0xCF,
                (byte) 0xFC, 0x42, (byte) 0xC8, (byte) 0xFC, 0x42, (byte) 0xC1, (byte) 0xFC, 0x43,
                (byte) 0xBA, (byte) 0xFB, 0x43, (byte) 0xB4, (byte) 0xFB, 0x43, (byte) 0xAD, (byte) 0xFB,
                0x44, (byte) 0xA6, (byte) 0xFA, 0x44, (byte) 0x9F, (byte) 0xFA, 0x45, (byte) 0x98,
                (byte) 0xFA, 0x45, (byte) 0x92, (byte) 0xF9, 0x45, (byte) 0x8B, (byte) 0xF9, 0x46,
                (byte) 0x84, (byte) 0xF9, 0x46, 0x7D, (byte) 0xF8, 0x46, 0x76, (byte) 0xF8,
                0x46, 0x6F, (byte) 0xF8, 0x47, 0x68, (byte) 0xF8, 0x47, 0x61,
                (byte) 0xF7, 0x47, 0x5A, (byte) 0xF7, 0x48, 0x53, (byte) 0xF7, 0x48,
                0x4C, (byte) 0xF6, 0x48, 0x45, (byte) 0xF6, 0x49, 0x3E, (byte) 0xF6,
                0x49, 0x37, (byte) 0xF6, 0x4A, 0x30, (byte) 0xF5, 0x4A, 0x29,
                (byte) 0xF5, 0x4A, 0x22, (byte) 0xF5, 0x4B, 0x1B, (byte) 0xF5, 0x4B,
                0x14, (byte) 0xF4, 0x4B, 0x0D, (byte) 0xF4, 0x4C, 0x06, (byte) 0xF4,
                0x4D, 0x04, (byte) 0xF1, 0x51, 0x0D, (byte) 0xE9, 0x55, 0x16,
                (byte) 0xE1, 0x59, 0x1F, (byte) 0xD9, 0x5D, 0x28, (byte) 0xD1, 0x61,
                0x31, (byte) 0xC9, 0x65, 0x3A, (byte) 0xC1, 0x69, 0x42, (byte) 0xB9,
                0x6D, 0x4B, (byte) 0xB1, 0x71, 0x54, (byte) 0xA9, 0x75, 0x5D,
                (byte) 0xA1, 0x79, 0x66, (byte) 0x99, 0x7D, 0x6F, (byte) 0x91, (byte) 0x81,
                0x78, (byte) 0x89, (byte) 0x86, (byte) 0x80, (byte) 0x81, (byte) 0x8A, (byte) 0x88, 0x7A,
                (byte) 0x8E, (byte) 0x90, 0x72, (byte) 0x92, (byte) 0x98, 0x6A, (byte) 0x96, (byte) 0xA1,
                0x62, (byte) 0x9A, (byte) 0xA9, 0x5A, (byte) 0x9E, (byte) 0xB1, 0x52, (byte) 0xA2,
                (byte) 0xBA, 0x4A, (byte) 0xA6, (byte) 0xC2, 0x42, (byte) 0xAA, (byte) 0xCA, 0x3A,
                (byte) 0xAE, (byte) 0xD3, 0x32, (byte) 0xB2, (byte) 0xDB, 0x2A, (byte) 0xB6, (byte) 0xE3,
                0x22, (byte) 0xBA, (byte) 0xEB, 0x1A, (byte) 0xBE, (byte) 0xF4, 0x12, (byte) 0xC2,
                (byte) 0xFC, 0x0A, (byte) 0xC6, (byte) 0xF5, 0x02, (byte) 0xCA, (byte) 0xE6, 0x09,
                (byte) 0xCE, (byte) 0xD8, 0x18, (byte) 0xD1, (byte) 0xCA, 0x27, (byte) 0xD5, (byte) 0xBB,
                0x36, (byte) 0xD8, (byte) 0xAD, 0x45, (byte) 0xDC, (byte) 0x9E, 0x54, (byte) 0xE0,
                (byte) 0x90, 0x62, (byte) 0xE3, (byte) 0x82, 0x6F, (byte) 0xE6, 0x71, 0x7C,
                (byte) 0xEA, 0x61, (byte) 0x89, (byte) 0xEE, 0x51, (byte) 0x96, (byte) 0xF2, 0x40,
                (byte) 0xA3, (byte) 0xF5, 0x30, (byte) 0xB0, (byte) 0xF9, 0x20, (byte) 0xBD, (byte) 0xFD,
                0x0F, (byte) 0xE3, (byte) 0xFF, 0x01, (byte) 0xE9, (byte) 0xFF, 0x01, (byte) 0xEE,
                (byte) 0xFF, 0x01, (byte) 0xF4, (byte) 0xFF, 0x00, (byte) 0xFA, (byte) 0xFF, 0x00,
                (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFF, (byte) 0xFF, 0x0A, (byte) 0xFF, (byte) 0xFF,
                0x15, (byte) 0xFF, (byte) 0xFF, 0x20, (byte) 0xFF, (byte) 0xFF, 0x2B, (byte) 0xFF,
                (byte) 0xFF, 0x36, (byte) 0xFF, (byte) 0xFF, 0x41, (byte) 0xFF, (byte) 0xFF, 0x4C,
                (byte) 0xFF, (byte) 0xFF, 0x57, (byte) 0xFF, (byte) 0xFF, 0x62, (byte) 0xFF, (byte) 0xFF,
                0x6D, (byte) 0xFF, (byte) 0xFF, 0x78, (byte) 0xFF, (byte) 0xFF, (byte) 0x81, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x8A, (byte) 0xFF, (byte) 0xFF, (byte) 0x92, (byte) 0xFF, (byte) 0xFF, (byte) 0x9A,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xA3, (byte) 0xFF, (byte) 0xFF, (byte) 0xAB, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xB4, (byte) 0xFF, (byte) 0xFF, (byte) 0xBC, (byte) 0xFF, (byte) 0xFF, (byte) 0xC4, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xCD, (byte) 0xFF, (byte) 0xFF, (byte) 0xD5, (byte) 0xFF, (byte) 0xFF, (byte) 0xDD,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xE6, (byte) 0xFF, (byte) 0xFF, (byte) 0xEE, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xF7, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xF9,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xF3, (byte) 0xFF, (byte) 0xFF, (byte) 0xED, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xE7, (byte) 0xFF, (byte) 0xFF, (byte) 0xE1, (byte) 0xFF, (byte) 0xFF, (byte) 0xDB, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xD5, (byte) 0xFF, (byte) 0xFF, (byte) 0xCF, (byte) 0xFF, (byte) 0xFF, (byte) 0xCA,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xC4, (byte) 0xFF, (byte) 0xFF, (byte) 0xBE, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xB8, (byte) 0xFF, (byte) 0xFF, (byte) 0xB2, (byte) 0xFF, (byte) 0xFF, (byte) 0xAC, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xA6, (byte) 0xFF, (byte) 0xFF, (byte) 0xA0, (byte) 0xFF, (byte) 0xFF, (byte) 0x9B,
                (byte) 0xFF, (byte) 0xFF, (byte) 0x96, (byte) 0xFF, (byte) 0xFF, (byte) 0x90, (byte) 0xFF, (byte) 0xFF,
                (byte) 0x8B, (byte) 0xFF, (byte) 0xFF, (byte) 0x86, (byte) 0xFF, (byte) 0xFF, (byte) 0x81, (byte) 0xFF,
                (byte) 0xFF, 0x7B, (byte) 0xFF, (byte) 0xFF, 0x76, (byte) 0xFF, (byte) 0xFF, 0x71,
                (byte) 0xFF, (byte) 0xFF, 0x6B, (byte) 0xFF, (byte) 0xFF, 0x66, (byte) 0xFF, (byte) 0xFF,
                0x61, (byte) 0xFF, (byte) 0xFF, 0x5C, (byte) 0xFF, (byte) 0xFF, 0x56, (byte) 0xFF,
                (byte) 0xFF, 0x51, (byte) 0xFF, (byte) 0xFF, 0x4C, (byte) 0xFF, (byte) 0xFF, 0x47,
                (byte) 0xFF, (byte) 0xFF, 0x41, (byte) 0xF9, (byte) 0xFF, 0x40, (byte) 0xF0, (byte) 0xFF,
                0x40, (byte) 0xE8, (byte) 0xFF, 0x40, (byte) 0xDF, (byte) 0xFF, 0x40, (byte) 0xD7,
                (byte) 0xFF, 0x40, (byte) 0xCF, (byte) 0xFF, 0x40, (byte) 0xC6, (byte) 0xFF, 0x40,
                (byte) 0xBE, (byte) 0xFF, 0x40, (byte) 0xB5, (byte) 0xFF, 0x40, (byte) 0xAD, (byte) 0xFF,
                0x40, (byte) 0xA4, (byte) 0xFF, 0x40, (byte) 0x9C, (byte) 0xFF, 0x40, (byte) 0x95,
                (byte) 0xFF, 0x40, (byte) 0x8D, (byte) 0xFF, 0x40, (byte) 0x86, (byte) 0xFF, 0x40,
                0x7E, (byte) 0xFF, 0x40, 0x77, (byte) 0xFF, 0x40, 0x6F, (byte) 0xFF,
                0x40, 0x68, (byte) 0xFF, 0x40, 0x60, (byte) 0xFF, 0x40, 0x59,
                (byte) 0xFF, 0x40, 0x51, (byte) 0xFF, 0x40, 0x4A, (byte) 0xFA, 0x43,
                0x42, (byte) 0xF3, 0x48, 0x3E, (byte) 0xED, 0x4E, 0x3D, (byte) 0xE6,
                0x53, 0x3B, (byte) 0xDF, 0x58, 0x39, (byte) 0xD8, 0x5E, 0x37,
                (byte) 0xD2, 0x63, 0x35, (byte) 0xCB, 0x68, 0x34, (byte) 0xC4, 0x6D,
                0x32, (byte) 0xBD, 0x73, 0x30, (byte) 0xB7, 0x78, 0x2E, (byte) 0xB0,
                0x7D, 0x2D, (byte) 0xA9, (byte) 0x83, 0x2B, (byte) 0xA2, (byte) 0x88, 0x29,
                (byte) 0x9C, (byte) 0x8D, 0x27, (byte) 0x95, (byte) 0x92, 0x25, (byte) 0x8E, (byte) 0x98,
                0x24, (byte) 0x87, (byte) 0x9D, 0x22, (byte) 0x81, (byte) 0xA2, 0x20, 0x7A,
                (byte) 0xA6, 0x1E, 0x74, (byte) 0xAB, 0x1D, 0x6E, (byte) 0xB0, 0x1B,
                0x68, (byte) 0xB5, 0x1A, 0x61, (byte) 0xB9, 0x18, 0x5B, (byte) 0xBE,
                0x17, 0x55, (byte) 0xC3, 0x15, 0x4F, (byte) 0xC8, 0x13, 0x48,
                (byte) 0xCD, 0x12, 0x42, (byte) 0xD1, 0x10, 0x3C, (byte) 0xD6, 0x0F,
                0x36, (byte) 0xDB, 0x0D, 0x2F, (byte) 0xE0, 0x0C, 0x29, (byte) 0xE4,
                0x0A, 0x23, (byte) 0xE9, 0x08, 0x1D, (byte) 0xEE, 0x07, 0x16,
                (byte) 0xF3, 0x05, 0x10, (byte) 0xF7, 0x04, 0x0A, (byte) 0xFC, 0x02,
                0x04, (byte) 0xFB, 0x01, 0x04, (byte) 0xEF, 0x00, 0x12, (byte) 0xE4,
                0x00, 0x1F, (byte) 0xD9, 0x00, 0x2D, (byte) 0xCE, 0x00, 0x3B,
                (byte) 0xC2, 0x00, 0x48, (byte) 0xB7, 0x00, 0x56, (byte) 0xAC, 0x00,
                0x64, (byte) 0xA0, 0x00, 0x72, (byte) 0x95, 0x00, 0x7F, (byte) 0x8A,
                0x00, (byte) 0x88, 0x7F, 0x00, (byte) 0x92, 0x75, 0x00, (byte) 0x9C,
                0x6B, 0x00, (byte) 0xA6, 0x61, 0x00, (byte) 0xB0, 0x57, 0x00,
                (byte) 0xBA, 0x4E, 0x00, (byte) 0xC4, 0x44, 0x00, (byte) 0xCE, 0x3A,
                0x00, (byte) 0xD8, 0x30, 0x00, (byte) 0xE2, 0x27, 0x00, (byte) 0xEC,
                0x1D, 0x00, (byte) 0xF6, 0x13, 0x00, (byte) 0xFF, 0x09, 0x00
        };

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(palette.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(palette);
        byteBuffer.position(0);

        // Create 2D texture with height 1 to mimic 1D texture behavior
        GLES32.glTexImage2D(
                GLES32.GL_TEXTURE_2D,
                0,
                GLES32.GL_RGB,
                palette.length / 4,
                1, // simulating 1D texture as 2D
                0,
                GLES32.GL_RGB,
                GLES32.GL_UNSIGNED_BYTE,
                byteBuffer);

        // Set texture parameters for filtering and wrapping
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    public int load2DTexture(int imageResourceID) {
        // Set up bitmap factory options to avoid scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;  // Ensure the image is not scaled

        // Decode the image resource into a bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageResourceID, options);

        // Generate a texture ID
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);

        // Set pixel storage mode
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Texture filtering and mipmap setup
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        // Load the bitmap data into the texture
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);

        // Generate mipmaps for the texture
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);

        // Release the bitmap to free memory
        bitmap.recycle();

        // Unbind the texture to avoid accidental modification
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    public int load2DTextureArray(int[] imageResourceIDs) {
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Bitmap factory options to avoid scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        // Load each image as a bitmap
        Bitmap[] bitmaps = new Bitmap[imageResourceIDs.length];
        int width = 0, height = 0;
        for (int i = 0; i < imageResourceIDs.length; i++) {
            bitmaps[i] = BitmapFactory.decodeResource(context.getResources(), imageResourceIDs[i], options);
            if (i == 0) {
                width = bitmaps[i].getWidth();
                height = bitmaps[i].getHeight();
            }
        }

        // Create the 3D texture with the specified depth
        GLES32.glTexImage3D(GLES32.GL_TEXTURE_2D_ARRAY, 0, GLES32.GL_RGBA, width, height, imageResourceIDs.length,
                0, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, null);

        // Upload each 2D slice to the 3D texture
        for (int i = 0; i < bitmaps.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bitmaps[i].getByteCount());
            buffer.order(ByteOrder.nativeOrder());
            bitmaps[i].copyPixelsToBuffer(buffer);
            buffer.position(0);

            GLES32.glTexSubImage3D(GLES32.GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, width, height, 1,
                    GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, buffer);

            bitmaps[i].recycle(); // Free memory for each bitmap after use
        }

        // Set texture parameters
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D_ARRAY);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, 0);

        return texture[0];
    }

    public int generate2DTextureArray() {
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        final int TEXTURE_SIZE = 32; // Texture dimension, 32x32
        final int NUM_TEXTURES = 384; // Total number of texture (for the 2D array)
        ByteBuffer texData = ByteBuffer.allocateDirect(TEXTURE_SIZE * TEXTURE_SIZE * 4); // 4 bytes per pixel (RGBA)
        texData.order(ByteOrder.nativeOrder());
        IntBuffer mutatedData = IntBuffer.allocate(TEXTURE_SIZE * TEXTURE_SIZE);
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            for (int j = 0; j < TEXTURE_SIZE; j++) {
                byte value = (byte) ((i ^ j) << 3);
                texData.put(i * TEXTURE_SIZE * 4 + j * 4, value);   // R
                texData.put(i * TEXTURE_SIZE * 4 + j * 4 + 1, value); // G
                texData.put(i * TEXTURE_SIZE * 4 + j * 4 + 2, value); // B
                texData.put(i * TEXTURE_SIZE * 4 + j * 4 + 3, (byte) 255); // A (fully opaque)
            }
        }

        GLES32.glTexStorage3D(GLES32.GL_TEXTURE_2D_ARRAY, 1, GLES32.GL_RGBA8, TEXTURE_SIZE, TEXTURE_SIZE, NUM_TEXTURES);
        for (int i = 0; i < NUM_TEXTURES; i++) {
            // Generate random value for mutation
            int r = ((int) (Math.random() * Integer.MAX_VALUE) & 0xFCFF3F) << (int) ((Math.random() * Integer.MAX_VALUE) % 12);

            // Apply mutation to texData for each texture layer
            for (int j = 0; j < TEXTURE_SIZE * TEXTURE_SIZE; j++) {
                int originalValue = texData.get(j * 4) & 0xFF;   // R value
                int greenValue = texData.get(j * 4 + 1) & 0xFF;  // G value
                int blueValue = texData.get(j * 4 + 2) & 0xFF;   // B value
                int alphaValue = texData.get(j * 4 + 3) & 0xFF;  // A value (alpha)

                // Convert RGBA bytes to an unsigned integer representation (like unsigned int)
                int texDataValue = (originalValue << 24) | (greenValue << 16) | (blueValue << 8) | alphaValue;
                int mutatedValue = (texDataValue & r) | 0x20202020;
                mutatedData.put(j, mutatedValue);
            }

            // Upload the mutated data to the appropriate layer of the texture array
            GLES32.glTexSubImage3D(GLES32.GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, TEXTURE_SIZE, TEXTURE_SIZE, 1, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, mutatedData);
            GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);
        }

        // Set texture parameters
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D_ARRAY);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, 0);

        return texture[0];
    }

    public int loadCubeMapTexture(int[] imageResourceIDs) {
        // Create texture object
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_CUBE_MAP, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Bitmap factory options to avoid scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        Bitmap bitmap;
        for (int i = 0; i < imageResourceIDs.length; i++) {
            // Load bitmap for each cube face
            bitmap = BitmapFactory.decodeResource(context.getResources(), imageResourceIDs[i], options);

            // Upload the bitmap to the appropriate cube map face
            GLUtils.texImage2D(GLES32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bitmap, 0);

            // Recycle the bitmap to free memory
            bitmap.recycle();
        }

        // Generate mipmaps for the cube map texture as a whole
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_CUBE_MAP);

        // Set texture filtering parameters for the cube map
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_CUBE_MAP, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_CUBE_MAP, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        // Unbind the cube map texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_CUBE_MAP, 0);

        // Return the generated texture ID
        return texture[0];
    }

    public int load3DTexture(int[] imageResourceIDs) {
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_3D, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Bitmap factory options to avoid scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        // Load each image as a bitmap
        Bitmap[] bitmaps = new Bitmap[imageResourceIDs.length];
        int width = 0, height = 0;
        for (int i = 0; i < imageResourceIDs.length; i++) {
            bitmaps[i] = BitmapFactory.decodeResource(context.getResources(), imageResourceIDs[i], options);
            if (i == 0) {
                width = bitmaps[i].getWidth();
                height = bitmaps[i].getHeight();
            }
        }

        // Create the 3D texture with the specified depth
        GLES32.glTexImage3D(GLES32.GL_TEXTURE_3D, 0, GLES32.GL_RGBA, width, height, imageResourceIDs.length,
                0, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, null);

        // Upload each 2D slice to the 3D texture
        for (int i = 0; i < bitmaps.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bitmaps[i].getByteCount());
            buffer.order(ByteOrder.nativeOrder());
            bitmaps[i].copyPixelsToBuffer(buffer);
            buffer.position(0);

            GLES32.glTexSubImage3D(GLES32.GL_TEXTURE_3D, 0, 0, 0, i, width, height, 1,
                    GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, buffer);

            bitmaps[i].recycle(); // Free memory for each bitmap after use
        }

        // Set texture parameters
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_3D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_3D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_3D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_3D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_3D, GLES32.GL_TEXTURE_WRAP_R, GLES32.GL_CLAMP_TO_EDGE);

        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_3D);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_3D, 0);

        return texture[0];
    }

}

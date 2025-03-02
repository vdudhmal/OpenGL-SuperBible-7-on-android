package com.vnd.superbible7.chapter14;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.OpenGLInfo;
import com.vnd.superbible7.common.Timer;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x6_PersistentMappedFractal extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    // Fractal dimensions
    final int FRACTAL_WIDTH = 512;
    final int FRACTAL_HEIGHT = 512;
    final byte[][][] fractalImage = new byte[FRACTAL_HEIGHT][FRACTAL_WIDTH][4];
    final FractalParams fractalParams = new FractalParams();
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[1];
    private final Timer timer = new Timer();
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x6_PersistentMappedFractal(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // Event related
        // create and set gestureDetector object
        gestureDetector = new GestureDetector(context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);
    }

    // implementation of 3 methods of GLSurfaceView.renderer interface
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initialize(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        display();
        update();
    }

    // implementation of onTouch event of ViewClass
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!gestureDetector.onTouchEvent(event)) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    // implementation of 3 methods of onDoubleTap listener interface
    @Override
    public boolean onDoubleTap(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        return true;
    }

    // implementation of 6 methods of onGesture listener interface
    @Override
    public boolean onDown(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, @NonNull MotionEvent event2, float velocityX, float velocityY) {
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent event) {
    }

    @Override
    public boolean onScroll(MotionEvent event1, @NonNull MotionEvent event2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent event) {
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent event) {
        return true;
    }

    // implementation of private methods
    private void initialize(GL10 gl) {
        // code
        OpenGLInfo.print(gl);

        // vertex shader
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "out vec2 oTextureCoordinates;" +
                        "void main(void)" +
                        "{" +
                        "vec2 pos = vec2(float(gl_VertexID & 2), float(gl_VertexID * 2 & 2));" +
                        "oTextureCoordinates = pos * 0.5;" +
                        "gl_Position = vec4(pos - vec2(1.0), 0.0, 1.0);" +
                        "}";
        int vertexShaderObject = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject, vertexShaderSourceCode);
        GLES32.glCompileShader(vertexShaderObject);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
        GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec2 oTextureCoordinates;" +
                        "uniform sampler2D uTextureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = texture(uTextureSampler, oTextureCoordinates);" +
                        "}";
        int fragmentShaderObject = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject, fragmentShaderSourceCode);
        GLES32.glCompileShader(fragmentShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject, vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
        GLES32.glLinkProgram(shaderProgramObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        uniforms = new Uniforms();
        uniforms.loadUniformLocations(shaderProgramObject);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // loading images to create texture
        updateTexture();

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);
    }

    private void resize(int width, int height) {
        // code
        if (height <= 0) {
            height = 1;
        }

        if (width <= 0) {
            width = 1;
        }

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // Clear the color buffer
        final float[] gray = {0.75f, 0.75f, 0.75f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, gray, 0);

        double currentTime = timer.getTotalTime(); // Assuming getCurrentTime() is implemented and returns time in seconds.
        float nowTime = (float) currentTime;

        // Update fractal parameters
        fractalParams.C[0] = (1.5f - (float) Math.cos(nowTime * 0.4f) * 0.5f) * 0.3f;
        fractalParams.C[1] = (1.5f + (float) Math.cos(nowTime * 0.5f) * 0.5f) * 0.3f;
        fractalParams.offset[0] = (float) Math.cos(nowTime * 0.14f) * 0.25f;
        fractalParams.offset[1] = (float) Math.cos(nowTime * 0.25f) * 0.25f;
        fractalParams.zoom = ((float) Math.sin(nowTime) + 1.3f) * 0.7f;

        // Update texture with new fractal data
        updateTexture(); // Assuming this function is implemented to handle texture updates

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Draw the fractal quad
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        // Stop using the program
        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    /**
     * @noinspection EmptyMethod
     */
    private void update() {
    }

    // Java method to update texture
    void updateTexture() {
        updateFractal(); // Placeholder for the standard version

        // Create a new texture
        int[] texture1 = new int[1];
        GLES32.glGenTextures(1, texture1, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture1[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // Set texture parameters
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);

        // Convert fractalImage array to ByteBuffer
        ByteBuffer fractalImageBuffer = ByteBuffer.allocateDirect(FRACTAL_WIDTH * FRACTAL_HEIGHT * 4);
        fractalImageBuffer.put(flattenFractalImage(fractalImage));
        fractalImageBuffer.position(0);

        // Upload texture to GPU
        GLES32.glTexImage2D(
                GLES32.GL_TEXTURE_2D,
                0,
                GLES32.GL_RGBA,
                FRACTAL_WIDTH,
                FRACTAL_HEIGHT,
                0,
                GLES32.GL_RGBA,
                GLES32.GL_UNSIGNED_BYTE,
                fractalImageBuffer
        );

        // Unbind the texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        // Delete the old texture if it exists
        if (texture[0] != 0) {
            GLES32.glDeleteTextures(1, texture, 0);
        }

        // Update the texture handle
        texture[0] = texture1[0];
    }

    // Helper method to flatten the fractal image array into a byte array
    private byte[] flattenFractalImage(byte[][][] fractalImage) {
        byte[] flattened = new byte[FRACTAL_WIDTH * FRACTAL_HEIGHT * 4];
        int index = 0;
        for (int y = 0; y < FRACTAL_HEIGHT; y++) {
            for (int x = 0; x < FRACTAL_WIDTH; x++) {
                flattened[index++] = fractalImage[y][x][0];
                flattened[index++] = fractalImage[y][x][1];
                flattened[index++] = fractalImage[y][x][2];
                flattened[index++] = fractalImage[y][x][3];
            }
        }
        return flattened;
    }

    public void uninitialize() {
        // code
        if (texture[0] > 0) {
            GLES32.glDeleteTextures(1, texture, 0);
            texture[0] = 0;
        }
        if (shaderProgramObject > 0) {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject);

            // Delete attached shaders
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }

            // Stop using the program
            GLES32.glUseProgram(0);

            // Delete the program
            GLES32.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }
    }

    public void updateFractal() {
        final float threshSquared = 256.0f;

        for (int y = 0; y < FRACTAL_HEIGHT; y++) {
            for (int x = 0; x < FRACTAL_WIDTH; x++) {
                // Initialize Z
                float[] Z = new float[2];
                Z[0] = fractalParams.zoom * ((float) x / FRACTAL_WIDTH - 0.5f) + fractalParams.offset[0];
                Z[1] = fractalParams.zoom * ((float) y / FRACTAL_HEIGHT - 0.5f) + fractalParams.offset[1];

                int iteration;
                for (iteration = 0; iteration < 256; iteration++) {
                    // Compute Z squared
                    float[] ZSquared = new float[2];
                    ZSquared[0] = Z[0] * Z[0] - Z[1] * Z[1];
                    ZSquared[1] = 2.0f * Z[0] * Z[1];

                    // Update Z with Z squared + C
                    Z[0] = ZSquared[0] + fractalParams.C[0];
                    Z[1] = ZSquared[1] + fractalParams.C[1];

                    // Check if the magnitude squared exceeds threshold
                    if ((Z[0] * Z[0] + Z[1] * Z[1]) > threshSquared) {
                        break;
                    }
                }

                // Set the color for each pixel in fractalImage
                fractalImage[y][x][0] = (byte) iteration; // Red
                fractalImage[y][x][1] = (byte) iteration; // Green
                fractalImage[y][x][2] = (byte) iteration; // Blue
                fractalImage[y][x][3] = (byte) 255;       // Alpha
            }
        }
    }

    private static class Uniforms {
        private int textureSampler = -1;

        private void loadUniformLocations(int programId) {
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
        }
    }

    // Fractal parameters as a class to mimic the struct
    static class FractalParams {
        final float[] C = new float[2]; // vec2 represented as an array of size 2
        final float[] offset = new float[2]; // vec2 for offset
        float zoom;
    }
}

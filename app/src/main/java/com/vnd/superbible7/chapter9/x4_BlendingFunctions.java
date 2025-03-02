package com.vnd.superbible7.chapter9;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.Attributes;
import com.vnd.superbible7.common.OpenGLInfo;
import com.vnd.superbible7.common.Timer;
import com.vnd.superbible7.common.shapes.IndexedCube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x4_BlendingFunctions extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final int[] BLEND_FUNCS = {
            GLES32.GL_ZERO,
            GLES32.GL_ONE,
            GLES32.GL_SRC_COLOR,
            GLES32.GL_ONE_MINUS_SRC_COLOR,
            GLES32.GL_DST_COLOR,
            GLES32.GL_ONE_MINUS_DST_COLOR,
            GLES32.GL_SRC_ALPHA,
            GLES32.GL_ONE_MINUS_SRC_ALPHA,
            GLES32.GL_DST_ALPHA,
            GLES32.GL_ONE_MINUS_DST_ALPHA,
            GLES32.GL_CONSTANT_COLOR,
            GLES32.GL_ONE_MINUS_CONSTANT_COLOR,
            GLES32.GL_CONSTANT_ALPHA,
            GLES32.GL_ONE_MINUS_CONSTANT_ALPHA,
            GLES32.GL_SRC_ALPHA_SATURATE,
            //GLES32.GL_SRC1_COLOR,
            //GLES32.GL_ONE_MINUS_SRC1_COLOR,
            //GLES32.GL_SRC1_ALPHA,
            //GLES32.GL_ONE_MINUS_SRC1_ALPHA
    };
    private static final int NUM_BLEND_FUNCS = BLEND_FUNCS.length;
    private static final float X_SCALE = 20.0f / NUM_BLEND_FUNCS;
    private static final float Y_SCALE = 16.0f / NUM_BLEND_FUNCS;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;
    private IndexedCube indexedCube;

    public x4_BlendingFunctions(Context context) {
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
                        "in vec4 aVertex;" +
                        "uniform mat4 modelViewProjectionMatrix;" +
                        "out VS_OUT" +
                        "{" +
                        "vec4 color;" +
                        "} vs_out;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = modelViewProjectionMatrix * aVertex;" +
                        "vs_out.color = aVertex * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);" +
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
                        "in VS_OUT" +
                        "{" +
                        "vec4 color;" +
                        "} fs_in;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = fs_in.color;" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "aVertex");
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

        // Render objects geometry
        indexedCube = new IndexedCube();

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

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

        // Get time elapsed since the program started
        double currentTime = timer.getTotalTime();

        // Clear the color buffer
        final float[] orange = {0.6f, 0.4f, 0.1f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, orange, 0);

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendColor(0.2f, 0.5f, 0.7f, 0.5f);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        for (int j = 0; j < NUM_BLEND_FUNCS; j++) {
            for (int i = 0; i < NUM_BLEND_FUNCS; i++) {
                // Translation matrix
                float[] translationMatrix = new float[16];
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, 10.0f - X_SCALE * i, 7.5f - Y_SCALE * j, -22.5f);

                // Rotation matrices
                float[] rotationMatrix1 = new float[16];
                Matrix.setRotateM(rotationMatrix1, 0, (float) currentTime * -45.0f, 0.0f, 1.0f, 0.0f);

                float[] rotationMatrix2 = new float[16];
                Matrix.setRotateM(rotationMatrix2, 0, (float) currentTime * -21.0f, 1.0f, 0.0f, 0.0f);

                // Combine rotation matrices
                float[] rotationMatrix = new float[16];
                Matrix.multiplyMM(rotationMatrix, 0, rotationMatrix1, 0, rotationMatrix2, 0);

                // Model-View Matrix
                float[] modelViewMatrix = new float[16];
                Matrix.multiplyMM(modelViewMatrix, 0, translationMatrix, 0, rotationMatrix, 0);

                // Model-View-Projection Matrix
                float[] modelViewProjectionMatrix = new float[16];
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

                // Pass the MVP matrix to the shader
                GLES32.glUniformMatrix4fv(uniforms.modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);

                // Set blend function
                GLES32.glBlendFunc(BLEND_FUNCS[i], BLEND_FUNCS[j]);

                //  Draw cube
                indexedCube.draw();
            }
        }

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

    public void uninitialize() {
        // code
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

        // cube
        indexedCube.cleanup();
    }

    private static class Uniforms {
        private int modelViewProjectionMatrix = -1;

        private void loadUniformLocations(int programId) {
            modelViewProjectionMatrix = GLES32.glGetUniformLocation(programId, "modelViewProjectionMatrix");
        }
    }
}

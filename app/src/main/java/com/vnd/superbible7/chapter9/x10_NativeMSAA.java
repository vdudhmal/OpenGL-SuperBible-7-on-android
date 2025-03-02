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

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x10_NativeMSAA extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final Timer timer = new Timer();
    private final Uniforms[] uniforms = new Uniforms[2];
    boolean wireframe = false;
    private GestureDetector gestureDetector = null;
    private boolean manyCubes = false;
    private IndexedCube indexedCube;

    public x10_NativeMSAA(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);

        // Set up MSAA
        setEGLConfigChooser((egl, display) -> {
            int[] configSpec = {
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_SAMPLE_BUFFERS, 1, // Request a multisample buffer
                    EGL10.EGL_SAMPLES, 4,        // Number of samples (4x MSAA)
                    EGL10.EGL_NONE
            };

            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfig = new int[1];
            egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);

            if (numConfig[0] == 0) {
                // Fallback without MSAA if no config was found
                int[] fallbackConfigSpec = {
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_NONE
                };
                egl.eglChooseConfig(display, fallbackConfigSpec, configs, 1, numConfig);
            }
            return configs[0];
        });

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
        wireframe = !wireframe;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        manyCubes = !manyCubes;
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
                        "void main(void)" +
                        "{" +
                        "gl_Position = modelViewProjectionMatrix * aVertex;" +
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

        // geometry shader
        final String geometryShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "layout (triangles) in;" +
                        "layout (line_strip, max_vertices = 6) out;" +
                        "void main(void)" +
                        "{" +
                        "for (int i = 0; i < gl_in.length(); i++)" +
                        "{" +
                        "gl_Position = gl_in[i].gl_Position;" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[(i + 1) % 3].gl_Position;" +
                        "EmitVertex();" +
                        "EndPrimitive();" +
                        "}" +
                        "}";
        int geometryShaderObject = GLES32.glCreateShader(GLES32.GL_GEOMETRY_SHADER);
        GLES32.glShaderSource(geometryShaderObject, geometryShaderSourceCode);
        GLES32.glCompileShader(geometryShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(geometryShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(geometryShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(geometryShaderObject);
                System.out.println("VND: geometry shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vec4(1.0);" +
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
        shaderProgramObject[0] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[0], vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], geometryShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject[0], Attributes.VERTEX, "aVertex");
        GLES32.glLinkProgram(shaderProgramObject[0]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject[0], GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject[0], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject[0]);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        uniforms[0] = new Uniforms();
        uniforms[0].loadUniformLocations(shaderProgramObject[0]);

        // Shader program
        shaderProgramObject[1] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[1], vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject[1], fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject[1], Attributes.VERTEX, "aVertex");
        GLES32.glLinkProgram(shaderProgramObject[1]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject[1], GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject[1], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject[1]);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        uniforms[1] = new Uniforms();
        uniforms[1].loadUniformLocations(shaderProgramObject[1]);

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

        int[] samples = new int[1];
        GLES32.glGetIntegerv(GLES32.GL_SAMPLES, samples, 0);
        System.out.println("MSAA Number of samples: " + samples[0]);

        // GLES32.glEnable(GLES32.GL_MULTISAMPLE);

        if (manyCubes) {
            for (int i = 0; i < 24; i++) {
                float f = (float) i + (float) currentTime * 0.3f;

                // Apply transformations
                float[] modelViewMatrix = new float[16];
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -6.0f);
                Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 21.0f, 1.0f, 0.0f, 0.0f);
                Matrix.translateM(modelViewMatrix, 0,
                        (float) Math.sin(2.1f * f) * 2.0f,
                        (float) Math.cos(1.7f * f) * 2.0f,
                        (float) Math.sin(1.3f * f) * (float) Math.cos(1.5f * f) * 2.0f);

                // Pass modelViewProjectionMatrix to the shader
                float[] modelViewProjectionMatrix = new float[16];
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

                if (wireframe) {
                    GLES32.glLineWidth(3.0f);
                }

                // Use the program
                GLES32.glUseProgram(shaderProgramObject[wireframe ? 0 : 1]);

                // Pass data to vertex shader
                GLES32.glUniformMatrix4fv(uniforms[wireframe ? 0 : 1].modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);

                //  Draw cube
                indexedCube.draw();
            }
        } else {
            float f = (float) currentTime * 0.3f;

            // Apply transformations
            float[] modelViewMatrix = new float[16];
            Matrix.setIdentityM(modelViewMatrix, 0);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -4.0f);
            Matrix.translateM(modelViewMatrix, 0, (float) Math.sin(2.1f * f) * 0.5f,
                    (float) Math.cos(1.7f * f) * 0.5f,
                    (float) Math.sin(1.3f * f) * (float) Math.cos(1.5f * f) * 2.0f);
            Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 81.0f, 1.0f, 0.0f, 0.0f);

            float[] modelViewProjectionMatrix = new float[16];
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

            if (wireframe) {
                GLES32.glLineWidth(3.0f);
            }

            // Use the program
            GLES32.glUseProgram(shaderProgramObject[wireframe ? 0 : 1]);

            // Pass data to vertex shader
            GLES32.glUniformMatrix4fv(uniforms[wireframe ? 0 : 1].modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);

            //  Draw cube
            indexedCube.draw();
        }

        // Stop using the program
        GLES32.glUseProgram(0);

        // GLES32.glDisable(GLES32.GL_MULTISAMPLE);

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
        if (shaderProgramObject[0] > 0) {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[0]);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject[0], GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject[0], numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject[0], pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            // Stop using the program
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject[0]);
            shaderProgramObject[0] = 0;
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

package com.vnd.superbible7.chapter13;

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
import com.vnd.superbible7.common.TextureHelper;
import com.vnd.superbible7.common.Timer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x13_JuliaFractal extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    final float time_offset = 0.0f;
    final float zoom = 1.0f;
    final float x_offset = 0.0f;
    final float y_offset = 0.0f;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[3];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x13_JuliaFractal(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // Event related
        // create and set gestureDetector object
        gestureDetector = new GestureDetector(context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);

        // Texture related
        textureHelper = new TextureHelper(context);
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
                        "uniform float zoom;" +
                        "uniform vec2 offset;" +
                        "out vec2 initial_z;" +
                        "void main(void)" +
                        "{" +
                        "const vec4 vertices[] = vec4[](" +
                        "vec4(-1.0, -1.0, 0.5, 1.0)," +
                        "vec4( 1.0, -1.0, 0.5, 1.0)," +
                        "vec4( 1.0, 1.0, 0.5, 1.0)," +
                        "vec4(-1.0, 1.0, 0.5, 1.0));" +
                        "initial_z = (vertices[gl_VertexID].xy * zoom) + offset;" +
                        "gl_Position = vertices[gl_VertexID];" +
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
                        "in vec2 initial_z;" +
                        "out vec4 fragColor;" +
                        "uniform vec2 C;" +
                        "uniform sampler2D textureSampler;" +
                        "void main(void){" +
                        "vec2 Z = initial_z;" +
                        "int iterations = 0;" +
                        "const float threshold_squared = 16.0;" +
                        "const int max_iterations = 256;" +
                        "while (iterations < max_iterations && dot(Z, Z) < threshold_squared) {" +
                        "vec2 Z_squared;" +
                        "Z_squared.x = Z.x * Z.x - Z.y * Z.y;" +
                        "Z_squared.y = 2.0 * Z.x * Z.y;" +
                        "Z = Z_squared + C;" +
                        "iterations++;" +
                        "}" +
                        "if (iterations == max_iterations) {" +
                        "fragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
                        "} else {" +
                        "fragColor = texture(textureSampler, vec2(float(iterations) / float(max_iterations), 0.0));" +
                        "}" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.NORMAL, "aNormal");
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
        texture[0] = textureHelper.generateJuliaTexture();

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

        // Get time elapsed since the program started
        double currentTime = 0.0;
        boolean paused = false;
        if (!paused) {
            currentTime = timer.getTotalTime();
        }
        float r = (float) (currentTime + time_offset);
        float[] C = {(float) ((Math.sin(r * 0.1f) + Math.cos(r * 0.23f)) * 0.5f), (float) ((Math.cos(r * 0.13f) + Math.sin(r * 0.21f)) * 0.5f)};
        float[] offset = {x_offset, y_offset};

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Pass data to vertex shader
        GLES32.glUniform2fv(uniforms.C, 1, C, 0);
        GLES32.glUniform2fv(uniforms.offset, 1, offset, 0);
        GLES32.glUniform1f(uniforms.zoom, zoom);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Draw screen
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, 4);

        // Unbind texture
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

    private static class Uniforms {
        private int zoom = -1;
        private int offset = -1;
        private int C = -1;
        private int textureSampler = -1;

        private void loadUniformLocations(int programId) {
            zoom = GLES32.glGetUniformLocation(programId, "zoom");
            offset = GLES32.glGetUniformLocation(programId, "offset");
            C = GLES32.glGetUniformLocation(programId, "C");
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
        }
    }
}

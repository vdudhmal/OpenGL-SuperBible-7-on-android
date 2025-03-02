package com.vnd.superbible7.chapter8;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x10_QuadRendering extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final Timer timer = new Timer();
    private final Uniforms[] uniforms = new Uniforms[2];
    boolean paused;
    boolean enable_gs;
    private GestureDetector gestureDetector = null;

    public x10_QuadRendering(Context context) {
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
        enable_gs = !enable_gs;
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
        paused = !paused;
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

        String[] vertexShaderSourceCode = new String[2];
        int[] vertexShaderObject = new int[2];
        String[] fragmentShaderSourceCode = new String[2];
        int[] fragmentShaderObject = new int[2];

        // vertex shader
        vertexShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        "uniform mat4 modelViewProjectionMatrix;" +
                        "uniform int vid_offset;" +
                        "out vec4 vsoColor;" +
                        "void main(void)" +
                        "{" +
                        "const vec4 vertices[] = vec4[](" +
                        "vec4(-0.5, -0.5, 0.0, 1.0)," +
                        "vec4( 0.5, -0.5, 0.0, 1.0)," +
                        "vec4( 0.5,  0.5, 0.0, 1.0)," +
                        "vec4(-0.5,  0.5, 0.0, 1.0));" +
                        "const vec4 colors[] = vec4[](" +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(1.0, 1.0, 1.0, 1.0));" +
                        "gl_Position = modelViewProjectionMatrix * vertices[(gl_VertexID + vid_offset) % 4];" +
                        "vsoColor = colors[(gl_VertexID + vid_offset) % 4];" +
                        "}";
        vertexShaderObject[0] = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject[0], vertexShaderSourceCode[0]);
        GLES32.glCompileShader(vertexShaderObject[0]);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
        GLES32.glGetShaderiv(vertexShaderObject[0], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject[0], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject[0]);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // geometry shader
        final String geometryShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "layout (lines_adjacency) in;" +
                        "layout (triangle_strip, max_vertices = 6) out;" +
                        "in vec4 vsoColor[4];" +
                        "flat out vec4 gsoColor[4];" +
                        "out vec2 gsoUv;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = gl_in[0].gl_Position;" +
                        "gsoUv = vec2(1.0, 0.0);" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[1].gl_Position;" +
                        "gsoUv = vec2(0.0, 0.0);" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[2].gl_Position;" +
                        "gsoUv = vec2(0.0, 1.0);" +
                        "const int idx0 = 0;" +
                        "const int idx1 = 1;" +
                        "const int idx2 = 2;" +
                        "const int idx3 = 3;" +
                        // We're only writing the output color for the last
                        // vertex here because they're flat attributes,
                        // and the last vertex is the provoking vertex by default
                        "gsoColor[0] = vsoColor[idx0];" +
                        "gsoColor[1] = vsoColor[idx1];" +
                        "gsoColor[2] = vsoColor[idx2];" +
                        "gsoColor[3] = vsoColor[idx3];" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[0].gl_Position;" +
                        "gsoUv = vec2(1.0, 0.0);" +
                        "gsoColor[0] = vsoColor[idx0];" +
                        "gsoColor[1] = vsoColor[idx1];" +
                        "gsoColor[2] = vsoColor[idx2];" +
                        "gsoColor[3] = vsoColor[idx3];" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[2].gl_Position;" +
                        "gsoUv = vec2(0.0, 1.0);" +
                        "gsoColor[0] = vsoColor[idx0];" +
                        "gsoColor[1] = vsoColor[idx1];" +
                        "gsoColor[2] = vsoColor[idx2];" +
                        "gsoColor[3] = vsoColor[idx3];" +
                        "EmitVertex();" +
                        "gl_Position = gl_in[3].gl_Position;" +
                        "gsoUv = vec2(1.0, 1.0);" +
                        "gsoColor[0] = vsoColor[idx0];" +
                        "gsoColor[1] = vsoColor[idx1];" +
                        "gsoColor[2] = vsoColor[idx2];" +
                        "gsoColor[3] = vsoColor[idx3];" +
                        "EmitVertex();" +
                        "EndPrimitive();" +
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
        fragmentShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "flat in vec4 gsoColor[4];" +
                        "in vec2 gsoUv;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "vec4 c1 = mix(gsoColor[0], gsoColor[1], gsoUv.x);" +
                        "vec4 c2 = mix(gsoColor[2], gsoColor[3], gsoUv.x);" +
                        "fragColor = mix(c1, c2, gsoUv.y);" +
                        "}";
        fragmentShaderObject[0] = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject[0], fragmentShaderSourceCode[0]);
        GLES32.glCompileShader(fragmentShaderObject[0]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject[0], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject[0], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject[0]);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject[0] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[0], vertexShaderObject[0]);
        GLES32.glAttachShader(shaderProgramObject[0], geometryShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject[0]);
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

        // vertex shader
        vertexShaderSourceCode[1] =
                "#version 320 es" +
                        "\n" +
                        "uniform mat4 modelViewProjectionMatrix;" +
                        "uniform int vid_offset;" +
                        "out vec4 vsoColor;" +
                        "void main(void)" +
                        "{" +
                        "const vec4 vertices[] = vec4[](" +
                        "vec4(-0.5, -0.5, 0.0, 1.0)," +
                        "vec4( 0.5, -0.5, 0.0, 1.0)," +
                        "vec4( 0.5,  0.5, 0.0, 1.0)," +
                        "vec4(-0.5,  0.5, 0.0, 1.0));" +
                        "const vec4 colors[] = vec4[](" +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(0.0, 0.0, 0.0, 1.0)," +
                        "vec4(1.0, 1.0, 1.0, 1.0));" +
                        "gl_Position = modelViewProjectionMatrix * vertices[(gl_VertexID + vid_offset) % 4];" +
                        "vsoColor = colors[(gl_VertexID + vid_offset) % 4];" +
                        "}";
        vertexShaderObject[1] = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject[1], vertexShaderSourceCode[1]);
        GLES32.glCompileShader(vertexShaderObject[1]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(vertexShaderObject[1], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject[1], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject[1]);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        fragmentShaderSourceCode[1] =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec4 vsoColor;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vsoColor;" +
                        "}";
        fragmentShaderObject[1] = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject[1], fragmentShaderSourceCode[1]);
        GLES32.glCompileShader(fragmentShaderObject[1]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject[1], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject[1], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject[1]);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject[1] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[1], vertexShaderObject[1]);
        GLES32.glAttachShader(shaderProgramObject[1], fragmentShaderObject[1]);
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

        double currentTime = 0.0;
        if (!paused) {
            currentTime = timer.getTotalTime();
        }

        // Clear the color buffer
        final float[] green = {0.0f, 0.25f, 0.0f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, green, 0);

        // Transformation
        float[] modelViewMatrix = new float[16];
        float[] mvpMatrix = new float[16];

        // Create the model view matrix
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -2.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 5.0f, 0.0f, 0.0f, 1.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 30.0f, 1.0f, 0.0f, 0.0f);

        // Multiply the projection matrix by the model view matrix to get the final mvp matrix
        Matrix.multiplyMM(mvpMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

        // Set the appropriate shader program and uniforms based on enable_gs
        if (enable_gs) {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[0]);

            // Push the mvp matrix into the vertex shader's mvp uniform
            GLES32.glUniformMatrix4fv(uniforms[0].modelViewProjectionMatrix, 1, false, mvpMatrix, 0);
            GLES32.glUniform1i(uniforms[0].vid_offset, 0);

            // Bind the vertex array and draw
            GLES32.glDrawArrays(GLES32.GL_LINES_ADJACENCY, 0, 4);

        } else {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[1]);

            // Push the mvp matrix into the vertex shader's mvp uniform
            GLES32.glUniformMatrix4fv(uniforms[1].modelViewProjectionMatrix, 1, false, mvpMatrix, 0);
            GLES32.glUniform1i(uniforms[1].vid_offset, 0);

            // Bind the vertex array and draw
            GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, 4);

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
    }

    private static class Uniforms {
        private int modelViewProjectionMatrix = -1;
        private int vid_offset = -1;

        private void loadUniformLocations(int programId) {
            modelViewProjectionMatrix = GLES32.glGetUniformLocation(programId, "modelViewProjectionMatrix");
            vid_offset = GLES32.glGetUniformLocation(programId, "vid_offset");
        }
    }
}

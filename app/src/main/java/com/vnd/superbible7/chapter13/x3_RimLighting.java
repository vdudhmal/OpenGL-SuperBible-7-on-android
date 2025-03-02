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
import com.vnd.superbible7.common.Timer;
import com.vnd.superbible7.common.shapes.Torus;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x3_RimLighting extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] rim_color = {1.0f, 1.0f, 0.0f};
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    float rim_power = 2.5f;
    private Torus torus;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x3_RimLighting(Context context) {
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
        rim_power /= 1.5f;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        rim_power *= 1.5f;
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
                        "in vec3 aNormal;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "vec3 uLightPosition = vec3(100.0, 100.0, 100.0);" +
                        "out vec3 oN;" +
                        "out vec3 oL;" +
                        "out vec3 oV;" +
                        "void main(void)" +
                        "{" +
                        // Calculate view-space coordinate
                        "vec4 P = modelViewMatrix * aVertex;" +
                        // Calculate normal in view-space
                        "oN = mat3(modelViewMatrix) * aNormal;" +
                        // Calculate light vector
                        "oL = uLightPosition - P.xyz;" +
                        // Calculate view vector
                        "oV = -P.xyz;" +
                        // Calculate the clip-space position of each vertex
                        "gl_Position = projectionMatrix * P;" +
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
                        "in vec3 oN;" +
                        "in vec3 oL;" +
                        "in vec3 oV;" +
                        "vec3 diffuse_albedo = vec3(0.3, 0.5, 0.2);" +
                        "vec3 specular_albedo = vec3(0.7);" +
                        "float specular_power = 128.0;" +
                        "uniform vec3 rim_color;" +
                        "uniform float rim_power;" +
                        "out vec4 fragColor;" +
                        "vec3 calculate_rim(vec3 N, vec3 V)" +
                        "{" +
                        "float f = 1.0 - dot(N, V);" +
                        "f = smoothstep(0.0, 1.0, f);" +
                        "f = pow(f, rim_power);" +
                        "return f * rim_color;" +
                        "}" +
                        "void main(void)" +
                        "{" +
                        // Normalize the incoming N, L and V vectors
                        "vec3 N = normalize(oN);" +
                        "vec3 L = normalize(oL);" +
                        "vec3 V = normalize(oV);" +
                        // Calculate R locally
                        "vec3 R = reflect(-L, N);" +
                        // Compute the diffuse and specular components for each fragment
                        "vec3 diffuse = max(dot(N, L), 0.0) * diffuse_albedo;" +
                        "vec3 specular = pow(max(dot(R, V), 0.0), specular_power) * specular_albedo;" +
                        "vec3 rim = calculate_rim(N, V);" +
                        "fragColor = vec4(diffuse + specular + rim, 1.0);" +
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

        // Render objects geometry
        torus = new Torus();

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
        float f = (float) currentTime;

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Apply transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -7.5f);
        Matrix.rotateM(modelViewMatrix, 0, f * 50.0f, 0.0f, 1.0f, 0.0f);

        // Pass data to vertex shader
        GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
        GLES32.glUniform3fv(uniforms.rim_color, 1, rim_color, 0);
        GLES32.glUniform1f(uniforms.rim_power, rim_power);

        // Draw torus
        torus.draw();

        // Stop using the program
        GLES32.glUseProgram(0);

        GLES32.glDisable(GLES32.GL_BLEND);

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

        // torus
        torus.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int rim_color = -1;
        private int rim_power = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            rim_color = GLES32.glGetUniformLocation(programId, "rim_color");
            rim_power = GLES32.glGetUniformLocation(programId, "rim_power");
        }
    }
}

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
import com.vnd.superbible7.common.shapes.Sphere;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_PhongLighting extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final Uniforms[] uniforms = new Uniforms[2];
    private boolean vertexShaderEnabled = false;
    private boolean manyObjects = false;
    private GestureDetector gestureDetector = null;
    private Sphere sphere;

    public x1_PhongLighting(Context context) {
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
        manyObjects = !manyObjects;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        vertexShaderEnabled = !vertexShaderEnabled;
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
        final String vertexShaderSourceCode_pv =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aVertex;" +
                        "in vec3 aNormal;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "vec3 uLightPosition = vec3(100.0, 100.0, 100.0);" +
                        "vec3 diffuse_albedo = vec3(0.5, 0.2, 0.7);" +
                        "uniform vec3 specular_albedo;" +
                        "uniform float specular_power;" +
                        "vec3 ambient = vec3(0.1, 0.1, 0.1);" +
                        "out vec3 oColor;" +
                        "void main(void)" +
                        "{" +
                        // Calculate view-space coordinate
                        "vec4 P = modelViewMatrix * aVertex;" +
                        // Calculate normal in view space
                        "vec3 N = mat3(modelViewMatrix) * aNormal;" +
                        // Calculate view-space light vector
                        "vec3 L = uLightPosition - P.xyz;" +
                        // Calculate view vector (simply the negative of the view-space position)
                        "vec3 V = -P.xyz;" +
                        // Normalize all three vectors
                        "N = normalize(N);" +
                        "L = normalize(L);" +
                        "V = normalize(V);" +
                        // Calculate R by reflecting -L around the plane defined by N
                        "vec3 R = reflect(-L, N);" +
                        // Calculate the diffuse and specular contributions
                        "vec3 diffuse = max(dot(N, L), 0.0) * diffuse_albedo;" +
                        "vec3 specular = pow(max(dot(R, V), 0.0), specular_power) * specular_albedo;" +
                        // Send the color output to the fragment shader
                        "oColor = ambient + diffuse + specular;" +
                        // Calculate the clip-space position of each vertex
                        "gl_Position = projectionMatrix * P;" +
                        "}";
        int vertexShaderObject_pv = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject_pv, vertexShaderSourceCode_pv);
        GLES32.glCompileShader(vertexShaderObject_pv);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
        GLES32.glGetShaderiv(vertexShaderObject_pv, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject_pv, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject_pv);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode_pv =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 oColor;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vec4(oColor, 1.0f);" +
                        "}";
        int fragmentShaderObject_pv = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject_pv, fragmentShaderSourceCode_pv);
        GLES32.glCompileShader(fragmentShaderObject_pv);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject_pv, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject_pv, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject_pv);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject[0] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[0], vertexShaderObject_pv);
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject_pv);
        GLES32.glBindAttribLocation(shaderProgramObject[0], Attributes.VERTEX, "aVertex");
        GLES32.glBindAttribLocation(shaderProgramObject[0], Attributes.NORMAL, "aNormal");
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
        final String vertexShaderSourceCode_pf =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
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
        int vertexShaderObject_pf = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject_pf, vertexShaderSourceCode_pf);
        GLES32.glCompileShader(vertexShaderObject_pf);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(vertexShaderObject_pf, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject_pf, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject_pf);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode_pf =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 oN;" +
                        "in vec3 oL;" +
                        "in vec3 oV;" +
                        "vec3 diffuse_albedo = vec3(0.5, 0.2, 0.7);" +
                        "uniform vec3 specular_albedo;" +
                        "uniform float specular_power;" +
                        "vec3 ambient = vec3(0.1, 0.1, 0.1);" +
                        "out vec4 fragColor;" +
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
                        // Write final color to the framebuffer
                        "fragColor = vec4(ambient + diffuse + specular, 1.0);" +
                        "}";
        int fragmentShaderObject_pf = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject_pf, fragmentShaderSourceCode_pf);
        GLES32.glCompileShader(fragmentShaderObject_pf);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject_pf, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject_pf, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject_pf);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject[1] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[1], vertexShaderObject_pf);
        GLES32.glAttachShader(shaderProgramObject[1], fragmentShaderObject_pf);
        GLES32.glBindAttribLocation(shaderProgramObject[1], Attributes.VERTEX, "aVertex");
        GLES32.glBindAttribLocation(shaderProgramObject[1], Attributes.NORMAL, "aNormal");
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
        sphere = new Sphere();

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE); // better for embedded

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

        final float[] gray = {0.1f, 0.1f, 0.1f, 0.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, gray, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject[vertexShaderEnabled ? 0 : 1]);

        if (!manyObjects) {
            float[] modelMatrix = new float[16];
            Matrix.setIdentityM(modelMatrix, 0);

            float[] viewMatrix = new float[16];
            Matrix.setLookAtM(viewMatrix, 0,
                    0.0f, 0.0f, 1.5f,   // Eye position
                    0.0f, 0.0f, 0.0f,   // Center of the scene
                    0.0f, 1.0f, 0.0f);  // Up direction

            // Combine model and view matrices to create the modelViewMatrix
            float[] modelViewMatrix = new float[16];
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);

            // push above mvp into vertex shaders mvp uniform
            GLES32.glUniformMatrix4fv(uniforms[vertexShaderEnabled ? 0 : 1].modelViewMatrix, 1, false, modelViewMatrix, 0);
            GLES32.glUniformMatrix4fv(uniforms[vertexShaderEnabled ? 0 : 1].projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
            final float[] specular_albedo = {0.7f, 0.7f, 0.7f, 1.0f};
            GLES32.glUniform3fv(uniforms[vertexShaderEnabled ? 0 : 1].specular_albedo, 1, specular_albedo, 0);
            GLES32.glUniform1f(uniforms[vertexShaderEnabled ? 0 : 1].specular_power, 128.0f);

            // sphere
            sphere.draw();
        } else {
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 6; i++) {
                    // transformations
                    float[] modelMatrix = new float[16];
                    Matrix.setIdentityM(modelMatrix, 0);
                    Matrix.translateM(modelMatrix, 0, (float) i * 2.0f - 5.0f, 2.5f - (float) j * 1.5f, 0.0f);

                    // Set up the view matrix using LookAt
                    float[] viewMatrix = new float[16];
                    Matrix.setLookAtM(viewMatrix, 0,
                            0.0f, 0.0f, 8.0f,   // Eye position
                            0.0f, 0.0f, 0.0f,   // Center of the scene
                            0.0f, 1.0f, 0.0f);  // Up direction

                    // Combine model and view matrices to create the modelViewMatrix
                    float[] modelViewMatrix = new float[16];
                    Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);

                    // push above mvp into vertex shaders mvp uniform
                    GLES32.glUniformMatrix4fv(uniforms[vertexShaderEnabled ? 0 : 1].modelViewMatrix, 1, false, modelViewMatrix, 0);
                    GLES32.glUniformMatrix4fv(uniforms[vertexShaderEnabled ? 0 : 1].projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
                    final float albedo_value = ((float) i / 9.0f) + (1.0f / 9.0f);
                    final float[] specular_albedo = {albedo_value, albedo_value, albedo_value};
                    GLES32.glUniform3fv(uniforms[vertexShaderEnabled ? 0 : 1].specular_albedo, 1, specular_albedo, 0);
                    GLES32.glUniform1f(uniforms[vertexShaderEnabled ? 0 : 1].specular_power, (float) Math.pow(2.0f, (float) j + 2.0f));

                    // sphere
                    sphere.draw();
                }
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
        int NO_PROGRAMS = 2;
        for (int i = 0; i < NO_PROGRAMS; i++) {
            if (shaderProgramObject[i] > 0) {
                // Use the program
                GLES32.glUseProgram(shaderProgramObject[i]);
                int[] retVal = new int[1];
                GLES32.glGetProgramiv(shaderProgramObject[i], GLES32.GL_ATTACHED_SHADERS, retVal, 0);
                int numShaders = retVal[0];
                if (numShaders > 0) {
                    int[] pShaders = new int[numShaders];
                    GLES32.glGetAttachedShaders(shaderProgramObject[i], numShaders, retVal, 0, pShaders, 0);
                    for (int j = 0; j < numShaders; j++) {
                        GLES32.glDetachShader(shaderProgramObject[i], pShaders[j]);
                        GLES32.glDeleteShader(pShaders[j]);
                        pShaders[j] = 0;
                    }
                }
                // Stop using the program
                GLES32.glUseProgram(0);
                GLES32.glDeleteProgram(shaderProgramObject[i]);
                shaderProgramObject[i] = 0;
            }
        }

        // sphere
        sphere.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int specular_albedo = -1;
        private int specular_power = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            specular_albedo = GLES32.glGetUniformLocation(programId, "specular_albedo");
            specular_power = GLES32.glGetUniformLocation(programId, "specular_power");
        }
    }
}

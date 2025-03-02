package com.vnd.superbible7.chapter8;

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
import com.vnd.superbible7.common.shapes.Tetrahedron;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x8_GeometryShaderTessellation extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    int shaderProgramObject;
    private Tetrahedron tetrahedron;
    private GestureDetector gestureDetector = null;
    private Uniforms uniforms;

    public x8_GeometryShaderTessellation(Context context) {
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
                        "void main(void)" +
                        "{" +
                        "gl_Position = aVertex;" +
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
                        "layout (triangle_strip, max_vertices = 12) out;" +
                        "uniform float stretch;" +
                        "flat out vec4 color;" +
                        "uniform mat4 projectionMatrix;" +
                        "uniform mat4 modelViewMatrix;" +
                        "void make_face(vec3 a, vec3 b, vec3 c)" +
                        "{" +
                        "vec3 face_normal = normalize(cross(c - a, c - b));" +
                        "vec4 face_color = vec4(1.0, 0.4, 0.7, 1.0) * (mat3(modelViewMatrix) * face_normal).z;" +
                        "gl_Position = projectionMatrix * modelViewMatrix * vec4(a, 1.0);" +
                        "color = face_color;" +
                        "EmitVertex();" +
                        "gl_Position = projectionMatrix * modelViewMatrix * vec4(b, 1.0);" +
                        "color = face_color;" +
                        "EmitVertex();" +
                        "gl_Position = projectionMatrix * modelViewMatrix * vec4(c, 1.0);" +
                        "color = face_color;" +
                        "EmitVertex();" +
                        "EndPrimitive();" +
                        "}" +
                        "void main(void)" +
                        "{" +
                        "int n;" +
                        "vec3 a = gl_in[0].gl_Position.xyz;" +
                        "vec3 b = gl_in[1].gl_Position.xyz;" +
                        "vec3 c = gl_in[2].gl_Position.xyz;" +
                        "vec3 d = (a + b) * stretch;" +
                        "vec3 e = (b + c) * stretch;" +
                        "vec3 f = (c + a) * stretch;" +
                        "a *= (2.0 - stretch);" +
                        "b *= (2.0 - stretch);" +
                        "c *= (2.0 - stretch);" +
                        "make_face(a, d, f);" +
                        "make_face(d, b, e);" +
                        "make_face(e, c, f);" +
                        "make_face(d, e, f);" +
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
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "flat in vec4 color;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = color;" +
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
        GLES32.glAttachShader(shaderProgramObject, geometryShaderObject);
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
        tetrahedron = new Tetrahedron();

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

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        float[] modelViewMatrix = new float[16];
        float[] rotationMatrixX = new float[16];
        float[] rotationMatrixY = new float[16];

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Initialize matrices
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.setIdentityM(rotationMatrixX, 0);
        Matrix.setIdentityM(rotationMatrixY, 0);

        // Translation transformation
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -5.0f);

        // Rotation around Y-axis
        Matrix.rotateM(rotationMatrixY, 0, (float) (currentTime * 71.0f), 0.0f, 1.0f, 0.0f);
        // Rotation around X-axis
        Matrix.rotateM(rotationMatrixX, 0, (float) (currentTime * 10.0f), 1.0f, 0.0f, 0.0f);

        // Combine transformations: modelViewMatrix * rotationMatrixY * rotationMatrixX
        Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, rotationMatrixY, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, rotationMatrixX, 0);

        // Pass data to vertex shader
        GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
        GLES32.glUniform1f(uniforms.stretch, (float) Math.sin(currentTime * 4.0f) * 0.75f + 1.0f);

        //  Draw tetrahedron
        tetrahedron.draw();

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

        // tetrahedron
        tetrahedron.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int stretch = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            stretch = GLES32.glGetUniformLocation(programId, "stretch");
        }
    }
}

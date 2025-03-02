package com.vnd.superbible7.chapter7;

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
import com.vnd.superbible7.common.shapes.Sphere;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x6_ClipDistance extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    private Sphere sphere;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x6_ClipDistance(Context context) {
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
                        "in vec3 aNormal;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "vec3 uLightPosition = vec3(100.0, 100.0, 100.0);" +
                        // Clip plane
                        "uniform vec4 clip_plane;" +
                        "uniform vec4 clip_sphere;" +
                        "out vec3 oN;" +
                        "out vec3 oL;" +
                        "out vec3 oV;" +
                        "out float clipDistance[2];" +
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
                        // Write clip distances
                        //"gl_ClipDistance[0] = dot(aVertex, clip_plane);" +
                        "clipDistance[0] = dot(aVertex, clip_plane);" +
                        //"gl_ClipDistance[1] = length(aVertex.xyz / aVertex.w - clip_sphere.xyz) - clip_sphere.w;" +
                        "clipDistance[1] = length(aVertex.xyz / aVertex.w - clip_sphere.xyz) - clip_sphere.w;" +
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
                        "in float clipDistance[2];" +
                        "vec3 diffuse_albedo = vec3(0.3, 0.5, 0.2);" +
                        "vec3 specular_albedo = vec3(0.7);" +
                        "float specular_power = 128.0;" +
                        "vec3 rim_color = vec3(0.1, 0.2, 0.2);" +
                        "float rim_power = 5.0;" +
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
                        // Discard if outside the clip plane
                        "if (clipDistance[0] < 0.0 || clipDistance[1] < 0.0) {" +
                        "discard;" +
                        "}" +
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
                        // Write final color to the framebuffer
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
        sphere = new Sphere();

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

        // Compute plane_matrix
        float[] planeMatrix = new float[16];
        Matrix.setIdentityM(planeMatrix, 0);
        Matrix.rotateM(planeMatrix, 0, f * 6.0f, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(planeMatrix, 0, f * 7.3f, 0.0f, 1.0f, 0.0f);

        // Extract the first column of the plane_matrix as a plane vector
        float[] plane = new float[4];
        plane[0] = planeMatrix[0]; // x
        plane[1] = planeMatrix[4]; // y
        plane[2] = planeMatrix[8]; // z
        plane[3] = 0.0f;           // w

        // Normalize the plane vector
        float planeMagnitude = (float) Math.sqrt(plane[0] * plane[0] + plane[1] * plane[1] + plane[2] * plane[2] + plane[3] * plane[3]);
        if (planeMagnitude != 0) {
            plane[0] /= planeMagnitude;
            plane[1] /= planeMagnitude;
            plane[2] /= planeMagnitude;
            plane[3] /= planeMagnitude;
        }

        // Compute clip_sphere
        float[] clipSphere = new float[4];
        clipSphere[0] = (float) Math.sin(f * 0.7f) * 3.0f; // x
        clipSphere[1] = (float) Math.cos(f * 1.9f) * 3.0f; // y
        clipSphere[2] = (float) Math.sin(f * 0.1f) * 3.0f; // z
        clipSphere[3] = (float) Math.cos(f * 1.7f) + 2.5f; // w

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Apply transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -1.5f);
        Matrix.rotateM(modelViewMatrix, 0, f * 0.34f, 0.0f, 1.0f, 0.0f);

        // Push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
        GLES32.glUniform4fv(uniforms.clip_plane, 1, plane, 0);
        GLES32.glUniform4fv(uniforms.clip_sphere, 1, clipSphere, 0);

        // Note: GL_CLIP_DISTANCE is not supported in OpenGL ES, so you may want to implement clipping in the shader instead
        // Enable clipping planes
        //GLES32.glEnable(GLES32.GL_CLIP_DISTANCE0);
        //GLES32.glEnable(GLES32.GL_CLIP_DISTANCE1);

        // Draw sphere
        sphere.draw();

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

        // sphere
        sphere.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int clip_plane = -1;
        private int clip_sphere = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            clip_plane = GLES32.glGetUniformLocation(programId, "clip_plane");
            clip_sphere = GLES32.glGetUniformLocation(programId, "clip_sphere");
        }
    }
}

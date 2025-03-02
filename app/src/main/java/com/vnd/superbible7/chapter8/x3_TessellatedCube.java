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
import com.vnd.superbible7.common.shapes.TessellatedCube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x3_TessellatedCube extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private TessellatedCube tessellatedCube;
    private boolean manyCubes = false;
    private Uniforms uniforms;

    public x3_TessellatedCube(Context context) {
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

        // tesselation control shader
        final String tcsShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (vertices = 4) out;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "void main(void)" +
                        "{" +
                        "vec4 pos[4];" +
                        "float edgeSubdivisions[4];" +
                        "float insideSubdivisions = 0.0;" +
                        "if (gl_InvocationID == 0)" +
                        "{" +
                        "for (int i = 0; i < 4; i++)" +
                        "{" +
                        "pos[i] = projectionMatrix * modelViewMatrix * gl_in[i].gl_Position;" +
                        "}" +
                        "edgeSubdivisions[0] = max(2.0, distance(pos[0].xy / pos[0].w, pos[1].xy / pos[1].w) * 16.0);" +
                        "edgeSubdivisions[1] = max(2.0, distance(pos[1].xy / pos[1].w, pos[3].xy / pos[3].w) * 16.0);" +
                        "edgeSubdivisions[2] = max(2.0, distance(pos[2].xy / pos[2].w, pos[3].xy / pos[3].w) * 16.0);" +
                        "edgeSubdivisions[3] = max(2.0, distance(pos[2].xy / pos[2].w, pos[0].xy / pos[0].w) * 16.0);" +
                        "for (int i = 0; i < 4; i++)" +
                        "{" +
                        "insideSubdivisions = max(insideSubdivisions, edgeSubdivisions[i]);" +
                        "}" +
                        "gl_TessLevelInner[0] = insideSubdivisions;" +
                        "gl_TessLevelInner[1] = insideSubdivisions;" +
                        "gl_TessLevelOuter[0] = edgeSubdivisions[0];" +
                        "gl_TessLevelOuter[1] = edgeSubdivisions[1];" +
                        "gl_TessLevelOuter[2] = edgeSubdivisions[2];" +
                        "gl_TessLevelOuter[3] = edgeSubdivisions[3];" +
                        "}" +
                        "gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;" +
                        "}";
        int tcsShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_CONTROL_SHADER);
        GLES32.glShaderSource(tcsShaderObject, tcsShaderSourceCode);
        GLES32.glCompileShader(tcsShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tcsShaderObject);
                System.out.println("VND: tcs shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        final String tesShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (quads, fractional_odd_spacing, ccw) in;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "out vec3 normal;" +
                        "void main(void)" +
                        "{" +
                        "vec4 mid1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);" +
                        "vec4 mid2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);" +
                        "vec4 pos = mix(mid1, mid2, gl_TessCoord.y);" +
                        "pos.xyz = /* normalize*/(pos.xyz) * 0.25;" +
                        "normal = normalize(mat3(modelViewMatrix) * pos.xyz);" +
                        "gl_Position = projectionMatrix * modelViewMatrix * pos;" +
                        "}";
        int tesShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject, tesShaderSourceCode);
        GLES32.glCompileShader(tesShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 normal;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vec4(abs(normal), 1.0);" +
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
        GLES32.glAttachShader(shaderProgramObject, tcsShaderObject);
        GLES32.glAttachShader(shaderProgramObject, tesShaderObject);
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
        tessellatedCube = new TessellatedCube();

        // Tell OpenGL by how many vertices one patch is created
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 4);

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
        final float[] green = {0.0f, 0.25f, 0.0f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, green, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        if (manyCubes) {
            for (int i = 0; i < 100; i++) {
                float f = (float) i + (float) currentTime * 0.03f;

                // Apply transformations
                float[] modelViewMatrix = new float[16];
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.0f);
                Matrix.translateM(modelViewMatrix, 0,
                        (float) (Math.sin(2.1f * f) * 4.0f),
                        (float) (Math.cos(1.7f * f) * 4.0f),
                        (float) (Math.sin(4.3f * f) * Math.cos(3.5f * f) * 30.0f));
                Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 3.0f, 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 5.0f, 0.0f, 1.0f, 0.0f);

                // Pass modelViewMatrix to the shader
                GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
                GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);

                //  Draw cube
                tessellatedCube.draw();
            }
        } else {
            float f = (float) currentTime * 0.03f;

            // Apply transformations
            float[] modelViewMatrix = new float[16];
            Matrix.setIdentityM(modelViewMatrix, 0);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.0f);
            Matrix.translateM(modelViewMatrix, 0,
                    0.0f,
                    0.0f,
                    (float) (Math.sin(1.3f * f) * Math.cos(1.5f * f) * 15.0f));
            Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 81.0f, 1.0f, 0.0f, 0.0f);

            // Pass modelViewMatrix to the shader
            GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
            GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);

            //  Draw cube
            tessellatedCube.draw();
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
        tessellatedCube.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
        }
    }
}

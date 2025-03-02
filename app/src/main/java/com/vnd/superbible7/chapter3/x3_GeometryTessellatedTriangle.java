package com.vnd.superbible7.chapter3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.OpenGLInfo;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x3_GeometryTessellatedTriangle extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] maxTesselationLevel = new int[1];
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private int inside1Subdivisions = 3;
    private int inside2Subdivisions = 0;

    private int edge1Subdivisions = 1;
    private int edge2Subdivisions = 1;
    private int edge3Subdivisions = 1;
    private int edge4Subdivisions = 0;
    private Uniforms uniforms;

    public x3_GeometryTessellatedTriangle(Context context) {
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
        edge1Subdivisions = (edge1Subdivisions + 1) % maxTesselationLevel[0];
        edge2Subdivisions = (edge2Subdivisions + 1) % maxTesselationLevel[0];
        edge3Subdivisions = (edge3Subdivisions + 1) % maxTesselationLevel[0];
        edge4Subdivisions = (edge4Subdivisions + 1) % maxTesselationLevel[0];
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        inside1Subdivisions = (inside1Subdivisions + 1) % maxTesselationLevel[0];
        inside2Subdivisions = (inside2Subdivisions + 1) % maxTesselationLevel[0];
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
                        "void main(void)" +
                        "{" +
                        "const vec4 vertices[] = vec4[](" +
                        "vec4( 0.5, -0.625, 0.5, 1.0)," +
                        "vec4(-0.5, -0.625, 0.5, 1.0)," +
                        "vec4( 0.5,  0.625, 0.5, 1.0));" +
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

        // tesselation control shader
        final String tcsShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (vertices = 3) out;" +
                        "uniform int inside1Subdivisions;" +
                        "uniform int inside2Subdivisions;" +
                        "uniform int edge1Subdivisions;" +
                        "uniform int edge2Subdivisions;" +
                        "uniform int edge3Subdivisions;" +
                        "uniform int edge4Subdivisions;" +
                        "void main(void)" +
                        "{" +
                        "if (gl_InvocationID == 0)" +
                        "{" +
                        // Only the first element of gl_TessLevelInner[] array is used, and
                        // this level is applied to the entirety of the inner area of the
                        // tessellated triangle.
                        "gl_TessLevelInner[0] = float(inside1Subdivisions);" +
                        "gl_TessLevelInner[1] = float(inside2Subdivisions);" + // doesnt do anything
                        // The first three elements of the gl_TessLevelOuter[] array are used
                        //  to set the tessellation factors for the three edges of the triangle.
                        "gl_TessLevelOuter[0] = float(edge1Subdivisions);" +
                        "gl_TessLevelOuter[1] = float(edge2Subdivisions);" +
                        "gl_TessLevelOuter[2] = float(edge3Subdivisions);" +
                        "gl_TessLevelOuter[3] = float(edge4Subdivisions);" + // doesnt do anything
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
                        // When the tessellation mode is set to triangles (again, using an
                        // input layout qualifier in the tessellation control shader), the
                        // tessellation engine produces a triangle that is then broken into
                        // many smaller triangles.
                        "layout (triangles, equal_spacing, cw) in;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) + (gl_TessCoord.y * gl_in[1].gl_Position) + (gl_TessCoord.z * gl_in[2].gl_Position);" +
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
                        "fragColor = vec4(0.0, 0.8, 1.0, 1.0);" +
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
        GLES32.glAttachShader(shaderProgramObject, geometryShaderObject);
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

        // Tell OpenGL by how many vertices one patch is created
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 3);

        // Get maximum tesselation level
        GLES32.glGetIntegerv(GLES32.GL_MAX_TESS_GEN_LEVEL, maxTesselationLevel, 0);
        System.out.println("VND: maxTesselationLevel: " + maxTesselationLevel[0]);

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

        // Clear the color buffer
        final float[] green = {0.0f, 0.25f, 0.0f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, green, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Pass data to vertex shader
        GLES32.glUniform1i(uniforms.inside1Subdivisions, inside1Subdivisions);
        GLES32.glUniform1i(uniforms.inside2Subdivisions, inside2Subdivisions);
        GLES32.glUniform1i(uniforms.edge1Subdivisions, edge1Subdivisions);
        GLES32.glUniform1i(uniforms.edge2Subdivisions, edge2Subdivisions);
        GLES32.glUniform1i(uniforms.edge3Subdivisions, edge3Subdivisions);
        GLES32.glUniform1i(uniforms.edge4Subdivisions, edge4Subdivisions);

        // Draw tessellated triangle
        GLES32.glLineWidth(3.0f);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, 3);

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
    }

    private static class Uniforms {
        private int inside1Subdivisions = -1;
        private int inside2Subdivisions = -1;
        private int edge1Subdivisions = -1;
        private int edge2Subdivisions = -1;
        private int edge3Subdivisions = -1;
        private int edge4Subdivisions = -1;

        private void loadUniformLocations(int programId) {
            inside1Subdivisions = GLES32.glGetUniformLocation(programId, "inside1Subdivisions");
            inside2Subdivisions = GLES32.glGetUniformLocation(programId, "inside2Subdivisions");
            edge1Subdivisions = GLES32.glGetUniformLocation(programId, "edge1Subdivisions");
            edge2Subdivisions = GLES32.glGetUniformLocation(programId, "edge2Subdivisions");
            edge3Subdivisions = GLES32.glGetUniformLocation(programId, "edge3Subdivisions");
            edge4Subdivisions = GLES32.glGetUniformLocation(programId, "edge4Subdivisions");
        }
    }
}

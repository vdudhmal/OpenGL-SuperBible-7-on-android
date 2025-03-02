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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_TessellationModes extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] maxTesselationLevel = new int[1];
    private final int[] shaderProgramObject = new int[4];
    private final int NO_PROGRAMS = 4;
    private final Uniforms[] uniforms = new Uniforms[NO_PROGRAMS];
    private GestureDetector gestureDetector = null;
    private int programIndex = 0;
    private int inside1Subdivisions = 6;
    private int inside2Subdivisions = 3;
    private int edge1Subdivisions = 8;
    private int edge2Subdivisions = 5;
    private int edge3Subdivisions = 3;
    private int edge4Subdivisions = 1;

    public x1_TessellationModes(Context context) {
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
        inside1Subdivisions = (inside1Subdivisions + 1) % maxTesselationLevel[0];
        inside2Subdivisions = (inside2Subdivisions + 1) % maxTesselationLevel[0];
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        programIndex = (programIndex + 1) % NO_PROGRAMS;
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
        edge1Subdivisions = (edge1Subdivisions + 1) % maxTesselationLevel[0];
        edge2Subdivisions = (edge2Subdivisions + 1) % maxTesselationLevel[0];
        edge3Subdivisions = (edge3Subdivisions + 1) % maxTesselationLevel[0];
        edge4Subdivisions = (edge4Subdivisions + 1) % maxTesselationLevel[0];
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
                        "vec4( 0.5,  0.625, 0.5, 1.0)," +
                        "vec4(-0.5,  0.625, 0.5, 1.0));" +
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
                        "layout (vertices = 4) out;" +
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
                        "gl_TessLevelInner[1] = float(inside2Subdivisions);" +
                        // The first three elements of the gl_TessLevelOuter[] array are used
                        //  to set the tessellation factors for the three edges of the triangle.
                        "gl_TessLevelOuter[0] = float(edge1Subdivisions);" +
                        "gl_TessLevelOuter[1] = float(edge2Subdivisions);" +
                        "gl_TessLevelOuter[2] = float(edge3Subdivisions);" +
                        "gl_TessLevelOuter[3] = float(edge4Subdivisions);" +
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

        String[] tesShaderSourceCode = new String[NO_PROGRAMS];
        int[] tesShaderObject = new int[NO_PROGRAMS];

        // tesselation evaluation shader
        tesShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        // When the tessellation mode is set to triangles (again, using an
                        // input layout qualifier in the tessellation control shader), the
                        // tessellation engine produces a triangle that is then broken into
                        // many smaller triangles.
                        "layout (isolines, equal_spacing, cw) in;" +
                        "void main(void)" +
                        "{" +
                        "vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);" +
                        "vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);" +
                        "gl_Position = mix(p1, p2, gl_TessCoord.y);" +
                        "}";
        tesShaderObject[0] = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject[0], tesShaderSourceCode[0]);
        GLES32.glCompileShader(tesShaderObject[0]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject[0], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject[0], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject[0]);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        tesShaderSourceCode[1] =
                "#version 320 es" +
                        "\n" +
                        // When the tessellation mode is set to triangles (again, using an
                        // input layout qualifier in the tessellation control shader), the
                        // tessellation engine produces a triangle that is then broken into
                        // many smaller triangles.
                        "layout (isolines) in;" +
                        "void main(void)" +
                        "{" +
                        "float r = (gl_TessCoord.y + gl_TessCoord.x / gl_TessLevelOuter[0]);" +
                        "float t = gl_TessCoord.x * 2.0 * 3.14159;" +
                        "gl_Position = vec4(sin(t) * r, cos(t) * r, 0.5, 1.0);" +
                        "}";
        tesShaderObject[1] = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject[1], tesShaderSourceCode[1]);
        GLES32.glCompileShader(tesShaderObject[1]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject[1], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject[1], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject[0]);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        tesShaderSourceCode[2] =
                "#version 320 es" +
                        "\n" +
                        // When the tessellation mode is set to triangles (again, using an
                        // input layout qualifier in the tessellation control shader), the
                        // tessellation engine produces a triangle that is then broken into
                        // many smaller triangles.
                        "layout (triangles) in;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) + (gl_TessCoord.y * gl_in[1].gl_Position) +(gl_TessCoord.z * gl_in[2].gl_Position);" +
                        "}";
        tesShaderObject[2] = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject[2], tesShaderSourceCode[2]);
        GLES32.glCompileShader(tesShaderObject[2]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject[2], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject[2], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject[0]);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        tesShaderSourceCode[3] =
                "#version 320 es" +
                        "\n" +
                        // When the tessellation mode is set to triangles (again, using an
                        // input layout qualifier in the tessellation control shader), the
                        // tessellation engine produces a triangle that is then broken into
                        // many smaller triangles.
                        "layout (quads) in;" +
                        "void main(void)" +
                        "{" +
                        "vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);" +
                        "vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);" +
                        "gl_Position = mix(p1, p2, gl_TessCoord.y);" +
                        "}";
        tesShaderObject[3] = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject[3], tesShaderSourceCode[3]);
        GLES32.glCompileShader(tesShaderObject[3]);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject[3], GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject[3], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject[3]);
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

        // Shader programs
        for (int i = 0; i < NO_PROGRAMS; i++) {
            shaderProgramObject[i] = GLES32.glCreateProgram();
            GLES32.glAttachShader(shaderProgramObject[i], vertexShaderObject);
            GLES32.glAttachShader(shaderProgramObject[i], tcsShaderObject);
            GLES32.glAttachShader(shaderProgramObject[i], tesShaderObject[i]);
            if (i == 2 || i == 3) {
                GLES32.glAttachShader(shaderProgramObject[i], geometryShaderObject);
            }
            GLES32.glAttachShader(shaderProgramObject[i], fragmentShaderObject);
            GLES32.glBindAttribLocation(shaderProgramObject[i], Attributes.VERTEX, "aVertex");
            GLES32.glLinkProgram(shaderProgramObject[i]);
            status[0] = 0;
            infoLogLength[0] = 0;
            GLES32.glGetProgramiv(shaderProgramObject[i], GLES32.GL_LINK_STATUS, status, 0);
            if (status[0] == GLES32.GL_FALSE) {
                GLES32.glGetProgramiv(shaderProgramObject[i], GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
                if (infoLogLength[0] > 0) {
                    szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject[i]);
                    System.out.println("VND: shader[" + i + "] program linking error log: " + szInfoLog);
                }
                uninitialize();
            }

            // get shader uniform locations - must be after linkage
            uniforms[i] = new Uniforms();
            uniforms[i].loadUniformLocations(shaderProgramObject[i]);
        }

        // Tell OpenGL by how many vertices one patch is created
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 4);

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

        // Use the program
        GLES32.glUseProgram(shaderProgramObject[programIndex]);

        // Pass data to vertex shader
        GLES32.glUniform1i(uniforms[programIndex].inside1Subdivisions, inside1Subdivisions);
        GLES32.glUniform1i(uniforms[programIndex].inside2Subdivisions, inside2Subdivisions);
        GLES32.glUniform1i(uniforms[programIndex].edge1Subdivisions, edge1Subdivisions);
        GLES32.glUniform1i(uniforms[programIndex].edge2Subdivisions, edge2Subdivisions);
        GLES32.glUniform1i(uniforms[programIndex].edge3Subdivisions, edge3Subdivisions);
        GLES32.glUniform1i(uniforms[programIndex].edge4Subdivisions, edge4Subdivisions);

        GLES32.glLineWidth(3.0f);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, 4);

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
        for (int i = 0; i < NO_PROGRAMS; i++) {
            if (shaderProgramObject[i] > 0) {
                // Use the program
                GLES32.glUseProgram(shaderProgramObject[i]);

                // Delete attached shaders
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

                // Delete the program
                GLES32.glDeleteProgram(shaderProgramObject[i]);
                shaderProgramObject[i] = 0;
            }
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

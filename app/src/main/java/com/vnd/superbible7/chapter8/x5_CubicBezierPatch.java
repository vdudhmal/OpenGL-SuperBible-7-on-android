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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x5_CubicBezierPatch extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int[] vbo_position_patch = new int[1];
    private final int[] vbo_index_patch = new int[1];
    private final int[] vao_patch = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final Timer timer = new Timer();
    private final Uniforms[] uniforms = new Uniforms[2];
    boolean show_cage = true;
    boolean show_points = true;
    boolean paused = false;
    private GestureDetector gestureDetector = null;

    public x5_CubicBezierPatch(Context context) {
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
        show_cage = !show_cage;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        show_points = !show_points;
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
                        "in vec4 aVertex;" +
                        "uniform mat4 modelViewMatrix;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = modelViewMatrix * aVertex;" +
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

        // tesselation control shader
        final String tcsShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (vertices = 16) out;" +
                        "void main(void)" +
                        "{" +
                        "if (gl_InvocationID == 0)" +
                        "{" +
                        "gl_TessLevelInner[0] = 16.0;" +
                        "gl_TessLevelInner[1] = 16.0;" +
                        "gl_TessLevelOuter[0] = 16.0;" +
                        "gl_TessLevelOuter[1] = 16.0;" +
                        "gl_TessLevelOuter[2] = 16.0;" +
                        "gl_TessLevelOuter[3] = 16.0;" +
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
                        "layout (quads, equal_spacing, cw) in;" +
                        "uniform mat4 projectionMatrix;" +
                        "out vec3 N;" +
                        "const float epsilon = 0.001;" +
                        "vec4 quadratic_bezier(vec4 A, vec4 B, vec4 C, float t)" +
                        "{" +
                        "vec4 D = mix(A, B, t);" +
                        "vec4 E = mix(B, C, t);" +
                        "return mix(D, E, t);" +
                        "}" +
                        "vec4 cubic_bezier(vec4 A, vec4 B, vec4 C, vec4 D, float t)" +
                        "{" +
                        "vec4 E = mix(A, B, t);" +
                        "vec4 F = mix(B, C, t);" +
                        "vec4 G = mix(C, D, t);" +
                        "return quadratic_bezier(E, F, G, t);" +
                        "}" +
                        "vec4 evaluate_patch(vec2 at)" +
                        "{" +
                        "vec4 P[4];" +
                        "for (int i = 0; i < 4; i++)" +
                        "{" +
                        "P[i] = cubic_bezier(gl_in[i + 0].gl_Position, gl_in[i + 4].gl_Position, gl_in[i + 8].gl_Position, gl_in[i + 12].gl_Position, at.y);" +
                        "}" +
                        "return cubic_bezier(P[0], P[1], P[2], P[3], at.x);" +
                        "}" +
                        "void main(void)" +
                        "{" +
                        "vec4 p1 = evaluate_patch(gl_TessCoord.xy);" +
                        "vec4 p2 = evaluate_patch(gl_TessCoord.xy + vec2(0.0, epsilon));" +
                        "vec4 p3 = evaluate_patch(gl_TessCoord.xy + vec2(epsilon, 0.0));" +
                        "vec3 v1 = normalize(p2.xyz - p1.xyz);" +
                        "vec3 v2 = normalize(p3.xyz - p1.xyz);" +
                        "N = cross(v1, v2);" +
                        "gl_Position = projectionMatrix * p1;" +
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
        fragmentShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 N;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "vec3 nn = normalize(N);" +
                        "vec4 c = vec4(1.0, -1.0, 0.0, 0.0) * nn.z + vec4(0.0, 0.0, 0.0, 1.0);" +
                        "fragColor = clamp(c, vec4(0.0), vec4(1.0));" +
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
        GLES32.glAttachShader(shaderProgramObject[0], tcsShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], tesShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject[0]);
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

        // vertex shader
        vertexShaderSourceCode[1] =
                "#version 320 es" +
                        "\n" +
                        "in vec4 aVertex;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "void main(void)" +
                        "{" +
                        "gl_PointSize = 20.0f;" +
                        "gl_Position = projectionMatrix * modelViewMatrix * aVertex;" +
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
                        "uniform vec4 draw_color;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = draw_color;" +
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

        // Define the vertex indices for the patch
        final short[] patch_index = {
                0, 1, 1, 2, 2, 3,
                4, 5, 5, 6, 6, 7,
                8, 9, 9, 10, 10, 11,
                12, 13, 13, 14, 14, 15,
                0, 4, 4, 8, 8, 12,
                1, 5, 5, 9, 9, 13,
                2, 6, 6, 10, 10, 14,
                3, 7, 7, 11, 11, 15
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_patch, 0);
        GLES32.glBindVertexArray(vao_patch[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_patch, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_patch[0]);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, 16 * 3 * 4, null, GLES32.GL_DYNAMIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for index - vertex buffer object
        GLES32.glGenBuffers(1, vbo_index_patch, 0);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_index_patch[0]);
        ByteBuffer byteBufferIndices = ByteBuffer.allocateDirect(patch_index.length * 2);
        byteBufferIndices.order(ByteOrder.nativeOrder());
        ShortBuffer shortBufferIndices = byteBufferIndices.asShortBuffer();
        shortBufferIndices.put(patch_index);
        shortBufferIndices.position(0);
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, patch_index.length * 2, shortBufferIndices, GLES32.GL_STATIC_DRAW);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

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
        float t = (float) currentTime;

        // Assuming you have a method for creating a look-at matrix
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -4.0f);
        Matrix.rotateM(modelViewMatrix, 0, t * 10.0f, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, t * 17.0f, 1.0f, 0.0f, 0.0f);
        final float[] gray = {0.1f, 0.1f, 0.1f, 0.0f};

        // Clear the color buffer
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, gray, 0);

        // Define the patch positions as a float array
        float[] patch_position = {
                -1.0f, -1.0f, 0.0f,
                -0.33f, -1.0f, 0.0f,
                0.33f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,

                -1.0f, -0.33f, 0.0f,
                -0.33f, -0.33f, 0.0f,
                0.33f, -0.33f, 0.0f,
                1.0f, -0.33f, 0.0f,

                -1.0f, 0.33f, 0.0f,
                -0.33f, 0.33f, 0.0f,
                0.33f, 0.33f, 0.0f,
                1.0f, 0.33f, 0.0f,

                -1.0f, 1.0f, 0.0f,
                -0.33f, 1.0f, 0.0f,
                0.33f, 1.0f, 0.0f,
                1.0f, 1.0f, 0.0f
        };

        // Modify the z-coordinate based on the sine function
        for (int i = 0; i < 16; i++) {
            float fi = (float) i / 16.0f;
            patch_position[i * 3 + 2] = (float) Math.sin(t * (0.2f + fi * 0.3f));
        }

        // vbo for position - vertex buffer object
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_patch[0]);
        ByteBuffer byteBufferCubePosition = ByteBuffer.allocateDirect(patch_position.length * 4);
        byteBufferCubePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubePosition = byteBufferCubePosition.asFloatBuffer();
        floatBufferCubePosition.put(patch_position);
        floatBufferCubePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, 16 * 3 * 4, floatBufferCubePosition, GLES32.GL_DYNAMIC_DRAW);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // show patch
        {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[0]);

            // Push matrices into the vertex shader uniforms
            GLES32.glUniformMatrix4fv(uniforms[0].modelViewMatrix, 1, false, modelViewMatrix, 0);
            GLES32.glUniformMatrix4fv(uniforms[0].projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);

            //  Draw patch
            GLES32.glBindVertexArray(vao_patch[0]);
            GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 16);
            GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_index_patch[0]);
            GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, 16);
            GLES32.glBindVertexArray(0);

            // Stop using the program
            GLES32.glUseProgram(0);
        }

        // show control points and cage
        {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[1]);

            // Push matrices into the vertex shader uniforms
            GLES32.glUniformMatrix4fv(uniforms[1].modelViewMatrix, 1, false, modelViewMatrix, 0);
            GLES32.glUniformMatrix4fv(uniforms[1].projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);

            if (show_points) {
                // Set color for control points
                GLES32.glUniform4fv(uniforms[1].draw_color, 1, new float[]{0.2f, 0.7f, 0.9f, 1.0f}, 0);

                // Bind VAO and draw points
                GLES32.glBindVertexArray(vao_patch[0]);
                GLES32.glDrawArrays(GLES32.GL_POINTS, 0, 16);
                GLES32.glBindVertexArray(0);
            }

            if (show_cage) {
                // Set color for cage
                GLES32.glUniform4fv(uniforms[1].draw_color, 1, new float[]{0.7f, 0.9f, 0.2f, 1.0f}, 0);

                // Bind VAO and draw lines for the cage
                GLES32.glBindVertexArray(vao_patch[0]);
                GLES32.glLineWidth(3.0f);
                GLES32.glDrawElements(GLES32.GL_LINES, 48, GLES32.GL_UNSIGNED_SHORT, 0);
                GLES32.glBindVertexArray(0);
            }

            // Stop using the program
            GLES32.glUseProgram(0);
        }

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

        // patch
        if (vbo_position_patch[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_patch, 0);
            vbo_position_patch[0] = 0;
        }
        if (vbo_index_patch[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_index_patch, 0);
            vbo_index_patch[0] = 0;
        }
        if (vao_patch[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_patch, 0);
            vao_patch[0] = 0;
        }
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int draw_color = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            draw_color = GLES32.glGetUniformLocation(programId, "draw_color");
        }
    }
}

package com.vnd.superbible7.chapter7;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.OpenGLInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x5_SpringMass extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final int POSITION_A = 0;
    private static final int POSITION_B = 1;
    private static final int VELOCITY_A = 2;
    private static final int VELOCITY_B = 3;
    private static final int CONNECTION = 4;
    private static final int POINTS_X = 50;
    private static final int POINTS_Y = 50;
    private static final int POINTS_TOTAL = POINTS_X * POINTS_Y;
    private static final int CONNECTIONS_TOTAL = (POINTS_X - 1) * POINTS_Y + (POINTS_Y - 1) * POINTS_X;
    final int[] m_vao = new int[2];
    final int[] m_vbo = new int[5];
    final int[] m_pos_tbo = new int[2];
    final int[] m_index_buffer = new int[1];
    final int iterations_per_frame = 16;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    int m_iteration_index = 0;
    private GestureDetector gestureDetector = null;

    private x5_SpringMass(Context context) {
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

        String[] vertexShaderSourceCode = new String[2];
        int[] vertexShaderObject = new int[2];
        String[] fragmentShaderSourceCode = new String[2];
        int[] fragmentShaderObject = new int[2];

        // vertex shader
        vertexShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        "layout (location = 0) in vec4 position_mass;" +
                        "layout (location = 1) in vec3 velocity;" +
                        "layout (location = 2) in ivec4 connection;" +
                        "uniform highp samplerBuffer tex_position;" +
                        "out vec4 tf_position_mass;" +
                        "out vec3 tf_velocity;" +
                        "float t = 0.07;" +
                        "float k = 7.1;" +
                        "const vec3 gravity = vec3(0.0, -0.08, 0.0);" +
                        "float c = 2.8;" +
                        "float rest_length = 0.88;" +
                        "void main(void)" +
                        "{" +
                        "vec3 p = position_mass.xyz;" +
                        "float m = position_mass.w;" +
                        "vec3 u = velocity;" +
                        "vec3 F = gravity * m - c * u;" +
                        "bool fixed_node = true;" +
                        "for (int i = 0; i < 4; i++) {" +
                        "if (connection[i] != -1) {" +
                        "vec3 q = texelFetch(tex_position, connection[i]).xyz;" +
                        "vec3 d = q - p;" +
                        "float x = length(d);" +
                        "F += -k * (rest_length - x) * normalize(d);" +
                        "fixed_node = false;" +
                        "}" +
                        "}" +
                        "if (fixed_node)" +
                        "{" +
                        "F = vec3(0.0);" +
                        "}" +
                        "vec3 a = F / m;" +
                        "vec3 s = u * t + 0.5 * a * t * t;" +
                        "vec3 v = u + a * t;" +
                        "s = clamp(s, vec3(-25.0), vec3(25.0));" +
                        "tf_position_mass = vec4(p + s, m);" +
                        "tf_velocity = v;" +
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

        // fragment shader
        fragmentShaderSourceCode[0] =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vec4(1.0);" +
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
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject[0]);
        String[] tfVaryings = {"tf_position_mass", "tf_velocity"};
        GLES32.glTransformFeedbackVaryings(shaderProgramObject[0], tfVaryings, GLES32.GL_SEPARATE_ATTRIBS);
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

        // vertex shader
        vertexShaderSourceCode[1] =
                "#version 320 es" +
                        "\n" +
                        "in vec3 aPosition;" +
                        "void main(void)" +
                        "{" +
                        "gl_PointSize = 5.0f;" +
                        "gl_Position = vec4(aPosition * 0.03, 1.0);" +
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
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = vec4(1.0);" +
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

        // Initialize data arrays
        float[] initial_positions = new float[POINTS_TOTAL * 4];  // 4 for vec4 (x, y, z, w)
        float[] initial_velocities = new float[POINTS_TOTAL * 3];  // 3 for vec3 (x, y, z)
        int[] connection_vectors = new int[POINTS_TOTAL * 4];  // 4 for ivec4 (connections)
        int n = 0;
        for (int j = 0; j < POINTS_Y; j++) {
            float fj = (float) j / POINTS_Y;
            for (int i = 0; i < POINTS_X; i++) {
                float fi = (float) i / POINTS_X;

                // Position initialization
                initial_positions[n * 4] = (fi - 0.5f) * POINTS_X;
                initial_positions[n * 4 + 1] = (fj - 0.5f) * POINTS_Y;
                initial_positions[n * 4 + 2] = 0.6f * (float) Math.sin(fi) * (float) Math.cos(fj);
                initial_positions[n * 4 + 3] = 1.0f;

                // Velocity initialization
                initial_velocities[n * 3] = 0.0f;
                initial_velocities[n * 3 + 1] = 0.0f;
                initial_velocities[n * 3 + 2] = 0.0f;

                // Connection vector initialization
                connection_vectors[n * 4] = -1;
                connection_vectors[n * 4 + 1] = -1;
                connection_vectors[n * 4 + 2] = -1;
                connection_vectors[n * 4 + 3] = -1;

                if (j != POINTS_Y - 1) {
                    if (i != 0)
                        connection_vectors[n * 4] = n - 1;

                    if (j != 0)
                        connection_vectors[n * 4 + 1] = n - POINTS_X;

                    if (i != POINTS_X - 1)
                        connection_vectors[n * 4 + 2] = n + 1;

                    connection_vectors[n * 4 + 3] = n + POINTS_X;
                }
                n++;
            }
        }

        GLES32.glGenVertexArrays(2, m_vao, 0);
        GLES32.glGenBuffers(5, m_vbo, 0);

        // Set buffers and vertex attribute pointers
        for (int i = 0; i < 2; i++) {
            GLES32.glBindVertexArray(m_vao[i]);

            // Position buffer
            ByteBuffer positionBuffer = ByteBuffer.allocateDirect(initial_positions.length * 4) // 4 bytes per float
                    .order(ByteOrder.nativeOrder());
            FloatBuffer positionFloatBuffer = positionBuffer.asFloatBuffer();
            positionFloatBuffer.put(initial_positions);
            positionFloatBuffer.flip(); // Prepare the buffer for reading
            GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, m_vbo[POSITION_A + i]);
            GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, positionBuffer.capacity(), positionBuffer, GLES32.GL_DYNAMIC_COPY);
            GLES32.glVertexAttribPointer(0, 4, GLES32.GL_FLOAT, false, 0, 0);
            GLES32.glEnableVertexAttribArray(0);

            // Velocity buffer
            ByteBuffer velocityBuffer = ByteBuffer.allocateDirect(initial_velocities.length * 4) // 4 bytes per float
                    .order(ByteOrder.nativeOrder());
            FloatBuffer velocityFloatBuffer = velocityBuffer.asFloatBuffer();
            velocityFloatBuffer.put(initial_velocities);
            velocityFloatBuffer.flip(); // Prepare the buffer for reading
            GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, m_vbo[VELOCITY_A + i]);
            GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, velocityBuffer.capacity(), velocityBuffer, GLES32.GL_DYNAMIC_COPY);
            GLES32.glVertexAttribPointer(1, 3, GLES32.GL_FLOAT, false, 0, 0);
            GLES32.glEnableVertexAttribArray(1);

            // Connection buffer
            ByteBuffer connectionBuffer = ByteBuffer.allocateDirect(connection_vectors.length * 4) // 4 bytes per int
                    .order(ByteOrder.nativeOrder());
            IntBuffer connectionIntBuffer = connectionBuffer.asIntBuffer();
            connectionIntBuffer.put(connection_vectors);
            connectionIntBuffer.flip(); // Prepare the buffer for reading
            GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, m_vbo[CONNECTION]);
            GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, connectionBuffer.capacity(), connectionBuffer, GLES32.GL_STATIC_DRAW);
            GLES32.glVertexAttribIPointer(2, 4, GLES32.GL_INT, 0, 0);
            GLES32.glEnableVertexAttribArray(2);
        }

        // Generate texture buffers
        GLES32.glGenTextures(2, m_pos_tbo, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, m_pos_tbo[0]);
        GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, GLES32.GL_RGBA32F, m_vbo[POSITION_A]);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, m_pos_tbo[1]);
        GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, GLES32.GL_RGBA32F, m_vbo[POSITION_B]);

        int lines = (POINTS_X - 1) * POINTS_Y + (POINTS_Y - 1) * POINTS_X;

        // Generate index buffer for connections
        GLES32.glGenBuffers(1, m_index_buffer, 0);
        IntBuffer indexBuffer = ByteBuffer.allocateDirect(lines * 2 * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
        for (int j = 0; j < POINTS_Y; j++) {
            for (int i = 0; i < POINTS_X - 1; i++) {
                indexBuffer.put(i + j * POINTS_X);
                indexBuffer.put(1 + i + j * POINTS_X);
            }
        }
        for (int i = 0; i < POINTS_X; i++) {
            for (int j = 0; j < POINTS_Y - 1; j++) {
                indexBuffer.put(i + j * POINTS_X);
                indexBuffer.put(POINTS_X + i + j * POINTS_X);
            }
        }
        indexBuffer.flip();
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, m_index_buffer[0]);
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Integer.BYTES, indexBuffer, GLES32.GL_STATIC_DRAW);

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

        // Use the update program for the transform feedback loop
        GLES32.glUseProgram(shaderProgramObject[0]);
        GLES32.glEnable(GLES32.GL_RASTERIZER_DISCARD);
        for (int i = iterations_per_frame; i != 0; --i) {
            GLES32.glBindVertexArray(m_vao[m_iteration_index & 1]);
            GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, m_pos_tbo[m_iteration_index & 1]);
            m_iteration_index++;
            GLES32.glBindBufferBase(GLES32.GL_TRANSFORM_FEEDBACK_BUFFER, 0, m_vbo[POSITION_A + (m_iteration_index & 1)]);
            GLES32.glBindBufferBase(GLES32.GL_TRANSFORM_FEEDBACK_BUFFER, 1, m_vbo[VELOCITY_A + (m_iteration_index & 1)]);
            GLES32.glBeginTransformFeedback(GLES32.GL_POINTS);
            GLES32.glDrawArrays(GLES32.GL_POINTS, 0, POINTS_TOTAL);
            GLES32.glEndTransformFeedback();
        }
        GLES32.glDisable(GLES32.GL_RASTERIZER_DISCARD);

        // Use the render program to display the points and lines
        GLES32.glUseProgram(shaderProgramObject[1]);
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, POINTS_TOTAL);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, m_index_buffer[0]);
        GLES32.glDrawElements(GLES32.GL_LINES, CONNECTIONS_TOTAL * 2, GLES32.GL_UNSIGNED_INT, 0);

        // render
        requestRender();
    }

    /**
     * @noinspection EmptyMethod
     */
    private void update() {
    }

    private void uninitialize() {
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
        // Clean up resources (optional)
        GLES32.glDeleteBuffers(5, m_vbo, 0);
        GLES32.glDeleteVertexArrays(2, m_vao, 0);
        GLES32.glDeleteTextures(2, m_pos_tbo, 0);
        GLES32.glDeleteBuffers(1, m_index_buffer, 0);
    }
}

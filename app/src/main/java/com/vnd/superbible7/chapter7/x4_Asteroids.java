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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x4_Asteroids extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final float[] vertices = {
            // Front face
            -1.0f, 1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,

            // Right face
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,

            // Back face
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,

            // Left face
            -1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,

            // Bottom face
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,

            // Top face
            -1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f
    };
    private static final float[] normals = {
            // Front face
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,

            // Right face
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,

            // Back face
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,

            // Left face
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,

            // Bottom face
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,

            // Top face
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f
    };
    private final int NUM_DRAWS = 50000;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] vao = new int[1];
    private final int[] vbos = new int[Attributes.MAX];
    private final Timer timer = new Timer();
    private final int[] indirectBuffer = new int[1];
    private Uniforms uniforms;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private boolean useIndirect = true; // vs useInstanced

    public x4_Asteroids(Context context) {
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
        useIndirect = !useIndirect;
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
                        "in vec3 position_3;" +
                        "in vec3 normal;" +
                        "in uint draw_id;" +
                        "out VS_OUT" +
                        "{" +
                        "vec3 normal;" +
                        "vec4 color;" +
                        "} vs_out;" +
                        "uniform float time;" +
                        "uniform mat4 view_matrix;" +
                        "uniform mat4 viewproj_matrix;" +
                        "const vec4 color0 = vec4(0.29, 0.21, 0.18, 1.0);" +
                        "const vec4 color1 = vec4(0.58, 0.55, 0.51, 1.0);" +
                        "void main(void)" +
                        "{" +
                        "vec4 position = vec4(position_3, 1.0);" +
                        "mat4 m1;" +
                        "mat4 m2;" +
                        "mat4 m;" +
                        "float t = time * 0.1;" +
                        "float f = float(draw_id) / 30.0;" +
                        "float st = sin(t * 0.5 + f * 5.0);" +
                        "float ct = cos(t * 0.5 + f * 5.0);" +
                        "float j = fract(f);" +
                        "float d = cos(j * 3.14159);" +
                        // Rotate around Y
                        "m[0] = vec4(ct, 0.0, st, 0.0);" +
                        "m[1] = vec4(0.0, 1.0, 0.0, 0.0);" +
                        "m[2] = vec4(-st, 0.0, ct, 0.0);" +
                        "m[3] = vec4(0.0, 0.0, 0.0, 1.0);" +
                        // Translate in the XZ plane
                        "m1[0] = vec4(1.0, 0.0, 0.0, 0.0);" +
                        "m1[1] = vec4(0.0, 1.0, 0.0, 0.0);" +
                        "m1[2] = vec4(0.0, 0.0, 1.0, 0.0);" +
                        "m1[3] = vec4(260.0 + 30.0 * d, 5.0 * sin(f * 123.123), 0.0, 1.0);" +
                        "m = m * m1;" +
                        // Rotate around X
                        "st = sin(t * 2.1 * (600.0 + f) * 0.01);" +
                        "ct = cos(t * 2.1 * (600.0 + f) * 0.01);" +
                        "m1[0] = vec4(ct, st, 0.0, 0.0);" +
                        "m1[1] = vec4(-st, ct, 0.0, 0.0);" +
                        "m1[2] = vec4(0.0, 0.0, 1.0, 0.0);" +
                        "m1[3] = vec4(0.0, 0.0, 0.0, 1.0);" +
                        "m = m * m1;" +
                        // Rotate around Z
                        "st = sin(t * 1.7 * (700.0 + f) * 0.01);" +
                        "ct = cos(t * 1.7 * (700.0 + f) * 0.01);" +
                        "m1[0] = vec4(1.0, 0.0, 0.0, 0.0);" +
                        "m1[1] = vec4(0.0, ct, st, 0.0);" +
                        "m1[2] = vec4(0.0, -st, ct, 0.0);" +
                        "m1[3] = vec4(0.0, 0.0, 0.0, 1.0);" +
                        "m = m * m1;" +
                        // Non-uniform scale
                        "float f1 = 0.65 + cos(f * 1.1) * 0.2;" +
                        "float f2 = 0.65 + cos(f * 1.1) * 0.2;" +
                        "float f3 = 0.65 + cos(f * 1.3) * 0.2;" +
                        "m1[0] = vec4(f1, 0.0, 0.0, 0.0);" +
                        "m1[1] = vec4(0.0, f2, 0.0, 0.0);" +
                        "m1[2] = vec4(0.0, 0.0, f3, 0.0);" +
                        "m1[3] = vec4(0.0, 0.0, 0.0, 1.0);" +
                        "m = m * m1;" +
                        "gl_Position = viewproj_matrix * m * position;" +
                        "vs_out.normal = mat3(view_matrix * m) * normal;" +
                        "vs_out.color = mix(color0, color1, fract(j * 313.431));" +
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
                        "in VS_OUT" +
                        "{" +
                        "vec3 normal;" +
                        "vec4 color;" +
                        "} fs_in;" +
                        "out vec4 color;" +
                        "void main(void)" +
                        "{" +
                        "vec3 N = normalize(fs_in.normal);" +
                        "color = fs_in.color * abs(N.z);" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "position_3");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.NORMAL, "normal");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.ID, "draw_id");
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

        // Create the list of draw commands
        DrawArraysIndirectCommand drawCommands = new DrawArraysIndirectCommand(vertices.length / 3, NUM_DRAWS);

        // Generate and bind the buffer
        GLES32.glGenBuffers(1, indirectBuffer, 0);
        GLES32.glBindBuffer(GLES32.GL_DRAW_INDIRECT_BUFFER, indirectBuffer[0]);
        GLES32.glBufferData(GLES32.GL_DRAW_INDIRECT_BUFFER, drawCommands.getSize(), drawCommands.getData(), GLES32.GL_STATIC_DRAW);
        GLES32.glBindBuffer(GLES32.GL_DRAW_INDIRECT_BUFFER, 0);

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao, 0);
        GLES32.glGenBuffers(Attributes.MAX, vbos, 0);
        GLES32.glBindVertexArray(vao[0]);

        // Vertex buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.VERTEX]);
        FloatBuffer vertexBuffer = getFloatBuffer(vertices);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Normal buffer
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.NORMAL]);
        FloatBuffer normalBuffer = getFloatBuffer(normals);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, normals.length * 4, normalBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.NORMAL, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.NORMAL);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // ID buffer
        int[] draw_index = new int[NUM_DRAWS];
        for (int i = 0; i < NUM_DRAWS; i++) {
            draw_index[i] = i;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(draw_index.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(draw_index);
        intBuffer.position(0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[Attributes.ID]);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, draw_index.length * 4, intBuffer, GLES32.GL_STATIC_DRAW);
        // GLES32.glVertexAttribIPointer(Attributes.ID, 1, GLES32.GL_UNSIGNED_INT, 0, 0);
        GLES32.glVertexAttribPointer(Attributes.ID, 1, GLES32.GL_UNSIGNED_INT, false, 0, 0);
        GLES32.glVertexAttribDivisor(Attributes.ID, 1);
        GLES32.glEnableVertexAttribArray(Attributes.ID);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE); // better for embedded

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);
    }

    private FloatBuffer getFloatBuffer(float[] data) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);
        return floatBuffer;
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
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 1.0f, 2000.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        double total_time = timer.getTotalTime();
        float t = (float) total_time;

        float[] viewMatrix = new float[16];
        float[] viewProjMatrix = new float[16];
        Matrix.setLookAtM(viewMatrix, 0,
                100.0f * (float) Math.cos(t * 0.023f), 100.0f * (float) Math.cos(t * 0.023f), 300.0f * (float) Math.sin(t * 0.037f) - 600.0f,
                0.0f, 0.0f, 260.0f,
                0.1f - (float) Math.cos(t * 0.1f) * 0.3f, 1.0f, 0.0f);
        Matrix.multiplyMM(viewProjMatrix, 0, perspectiveProjectionMatrix, 0, viewMatrix, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Pass data to vertex shader
        GLES32.glUniform1f(uniforms.time, t);
        GLES32.glUniformMatrix4fv(uniforms.view_matrix, 1, false, viewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.viewproj_matrix, 1, false, viewProjMatrix, 0);

        GLES32.glBindVertexArray(vao[0]);
        if (useIndirect) {
            GLES32.glBindBuffer(GLES32.GL_DRAW_INDIRECT_BUFFER, indirectBuffer[0]);
            // GLES32.glMultiDrawArraysIndirect(GLES32.GL_TRIANGLES, null, NUM_DRAWS, 0);
            // Loop over each object to draw
            for (int i = 0; i < NUM_DRAWS; i++) {
                int offset = i * 4 * 4; // commandSize is the size of each command structure
                GLES32.glDrawArraysIndirect(GLES32.GL_TRIANGLES, offset);
            }
            GLES32.glBindBuffer(GLES32.GL_DRAW_INDIRECT_BUFFER, 0);
        } else {
            GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLES, 0, vertices.length / 3, NUM_DRAWS);
        }
        GLES32.glBindVertexArray(0);

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
        // Delete vbos for vertex attributes
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] > 0) {
                GLES32.glDeleteBuffers(1, vbos, i);
                vbos[i] = 0;
            }
        }

        // Delete the VAO
        if (vao[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao, 0);
            vao[0] = 0;
        }
    }

    private static class Uniforms {
        private int time = -1;
        private int view_matrix = -1;
        private int viewproj_matrix = -1;

        private void loadUniformLocations(int programId) {
            time = GLES32.glGetUniformLocation(programId, "time");
            view_matrix = GLES32.glGetUniformLocation(programId, "view_matrix");
            viewproj_matrix = GLES32.glGetUniformLocation(programId, "viewproj_matrix");
        }
    }

    private static class DrawArraysIndirectCommand {
        private final IntBuffer drawIndirectCommandsBuffer;

        public DrawArraysIndirectCommand(int count, int NUM_DRAWS) {
            DrawIndirectCommand[] drawIndirectCommands = new DrawIndirectCommand[NUM_DRAWS];
            for (int i = 0; i < NUM_DRAWS; i++) {
                drawIndirectCommands[i] = new DrawIndirectCommand(count, i);
            }
            drawIndirectCommandsBuffer = ByteBuffer.allocateDirect(NUM_DRAWS * 4 * 4) // Each command has 4 integers
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
            for (DrawIndirectCommand cmd : drawIndirectCommands) {
                drawIndirectCommandsBuffer.put(cmd.count);
                drawIndirectCommandsBuffer.put(cmd.primCount);
                drawIndirectCommandsBuffer.put(cmd.first);
                drawIndirectCommandsBuffer.put(cmd.baseInstance);
            }
            drawIndirectCommandsBuffer.position(0);
        }

        public int getSize() {
            return drawIndirectCommandsBuffer.capacity() * 4;
        }

        public IntBuffer getData() {
            return drawIndirectCommandsBuffer;
        }

        private static class DrawIndirectCommand {
            private final int count;
            private final int primCount;
            private final int first;
            private final int baseInstance;

            private DrawIndirectCommand(int count, int baseInstance) {
                this.count = count;
                this.primCount = 1;
                this.first = 0;
                this.baseInstance = baseInstance;
            }
        }
    }
}

package com.vnd.superbible7.chapter14;

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
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_ParallelParticles extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int MIN_PARTICLE_COUNT = 128;
    private final int MAX_PARTICLE_COUNT = MIN_PARTICLE_COUNT * 16;
    private final float[] position = new float[MAX_PARTICLE_COUNT * 3];
    private final float[][] velocity = new float[MAX_PARTICLE_COUNT][3];
    private final float[] initialPosition = new float[MAX_PARTICLE_COUNT * 3];
    private final float[][] initialVelocity = new float[MAX_PARTICLE_COUNT][3];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    private final Random random = new Random();
    private final int[] vao = new int[1];
    private final int[] vboPositionCPU = new int[1];
    private int PARTICLE_COUNT = MIN_PARTICLE_COUNT;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private int singleTap = 0;

    public x1_ParallelParticles(Context context) {
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
        singleTap = (singleTap + 1) % 5;
        PARTICLE_COUNT = (int) (MIN_PARTICLE_COUNT * Math.pow(2, singleTap));
        resetParticles();
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
                        "in vec3 aPosition;" +
                        "out vec4 oColor;" +
                        "void main(void)" +
                        "{" +
                        "gl_PointSize = 10.0f;" +
                        "oColor = vec4(0.6, 0.8, 0.8, 1.0) * (smoothstep(-10.0, 10.0, aPosition.z) * 0.6 + 0.4);" +
                        "gl_Position = vec4(aPosition * 0.2, 1.0);" +
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
                        "in vec4 oColor;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = oColor;" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "aPosition");
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

        initializeParticles();

        // Generate and bind VAO
        GLES32.glGenVertexArrays(1, vao, 0);
        GLES32.glBindVertexArray(vao[0]);

        // Generate and bind VBO for CPU position data
        GLES32.glGenBuffers(1, vboPositionCPU, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboPositionCPU[0]);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, MAX_PARTICLE_COUNT * 3 * 4, null, GLES32.GL_DYNAMIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Unbind VAO
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

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Bind the VAO
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboPositionCPU[0]);
        ByteBuffer byteBufferSquarePosition = ByteBuffer.allocateDirect(MAX_PARTICLE_COUNT * 3 * 4);
        byteBufferSquarePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferSquarePosition = byteBufferSquarePosition.asFloatBuffer();
        floatBufferSquarePosition.put(position);
        floatBufferSquarePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, MAX_PARTICLE_COUNT * 3 * 4, floatBufferSquarePosition, GLES32.GL_DYNAMIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Draw particles as points
        GLES32.glBindVertexArray(vao[0]);
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, PARTICLE_COUNT);
        GLES32.glBindVertexArray(0);

        // Stop using the program
        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    private void update() {
        updateParticles(timer.getTotalTime() * 0.001f);
        // updateParticles(timer.getDeltaTime());
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


    public void initializeParticles() {
        for (int i = 0; i < MAX_PARTICLE_COUNT; i++) {
            // Randomly initialize positions within the range [-3.0, 3.0]
            initialPosition[i * 3] = random.nextFloat() * 6.0f - 3.0f;
            initialPosition[i * 3 + 1] = random.nextFloat() * 6.0f - 3.0f;
            initialPosition[i * 3 + 2] = random.nextFloat() * 6.0f - 3.0f;

            // Initialize velocity based on position
            initialVelocity[i][0] = initialPosition[i * 3] * 0.001f;
            initialVelocity[i][1] = initialPosition[i * 3 + 1] * 0.001f;
            initialVelocity[i][2] = initialPosition[i * 3 + 2] * 0.001f;

            // Copy initial position to position array
            position[i * 3] = initialPosition[i * 3];
            position[i * 3 + 1] = initialPosition[i * 3 + 1];
            position[i * 3 + 2] = initialPosition[i * 3 + 2];

            // Copy initial velocity to velocity array
            velocity[i][0] = initialVelocity[i][0];
            velocity[i][1] = initialVelocity[i][1];
            velocity[i][2] = initialVelocity[i][2];
        }
    }

    public void updateParticles(double deltaTime) {
        float[] newPosition = new float[MAX_PARTICLE_COUNT * 3];
        float[][] newVelocity = new float[MAX_PARTICLE_COUNT][3];

        // Copy current positions and velocities
        System.arraycopy(position, 0, newPosition, 0, position.length);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            newVelocity[i][0] = velocity[i][0];
            newVelocity[i][1] = velocity[i][1];
            newVelocity[i][2] = velocity[i][2];
        }

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float[] myPosition = {position[i * 3], position[i * 3 + 1], position[i * 3 + 2]};
            float[] myVelocity = {velocity[i][0], velocity[i][1], velocity[i][2]};
            float[] deltaV = {0.0f, 0.0f, 0.0f};

            for (int j = 0; j < PARTICLE_COUNT; j++) {
                if (i != j) {
                    float[] otherPosition = {position[j * 3], position[j * 3 + 1], position[j * 3 + 2]};
                    float[] deltaPos = {
                            otherPosition[0] - myPosition[0],
                            otherPosition[1] - myPosition[1],
                            otherPosition[2] - myPosition[2]
                    };

                    float distance = (float) Math.sqrt(deltaPos[0] * deltaPos[0] + deltaPos[1] * deltaPos[1] + deltaPos[2] * deltaPos[2]);
                    distance = Math.max(distance, 0.005f);

                    float[] deltaDir = {
                            deltaPos[0] / distance,
                            deltaPos[1] / distance,
                            deltaPos[2] / distance
                    };

                    deltaV[0] += deltaDir[0] / (distance * distance);
                    deltaV[1] += deltaDir[1] / (distance * distance);
                    deltaV[2] += deltaDir[2] / (distance * distance);
                }
            }

            // Update position
            newPosition[i * 3] = myPosition[0] + myVelocity[0];
            newPosition[i * 3 + 1] = myPosition[1] + myVelocity[1];
            newPosition[i * 3 + 2] = myPosition[2] + myVelocity[2];

            // Update velocity
            newVelocity[i][0] = myVelocity[0] + deltaV[0] * (float) deltaTime * 0.001f;
            newVelocity[i][1] = myVelocity[1] + deltaV[1] * (float) deltaTime * 0.001f;
            newVelocity[i][2] = myVelocity[2] + deltaV[2] * (float) deltaTime * 0.001f;
        }

        // Apply updated positions and velocities
        System.arraycopy(newPosition, 0, position, 0, position.length);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            velocity[i][0] = newVelocity[i][0];
            velocity[i][1] = newVelocity[i][1];
            velocity[i][2] = newVelocity[i][2];
        }
    }

    public void resetParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            position[i * 3] = initialPosition[i * 3];
            position[i * 3 + 1] = initialPosition[i * 3 + 1];
            position[i * 3 + 2] = initialPosition[i * 3 + 2];

            velocity[i][0] = initialVelocity[i][0];
            velocity[i][1] = initialVelocity[i][1];
            velocity[i][2] = initialVelocity[i][2];
        }

        for (int i = PARTICLE_COUNT; i < MAX_PARTICLE_COUNT; i++) {
            position[i * 3] = 0.0f;
            position[i * 3 + 1] = 0.0f;
            position[i * 3 + 2] = 0.0f;
            velocity[i][0] = 0.0f;
            velocity[i][1] = 0.0f;
            velocity[i][2] = 0.0f;
        }

        // reset
        timer.reset();
    }
}

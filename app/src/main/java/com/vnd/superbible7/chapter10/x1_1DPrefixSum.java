package com.vnd.superbible7.chapter10;

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
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_1DPrefixSum extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final int NUM_ELEMENTS = 1024;
    private final Random random = new Random();
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final int[] dataBuffer = new int[2];
    private final float[] inputData = new float[NUM_ELEMENTS];
    private final float[] outputData = new float[NUM_ELEMENTS];
    private final float[] inputData1 = new float[NUM_ELEMENTS];
    private final float[] outputData1 = new float[NUM_ELEMENTS];  // Output data
    boolean done = false;
    private GestureDetector gestureDetector = null;

    public x1_1DPrefixSum(Context context) {
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
                        "void main(void)" +
                        "{" +
                        "gl_PointSize = 40.0f;" +
                        "gl_Position = vec4(0.0, 0.0, 0.0, 1.0);" +
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
        shaderProgramObject[0] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[0], vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject[0], fragmentShaderObject);
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

        // compute shader
        final String computeShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (local_size_x = 1024) in;" +
                        "layout (binding = 0) coherent readonly buffer block1 {" +
                        "float input_data[gl_WorkGroupSize.x];" +
                        "};" +
                        "layout (binding = 1) coherent writeonly buffer block2 {" +
                        "float output_data[gl_WorkGroupSize.x];" +
                        "};" +
                        "shared float shared_data[gl_WorkGroupSize.x * 2u];" +
                        "void main(void) {" +
                        "uint id = gl_LocalInvocationID.x;" +
                        "uint rd_id;" +
                        "uint wr_id;" +
                        "uint mask;" +
                        "const uint steps = uint(log2(float(gl_WorkGroupSize.x))) + 1u;" +
                        "uint step = 0u;" +
                        "shared_data[id * 2u] = input_data[id * 2u];" +
                        "shared_data[id * 2u + 1u] = input_data[id * 2u + 1u];" +
                        "barrier();" +
                        "for (step = 0u; step < steps; step++) {" +
                        "mask = (1u << step) - 1u;" +
                        "rd_id = ((id >> step) << (step + 1u)) + mask;" +
                        "wr_id = rd_id + 1u + (id & mask);" +
                        "shared_data[wr_id] += shared_data[rd_id];" +
                        "barrier();" +
                        "}" +
                        "output_data[id * 2u] = shared_data[id * 2u];" +
                        "output_data[id * 2u + 1u] = shared_data[id * 2u + 1u];" +
                        "}";
        int computeShaderObject = GLES32.glCreateShader(GLES32.GL_COMPUTE_SHADER);
        GLES32.glShaderSource(computeShaderObject, computeShaderSourceCode);
        GLES32.glCompileShader(computeShaderObject);
        GLES32.glGetShaderiv(computeShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(computeShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(computeShaderObject);
                System.out.println("VND: compute shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject[1] = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject[1], computeShaderObject);
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

        // Create buffers for input and output data
        GLES32.glGenBuffers(2, dataBuffer, 0);

        // Set up input buffer
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            inputData[i] = random.nextFloat();
            inputData1[i] = inputData[i];
        }
        GLES32.glBindBuffer(GLES32.GL_SHADER_STORAGE_BUFFER, dataBuffer[0]);
        FloatBuffer inputDataBuffer = getFloatBuffer(inputData);
        GLES32.glBufferData(GLES32.GL_SHADER_STORAGE_BUFFER, inputData.length * Float.BYTES, inputDataBuffer, GLES32.GL_DYNAMIC_COPY);
        GLES32.glBindBufferBase(GLES32.GL_SHADER_STORAGE_BUFFER, 0, dataBuffer[0]);
        GLES32.glBindBuffer(GLES32.GL_SHADER_STORAGE_BUFFER, 0);

        // Set up output buffer
        GLES32.glBindBuffer(GLES32.GL_SHADER_STORAGE_BUFFER, dataBuffer[1]);
        GLES32.glBufferData(GLES32.GL_SHADER_STORAGE_BUFFER, outputData.length * Float.BYTES, null, GLES32.GL_DYNAMIC_COPY);
        GLES32.glBindBufferBase(GLES32.GL_SHADER_STORAGE_BUFFER, 1, dataBuffer[1]);
        GLES32.glBindBuffer(GLES32.GL_SHADER_STORAGE_BUFFER, 0);

        System.out.print("VND: Input Data: ");
        for (int i = 0; i < inputData.length; i++) {
            // Print each float value with 2 decimal places
            System.out.printf("%.2f", inputData[i]);

            // Separate values with a space except for the last one
            if (i < inputData.length - 1) {
                System.out.print(" ");
            }
        }
        System.out.println();
        prefixSum(inputData1, outputData1, inputData1.length);

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

    private void prefixSum(float[] input, float[] output, int elements) {
        float sum = 0.0f;

        for (int i = 0; i < elements; i++) {
            sum += input[i];
            output[i] = sum;
        }
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
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // Run the compute shader
        GLES32.glUseProgram(shaderProgramObject[1]);
        GLES32.glDispatchCompute(1, 1, 1);  // Dispatch workgroups
        GLES32.glMemoryBarrier(GLES32.GL_SHADER_STORAGE_BARRIER_BIT);  // Wait for shader to finish
        GLES32.glFinish();

        // Read the output data from the buffer
        GLES32.glBindBuffer(GLES32.GL_SHADER_STORAGE_BUFFER, dataBuffer[1]);
        ByteBuffer byteBuffer = (ByteBuffer) GLES32.glMapBufferRange(GLES32.GL_SHADER_STORAGE_BUFFER, 0, outputData.length * Float.BYTES, GLES32.GL_MAP_READ_BIT);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.get(outputData);
        floatBuffer.position(0);
        GLES32.glUnmapBuffer(GLES32.GL_SHADER_STORAGE_BUFFER);

        // Stop using the program
        GLES32.glUseProgram(0);

        // Compare results with expected output
        if (!done) {
            float tolerance = 1e-3f;
            for (int i = 0; i < NUM_ELEMENTS; i++) {
                if (Math.abs(outputData1[i] - outputData[i]) > tolerance) {
                    System.out.println("MISMATCH i = " + i + ", expected = " + outputData1[i] + ", actual = " + outputData[i]);
                }
            }
            done = true;
        }

        // Use the program1
        GLES32.glUseProgram(shaderProgramObject[0]);

        // Draw point
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, 1);

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
        System.out.print("VND: Output Data: ");
        for (int i = 0; i < outputData.length; i++) {
            // Print each float value with 2 decimal places
            System.out.printf("%.2f", outputData[i]);

            // Separate values with a space except for the last one
            if (i < outputData.length - 1) {
                System.out.print(" ");
            }
        }
        System.out.println();

        if (shaderProgramObject[0] > 0) {
            // Use the program
            GLES32.glUseProgram(shaderProgramObject[0]);

            // Delete attached shaders
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

            // Delete the program
            GLES32.glDeleteProgram(shaderProgramObject[0]);
            shaderProgramObject[0] = 0;
        }
    }
}

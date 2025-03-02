package com.vnd.superbible7.chapter10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.R;
import com.vnd.superbible7.common.OpenGLInfo;
import com.vnd.superbible7.common.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x2_2DPrefixSum extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final int NUM_ELEMENTS = 1024;
    private final TextureHelper textureHelper;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final int[] texture = new int[3];
    private final float[] screenSize = new float[2];
    private GestureDetector gestureDetector = null;
    private Uniforms uniforms;

    public x2_2DPrefixSum(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // Event related
        // create and set gestureDetector object
        gestureDetector = new GestureDetector(context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);

        // Texture related
        textureHelper = new TextureHelper(context);
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
                        "const vec4 vertex[] = vec4[](" +
                        "vec4(-1.0, -1.0, 0.5, 1.0)," +
                        "vec4( 1.0, -1.0, 0.5, 1.0)," +
                        "vec4(-1.0,  1.0, 0.5, 1.0)," +
                        "vec4( 1.0,  1.0, 0.5, 1.0));" +
                        "gl_Position = vertex[gl_VertexID];" +
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
                        "uniform vec2 screenSize;" +
                        "uniform highp sampler2D input_image;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "float m = 1.0 + gl_FragCoord.x / 30.0;" +
                        "vec2 s = vec2(1.0) / vec2(textureSize(input_image, 0));" +
                        "vec2 C = gl_FragCoord.xy;" +
                        "C.y = 1800.0 - C.y;" +
                        "C.x = 100.0 + C.x;" +
                        "vec2 P0 = vec2(C * 2.0) + vec2(-m, -m);" +
                        "vec2 P1 = vec2(C * 2.0) + vec2(-m, m);" +
                        "vec2 P2 = vec2(C * 2.0) + vec2(m, -m);" +
                        "vec2 P3 = vec2(C * 2.0) + vec2(m, m);" +
                        "P0 *= s;" +
                        "P1 *= s;" +
                        "P2 *= s;" +
                        "P3 *= s;" +
                        "float a = imageLoad(input_image, P0).r;" +
                        "float b = imageLoad(input_image, P1).r;" +
                        "float c = imageLoad(input_image, P2).r;" +
                        "float d = imageLoad(input_image, P3).r;" +
                        "float f = a - b - c + d;" +
                        "m *= 2.0;" +
                        "fragColor = vec4(f) / float(m * m);" +
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

        // get shader uniform locations - must be after linkage
        uniforms = new Uniforms();
        uniforms.loadUniformLocations(shaderProgramObject[0]);

        // compute shader
        final String computeShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "precision highp int;" +
                        "layout (local_size_x = 1024) in;" +
                        "shared float shared_data[gl_WorkGroupSize.x * 2u];" +
                        "layout (binding = 0, r32f) readonly uniform highp image2D input_image;" +
                        "layout (binding = 1, r32f) writeonly uniform highp image2D output_image;" +
                        "void main(void) {" +
                        "uint id = gl_LocalInvocationID.x;" +
                        "uint rd_id;" +
                        "uint wr_id;" +
                        "uint mask;" +
                        "ivec2 P0 = ivec2(id * 2u, gl_WorkGroupID.x);" +
                        "ivec2 P1 = ivec2(id * 2u + 1u, gl_WorkGroupID.x);" +
                        "const uint steps = uint(log2(float(gl_WorkGroupSize.x))) + 1u;" +
                        "uint step = 0u;" +
                        "shared_data[P0.x] = imageLoad(input_image, P0).r;" +
                        "shared_data[P1.x] = imageLoad(input_image, P1).r;" +
                        "barrier();" +
                        "for (step = 0u; step < steps; step++) {" +
                        "mask = (1u << step) - 1u;" +
                        "rd_id = ((id >> step) << (step + 1u)) + mask;" +
                        "wr_id = rd_id + 1u + (id & mask);" +
                        "shared_data[wr_id] += shared_data[rd_id];" +
                        "barrier();" +
                        "}" +
                        "imageStore(output_image, P0.yx, vec4(shared_data[P0.x]));" +
                        "imageStore(output_image, P1.yx, vec4(shared_data[P1.x]));" +
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

        // loading texture to create texture
        GLES32.glGenTextures(3, texture, 0);
        texture[0] = textureHelper.load2DTexture(R.raw.salad_gray);
        //texture[1] = textureHelper.load2DTexture(R.raw.salad_gray);
        //texture[2] = textureHelper.load2DTexture(R.raw.salad_gray);

        // For the next textures, create empty textures with the R32F format
        for (int i = 1; i < 3; i++) {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[i]);
            GLES32.glTexStorage2D(GLES32.GL_TEXTURE_2D, 1, GLES32.GL_R32F, NUM_ELEMENTS, NUM_ELEMENTS);
        }

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

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

        screenSize[0] = width * 1.0f;
        screenSize[1] = height * 1.0f;

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // Run the compute shader row wise prefix sum
        GLES32.glUseProgram(shaderProgramObject[1]);
        GLES32.glBindImageTexture(0, texture[0], 0, false, 0, GLES32.GL_READ_ONLY, GLES32.GL_R32F);
        GLES32.glBindImageTexture(1, texture[1], 0, false, 0, GLES32.GL_WRITE_ONLY, GLES32.GL_R32F);
        GLES32.glDispatchCompute(NUM_ELEMENTS, 1, 1);
        GLES32.glMemoryBarrier(GLES32.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GLES32.glFinish();

        // Run the compute shader column wise prefix sum
        GLES32.glBindImageTexture(0, texture[1], 0, false, 0, GLES32.GL_READ_ONLY, GLES32.GL_R32F);
        GLES32.glBindImageTexture(1, texture[2], 0, false, 0, GLES32.GL_WRITE_ONLY, GLES32.GL_R32F);
        GLES32.glDispatchCompute(NUM_ELEMENTS, 1, 1);
        GLES32.glMemoryBarrier(GLES32.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GLES32.glFinish();

        // Stop using the program
        GLES32.glUseProgram(0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject[0]);

        // Pass data to vertex shader
        GLES32.glUniform2fv(uniforms.screenSize, 1, screenSize, 0);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[2]);
        GLES32.glUniform1i(uniforms.input_image, 0);

        // Draw point
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

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

    private static class Uniforms {
        private int input_image = -1;
        private int screenSize = -1;

        private void loadUniformLocations(int programId) {
            input_image = GLES32.glGetUniformLocation(programId, "input_image");
            screenSize = GLES32.glGetUniformLocation(programId, "screenSize");
        }
    }
}

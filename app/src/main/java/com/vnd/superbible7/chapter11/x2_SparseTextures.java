package com.vnd.superbible7.chapter11;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x2_SparseTextures extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final byte[] bit_reversal_table = {
            (byte) 0x00, (byte) 0x80, (byte) 0x40, (byte) 0xc0, (byte) 0x20, (byte) 0xa0, (byte) 0x60, (byte) 0xe0,
            (byte) 0x10, (byte) 0x90, (byte) 0x50, (byte) 0xd0, (byte) 0x30, (byte) 0xb0, (byte) 0x70, (byte) 0xf0,
            (byte) 0x08, (byte) 0x88, (byte) 0x48, (byte) 0xc8, (byte) 0x28, (byte) 0xa8, (byte) 0x68, (byte) 0xe8,
            (byte) 0x18, (byte) 0x98, (byte) 0x58, (byte) 0xd8, (byte) 0x38, (byte) 0xb8, (byte) 0x78, (byte) 0xf8,
            (byte) 0x04, (byte) 0x84, (byte) 0x44, (byte) 0xc4, (byte) 0x24, (byte) 0xa4, (byte) 0x64, (byte) 0xe4,
            (byte) 0x14, (byte) 0x94, (byte) 0x54, (byte) 0xd4, (byte) 0x34, (byte) 0xb4, (byte) 0x74, (byte) 0xf4,
            (byte) 0x0c, (byte) 0x8c, (byte) 0x4c, (byte) 0xcc, (byte) 0x2c, (byte) 0xac, (byte) 0x6c, (byte) 0xec,
            (byte) 0x1c, (byte) 0x9c, (byte) 0x5c, (byte) 0xdc, (byte) 0x3c, (byte) 0xbc, (byte) 0x7c, (byte) 0xfc,
            (byte) 0x02, (byte) 0x82, (byte) 0x42, (byte) 0xc2, (byte) 0x22, (byte) 0xa2, (byte) 0x62, (byte) 0xe2,
            (byte) 0x12, (byte) 0x92, (byte) 0x52, (byte) 0xd2, (byte) 0x32, (byte) 0xb2, (byte) 0x72, (byte) 0xf2,
            (byte) 0x0a, (byte) 0x8a, (byte) 0x4a, (byte) 0xca, (byte) 0x2a, (byte) 0xaa, (byte) 0x6a, (byte) 0xea,
            (byte) 0x1a, (byte) 0x9a, (byte) 0x5a, (byte) 0xda, (byte) 0x3a, (byte) 0xba, (byte) 0x7a, (byte) 0xfa,
            (byte) 0x06, (byte) 0x86, (byte) 0x46, (byte) 0xc6, (byte) 0x26, (byte) 0xa6, (byte) 0x66, (byte) 0xe6,
            (byte) 0x16, (byte) 0x96, (byte) 0x56, (byte) 0xd6, (byte) 0x36, (byte) 0xb6, (byte) 0x76, (byte) 0xf6,
            (byte) 0x0e, (byte) 0x8e, (byte) 0x4e, (byte) 0xce, (byte) 0x2e, (byte) 0xae, (byte) 0x6e, (byte) 0xee,
            (byte) 0x1e, (byte) 0x9e, (byte) 0x5e, (byte) 0xde, (byte) 0x3e, (byte) 0xbe, (byte) 0x7e, (byte) 0xfe,
            (byte) 0x01, (byte) 0x81, (byte) 0x41, (byte) 0xc1, (byte) 0x21, (byte) 0xa1, (byte) 0x61, (byte) 0xe1,
            (byte) 0x11, (byte) 0x91, (byte) 0x51, (byte) 0xd1, (byte) 0x31, (byte) 0xb1, (byte) 0x71, (byte) 0xf1,
            (byte) 0x09, (byte) 0x89, (byte) 0x49, (byte) 0xc9, (byte) 0x29, (byte) 0xa9, (byte) 0x69, (byte) 0xe9,
            (byte) 0x19, (byte) 0x99, (byte) 0x59, (byte) 0xd9, (byte) 0x39, (byte) 0xb9, (byte) 0x79, (byte) 0xf9,
            (byte) 0x05, (byte) 0x85, (byte) 0x45, (byte) 0xc5, (byte) 0x25, (byte) 0xa5, (byte) 0x65, (byte) 0xe5,
            (byte) 0x15, (byte) 0x95, (byte) 0x55, (byte) 0xd5, (byte) 0x35, (byte) 0xb5, (byte) 0x75, (byte) 0xf5,
            (byte) 0x0d, (byte) 0x8d, (byte) 0x4d, (byte) 0xcd, (byte) 0x2d, (byte) 0xad, (byte) 0x6d, (byte) 0xed,
            (byte) 0x1d, (byte) 0x9d, (byte) 0x5d, (byte) 0xdd, (byte) 0x3d, (byte) 0xbd, (byte) 0x7d, (byte) 0xfd,
            (byte) 0x03, (byte) 0x83, (byte) 0x43, (byte) 0xc3, (byte) 0x23, (byte) 0xa3, (byte) 0x63, (byte) 0xe3,
            (byte) 0x13, (byte) 0x93, (byte) 0x53, (byte) 0xd3, (byte) 0x33, (byte) 0xb3, (byte) 0x73, (byte) 0xf3,
            (byte) 0x0b, (byte) 0x8b, (byte) 0x4b, (byte) 0xcb, (byte) 0x2b, (byte) 0xab, (byte) 0x6b, (byte) 0xeb,
            (byte) 0x1b, (byte) 0x9b, (byte) 0x5b, (byte) 0xdb, (byte) 0x3b, (byte) 0xbb, (byte) 0x7b, (byte) 0xfb,
            (byte) 0x07, (byte) 0x87, (byte) 0x47, (byte) 0xc7, (byte) 0x27, (byte) 0xa7, (byte) 0x67, (byte) 0xe7,
            (byte) 0x17, (byte) 0x97, (byte) 0x57, (byte) 0xd7, (byte) 0x37, (byte) 0xb7, (byte) 0x77, (byte) 0xf7,
            (byte) 0x0f, (byte) 0x8f, (byte) 0x4f, (byte) 0xcf, (byte) 0x2f, (byte) 0xaf, (byte) 0x6f, (byte) 0xef,
            (byte) 0x1f, (byte) 0x9f, (byte) 0x5f, (byte) 0xdf, (byte) 0x3f, (byte) 0xbf, (byte) 0x7f, (byte) 0xff
    };
    private static int t = 0;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[1];
    private final int PAGE_SIZE = 128;
    private final Context context;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;
    private ByteBuffer smileyBuffer;
    private ByteBuffer blackBuffer;

    public x2_SparseTextures(Context context) {
        super(context);
        this.context = context;

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
                        "out vec2 uv;" +
                        "void main(void)" +
                        "{" +
                        "vec2 pos = vec2(float(gl_VertexID & 2), float(gl_VertexID * 2 & 2));" +
                        "uv = pos * 0.5;" +
                        "gl_Position = vec4(pos - vec2(1.0), 0.0, 1.0);" +
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
                        "in vec2 uv;" +
                        "uniform sampler2D textureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = textureLod(textureSampler, uv, 0.0);" +
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

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // loading images to create texture
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        //GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_SPARSE_ARB, GLES32.GL_TRUE);

        int TEX_SIZE = 16 * PAGE_SIZE;
        GLES32.glTexStorage2D(GLES32.GL_TEXTURE_2D, 11, GLES32.GL_RGBA8, TEX_SIZE, TEX_SIZE);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        byte[] textureData = new byte[PAGE_SIZE * PAGE_SIZE * 4];
        try {
            // Open the raw resource file
            InputStream inputStream = context.getResources().openRawResource(R.raw.smiley);
            int bytesRead = inputStream.read(textureData);
            if (bytesRead != PAGE_SIZE * PAGE_SIZE * 4) {
                throw new IOException("Failed to read the correct amount of data from the file.");
            }

            // Close the InputStream
            inputStream.close();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }

        smileyBuffer = ByteBuffer.allocateDirect(textureData.length * 4);
        smileyBuffer.order(ByteOrder.nativeOrder());
        smileyBuffer.put(textureData);
        smileyBuffer.position(0);

        // Create a buffer filled with black color (all zeros for RGBA)
        blackBuffer = ByteBuffer.allocateDirect(PAGE_SIZE * PAGE_SIZE * 4); // 4 bytes per pixel (RGBA)
        blackBuffer.order(ByteOrder.nativeOrder());
        for (int i = 0; i < PAGE_SIZE * PAGE_SIZE; i++) {
            blackBuffer.put((byte) 0);   // R
            blackBuffer.put((byte) 0);   // G
            blackBuffer.put((byte) 0);   // B
            blackBuffer.put((byte) 255); // A (fully opaque)
        }
        blackBuffer.position(0);

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

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

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        int r = (t & 0x100);
        int tile = bit_reversal_table[(t & 0xFF)];
        int x = (tile) & 0xF;
        int y = (tile >> 4) & 0xF;
        if (r == 0) {
            // Commit the texture page (set it as active for updates)
            // GLES32.glTexPageCommitmentARB(GLES32.GL_TEXTURE_2D,0, x * PAGE_SIZE, y * PAGE_SIZE, 0, PAGE_SIZE, PAGE_SIZE, 1, GLES32.GL_TRUE);
            GLES32.glTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, x * PAGE_SIZE, y * PAGE_SIZE, PAGE_SIZE, PAGE_SIZE, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, smileyBuffer);
        } else {
            // Uncommit the texture page (set it to black)
            //GLES32.glTexPageCommitmentARB(GLES32.GL_TEXTURE_2D, 0, x * PAGE_SIZE, y * PAGE_SIZE, 0, PAGE_SIZE, PAGE_SIZE, 1, GLES32.GL_FALSE);
            GLES32.glTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, x * PAGE_SIZE, y * PAGE_SIZE, PAGE_SIZE, PAGE_SIZE, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, blackBuffer);
        }
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Draw screen
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        // Stop using the program
        GLES32.glUseProgram(0);

        t += 17;

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
        if (texture[0] > 0) {
            GLES32.glDeleteTextures(1, texture, 0);
            texture[0] = 0;
        }
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
        private int textureSampler = -1;

        private void loadUniformLocations(int programId) {
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
        }
    }
}

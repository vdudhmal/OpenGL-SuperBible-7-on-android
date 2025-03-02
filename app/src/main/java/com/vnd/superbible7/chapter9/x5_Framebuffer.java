package com.vnd.superbible7.chapter9;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.Attributes;
import com.vnd.superbible7.common.Timer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x5_Framebuffer extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int FBO_WIDTH = 512;
    private final int FBO_HEIGHT = 512;
    private final int[] vbo_position = new int[1];
    private final int[] vao = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] FBO = new int[1];
    private final int[] RBO = new int[1];
    private final int[] texture_FBO = new int[1];
    private final int[] vbo_position1 = new int[1];
    private final int[] vao1 = new int[1];
    private final float[] perspectiveProjectionMatrix1 = new float[16];
    private final Timer timer = new Timer();
    private Boolean bFBOResult = false;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private int mvpMatrixUniform;
    private int textureSamplerUniform;
    private int winWidth = 0;
    private int winHeight = 0;
    private int shaderProgramObject1 = 0;
    private int mvpMatrixUniform1 = 0;

    public x5_Framebuffer(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // event related
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
        printGLInfo(gl);

        // vertex shader
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "in vec4 aPosition;" +
                        "uniform mat4 uMVPMatrix;" +
                        "in vec2 aTextureCoordinates;" +
                        "out vec2 oTextureCoordinates;" +
                        "out VS_OUT" +
                        "{" +
                        "vec4 color;" +
                        "vec2 texcoord;" +
                        "} vs_out;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = uMVPMatrix * aPosition;" +
                        "vs_out.color = aPosition * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);" +
                        "vs_out.texcoord = aTextureCoordinates;" +
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
                System.out.println("VND: vertex shader compilation error log:" + szInfoLog);
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
                        "vec4 color; " +
                        "vec2 texcoord;" +
                        "} fs_in;" +
                        "uniform highp sampler2D uTextureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = mix(fs_in.color, texture(uTextureSampler, fs_in.texcoord), 0.7);" +
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
                System.out.println("VND: fragment shader compilation error log:" + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject, vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.TEXCOORD, "aTextureCoordinates");
        GLES32.glLinkProgram(shaderProgramObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject);
                System.out.println("VND: shader program linking error log:" + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        mvpMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMVPMatrix");
        textureSamplerUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uTextureSampler");

        final float[] vertex_data = {
                // Position                 Tex Coord
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,

                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 1.0f, 1.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,
                0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, 0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, 0.25f, 0.0f, 1.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 0.0f, 1.0f,
                0.25f, 0.25f, -0.25f, 1.0f, 1.0f,
                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,

                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, -0.25f, 0.0f, 1.0f,

                -0.25f, -0.25f, 0.25f, 0.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 1.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,
                -0.25f, 0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 0.0f,
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao, 0);
        GLES32.glBindVertexArray(vao[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position[0]);
        ByteBuffer byteBufferCubePosition = ByteBuffer.allocateDirect(vertex_data.length * 4);
        byteBufferCubePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubePosition = byteBufferCubePosition.asFloatBuffer();
        floatBufferCubePosition.put(vertex_data);
        floatBufferCubePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertex_data.length * 4, floatBufferCubePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glVertexAttribPointer(Attributes.TEXCOORD, 2, GLES32.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GLES32.glEnableVertexAttribArray(Attributes.TEXCOORD);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // enable culling
        GLES32.glEnable(GLES32.GL_CULL_FACE); // better for embedded

        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);

        // FBO related code
        if (createFBO()) {
            bFBOResult = initialize1(gl);
        }
    }

    private Boolean initialize1(GL10 ignoredGl) {
        // code
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "in vec4 aPosition;" +
                        "uniform mat4 uMVPMatrix;" +
                        "in vec2 aTextureCoordinates;" +
                        "out vec2 oTextureCoordinates;" +
                        "out VS_OUT" +
                        "{" +
                        "vec4 color;" +
                        "vec2 texcoord;" +
                        "} vs_out;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = uMVPMatrix * aPosition;" +
                        "vs_out.color = aPosition * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);" +
                        "vs_out.texcoord = aTextureCoordinates;" +
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
                System.out.println("VND: vertex shader compilation error log:" + szInfoLog);
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
                        "vec4 color; " +
                        "vec2 texcoord;" +
                        "} fs_in;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = sin(fs_in.color * vec4(40.0, 20.0, 30.0, 1.0)) * 0.5 + vec4(0.5);" +
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
                System.out.println("VND: fragment shader compilation error log:" + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject1 = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject1, vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject1, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject1, Attributes.VERTEX, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject1, Attributes.TEXCOORD, "aTextureCoordinates");
        GLES32.glLinkProgram(shaderProgramObject1);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject1, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject1, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject1);
                System.out.println("VND: shader program linking error log:" + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        mvpMatrixUniform1 = GLES32.glGetUniformLocation(shaderProgramObject1, "uMVPMatrix");

        final float[] vertex_data = {
                // Position                 Tex Coord
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,

                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 1.0f, 1.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,
                0.25f, -0.25f, 0.25f, 0.0f, 1.0f,

                0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, 0.25f, -0.25f, 0.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, 0.25f, 0.0f, 1.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                -0.25f, 0.25f, -0.25f, 0.0f, 1.0f,
                0.25f, 0.25f, -0.25f, 1.0f, 1.0f,
                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,

                0.25f, -0.25f, -0.25f, 1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f, 0.0f, 0.0f,
                -0.25f, 0.25f, -0.25f, 0.0f, 1.0f,

                -0.25f, -0.25f, 0.25f, 0.0f, 0.0f,
                0.25f, -0.25f, 0.25f, 1.0f, 0.0f,
                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,

                0.25f, 0.25f, 0.25f, 1.0f, 1.0f,
                -0.25f, 0.25f, 0.25f, 0.0f, 1.0f,
                -0.25f, -0.25f, 0.25f, 0.0f, 0.0f,
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao1, 0);
        GLES32.glBindVertexArray(vao1[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position1, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position1[0]);
        ByteBuffer byteBufferCubePosition = ByteBuffer.allocateDirect(vertex_data.length * 4);
        byteBufferCubePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubePosition = byteBufferCubePosition.asFloatBuffer();
        floatBufferCubePosition.put(vertex_data);
        floatBufferCubePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertex_data.length * 4, floatBufferCubePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glVertexAttribPointer(Attributes.TEXCOORD, 2, GLES32.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GLES32.glEnableVertexAttribArray(Attributes.TEXCOORD);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // enable culling
        GLES32.glEnable(GLES32.GL_CULL_FACE); // better for embedded

        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // initialize1 perspectiveProjectionMatrix1
        Matrix.setIdentityM(perspectiveProjectionMatrix1, 0);

        return true;
    }

    Boolean createFBO() {
        // variable declarations
        int[] maxRenderBufferSize = new int[1];

        // check capacity of render buffer
        GLES32.glGetIntegerv(GLES32.GL_MAX_RENDERBUFFER_SIZE, maxRenderBufferSize, 0);
        if (maxRenderBufferSize[0] < FBO_WIDTH) {
            System.out.println("VND: Texture size overflow!\n");
            return false;
        }

        // create custom framebuffer
        GLES32.glGenFramebuffers(1, FBO, 0);
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, FBO[0]);

        // create texture for FBO in which we are going to render the sphere
        GLES32.glGenTextures(1, texture_FBO, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_FBO[0]);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGB, FBO_WIDTH, FBO_HEIGHT, 0, GLES32.GL_RGB, GLES32.GL_UNSIGNED_SHORT_5_6_5, null);
        // attach above texture to framebuffer at color attachment 0
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, texture_FBO[0], 0);
        // now create render buffer to hold depth of custom FBO
        GLES32.glGenRenderbuffers(1, RBO, 0);
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, RBO[0]);
        // set the storage of render buffer of texture size for depth
        GLES32.glRenderbufferStorage(GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH_COMPONENT16, FBO_WIDTH, FBO_HEIGHT);
        // attach above depth related FBO to depth attachment
        GLES32.glFramebufferRenderbuffer(GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_ATTACHMENT, GLES32.GL_RENDERBUFFER, RBO[0]);
        // check the framebuffer status
        if (GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER) != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("VND: Framebuffer creation status is not complete!\n");
            return false;
        }
        // unbind framebuffer
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);

        return true;
    }

    private void printGLInfo(GL10 gl) {
        // code
        System.out.println("VND: OpenGL ES Vendor:" + gl.glGetString(GL10.GL_VENDOR));
        System.out.println("VND: OpenGL ES Renderer:" + gl.glGetString(GL10.GL_RENDERER));
        System.out.println("VND: OpenGL ES Version:" + gl.glGetString(GL10.GL_VERSION));
        System.out.println("VND: OpenGL ES Shading Language Version:" + gl.glGetString(GLES32.GL_SHADING_LANGUAGE_VERSION));

        // listing of supported extensions
        int[] retVal = new int[1];
        GLES32.glGetIntegerv(GLES32.GL_NUM_EXTENSIONS, retVal, 0);
        int numExtensions = retVal[0];
        for (int i = 0; i < numExtensions; i++) {
            System.out.println("VND:" + GLES32.glGetStringi(GL10.GL_EXTENSIONS, i));
        }
    }

    private void resize(int width, int height) {
        // code
        if (height <= 0) {
            height = 1;
        }

        if (width <= 0) {
            width = 1;
        }

        winWidth = width;
        winHeight = height;

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void resize1() {
        // code
        // Viewport == binocular
        GLES32.glViewport(0, 0, FBO_WIDTH, FBO_HEIGHT);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix1, 0, 45.0f, 1.0f, 0.1f, 100.0f);
    }

    private void display() {
        if (bFBOResult) {
            display1();
            update1();
        }

        // call resize again to compensate change by display
        resize(winWidth, winHeight);

        // reset color
        GLES32.glClearColor(0.0f, 0.0f, 0.3f, 1.0f);

        double currentTime = timer.getTotalTime();
        float f = (float) currentTime * 0.3f;

        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        GLES32.glUseProgram(shaderProgramObject);

        // transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.0f);
        Matrix.translateM(modelViewMatrix, 0,
                (float) (Math.sin(2.1f * f) * 0.5f),
                (float) (Math.cos(1.7f * f) * 0.5f),
                (float) (Math.sin(1.3f * f) * Math.cos(1.5f * f) * 2.0f));
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 81.0f, 1.0f, 0.0f, 0.0f);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_FBO[0]);
        GLES32.glUniform1i(textureSamplerUniform, 0);

        GLES32.glBindVertexArray(vao[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 36);
        GLES32.glBindVertexArray(0);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    private void display1() {
        // bind with FBO
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, FBO[0]);

        // call resize sphere
        resize1();

        // set color black to compensate change done by display sphere
        GLES32.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);

        double currentTime = timer.getTotalTime();
        float f = (float) currentTime * 0.3f;

        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        GLES32.glUseProgram(shaderProgramObject1);

        // transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.0f);
        Matrix.translateM(modelViewMatrix, 0,
                (float) (Math.sin(2.1f * f) * 0.5f),
                (float) (Math.cos(1.7f * f) * 0.5f),
                (float) (Math.sin(1.3f * f) * Math.cos(1.5f * f) * 2.0f));
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) currentTime * 81.0f, 1.0f, 0.0f, 0.0f);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(mvpMatrixUniform1, 1, false, modelViewProjectionMatrix, 0);

        // bind vao
        GLES32.glBindVertexArray(vao1[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 36);
        GLES32.glBindVertexArray(0);

        GLES32.glUseProgram(0);

        // unbind framebuffer
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);

        // render
        requestRender();
    }

    private void update() {
    }

    private void update1() {

    }

    private void uninitialize() {
        // code
        uninitialize1();
        if (texture_FBO[0] > 0) {
            GLES32.glDeleteTextures(1, texture_FBO, 0);
            texture_FBO[0] = 0;
        }
        if (RBO[0] > 0) {
            GLES32.glDeleteRenderbuffers(1, RBO, 0);
            RBO[0] = 0;
        }
        if (FBO[0] > 0) {
            GLES32.glDeleteRenderbuffers(1, FBO, 0);
            FBO[0] = 0;
        }
        if (shaderProgramObject > 0) {
            GLES32.glUseProgram(shaderProgramObject);
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
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }

        // cube
        if (vbo_position[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position, 0);
            vbo_position[0] = 0;
        }
        if (vao[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao, 0);
            vao[0] = 0;
        }
    }

    private void uninitialize1() {
        // code
        if (shaderProgramObject > 0) {
            GLES32.glUseProgram(shaderProgramObject);
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
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }

        // sphere
        if (vbo_position1[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position1, 0);
            vbo_position1[0] = 0;
        }
        if (vao1[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao1, 0);
            vao1[0] = 0;
        }

        System.exit(0);
    }
}

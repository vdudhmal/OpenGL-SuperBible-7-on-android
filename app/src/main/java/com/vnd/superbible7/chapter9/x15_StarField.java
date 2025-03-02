package com.vnd.superbible7.chapter9;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.R;
import com.vnd.superbible7.common.Attributes;
import com.vnd.superbible7.common.OpenGLInfo;
import com.vnd.superbible7.common.TextureHelper;
import com.vnd.superbible7.common.Timer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x15_StarField extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    final Random random = new Random();
    private final int[] vbo_position_star = new int[1];
    private final int[] vbo_color_star = new int[1];
    private final int[] vao_star = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture_star = new int[1];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x15_StarField(Context context) {
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
                        "in vec4 aVertex;" +
                        "in vec4 aColor;" +
                        "uniform float time;" +
                        "uniform mat4 projectionMatrix;" +
                        "flat out vec4 oColor;" +
                        "void main(void)" +
                        "{" +
                        "vec4 newVertex = aVertex;" +
                        "newVertex.z += time;" +
                        "newVertex.z = fract(newVertex.z);" +
                        "float size = (30.0 * newVertex.z * newVertex.z);" +
                        "oColor = smoothstep(1.0, 7.0, size) * aColor;" +
                        "newVertex.z = (999.9 * newVertex.z) - 1000.0;" +
                        "gl_Position = projectionMatrix * newVertex;" +
                        "gl_PointSize = size;" +
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
                        "flat in vec4 oColor;" +
                        "uniform sampler2D textureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = oColor * texture(textureSampler, gl_PointCoord);" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "aVertex");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.COLOR, "aColor");
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

        float[] star_position = new float[1000 * 3];
        float[] star_color = new float[1000 * 3];

        for (int i = 0; i < 1000; i++) {
            star_position[i * 3] = (random.nextFloat() * 2.0f - 1.0f) * 100.0f;
            star_position[i * 3 + 1] = (random.nextFloat() * 2.0f - 1.0f) * 100.0f;
            star_position[i * 3 + 2] = random.nextFloat();

            star_color[i * 3] = 0.8f + random.nextFloat() * 0.2f;
            star_color[i * 3 + 1] = 0.8f + random.nextFloat() * 0.2f;
            star_color[i * 3 + 2] = 0.8f + random.nextFloat() * 0.2f;

            // Optional: Log the generated values
            System.out.println("star_position " + star_position[i * 3] + ", " + star_position[i * 3 + 1] + ", " + star_position[i * 3 + 2]);
            System.out.println("star_color " + star_color[i * 3] + ", " + star_color[i * 3 + 1] + ", " + star_color[i * 3 + 2]);
        }

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_star, 0);
        GLES32.glBindVertexArray(vao_star[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_star, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_star[0]);
        ByteBuffer byteBufferSquarePosition = ByteBuffer.allocateDirect(star_position.length * 4);
        byteBufferSquarePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferSquarePosition = byteBufferSquarePosition.asFloatBuffer();
        floatBufferSquarePosition.put(star_position);
        floatBufferSquarePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, star_position.length * 4, floatBufferSquarePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for color - vertex buffer object
        GLES32.glGenBuffers(1, vbo_color_star, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_color_star[0]);
        ByteBuffer byteBufferSquareTextureCoordinates = ByteBuffer.allocateDirect(star_color.length * 4);
        byteBufferSquareTextureCoordinates.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferSquareTextureCoordinates = byteBufferSquareTextureCoordinates.asFloatBuffer();
        floatBufferSquareTextureCoordinates.put(star_color);
        floatBufferSquareTextureCoordinates.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, star_color.length * 4, floatBufferSquareTextureCoordinates, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.COLOR, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.COLOR);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

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

        // loading images to create texture
        texture_star[0] = textureHelper.load2DTexture(R.raw.star);

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
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 1000.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // Get time elapsed since the program started
        double currentTime = timer.getTotalTime();
        float t = (float) currentTime;
        t *= 0.1f;
        t -= (float) Math.floor(t);

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // transformations
        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniform1f(uniforms.time, t);
        GLES32.glUniformMatrix4fv(uniforms.modelViewProjectionMatrix, 1, false, perspectiveProjectionMatrix, 0);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_star[0]);
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        GLES32.glBindVertexArray(vao_star[0]);
        //GLES32.glEnable(GL_PROGRAM_POINT_SIZE);
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, 1000);
        GLES32.glBindVertexArray(0);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        // Stop using the program
        GLES32.glUseProgram(0);

        GLES32.glDisable(GLES32.GL_BLEND);

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
        if (texture_star[0] > 0) {
            GLES32.glDeleteTextures(1, texture_star, 0);
            texture_star[0] = 0;
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

        // star
        if (vbo_color_star[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_color_star, 0);
            vbo_color_star[0] = 0;
        }
        if (vbo_position_star[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_star, 0);
            vbo_position_star[0] = 0;
        }
        if (vao_star[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_star, 0);
            vao_star[0] = 0;
        }
    }

    private static class Uniforms {
        private int modelViewProjectionMatrix = -1;
        private int textureSampler = -1;
        private int time = -1;

        private void loadUniformLocations(int programId) {
            modelViewProjectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
            time = GLES32.glGetUniformLocation(programId, "time");
        }
    }
}

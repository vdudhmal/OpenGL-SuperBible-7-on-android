package com.vnd.superbible7.chapter5;

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
import com.vnd.superbible7.common.Timer;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x8_AlienRain extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int MAX_DROPLETS = 256;
    private final float[] droplet_x = new float[MAX_DROPLETS];
    private final float[] droplet_rot_speed = new float[MAX_DROPLETS];
    private final float[] droplet_fall_speed = new float[MAX_DROPLETS];
    private final int[] imageResourceIDs = {
            R.raw.aliens_0, R.raw.aliens_1, R.raw.aliens_2, R.raw.aliens_3, R.raw.aliens_4, R.raw.aliens_5, R.raw.aliens_6, R.raw.aliens_7, R.raw.aliens_8, R.raw.aliens_9,
            R.raw.aliens_10, R.raw.aliens_11, R.raw.aliens_12, R.raw.aliens_13, R.raw.aliens_14, R.raw.aliens_15, R.raw.aliens_16, R.raw.aliens_17, R.raw.aliens_18, R.raw.aliens_19,
            R.raw.aliens_20, R.raw.aliens_21, R.raw.aliens_22, R.raw.aliens_23, R.raw.aliens_24, R.raw.aliens_25, R.raw.aliens_26, R.raw.aliens_27, R.raw.aliens_28, R.raw.aliens_29,
            R.raw.aliens_30, R.raw.aliens_31, R.raw.aliens_32, R.raw.aliens_33, R.raw.aliens_34, R.raw.aliens_35, R.raw.aliens_36, R.raw.aliens_37, R.raw.aliens_38, R.raw.aliens_39,
            R.raw.aliens_40, R.raw.aliens_41, R.raw.aliens_42, R.raw.aliens_43, R.raw.aliens_44, R.raw.aliens_45, R.raw.aliens_46, R.raw.aliens_47, R.raw.aliens_48, R.raw.aliens_49,
            R.raw.aliens_50, R.raw.aliens_51, R.raw.aliens_52, R.raw.aliens_53, R.raw.aliens_54, R.raw.aliens_55, R.raw.aliens_56, R.raw.aliens_57, R.raw.aliens_58, R.raw.aliens_59,
            R.raw.aliens_60, R.raw.aliens_61, R.raw.aliens_62, R.raw.aliens_63
    };
    private final Random random = new Random();
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[1];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x8_AlienRain(Context context) {
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
                        "#define MAX_DROPLETS 256" +
                        "\n" +
                        "uniform vec3 droplets[MAX_DROPLETS];" +
                        "out vec2 oTextureCoordinates;" +
                        "out int oTextureIndex;" +
                        "void main(void)" +
                        "{" +
                        "const vec2[4] position = vec2[4](vec2(-0.5, -0.5), vec2(0.5, -0.5), vec2(-0.5, 0.5), vec2(0.5, 0.5));" +
                        "oTextureCoordinates = position[gl_VertexID].xy + vec2(0.5);" +
                        "float co = cos(droplets[gl_InstanceID].z);" +
                        "float so = sin(droplets[gl_InstanceID].z);" +
                        "mat2 rot = mat2(vec2(co, so), vec2(-so, co));" +
                        "vec2 pos = 0.25 * rot * position[gl_VertexID];" +
                        "gl_Position = vec4(pos.x + droplets[gl_InstanceID].x, pos.y + droplets[gl_InstanceID].y, 0.5, 1.0);" +
                        "oTextureIndex = gl_InstanceID % 64;" +
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
                        "precision highp sampler2D;" +
                        "in vec2 oTextureCoordinates;" +
                        "flat in int oTextureIndex;" +
                        "uniform sampler2DArray textureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = texture(textureSampler, vec3(oTextureCoordinates, oTextureIndex));" +
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

        // initialize droplets
        for (int i = 0; i < MAX_DROPLETS; i++) {
            droplet_x[i] = random.nextFloat() * 2.0f - 1.0f;
            droplet_rot_speed[i] = (random.nextFloat() + 0.5f) * ((i % 2) == 0 ? -3.0f : 3.0f);
            droplet_fall_speed[i] = random.nextFloat() + 0.2f;
        }
        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // loading images to create texture
        texture[0] = textureHelper.load2DTextureArray(imageResourceIDs);

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

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA);

        // Droplets
        double t = timer.getTotalTime();
        float[] droplets = new float[MAX_DROPLETS * 3];
        for (int i = 0; i < MAX_DROPLETS; i++) {
            droplets[i * 3] = droplet_x[i]; // x
            droplets[i * 3 + 1] = (float) (2.0f - ((t + i) * droplet_fall_speed[i]) % 4.31f); // y
            droplets[i * 3 + 2] = (float) (t * droplet_rot_speed[i]); // z
        }

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, texture[0]);
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Pass data to vertex shader
        GLES32.glUniform3fv(uniforms.droplets, MAX_DROPLETS, droplets, 0);

        // Draw droplets
        GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLE_STRIP, 0, 4, MAX_DROPLETS);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, 0);

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
        if (texture[0] > 0) {
            GLES32.glDeleteTextures(1, texture, 0);
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
        private int droplets = -1;

        private void loadUniformLocations(int programId) {
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
            droplets = GLES32.glGetUniformLocation(programId, "droplets");
        }
    }
}

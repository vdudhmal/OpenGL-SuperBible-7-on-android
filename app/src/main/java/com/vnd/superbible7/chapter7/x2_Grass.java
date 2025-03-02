package com.vnd.superbible7.chapter7;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x2_Grass extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[4];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private final int[] grassVao = new int[1];
    private final int[] grassVbo = new int[1];
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x2_Grass(Context context) {
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
                        "in vec4 vVertex;" +
                        "out vec4 color;" +
                        "uniform mat4 modelViewProjectionMatrix;" +
                        "layout (binding = 0) uniform sampler2D grasspalette_texture;" +
                        "layout (binding = 1) uniform sampler2D length_texture;" +
                        "layout (binding = 2) uniform sampler2D orientation_texture;" +
                        "layout (binding = 3) uniform sampler2D grasscolor_texture;" +
                        "layout (binding = 4) uniform sampler2D bend_texture;" +
                        "int random(int seed, int iterations) {" +
                        "int value = seed;" +
                        "int n;" +
                        "for (n = 0; n < iterations; n++) {" +
                        "value = ((value >> 7) ^ (value << 9)) * 15485863;" +
                        "}" +
                        "return value;" +
                        "}" +
                        "vec4 random_vector(int seed) {" +
                        "int r = random(gl_InstanceID, 4);" +
                        "int g = random(r, 2);" +
                        "int b = random(g, 2);" +
                        "int a = random(b, 2);" +
                        "return vec4(float(r & 0x3FF) / 1024.0, float(g & 0x3FF) / 1024.0, float(b & 0x3FF) / 1024.0, float(a & 0x3FF) / 1024.0);" +
                        "}" +
                        "mat4 construct_rotation_matrix(float angle) {" +
                        "float st = sin(angle);" +
                        "float ct = cos(angle);" +
                        "return mat4(vec4(ct, 0.0, st, 0.0), vec4(0.0, 1.0, 0.0, 0.0), vec4(-st, 0.0, ct, 0.0), vec4(0.0, 0.0, 0.0, 1.0));" +
                        "}" +
                        "void main(void) {" +
                        "vec4 offset = vec4(float(gl_InstanceID >> 10) - 512.0, 0.0f, float(gl_InstanceID & 0x3FF) - 512.0, 0.0f);" +
                        "int number1 = random(gl_InstanceID, 3);" +
                        "int number2 = random(number1, 2);" +
                        "offset += vec4(float(number1 & 0xFF) / 256.0, 0.0f, float(number2 & 0xFF) / 256.0, 0.0f);" +
                        //"float angle = float(random(number2, 2) & 0x3FF) / 1024.0;" +
                        "vec2 texcoord = offset.xz / 1024.0 + vec2(0.5);" +
                        //"float bend_factor = float(random(number2, 7) & 0x3FF) / 1024.0;" +
                        "float bend_factor = texture(bend_texture, texcoord).r * 2.0;" +
                        "float bend_amount = cos(vVertex.y);" +
                        "float angle = texture(orientation_texture, texcoord).r * 2.0 * 3.141592;" +
                        "mat4 rot = construct_rotation_matrix(angle);" +
                        "vec4 position = (rot * (vVertex + vec4(0.0, 0.0, bend_amount * bend_factor, 0.0))) + offset;" +
                        "position *= vec4(1.0, texture(length_texture, texcoord).r * 0.9 + 0.3, 1.0, 1.0);" +
                        "gl_Position = modelViewProjectionMatrix * position;" +
                        //"color = vec4(random_vector(gl_InstanceID).xyz * vec3(0.1, 0.5, 0.1) + vec3(0.1, 0.4, 0.1), 1.0);" +
                        "color = texture(grasspalette_texture, texture(grasscolor_texture, texcoord).rb) + vec4(random_vector(gl_InstanceID).xyz * vec3(0.1, 0.5, 0.1), 1.0);" +
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
                        "in vec4 color;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = color;" +
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

        // Define the grass blade vertices
        final float[] GRASS_BLADE = {
                -0.3f, 0.0f,
                0.3f, 0.0f,
                -0.20f, 1.0f,
                0.1f, 1.3f,
                -0.05f, 2.3f,
                0.0f, 3.3f
        };

        // Generate and bind VAO
        GLES32.glGenVertexArrays(1, grassVao, 0);
        GLES32.glBindVertexArray(grassVao[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, grassVbo, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, grassVbo[0]);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(GRASS_BLADE.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(GRASS_BLADE);
        floatBuffer.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, GRASS_BLADE.length * 4, floatBuffer, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(Attributes.VERTEX, 2, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(Attributes.VERTEX);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // Unbind the VBO and VAO
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
        texture[0] = textureHelper.load2DTexture(R.raw.grass_orientation);
        texture[1] = textureHelper.load2DTexture(R.raw.grass_bend);
        texture[2] = textureHelper.load2DTexture(R.raw.grass_color);
        texture[3] = textureHelper.load2DTexture(R.raw.grass_length);

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
        float t = (float) currentTime * 0.02f;
        float r = 550.0f;

        // Set the view matrix (lookAt)
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.setLookAtM(modelViewMatrix, 0, (float) Math.sin(t) * r, 25.0f, (float) Math.cos(t) * r,
                0.0f, -50.0f, 0.0f,
                0.0f, 1.0f, 0.0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        GLES32.glUniformMatrix4fv(uniforms.modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glUniform1i(uniforms.textureSampler[0], 1);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE2);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[1]);
        GLES32.glUniform1i(uniforms.textureSampler[1], 2);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE3);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[2]);
        GLES32.glUniform1i(uniforms.textureSampler[2], 3);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE4);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[3]);
        GLES32.glUniform1i(uniforms.textureSampler[3], 4);

        // Draw
        GLES32.glBindVertexArray(grassVao[0]);
        GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLE_STRIP, 0, 6, 1024 * 1024);
        GLES32.glBindVertexArray(0);

        // Unbind texture
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
        private final int[] textureSampler = {-1, -1, -1, -1};
        private int modelViewProjectionMatrix = -1;

        private void loadUniformLocations(int programId) {
            textureSampler[0] = GLES32.glGetUniformLocation(programId, "orientation_texture");
            textureSampler[1] = GLES32.glGetUniformLocation(programId, "bend_texture");
            textureSampler[2] = GLES32.glGetUniformLocation(programId, "grasscolor_texture");
            textureSampler[3] = GLES32.glGetUniformLocation(programId, "length_texture");
            modelViewProjectionMatrix = GLES32.glGetUniformLocation(programId, "modelViewProjectionMatrix");
        }
    }
}

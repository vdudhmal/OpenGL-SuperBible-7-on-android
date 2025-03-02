package com.vnd.superbible7.chapter11;

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
import com.vnd.superbible7.common.TextureHelper;
import com.vnd.superbible7.common.Timer;
import com.vnd.superbible7.common.shapes.Torus;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_BindlessTextures extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private static final int NUM_TEXTURES = 384;
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[1];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private Torus torus;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x1_BindlessTextures(Context context) {
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
                        "in vec4 position;" +
                        "in vec3 normal;" +
                        "in vec2 tc;" +
                        "uniform mat4 model_matrix[384];" +
                        "out VS_OUT" +
                        "{" +
                        "vec3 N;" +
                        "vec3 L;" +
                        "vec3 V;" +
                        "vec2 tc;" +
                        "flat int instance_index;" +
                        "} vs_out;" +
                        "const vec3 light_pos = vec3(100.0, 100.0, 100.0);" +
                        "void main(void)" +
                        "{" +
                        "mat4 view_matrix = model_matrix[0];" +
                        "mat4 proj_matrix = model_matrix[1];" +
                        "mat4 mv_matrix = view_matrix * model_matrix[gl_InstanceID + 2];" +
                        "vec4 P = mv_matrix * position;" +
                        "vs_out.N = mat3(mv_matrix) * normal;" +
                        "vs_out.L = light_pos - P.xyz;" +
                        "vs_out.V = -P.xyz;" +
                        "vs_out.tc = tc * vec2(5.0, 1.0);" +
                        "vs_out.instance_index = gl_InstanceID;" +
                        "gl_Position = proj_matrix * P;" +
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
                        "layout (location = 0) out vec4 color;" +
                        "in VS_OUT" +
                        "{" +
                        "vec3 N;" +
                        "vec3 L;" +
                        "vec3 V;" +
                        "vec2 tc;" +
                        "flat int instance_index;" +
                        "} fs_in;" +
                        "const vec3 ambient = vec3(0.1, 0.1, 0.1);" +
                        "const vec3 diffuse_albedo = vec3(0.9, 0.9, 0.9);" +
                        "const vec3 specular_albedo = vec3(0.7);" +
                        "const float specular_power = 300.0;" +
                        "uniform sampler2DArray textureSampler;" +
                        "void main(void)" +
                        "{" +
                        "vec3 N = normalize(fs_in.N);" +
                        "vec3 L = normalize(fs_in.L);" +
                        "vec3 V = normalize(fs_in.V);" +
                        "vec3 H = normalize(L + V);" +
                        "vec3 diffuse = max(dot(N, L), 0.0) * diffuse_albedo;" +
                        "diffuse *= texture(textureSampler, vec3(fs_in.tc * 2.0, fs_in.instance_index)).rgb;" +
                        "vec3 specular = pow(max(dot(N, H), 0.0), specular_power) * specular_albedo;" +
                        "color = vec4(ambient + diffuse + specular, 1.0);" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "position");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.TEXCOORD, "tc");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.NORMAL, "normal");
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

        // Render objects geometry
        torus = new Torus();

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // loading images to create texture
        texture[0] = textureHelper.generate2DTextureArray();

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
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 500.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        // Get time elapsed since the program started
        double currentTime = timer.getTotalTime();
        float f = (float) currentTime;

        // Clear the color buffer
        final float[] gray = {0.2f, 0.2f, 0.2f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, gray, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);
        float[][] modelMatrix = new float[NUM_TEXTURES][16];
        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.translateM(viewMatrix, 0, 0.0f, 0.0f, -80.0f);
        float angle = f;
        float angle2 = 0.7f * f;
        float angle3 = 0.1f * f;
        for (int i = 0; i < NUM_TEXTURES; i++) {
            Matrix.setIdentityM(modelMatrix[i], 0);
            float x = (i % 32) * 4.0f - 62.0f;
            float y = (i / 32.0f) * 6.0f - 33.0f;
            float z = 15.0f * (float) Math.sin(angle * 0.19f)
                    + 3.0f * (float) Math.cos(angle2 * 6.26f)
                    + 40.0f * (float) Math.sin(angle3);
            Matrix.translateM(modelMatrix[i], 0, x, y, z);
            Matrix.rotateM(modelMatrix[i], 0, angle * 130.0f, 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(modelMatrix[i], 0, angle * 140.0f, 0.0f, 0.0f, 1.0f);
            Matrix.scaleM(modelMatrix[i], 0, 0.5f, 0.5f, 0.5f);
            angle += 1.0f;
            angle2 += 4.1f;
            angle3 += 0.01f;
        }

        // Flatten the 2D array into a 1D array for glUniformMatrix4fv
        float[] modelMatrixData = new float[NUM_TEXTURES * 16];
        for (int i = 0; i < NUM_TEXTURES; i++) {
            if (i == 0) {
                System.arraycopy(viewMatrix, 0, modelMatrixData, 0, 16);
            } else if (i == 1) {
                System.arraycopy(perspectiveProjectionMatrix, 0, modelMatrixData, 16, 16);
            } else {
                System.arraycopy(modelMatrix[i - 2], 0, modelMatrixData, i * 16, 16);
            }
        }

        // Pass data to vertex shader
        GLES32.glUniformMatrix4fv(uniforms.model_matrix, NUM_TEXTURES, false, modelMatrixData, 0);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, texture[0]);
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Draw torus
        torus.draw(NUM_TEXTURES);

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D_ARRAY, 0);

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

        // torus
        torus.cleanup();
    }

    private static class Uniforms {
        private int model_matrix = -1;
        private int textureSampler = -1;

        private void loadUniformLocations(int programId) {
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
            model_matrix = GLES32.glGetUniformLocation(programId, "model_matrix");
        }
    }
}

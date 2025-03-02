package com.vnd.superbible7.chapter13;

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
import com.vnd.superbible7.common.shapes.Torus;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x8_PerPixelGloss extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture = new int[2];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    private Torus torus;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private Uniforms uniforms;

    public x8_PerPixelGloss(Context context) {
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
                        "in vec3 aNormal;" +
                        "in vec2 aTexcoord;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "uniform int numberOfImages;" +
                        "uniform int imageIndex;" +
                        "out vec3 oView;" +
                        "out vec3 oNormal;" +
                        "out vec2 oTexcoord;" +
                        "void main(void)" +
                        "{" +
                        "oView = vec4(modelViewMatrix * aVertex).xyz;" +
                        "oNormal = mat3(modelViewMatrix) * aNormal;" +
                        "oTexcoord = aTexcoord;" +
                        "gl_Position = projectionMatrix * modelViewMatrix * aVertex;" +
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
                        "precision highp sampler3D;" +
                        "in vec3 oView;" +
                        "in vec3 oNormal;" +
                        "in vec2 oTexcoord;" +
                        "uniform int imageIndex;" +
                        "uniform int numberOfImages;" +
                        "uniform sampler2D texture2DSampler;" +
                        "uniform sampler3D texture3DSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        // u will be our normalized view vector
                        "vec3 u = normalize(oView);" +
                        // Reflect u about the plane defined by the normal at the fragment
                        "vec3 r = reflect(u, normalize(oNormal));" +
                        // Compute scale factor
                        "r.z += 1.0;" +
                        "float m = 0.5 * inversesqrt(dot(r, r));" +
                        // Sample gloss factor from glossmap texture
                        "float gloss = texture(texture2DSampler, oTexcoord * vec2(3.0, 1.0) * 2.0).r;" +
                        // Sample from scaled and biased texture coordinate
                        "vec3 env_coord = vec3(r.xy * m + vec2(0.5), gloss);" +
                        // Sample from two-level environment map
                        "fragColor = texture(texture3DSampler, env_coord);" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.TEXCOORD, "aTexcoord");
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.NORMAL, "aNormal");
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
        texture[0] = textureHelper.load2DTexture(R.raw.pattern1);
        int[] imageResourceIds = {R.raw.mountains3d_0, R.raw.mountains3d_1};
        texture[1] = textureHelper.load3DTexture(imageResourceIds);

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

        // Get time elapsed since the program started
        double currentTime = timer.getTotalTime();

        // Clear the color buffer
        final float[] gray = {0.2f, 0.2f, 0.2f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, gray, 0);

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Apply transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -10.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) (currentTime * 13.75f), 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) (currentTime * 7.75f), 0.0f, 0.0f, 1.0f);
        Matrix.rotateM(modelViewMatrix, 0, (float) (currentTime * 15.3f), 1.0f, 0.0f, 0.0f);

        // Pass data to vertex shader
        GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
        GLES32.glUniform1i(uniforms.numberOfImages, 2); // sharp and blur images
        GLES32.glUniform1i(uniforms.imageIndex, 0);

        // Bind textures
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glUniform1i(uniforms.texture2DSampler, 0);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_3D, texture[1]);
        GLES32.glUniform1i(uniforms.texture3DSampler, 1);

        // Draw torus
        torus.draw();

        // Unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_3D, 0);
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

        // torus
        torus.cleanup();
    }

    private static class Uniforms {
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int texture2DSampler = -1;
        private int texture3DSampler = -1;
        private int numberOfImages = -1;
        private int imageIndex = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            texture2DSampler = GLES32.glGetUniformLocation(programId, "texture2DSampler");
            texture3DSampler = GLES32.glGetUniformLocation(programId, "texture3DSampler");
            numberOfImages = GLES32.glGetUniformLocation(programId, "numberOfImages");
            imageIndex = GLES32.glGetUniformLocation(programId, "imageIndex");
        }
    }
}

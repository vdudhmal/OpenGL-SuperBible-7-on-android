package com.vnd.superbible7.chapter8;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x4_DisplacementMap extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Timer timer = new Timer();
    private final TextureHelper textureHelper;
    boolean enableFog = true;
    boolean enableDisplacement = true;
    boolean paused = false;
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;
    private int tex_color = 0;
    private int tex_displacement = 0;
    private Uniforms uniforms;

    public x4_DisplacementMap(Context context) {
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
        enableFog = !enableFog;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        enableDisplacement = !enableDisplacement;
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
        paused = !paused;
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
                        "out vec2 vs_out_tc;" +
                        "void main(void)" +
                        "{" +
                        "const vec4 vertices[] = vec4[](vec4(-0.5, 0.0, -0.5, 1.0)," +
                        "vec4( 0.5, 0.0, -0.5, 1.0)," +
                        "vec4(-0.5, 0.0,  0.5, 1.0)," +
                        "vec4( 0.5, 0.0,  0.5, 1.0));" +
                        "int x = gl_InstanceID & 63;" +
                        "int y = gl_InstanceID >> 6;" +
                        "vec2 offs = vec2(x, y);" +
                        "vs_out_tc = (vertices[gl_VertexID].xz + offs + vec2(0.5)) / 64.0;" +
                        "gl_Position = vertices[gl_VertexID] + vec4(float(x - 32), 0.0, float(y - 32), 0.0);" +
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

        // tesselation control shader
        final String tcsShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (vertices = 4) out;" +
                        "in vec2 vs_out_tc[];" +
                        "out vec2 tcs_out_tc[];" +
                        "uniform mat4 modelViewProjectionMatrix;" +
                        "void main(void)" +
                        "{" +
                        "if (gl_InvocationID == 0)" +
                        "{" +
                        "vec4 p0 = modelViewProjectionMatrix * gl_in[0].gl_Position;" +
                        "vec4 p1 = modelViewProjectionMatrix * gl_in[1].gl_Position;" +
                        "vec4 p2 = modelViewProjectionMatrix * gl_in[2].gl_Position;" +
                        "vec4 p3 = modelViewProjectionMatrix * gl_in[3].gl_Position;" +
                        "p0 /= p0.w;" +
                        "p1 /= p1.w;" +
                        "p2 /= p2.w;" +
                        "p3 /= p3.w;" +
                        "if (p0.z <= 0.0 ||" +
                        "p1.z <= 0.0 ||" +
                        "p2.z <= 0.0 ||" +
                        "p3.z <= 0.0)" +
                        "{" +
                        "gl_TessLevelOuter[0] = 0.0;" +
                        "gl_TessLevelOuter[1] = 0.0;" +
                        "gl_TessLevelOuter[2] = 0.0;" +
                        "gl_TessLevelOuter[3] = 0.0;" +
                        "}" +
                        "else" +
                        "{" +
                        "float l0 = length(p2.xy - p0.xy) * 16.0 + 1.0;" +
                        "float l1 = length(p3.xy - p2.xy) * 16.0 + 1.0;" +
                        "float l2 = length(p3.xy - p1.xy) * 16.0 + 1.0;" +
                        "float l3 = length(p1.xy - p0.xy) * 16.0 + 1.0;" +
                        "gl_TessLevelOuter[0] = l0;" +
                        "gl_TessLevelOuter[1] = l1;" +
                        "gl_TessLevelOuter[2] = l2;" +
                        "gl_TessLevelOuter[3] = l3;" +
                        "gl_TessLevelInner[0] = min(l1, l3);" +
                        "gl_TessLevelInner[1] = min(l0, l2);" +
                        "}" +
                        "}" +
                        "gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;" +
                        "tcs_out_tc[gl_InvocationID] = vs_out_tc[gl_InvocationID];" +
                        "}";
        int tcsShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_CONTROL_SHADER);
        GLES32.glShaderSource(tcsShaderObject, tcsShaderSourceCode);
        GLES32.glCompileShader(tcsShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tcsShaderObject);
                System.out.println("VND: tcs shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        final String tesShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (quads, fractional_odd_spacing) in;" +
                        "uniform sampler2D tex_displacement;" +
                        "uniform mat4 modelViewMatrix;" +
                        "uniform mat4 projectionMatrix;" +
                        "uniform float dmap_depth;" +
                        "in vec2 tcs_out_tc[];" +
                        "out vec2 tes_out_tc;" +
                        "out vec3 tes_out_world_coord;" +
                        "out vec3 tes_out_eye_coord;" +
                        "void main(void)" +
                        "{" +
                        "vec2 tc1 = mix(tcs_out_tc[0], tcs_out_tc[1], gl_TessCoord.x);" +
                        "vec2 tc2 = mix(tcs_out_tc[2], tcs_out_tc[3], gl_TessCoord.x);" +
                        "vec2 tc = mix(tc2, tc1, gl_TessCoord.y);" +
                        "vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);" +
                        "vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);" +
                        "vec4 p = mix(p2, p1, gl_TessCoord.y);" +
                        "p.y += texture(tex_displacement, tc).r * dmap_depth;" +
                        "vec4 P_eye = modelViewMatrix * p;" +
                        "tes_out_tc = tc;" +
                        "tes_out_world_coord = p.xyz;" +
                        "tes_out_eye_coord = P_eye.xyz;" +
                        "gl_Position = projectionMatrix * P_eye;" +
                        "}";
        int tesShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject, tesShaderSourceCode);
        GLES32.glCompileShader(tesShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "out vec4 fragColor;" +
                        "uniform sampler2D tex_color;" +
                        "uniform bool enableFog;" +
                        "vec4 fog_color = vec4(0.7, 0.8, 0.9, 0.0);" +
                        "in vec2 tes_out_tc;" +
                        "in vec3 tes_out_world_coord;" +
                        "in vec3 tes_out_eye_coord;" +
                        "vec4 fog(vec4 c)" +
                        "{" +
                        "float z = length(tes_out_eye_coord);" +
                        "float de = 0.025 * smoothstep(0.0, 6.0, 10.0 - tes_out_world_coord.y);" +
                        "float di = 0.045 * (smoothstep(0.0, 40.0, 20.0 - tes_out_world_coord.y));" +
                        "float extinction = exp(-z * de);" +
                        "float inscattering = exp(-z * di);" +
                        "return c * extinction + fog_color * (1.0 - inscattering);" +
                        "}" +
                        "void main(void)" +
                        "{" +
                        "vec4 landscape = texture(tex_color, tes_out_tc);" +
                        "if (enableFog)" +
                        "{" +
                        "fragColor = fog(landscape);" +
                        "}" +
                        "else" +
                        "{" +
                        "fragColor = landscape;" +
                        "}" +
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
        GLES32.glAttachShader(shaderProgramObject, tcsShaderObject);
        GLES32.glAttachShader(shaderProgramObject, tesShaderObject);
        GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject, Attributes.VERTEX, "aVertex");
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
        tex_color = textureHelper.load2DTexture(R.raw.terragen_color);
        tex_displacement = textureHelper.load2DTexture(R.raw.terragen1);

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

        // Clear the color buffer
        final float[] black = {0.85f, 0.95f, 1.0f, 1.0f};
        GLES32.glClearBufferfv(GLES32.GL_COLOR, 0, black, 0);

        double currentTime = 0.0;
        if (!paused) {
            currentTime = timer.getTotalTime();
        }

        // Use the program
        GLES32.glUseProgram(shaderProgramObject);

        // Calculate time-dependent variables
        float t = (float) currentTime * 0.03f;
        float r = (float) Math.sin(t * 5.37f) * 15.0f + 16.0f;
        float h = (float) Math.cos(t * 4.79f) * 2.0f + 3.2f;

        float[] modelViewMatrix = new float[16];
        Matrix.setLookAtM(modelViewMatrix, 0,
                (float) Math.sin(t) * r, h, (float) Math.cos(t) * r,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0);

        // Push data to vertex shader
        GLES32.glUniformMatrix4fv(uniforms.modelViewMatrix, 1, false, modelViewMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.projectionMatrix, 1, false, perspectiveProjectionMatrix, 0);
        GLES32.glUniformMatrix4fv(uniforms.modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);
        GLES32.glUniform1f(uniforms.dmap_depth, enableDisplacement ? 6.0f : 0.0f);
        GLES32.glUniform1i(uniforms.enableFog, enableFog ? 1 : 0);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tex_displacement);
        GLES32.glUniform1i(uniforms.tex_displacement, 0);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tex_color);
        GLES32.glUniform1i(uniforms.tex_color, 1);

        //  Draw cube
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 4);
        GLES32.glDrawArraysInstanced(GLES32.GL_PATCHES, 0, 4, 64 * 64);

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
        private int modelViewMatrix = -1;
        private int projectionMatrix = -1;
        private int modelViewProjectionMatrix = -1;
        private int dmap_depth = -1;
        private int enableFog = -1;
        private int tex_color = -1;
        private int tex_displacement = -1;

        private void loadUniformLocations(int programId) {
            modelViewMatrix = GLES32.glGetUniformLocation(programId, "modelViewMatrix");
            projectionMatrix = GLES32.glGetUniformLocation(programId, "projectionMatrix");
            modelViewProjectionMatrix = GLES32.glGetUniformLocation(programId, "modelViewProjectionMatrix");
            dmap_depth = GLES32.glGetUniformLocation(programId, "dmap_depth");
            enableFog = GLES32.glGetUniformLocation(programId, "enableFog");
            tex_color = GLES32.glGetUniformLocation(programId, "tex_color");
            tex_displacement = GLES32.glGetUniformLocation(programId, "tex_displacement");
        }
    }
}

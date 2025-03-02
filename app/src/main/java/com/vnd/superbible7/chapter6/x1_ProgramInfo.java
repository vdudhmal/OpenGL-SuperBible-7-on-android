package com.vnd.superbible7.chapter6;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.superbible7.common.OpenGLInfo;

import java.nio.IntBuffer;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x1_ProgramInfo extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private GestureDetector gestureDetector = null;
    private int shaderProgramObject = 0;

    public x1_ProgramInfo(Context context) {
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
    @SuppressLint("DefaultLocale")
    private void initialize(GL10 gl) {
        // code
        OpenGLInfo.print(gl);

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es\n" +
                        "precision mediump float;" +
                        "in vec2 tc;" +
                        "in vec3 normal[4];" +
                        "out vec4 color;" +
                        "layout (location = 2) out ivec2 data;" +
                        "out float extra;" +
                        "void main(void)" +
                        "{" +
                        "float val = abs(tc.x + tc.y) * 20.0f;" +
                        "color = vec4(fract(val) >= 0.5 ? 1.0 : 0.25) + normal[3].xyxy;" +
                        "data = ivec2(1, 2);" +
                        "extra = 9.0;" +
                        "}";
        int fragmentShaderObject = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject, fragmentShaderSourceCode);
        GLES32.glCompileShader(fragmentShaderObject);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
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
        GLES32.glProgramParameteri(shaderProgramObject, GLES32.GL_PROGRAM_SEPARABLE, GLES32.GL_TRUE);
        //GLES32.glAttachShader(shaderProgramObject, vertexShaderObject);
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

        final Map<Integer, String> typeToNameMap = Map.ofEntries(
                Map.entry(GLES32.GL_FLOAT, "float"),
                Map.entry(GLES32.GL_FLOAT_VEC2, "vec2"),
                Map.entry(GLES32.GL_FLOAT_VEC3, "vec3"),
                Map.entry(GLES32.GL_FLOAT_VEC4, "vec4"),
                Map.entry(GLES32.GL_INT, "int"),
                Map.entry(GLES32.GL_INT_VEC2, "ivec2"),
                Map.entry(GLES32.GL_INT_VEC3, "ivec3"),
                Map.entry(GLES32.GL_INT_VEC4, "ivec4"),
                Map.entry(GLES32.GL_UNSIGNED_INT, "uint"),
                Map.entry(GLES32.GL_UNSIGNED_INT_VEC2, "uvec2"),
                Map.entry(GLES32.GL_UNSIGNED_INT_VEC3, "uvec3"),
                Map.entry(GLES32.GL_UNSIGNED_INT_VEC4, "uvec4"),
                Map.entry(GLES32.GL_BOOL, "bool"),
                Map.entry(GLES32.GL_BOOL_VEC2, "bvec2"),
                Map.entry(GLES32.GL_BOOL_VEC3, "bvec3"),
                Map.entry(GLES32.GL_BOOL_VEC4, "bvec4")
        );

        System.out.println("\n\n");
        int[] outputs = new int[1];
        GLES32.glGetProgramInterfaceiv(shaderProgramObject, GLES32.GL_PROGRAM_OUTPUT, GLES32.GL_ACTIVE_RESOURCES, outputs, 0);

        IntBuffer props = IntBuffer.wrap(new int[]{
                GLES32.GL_TYPE,              // Query type.
                GLES32.GL_LOCATION,          // Query location.
                GLES32.GL_ARRAY_SIZE         // Query array size.
        });

        StringBuilder log = new StringBuilder(GLES32.glGetProgramInfoLog(shaderProgramObject));
        log.append("Program linked\n");

        IntBuffer paramsBuffer = IntBuffer.allocate(3);
        for (int i = 0; i < outputs[0]; i++) {
            // Not yet implemented!
            // String name = GLES32.glGetProgramResourceName(shaderProgramObject, GLES32.GL_PROGRAM_OUTPUT, i);
            String name = "";

            GLES32.glGetProgramResourceiv(shaderProgramObject, GLES32.GL_PROGRAM_OUTPUT, i, 3, props, 3, null, paramsBuffer);
            String typeName = typeToNameMap.getOrDefault(paramsBuffer.get(0), "unknown");
            if (paramsBuffer.get(2) > 0) { // If it's an array
                log.append(String.format("Index %d: %s %s[%d] @ location %d\n", i, typeName, name, paramsBuffer.get(2), paramsBuffer.get(1)));
            } else { // Non-array attribute
                log.append(String.format("Index %d: %s %s @ location %d\n", i, typeName, name, paramsBuffer.get(1)));
            }
        }
        System.out.println(log);
        System.out.println("\n\n");

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

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

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
}

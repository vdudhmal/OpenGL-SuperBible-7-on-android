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
import com.vnd.superbible7.common.TextureHelper;

import java.nio.Buffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class x3_RGTCCompressor extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] shaderProgramObject = new int[2];
    private final int[] output_buffer = new int[1];
    private final int[] output_buffer_texture = new int[1];
    private final int[] input_texture = new int[1];
    private final int[] output_texture = new int[1];
    private final TextureHelper textureHelper;
    private final int TEXTURE_WIDTH = 512;
    private final int TEXTURE_HEIGHT = 512;
    boolean showInput = false;
    private GestureDetector gestureDetector = null;
    private Uniforms uniforms;

    public x3_RGTCCompressor(Context context) {
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
        showInput = !showInput;
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
                        "vec2 pos[] = vec2[](vec2(-1.0, -1.0), vec2( 1.0, -1.0), vec2(-1.0,  1.0), vec2( 1.0,  1.0));" +
                        "gl_Position = vec4(pos[gl_VertexID], 0.0, 1.0);" +
                        "uv = pos[gl_VertexID] * vec2(0.5, -0.5) + vec2(0.5);" +
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
                        "in vec2 uv;" +
                        "uniform sampler2D textureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = texelFetch(textureSampler, ivec2(gl_FragCoord.x, 511.0 - gl_FragCoord.y), 0).xxxx;" +
                        //"fragColor = texture(textureSampler, uv);" +
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
                        "precision highp sampler2D;" +
                        " layout (local_size_x = 1) in;" +
                        " layout (local_size_y = 1) in;" +
                        " layout (binding = 0) uniform sampler2D input_image;" +
                        " layout (binding = 0, rgba32ui) writeonly uniform uimageBuffer output_buffer;" +
                        " uniform PARAMS" +
                        " {" +
                        "     uint        uImageWidth;" +
                        " };" +
                        " void fetchTexels(uvec2 blockCoord, out float texels[16])" +
                        " {" +
                        "     vec2 texSize = vec2(textureSize(input_image, 0));" +
                        "     vec2 tl = (vec2(blockCoord * 4u) + vec2(1.0)) / texSize;" +
                        "     " +
                        "     vec4 tx0 = textureGatherOffset(input_image, tl, ivec2(0, 0));" +
                        "     vec4 tx1 = textureGatherOffset(input_image, tl, ivec2(2, 0));" +
                        "     vec4 tx2 = textureGatherOffset(input_image, tl, ivec2(0, 2));" +
                        "     vec4 tx3 = textureGatherOffset(input_image, tl, ivec2(2, 2));" +
                        "     texels[0] = tx0.w;" +
                        "     texels[1] = tx0.z;" +
                        "     texels[2] = tx1.w;" +
                        "     texels[3] = tx1.z;" +
                        "     texels[4] = tx0.x;" +
                        "     texels[5] = tx0.y;" +
                        "     texels[6] = tx1.x;" +
                        "     texels[7] = tx1.y;" +
                        "     texels[8] = tx2.w;" +
                        "     texels[9] = tx2.z;" +
                        "     texels[10] = tx3.w;" +
                        "     texels[11] = tx3.z;" +
                        "     texels[12] = tx2.x;" +
                        "     texels[13] = tx2.y;" +
                        "     texels[14] = tx3.x;" +
                        "     texels[15] = tx3.y;" +
                        " }" +
                        " void fetchTexels__(uvec2 blockCoord, out float texels[16])" +
                        " {" +
                        "     ivec2 tl = ivec2(blockCoord * 4u);" +
                        "     texels[0] = texelFetch(input_image, tl + ivec2(0, 0), 0).x;" +
                        "     texels[1] = texelFetch(input_image, tl + ivec2(1, 0), 0).x;" +
                        "     texels[2] = texelFetch(input_image, tl + ivec2(2, 0), 0).x;" +
                        "     texels[3] = texelFetch(input_image, tl + ivec2(3, 0), 0).x;" +
                        "     texels[4] = texelFetch(input_image, tl + ivec2(0, 1), 0).x;" +
                        "     texels[5] = texelFetch(input_image, tl + ivec2(1, 1), 0).x;" +
                        "     texels[6] = texelFetch(input_image, tl + ivec2(2, 1), 0).x;" +
                        "     texels[7] = texelFetch(input_image, tl + ivec2(3, 1), 0).x;" +
                        "     texels[8] = texelFetch(input_image, tl + ivec2(0, 2), 0).x;" +
                        "     texels[9] = texelFetch(input_image, tl + ivec2(1, 2), 0).x;" +
                        "     texels[10] = texelFetch(input_image, tl + ivec2(2, 2), 0).x;" +
                        "     texels[11] = texelFetch(input_image, tl + ivec2(3, 2), 0).x;" +
                        "     texels[12] = texelFetch(input_image, tl + ivec2(0, 3), 0).x;" +
                        "     texels[13] = texelFetch(input_image, tl + ivec2(1, 3), 0).x;" +
                        "     texels[14] = texelFetch(input_image, tl + ivec2(2, 3), 0).x;" +
                        "     texels[15] = texelFetch(input_image, tl + ivec2(3, 3), 0).x;" +
                        " }" +
                        " void buildPalette(float texels[16], out float palette[8])" +
                        " {" +
                        "     float minValue = 1.0;" +
                        "     float maxValue = 0.0;" +
                        "     int i;" +
                        "     for (i = 0; i < 16; i++)" +
                        "     {" +
                        "         maxValue = max(texels[i], maxValue);" +
                        "         minValue = min(texels[i], minValue);" +
                        "     }" +
                        "     palette[0] = maxValue;" +
                        "     palette[1] = minValue;" +
                        "     palette[2] = mix(maxValue, minValue, 1.0 / 7.0);" +
                        "     palette[3] = mix(maxValue, minValue, 2.0 / 7.0);" +
                        "     palette[4] = mix(maxValue, minValue, 3.0 / 7.0);" +
                        "     palette[5] = mix(maxValue, minValue, 4.0 / 7.0);" +
                        "     palette[6] = mix(maxValue, minValue, 5.0 / 7.0);" +
                        "     palette[7] = mix(maxValue, minValue, 6.0 / 7.0);" +
                        " }" +
                        " void buildPalette2(float texels[16], out float palette[8])" +
                        " {" +
                        "     float minValue = 1.0;" +
                        "     float maxValue = 0.0;" +
                        "     int i;" +
                        "     for (i = 0; i < 16; i++)" +
                        "     {" +
                        "         if (texels[i] != 1.0)" +
                        "         {" +
                        "             maxValue = max(texels[i], maxValue);" +
                        "         }" +
                        "         if (texels[i] != 0.0)" +
                        "         {" +
                        "             minValue = min(texels[i], minValue);" +
                        "         }" +
                        "     }" +
                        "     palette[0] = minValue;" +
                        "     palette[1] = maxValue;" +
                        "     palette[2] = mix(minValue, maxValue, 1.0 / 5.0);" +
                        "     palette[3] = mix(minValue, maxValue, 2.0 / 5.0);" +
                        "     palette[4] = mix(minValue, maxValue, 3.0 / 5.0);" +
                        "     palette[5] = mix(minValue, maxValue, 4.0 / 5.0);" +
                        "     palette[6] = 0.0;" +
                        "     palette[7] = 1.0;" +
                        " }" +
                        " float paletizeTexels(float texels[16], float palette[8], out uint entries[16])" +
                        " {" +
                        "     int i, j;" +
                        "     float totalError = 0.0;" +
                        "     for (i = 0; i < 16; i++)" +
                        "     {" +
                        "         int bestEntryIndex = 0;" +
                        "         float texel = texels[i];" +
                        "         float bestError = abs(texel - palette[0]);" +
                        "         for (j = 1; j < 8; j++)" +
                        "         {" +
                        "             float absError = abs(texel - palette[j]);" +
                        "             if (absError < bestError)" +
                        "             {" +
                        "                 bestError = absError;" +
                        "                 bestEntryIndex = j;" +
                        "             }" +
                        "         }" +
                        "         entries[i] = uint(bestEntryIndex);" +
                        "         totalError += bestError;" +
                        "     }" +
                        "     return totalError;" +
                        " }" +
                        " void packRGTC(float palette0," +
                        "               float palette1," +
                        "               uint entries[16]," +
                        "               out uvec2 block)" +
                        " {" +
                        "     uint t0 = 0u;" +
                        "     uint t1 = 0u;" +
                        "     t0 = (entries[0] << 0u) +" +
                        "          (entries[1] << 3u) +" +
                        "          (entries[2] << 6u) +" +
                        "          (entries[3] << 9u) +" +
                        "          (entries[4] << 12) +" +
                        "          (entries[5] << 15) +" +
                        "          (entries[6] << 18) +" +
                        "          (entries[7] << 21);" +
                        "     t1 = (entries[8] << 0u) +" +
                        "          (entries[9] << 3u) +" +
                        "          (entries[10] << 6u) +" +
                        "          (entries[11] << 9u) +" +
                        "          (entries[12] << 12u) +" +
                        "          (entries[13] << 15u) +" +
                        "          (entries[14] << 18u) +" +
                        "          (entries[15] << 21u);" +
                        "     block.x = (uint(palette0 * 255.0) << 0u) +" +
                        "               (uint(palette1 * 255.0) << 8u) +" +
                        "               (t0 << 16u);" +
                        "     block.y = (t0 >> 16u) + (t1 << 8u);" +
                        " }" +
                        " void main(void)" +
                        " {" +
                        "     float texels[16];" +
                        "     float palette[8];" +
                        "     uint entries[16];" +
                        "     float palette2[8];" +
                        "     uint entries2[16];" +
                        "     uvec2 compressed_block;" +
                        "     fetchTexels(gl_GlobalInvocationID.xy, texels);" +
                        "     buildPalette(texels, palette);" +
                        "     buildPalette2(texels, palette2);" +
                        "     float error1 = paletizeTexels(texels, palette, entries);" +
                        "     float error2 = paletizeTexels(texels, palette2, entries2);" +
                        "     if (error1 < error2)" +
                        "     {" +
                        "         packRGTC(palette[0]," +
                        "                  palette[1]," +
                        "                  entries," +
                        "                  compressed_block);" +
                        "     }" +
                        "     else" +
                        "     {" +
                        "         packRGTC(palette2[0]," +
                        "                  palette2[1]," +
                        "                  entries2," +
                        "                  compressed_block);" +
                        "     }" +
                        "     imageStore(output_buffer," +
                        "                int(gl_GlobalInvocationID.y * 128u + gl_GlobalInvocationID.x)," +
                        "                compressed_block.xyxy);" +
                        " }";
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

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        // Set the clear color of window to black
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // loading images to create texture
        input_texture[0] = textureHelper.load2DTexture(R.raw.gllogodistsm);

        GLES32.glGenTextures(1, output_texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, output_texture[0]);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        // glTexStorage2D(GL_TEXTURE_2D, 1, GL_COMPRESSED_RED_RGTC1, TEXTURE_WIDTH / 4, TEXTURE_HEIGHT / 4);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        GLES32.glGenBuffers(1, output_buffer, 0);
        GLES32.glBindBuffer(GLES32.GL_TEXTURE_BUFFER, output_buffer[0]);
        //GLES32.glBufferStorage(GLES32.GL_TEXTURE_BUFFER, TEXTURE_WIDTH * TEXTURE_HEIGHT / 2, null, GLES32.GL_MAP_READ_BIT);
        GLES32.glBufferData(GLES32.GL_TEXTURE_BUFFER, TEXTURE_WIDTH * TEXTURE_HEIGHT / 2, null, GLES32.GL_DYNAMIC_COPY);
        GLES32.glBindBuffer(GLES32.GL_TEXTURE_BUFFER, 0);

        GLES32.glGenTextures(1, output_buffer_texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, output_buffer_texture[0]);
        GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, GLES32.GL_RGBA32UI, output_buffer[0]);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, 0);

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

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, input_texture[0]);
        GLES32.glBindImageTexture(0, output_buffer_texture[0], 0, false, 0, GLES32.GL_WRITE_ONLY, GLES32.GL_RGBA32UI);

        // Run the compute shader
        GLES32.glUseProgram(shaderProgramObject[1]);
        GLES32.glDispatchCompute(TEXTURE_WIDTH / 4, TEXTURE_WIDTH / 4, 1);
        GLES32.glMemoryBarrier(GLES32.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GLES32.glFinish();

        // Read the output data from the buffer
        GLES32.glBindBuffer(GLES32.GL_PIXEL_UNPACK_BUFFER, output_buffer[0]);
        Buffer data = GLES32.glMapBufferRange(GLES32.GL_TEXTURE_BUFFER, 0, TEXTURE_WIDTH * TEXTURE_HEIGHT / 2, GLES32.GL_MAP_READ_BIT);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, output_texture[0]);
        //GLES32.glCompressedTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, 0, 0, 1024, 1024, GLES32.GL_COMPRESSED_R11_EAC, 1024 * 1024 / 2, data);
        GLES32.glCompressedTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_COMPRESSED_TEXTURE_FORMATS, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, TEXTURE_WIDTH * TEXTURE_HEIGHT / 2, data);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glUnmapBuffer(GLES32.GL_TEXTURE_BUFFER);

        // Stop using the program
        GLES32.glUseProgram(0);

        // Use the program1
        GLES32.glUseProgram(shaderProgramObject[0]);

        // Bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        if (showInput) {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, input_texture[0]);
        } else {
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, output_texture[0]);
        }
        GLES32.glUniform1i(uniforms.textureSampler, 0);

        // Draw point
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

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
        private int textureSampler = -1;

        private void loadUniformLocations(int programId) {
            textureSampler = GLES32.glGetUniformLocation(programId, "textureSampler");
        }
    }
}

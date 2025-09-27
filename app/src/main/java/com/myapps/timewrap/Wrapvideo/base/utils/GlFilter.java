package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.res.Resources;
import android.opengl.GLES20;
import java.util.HashMap;

public class GlFilter {
    protected static final String DEFAULT_FRAGMENT_SHADER = "precision mediump float;\nvarying highp vec2 vTextureCoord;\nuniform lowp sampler2D sTexture;\nvoid main() {\ngl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n";
    public static final String DEFAULT_UNIFORM_SAMPLER = "sTexture";
    protected static final String DEFAULT_VERTEX_SHADER = "attribute highp vec4 aPosition;\nattribute highp vec4 aTextureCoord;\nvarying highp vec2 vTextureCoord;\nvoid main() {\ngl_Position = aPosition;\nvTextureCoord = aTextureCoord.xy;\n}\n";
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final float[] VERTICES_DATA = {-1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f};
    protected static final int VERTICES_DATA_POS_OFFSET = 0;
    protected static final int VERTICES_DATA_POS_SIZE = 3;
    protected static final int VERTICES_DATA_STRIDE_BYTES = 20;
    protected static final int VERTICES_DATA_UV_OFFSET = 12;
    protected static final int VERTICES_DATA_UV_SIZE = 2;
    private int fragmentShader;
    private String fragmentShaderSource;
    private final HashMap<String, Integer> handleMap;
    private int program;
    private int vertexBufferName;
    private int vertexShader;
    private final String vertexShaderSource;

    public void onDraw() {
    }

    public void setFrameSize(int i, int i2) {
    }

    public GlFilter() {
        this(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }

    public GlFilter(Resources resources, int i, int i2) {
        this(resources.getString(i), resources.getString(i2));
    }

    public GlFilter(String str, String str2) {
        this.handleMap = new HashMap<>();
        this.vertexShaderSource = str;
        this.fragmentShaderSource = str2;
    }

    public void setup() {
        release();
        this.vertexShader = GlUtil.loadShader(35633, this.vertexShaderSource);
        int loadShader = GlUtil.loadShader(35632, this.fragmentShaderSource);
        this.fragmentShader = loadShader;
        this.program = GlUtil.createProgram(this.vertexShader, loadShader);
        this.vertexBufferName = GlUtil.createBuffer(VERTICES_DATA);
        getHandle("aPosition");
        getHandle("aTextureCoord");
        getHandle(DEFAULT_UNIFORM_SAMPLER);
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(1, 771);
    }

    public void setFragmentShaderSource(String str) {
        this.fragmentShaderSource = str;
    }

    public void release() {
        GLES20.glDeleteProgram(this.program);
        this.program = 0;
        GLES20.glDeleteShader(this.vertexShader);
        this.vertexShader = 0;
        GLES20.glDeleteShader(this.fragmentShader);
        this.fragmentShader = 0;
        GLES20.glDeleteBuffers(1, new int[]{this.vertexBufferName}, 0);
        this.vertexBufferName = 0;
        this.handleMap.clear();
    }

    public void draw(int i) {
        useProgram();
        GLES20.glBindBuffer(34962, this.vertexBufferName);
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"));
        GLES20.glVertexAttribPointer(getHandle("aPosition"), 3, 5126, false, 20, 0);
        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), 2, 5126, false, 20, 12);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, i);
        GLES20.glUniform1i(getHandle(DEFAULT_UNIFORM_SAMPLER), 0);
        onDraw();
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(getHandle("aPosition"));
        GLES20.glDisableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindBuffer(34962, 0);
    }

    public final void useProgram() {
        GLES20.glUseProgram(this.program);
    }

    public final int getVertexBufferName() {
        return this.vertexBufferName;
    }

    public final int getHandle(String str) {
        Integer num = this.handleMap.get(str);
        if (num != null) {
            return num.intValue();
        }
        int glGetAttribLocation = GLES20.glGetAttribLocation(this.program, str);
        if (glGetAttribLocation == -1) {
            glGetAttribLocation = GLES20.glGetUniformLocation(this.program, str);
        }
        if (glGetAttribLocation != -1) {
            this.handleMap.put(str, Integer.valueOf(glGetAttribLocation));
            return glGetAttribLocation;
        }
        throw new IllegalStateException("Could not get attrib or uniform location for " + str);
    }
}

package com.astro.app.core.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.LabelPrimitive;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders text labels in the sky map using OpenGL ES 2.0.
 *
 * <p>This renderer uses a texture atlas approach: all labels are pre-rendered
 * to a bitmap using Android's Canvas API, then uploaded as a single OpenGL
 * texture. Each label is drawn as a textured quad (billboard) that always
 * faces the camera.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Texture atlas for efficient batch rendering</li>
 *   <li>Billboarding - labels always face the camera</li>
 *   <li>Night mode support (red tint)</li>
 *   <li>Font size and color per label</li>
 *   <li>Label offset for positioning below stars</li>
 * </ul>
 *
 * <h3>Rendering Pipeline:</h3>
 * <ol>
 *   <li>Labels are collected and measured</li>
 *   <li>A texture atlas is created with all labels</li>
 *   <li>Each label stores its UV coordinates in the atlas</li>
 *   <li>At render time, quads are drawn at world positions using the atlas</li>
 * </ol>
 */
public class LabelRenderer {

    private static final String TAG = "LabelRenderer";

    /** Number of floats per vertex: position(3) + color(4) + texCoord(2) */
    private static final int FLOATS_PER_VERTEX = 9;

    /** Bytes per float */
    private static final int BYTES_PER_FLOAT = 4;

    /** Stride in bytes */
    private static final int STRIDE = FLOATS_PER_VERTEX * BYTES_PER_FLOAT;

    /** Vertices per label (quad) */
    private static final int VERTICES_PER_LABEL = 4;

    /** Indices per label (2 triangles) */
    private static final int INDICES_PER_LABEL = 6;

    /** Default texture atlas size */
    private static final int DEFAULT_ATLAS_SIZE = 1024;

    /** Padding between labels in the atlas */
    private static final int ATLAS_PADDING = 2;

    // Vertex shader for label rendering
    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
            "    vColor = aColor;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";

    // Fragment shader for textured labels
    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform bool uNightMode;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
            "    vec4 color = vColor * texColor;\n" +
            "    \n" +
            "    if (uNightMode) {\n" +
            "        // Convert to grayscale and tint red\n" +
            "        float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;\n" +
            "        color = vec4(gray * 0.8, gray * 0.1, gray * 0.1, color.a);\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = color;\n" +
            "}\n";

    private ShaderProgram shaderProgram;
    private boolean initialized = false;

    // Vertex buffer objects
    private int[] vboIds = new int[2]; // 0: vertex buffer, 1: index buffer
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;

    // Texture
    private int[] textureIds = new int[1];
    private int atlasWidth;
    private int atlasHeight;
    private float texelWidth;
    private float texelHeight;

    // Label data
    private List<LabelData> labelDataList = new ArrayList<>();
    private int labelCount = 0;
    private boolean nightMode = false;
    private float fontScale = 1.0f;

    // Paint for rendering text to bitmap
    private Paint textPaint;

    // Attribute and uniform locations
    private int positionHandle;
    private int colorHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int textureHandle;
    private int nightModeHandle;

    /**
     * Internal class to store label rendering data.
     */
    private static class LabelData {
        LabelPrimitive primitive;
        int width;
        int height;
        float u1, v1, u2, v2; // Texture coordinates in atlas
    }

    /**
     * Creates a new LabelRenderer.
     */
    public LabelRenderer() {
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    }

    /**
     * Initializes the renderer. Must be called on the GL thread.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        shaderProgram = new ShaderProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        if (!shaderProgram.isValid()) {
            Log.e(TAG, "Failed to create shader program");
            return;
        }

        // Get attribute and uniform locations
        positionHandle = shaderProgram.getAttribLocation("aPosition");
        colorHandle = shaderProgram.getAttribLocation("aColor");
        texCoordHandle = shaderProgram.getAttribLocation("aTexCoord");
        mvpMatrixHandle = shaderProgram.getUniformLocation("uMVPMatrix");
        textureHandle = shaderProgram.getUniformLocation("uTexture");
        nightModeHandle = shaderProgram.getUniformLocation("uNightMode");

        // Generate VBOs and texture
        GLES20.glGenBuffers(2, vboIds, 0);
        GLES20.glGenTextures(1, textureIds, 0);

        initialized = true;
        Log.d(TAG, "LabelRenderer initialized");
    }

    /**
     * Updates the labels to be rendered.
     *
     * <p>This regenerates the texture atlas with all labels.</p>
     *
     * @param labels List of label primitives
     */
    public void updateLabels(@NonNull List<LabelPrimitive> labels) {
        if (labels.isEmpty()) {
            labelCount = 0;
            labelDataList.clear();
            return;
        }

        // Prepare label data and measure text
        labelDataList.clear();
        for (LabelPrimitive label : labels) {
            LabelData data = new LabelData();
            data.primitive = label;

            textPaint.setTextSize(label.getFontSize() * fontScale);

            // Measure text
            data.width = (int) Math.ceil(textPaint.measureText(label.getText())) + ATLAS_PADDING * 2;
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            data.height = (int) Math.ceil(metrics.descent - metrics.ascent) + ATLAS_PADDING * 2;

            labelDataList.add(data);
        }

        // Create texture atlas
        createTextureAtlas();

        // Update vertex buffer
        updateVertexBuffer();
    }

    /**
     * Creates the texture atlas containing all labels.
     */
    private void createTextureAtlas() {
        if (labelDataList.isEmpty()) return;

        // Calculate atlas size needed
        atlasWidth = DEFAULT_ATLAS_SIZE;
        atlasHeight = DEFAULT_ATLAS_SIZE;

        // Layout labels in the atlas using simple row packing
        int currentX = 0;
        int currentY = 0;
        int rowHeight = 0;

        for (LabelData data : labelDataList) {
            // Check if we need to wrap to next row
            if (currentX + data.width > atlasWidth) {
                currentX = 0;
                currentY += rowHeight;
                rowHeight = 0;
            }

            // Check if we need a larger atlas
            if (currentY + data.height > atlasHeight) {
                // Double the atlas height
                atlasHeight *= 2;
                if (atlasHeight > 4096) {
                    Log.w(TAG, "Atlas size exceeds 4096, some labels may be truncated");
                    break;
                }
            }

            // Store UV coordinates
            data.u1 = (float) currentX / atlasWidth;
            data.v1 = (float) currentY / atlasHeight;
            data.u2 = (float) (currentX + data.width) / atlasWidth;
            data.v2 = (float) (currentY + data.height) / atlasHeight;

            currentX += data.width;
            rowHeight = Math.max(rowHeight, data.height);
        }

        texelWidth = 1.0f / atlasWidth;
        texelHeight = 1.0f / atlasHeight;

        // Create bitmap and draw labels
        Bitmap bitmap = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        for (LabelData data : labelDataList) {
            LabelPrimitive label = data.primitive;

            // Set text color (force full alpha for atlas, color will be applied via vertex)
            textPaint.setColor(0xFFFFFFFF); // White, color applied in shader
            textPaint.setTextSize(label.getFontSize() * fontScale);

            // Calculate position in atlas
            float x = data.u1 * atlasWidth + ATLAS_PADDING;
            float y = data.v1 * atlasHeight + ATLAS_PADDING - textPaint.getFontMetrics().ascent;

            canvas.drawText(label.getText(), x, y, textPaint);
        }

        // Upload to OpenGL texture
        if (initialized) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        bitmap.recycle();
        labelCount = labelDataList.size();
    }

    /**
     * Updates the vertex buffer with billboard quads for all labels.
     */
    private void updateVertexBuffer() {
        if (labelDataList.isEmpty()) return;

        int vertexCount = labelCount * VERTICES_PER_LABEL;
        int indexCount = labelCount * INDICES_PER_LABEL;

        // Allocate vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexCount * STRIDE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Allocate index buffer
        indexBuffer = ByteBuffer.allocateDirect(indexCount * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        // Fill buffers
        short vertexIndex = 0;
        for (LabelData data : labelDataList) {
            addLabelToBuffer(data, vertexIndex);

            // Add indices
            short baseIndex = (short) (vertexIndex * VERTICES_PER_LABEL);
            indexBuffer.put(baseIndex);
            indexBuffer.put((short) (baseIndex + 1));
            indexBuffer.put((short) (baseIndex + 2));
            indexBuffer.put((short) (baseIndex + 1));
            indexBuffer.put((short) (baseIndex + 3));
            indexBuffer.put((short) (baseIndex + 2));

            vertexIndex++;
        }

        vertexBuffer.position(0);
        indexBuffer.position(0);

        // Upload to VBOs
        if (initialized) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * BYTES_PER_FLOAT,
                    vertexBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 2,
                    indexBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    /**
     * Adds a label's vertex data to the buffer.
     */
    private void addLabelToBuffer(LabelData data, int labelIndex) {
        LabelPrimitive label = data.primitive;
        GeocentricCoords location = label.getLocation();
        float[] pos = location.toVector3();

        // Apply offset to position the label below the point
        float offset = label.getOffset();
        float x = pos[0];
        float y = pos[1];
        float z = pos[2];

        // Calculate billboard size in world space
        float worldWidth = data.width * 0.0001f;
        float worldHeight = data.height * 0.0001f;

        // Calculate billboard vectors
        float nx = x, ny = y, nz = z; // Normal (pointing outward from sphere)

        // Create perpendicular vectors for billboarding
        float ux, uy, uz;
        if (Math.abs(ny) > 0.99f) {
            ux = 1; uy = 0; uz = 0;
        } else {
            ux = nz;
            uy = 0;
            uz = -nx;
            float len = (float) Math.sqrt(ux * ux + uz * uz);
            if (len > 0) {
                ux /= len;
                uz /= len;
            }
        }

        // v = cross(u, normal)
        float vx = uy * nz - uz * ny;
        float vy = uz * nx - ux * nz;
        float vz = ux * ny - uy * nx;

        // Apply offset in the down direction (negative v)
        x -= vx * offset;
        y -= vy * offset;
        z -= vz * offset;

        // Scale by size
        float hw = worldWidth * 0.5f;
        float hh = worldHeight * 0.5f;

        // Color - extract from ARGB int
        int color = label.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Bottom-left
        addVertex(x - ux * hw - vx * hh, y - uy * hw - vy * hh, z - uz * hw - vz * hh,
                r, g, b, a, data.u1, data.v2);
        // Top-left
        addVertex(x - ux * hw + vx * hh, y - uy * hw + vy * hh, z - uz * hw + vz * hh,
                r, g, b, a, data.u1, data.v1);
        // Bottom-right
        addVertex(x + ux * hw - vx * hh, y + uy * hw - vy * hh, z + uz * hw - vz * hh,
                r, g, b, a, data.u2, data.v2);
        // Top-right
        addVertex(x + ux * hw + vx * hh, y + uy * hw + vy * hh, z + uz * hw + vz * hh,
                r, g, b, a, data.u2, data.v1);
    }

    /**
     * Adds a single vertex to the buffer.
     */
    private void addVertex(float x, float y, float z, float r, float g, float b, float a,
                           float u, float v) {
        vertexBuffer.put(x);
        vertexBuffer.put(y);
        vertexBuffer.put(z);
        vertexBuffer.put(r);
        vertexBuffer.put(g);
        vertexBuffer.put(b);
        vertexBuffer.put(a);
        vertexBuffer.put(u);
        vertexBuffer.put(v);
    }

    /**
     * Draws all labels.
     *
     * @param mvpMatrix The combined model-view-projection matrix
     */
    public void draw(@NonNull float[] mvpMatrix) {
        if (!initialized || labelCount == 0) {
            return;
        }

        shaderProgram.use();

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(nightModeHandle, nightMode ? 1 : 0);

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glUniform1i(textureHandle, 0);

        // Bind VBOs
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);

        // Position attribute
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, STRIDE, 0);

        // Color attribute
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, STRIDE,
                3 * BYTES_PER_FLOAT);

        // Texture coordinate attribute
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, STRIDE,
                7 * BYTES_PER_FLOAT);

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, labelCount * INDICES_PER_LABEL,
                GLES20.GL_UNSIGNED_SHORT, 0);

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Sets the font scale factor.
     *
     * @param scale The scale factor (1.0 = normal size)
     */
    public void setFontScale(float scale) {
        this.fontScale = scale;
    }

    /**
     * Enables or disables night mode (red tint).
     *
     * @param enabled true to enable night mode
     */
    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
    }

    /**
     * Returns the number of labels currently loaded.
     *
     * @return Label count
     */
    public int getLabelCount() {
        return labelCount;
    }

    /**
     * Releases all OpenGL resources.
     */
    public void release() {
        if (initialized) {
            GLES20.glDeleteBuffers(2, vboIds, 0);
            GLES20.glDeleteTextures(1, textureIds, 0);
            if (shaderProgram != null) {
                shaderProgram.release();
            }
            initialized = false;
        }
        labelDataList.clear();
    }
}

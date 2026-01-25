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

    // Attribute and uniform locations (initialized to -1 = invalid)
    private int positionHandle = -1;
    private int colorHandle = -1;
    private int texCoordHandle = -1;
    private int mvpMatrixHandle = -1;
    private int textureHandle = -1;
    private int nightModeHandle = -1;

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
     * Create a new LabelRenderer and prepare its text paint used for drawing label glyphs.
     *
     * The constructor initializes the internal Paint instance with anti-aliasing enabled and a
     * sans-serif typeface for composing label bitmaps.
     */
    public LabelRenderer() {
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    }

    /**
     * Initializes OpenGL resources and shader state required for rendering labels.
     *
     * <p>Must be called on the GL thread. This method is idempotent: if the renderer is already
     * initialized it returns immediately. On success it prepares and validates the shader program,
     * queries attribute and uniform locations, creates vertex and texture buffers, and marks the
     * renderer as initialized. If shader program creation fails the renderer remains uninitialized.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        Log.d(TAG, "Initializing LabelRenderer...");

        shaderProgram = new ShaderProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        if (!shaderProgram.isValid()) {
            Log.e(TAG, "Failed to create shader program - LabelRenderer will not render");
            return;
        }

        // Get attribute and uniform locations
        positionHandle = shaderProgram.getAttribLocation("aPosition");
        colorHandle = shaderProgram.getAttribLocation("aColor");
        texCoordHandle = shaderProgram.getAttribLocation("aTexCoord");
        mvpMatrixHandle = shaderProgram.getUniformLocation("uMVPMatrix");
        textureHandle = shaderProgram.getUniformLocation("uTexture");
        nightModeHandle = shaderProgram.getUniformLocation("uNightMode");

        // Log attribute/uniform locations for debugging
        Log.d(TAG, "Attribute locations: aPosition=" + positionHandle +
                ", aColor=" + colorHandle + ", aTexCoord=" + texCoordHandle);
        Log.d(TAG, "Uniform locations: uMVPMatrix=" + mvpMatrixHandle +
                ", uTexture=" + textureHandle + ", uNightMode=" + nightModeHandle);

        // Generate VBOs and texture
        GLES20.glGenBuffers(2, vboIds, 0);
        ShaderProgram.checkGLError("glGenBuffers");
        GLES20.glGenTextures(1, textureIds, 0);
        ShaderProgram.checkGLError("glGenTextures");

        initialized = true;
        Log.d(TAG, "LabelRenderer initialized successfully");
    }

    /**
     * Replace the current set of labels and rebuild the texture atlas and vertex buffers.
     *
     * <p>If {@code labels} is empty the renderer's labels are cleared. Otherwise each label's
     * text is measured to determine atlas placement, a new texture atlas is generated, and
     * the vertex/index buffers are updated for rendering.</p>
     *
     * @param labels the list of label primitives to render; replaces any existing labels
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
     * Builds a texture atlas for all labels and prepares GL upload data.
     *
     * Lays out each label into a bitmap atlas (row-based packing, doubling height when needed),
     * computes per-label UV coordinates and texel sizes, renders white text into the atlas
     * using the configured Paint and fontScale, uploads the bitmap to the GL texture when
     * the renderer is initialized, and updates labelCount.
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
     * Builds interleaved vertex and index buffers for all label billboards and uploads them to GL when initialized.
     *
     * Allocates a direct FloatBuffer for vertex attributes and a ShortBuffer for element indices, populates both
     * from the current labelDataList, resets buffer positions, and, if the renderer has been initialized, uploads
     * the data into the vertex and element array buffer objects.
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
     * Appends a label's billboarded quad (four vertices) into the renderer's vertex buffer.
     *
     * Computes a billboard oriented around the label's geocentric position, applies the
     * label offset and world-space size derived from the label's pixel dimensions, extracts
     * the label color from its ARGB integer, and writes four vertices with corresponding
     * texture coordinates into the vertex buffer via addVertex(...).
     *
     * @param data       label layout and atlas UVs for the label to add
     * @param labelIndex index of the label within the current label list (used to determine
     *                   placement order in buffers)
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
         * Appends a vertex's attributes (position, color, and texture coordinates) to the internal vertex buffer.
         *
         * @param x world-space X coordinate of the vertex
         * @param y world-space Y coordinate of the vertex
         * @param z world-space Z coordinate of the vertex
         * @param r red color component in the range 0.0 to 1.0
         * @param g green color component in the range 0.0 to 1.0
         * @param b blue color component in the range 0.0 to 1.0
         * @param a alpha (opacity) component in the range 0.0 to 1.0
         * @param u horizontal texture coordinate (atlas space, typically 0.0 to 1.0)
         * @param v vertical texture coordinate (atlas space, typically 0.0 to 1.0)
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
     * Renders all prepared label quads into the current OpenGL ES 2.0 context using the provided matrix.
     *
     * @param mvpMatrix the combined model‑view‑projection matrix used to transform label vertices into clip space
     */
    public void draw(@NonNull float[] mvpMatrix) {
        if (!initialized || labelCount == 0) {
            return;
        }

        // Check if shader program is valid before using
        if (shaderProgram == null || !shaderProgram.isValid()) {
            Log.w(TAG, "Shader program is not valid, skipping draw");
            return;
        }

        shaderProgram.use();

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set uniforms (only if handles are valid)
        if (mvpMatrixHandle >= 0) {
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        }
        if (nightModeHandle >= 0) {
            GLES20.glUniform1i(nightModeHandle, nightMode ? 1 : 0);
        }

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        if (textureHandle >= 0) {
            GLES20.glUniform1i(textureHandle, 0);
        }

        // Bind VBOs
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);

        // Position attribute (only enable if handle is valid)
        if (positionHandle >= 0) {
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, STRIDE, 0);
        }

        // Color attribute
        if (colorHandle >= 0) {
            GLES20.glEnableVertexAttribArray(colorHandle);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, STRIDE,
                    3 * BYTES_PER_FLOAT);
        }

        // Texture coordinate attribute
        if (texCoordHandle >= 0) {
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, STRIDE,
                    7 * BYTES_PER_FLOAT);
        }

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, labelCount * INDICES_PER_LABEL,
                GLES20.GL_UNSIGNED_SHORT, 0);

        // Cleanup (only disable if handles are valid)
        if (positionHandle >= 0) {
            GLES20.glDisableVertexAttribArray(positionHandle);
        }
        if (colorHandle >= 0) {
            GLES20.glDisableVertexAttribArray(colorHandle);
        }
        if (texCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Adjusts the multiplier applied to label font sizes when composing the texture atlas.
     *
     * @param scale multiplier for label font sizes; 1.0 preserves original size, values greater than 1 enlarge text, values between 0 and 1 reduce text size
     */
    public void setFontScale(float scale) {
        this.fontScale = scale;
    }

    /**
     * Toggle night mode that applies a red tint to rendered labels.
     *
     * @param enabled true to enable night mode, false to disable it
     */
    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
    }

    /**
     * Gets the number of labels currently loaded.
     *
     * @return the number of labels currently loaded
     */
    public int getLabelCount() {
        return labelCount;
    }

    /**
     * Indicates whether the renderer is ready for drawing operations.
     *
     * @return `true` if the renderer has been initialized and the shader program is present and valid, `false` otherwise.
     */
    public boolean isReady() {
        return initialized && shaderProgram != null && shaderProgram.isValid();
    }

    /**
     * Releases GPU resources used by the renderer and clears cached label data.
     *
     * Deletes vertex/index buffers and the atlas texture, releases the shader program if present,
     * resets the initialized flag, and clears the internal list of label data. Safe to call when
     * the renderer is not initialized.
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
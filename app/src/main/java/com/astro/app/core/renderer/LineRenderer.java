package com.astro.app.core.renderer;

import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.LinePrimitive;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Renders line primitives (constellation lines, grids) using OpenGL ES 2.0.
 *
 * <p>This renderer draws lines as textured quads to achieve anti-aliased,
 * variable-width lines. Each line segment is converted into a quad that
 * is always perpendicular to the view direction.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Anti-aliased line rendering</li>
 *   <li>Variable line width support</li>
 *   <li>Night mode support (red tint)</li>
 *   <li>Alpha blending for transparency</li>
 *   <li>Batch rendering for performance</li>
 * </ul>
 *
 * <h3>Vertex Data Layout (per vertex):</h3>
 * <ul>
 *   <li>Position (x, y, z) - 3 floats</li>
 *   <li>Color (r, g, b, a) - 4 floats</li>
 *   <li>Texture coordinates (u, v) - 2 floats</li>
 * </ul>
 */
public class LineRenderer {

    private static final String TAG = "LineRenderer";

    /** Number of floats per vertex: position(3) + color(4) + texCoord(2) */
    private static final int FLOATS_PER_VERTEX = 9;

    /** Bytes per float */
    private static final int BYTES_PER_FLOAT = 4;

    /** Stride in bytes */
    private static final int STRIDE = FLOATS_PER_VERTEX * BYTES_PER_FLOAT;

    /** Vertices per line segment (quad) */
    private static final int VERTICES_PER_SEGMENT = 4;

    /** Indices per line segment (2 triangles) */
    private static final int INDICES_PER_SEGMENT = 6;

    // Vertex shader for line rendering
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

    // Fragment shader for anti-aliased lines
    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
            "uniform bool uNightMode;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    // Distance from center of line (v coordinate)\n" +
            "    float dist = abs(vTexCoord.y - 0.5) * 2.0;\n" +
            "    \n" +
            "    // Soft edge for anti-aliasing\n" +
            "    float alpha = 1.0 - smoothstep(0.7, 1.0, dist);\n" +
            "    \n" +
            "    vec4 color = vColor;\n" +
            "    if (uNightMode) {\n" +
            "        // Convert to grayscale and tint red\n" +
            "        float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;\n" +
            "        color = vec4(gray * 0.8, gray * 0.1, gray * 0.1, color.a);\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
            "}\n";

    private ShaderProgram shaderProgram;
    private boolean initialized = false;

    // Vertex buffer objects
    private int[] vboIds = new int[2]; // 0: vertex buffer, 1: index buffer
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;

    // Current state
    private int segmentCount = 0;
    private boolean nightMode = false;
    private float lineWidthFactor = 1.0f;

    // Attribute and uniform locations
    private int positionHandle;
    private int colorHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int nightModeHandle;

    /**
     * Creates a new LineRenderer.
     */
    public LineRenderer() {
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
        nightModeHandle = shaderProgram.getUniformLocation("uNightMode");

        // Generate VBOs
        GLES20.glGenBuffers(2, vboIds, 0);

        initialized = true;
        Log.d(TAG, "LineRenderer initialized");
    }

    /**
     * Updates the line data to be rendered.
     *
     * @param lines List of line primitives
     */
    public void updateLines(@NonNull List<LinePrimitive> lines) {
        if (lines.isEmpty()) {
            segmentCount = 0;
            return;
        }

        // Count total segments
        int totalSegments = 0;
        for (LinePrimitive line : lines) {
            if (line.getVertexCount() >= 2) {
                totalSegments += line.getVertexCount() - 1;
            }
        }

        if (totalSegments == 0) {
            segmentCount = 0;
            return;
        }

        segmentCount = totalSegments;
        int vertexCount = segmentCount * VERTICES_PER_SEGMENT;
        int indexCount = segmentCount * INDICES_PER_SEGMENT;

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
        for (LinePrimitive line : lines) {
            List<GeocentricCoords> vertices = line.getVertices();
            if (vertices.size() < 2) continue;

            // Color components
            float r = ((line.getColor() >> 16) & 0xFF) / 255.0f;
            float g = ((line.getColor() >> 8) & 0xFF) / 255.0f;
            float b = (line.getColor() & 0xFF) / 255.0f;
            float a = ((line.getColor() >> 24) & 0xFF) / 255.0f;

            float lineWidth = line.getLineWidth() * lineWidthFactor * 0.001f;

            for (int i = 0; i < vertices.size() - 1; i++) {
                GeocentricCoords start = vertices.get(i);
                GeocentricCoords end = vertices.get(i + 1);

                addSegmentToBuffer(start, end, r, g, b, a, lineWidth);

                // Add indices for two triangles
                short baseIndex = (short) (vertexIndex * VERTICES_PER_SEGMENT);
                // Triangle 1: bottom-left, top-left, bottom-right
                indexBuffer.put(baseIndex);
                indexBuffer.put((short) (baseIndex + 1));
                indexBuffer.put((short) (baseIndex + 2));
                // Triangle 2: top-left, top-right, bottom-right
                indexBuffer.put((short) (baseIndex + 1));
                indexBuffer.put((short) (baseIndex + 3));
                indexBuffer.put((short) (baseIndex + 2));

                vertexIndex++;
            }
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
     * Adds a line segment's vertex data to the buffer.
     */
    private void addSegmentToBuffer(GeocentricCoords start, GeocentricCoords end,
                                    float r, float g, float b, float a, float lineWidth) {
        float[] p1 = start.toVector3();
        float[] p2 = end.toVector3();

        // Direction vector
        float dx = p2[0] - p1[0];
        float dy = p2[1] - p1[1];
        float dz = p2[2] - p1[2];

        // Midpoint for calculating perpendicular
        float mx = (p1[0] + p2[0]) * 0.5f;
        float my = (p1[1] + p2[1]) * 0.5f;
        float mz = (p1[2] + p2[2]) * 0.5f;

        // Perpendicular vector (cross product of direction and midpoint)
        // This makes the line face outward from the sphere
        float px = dy * mz - dz * my;
        float py = dz * mx - dx * mz;
        float pz = dx * my - dy * mx;

        // Normalize perpendicular
        float pLen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (pLen < 0.0001f) {
            // Fallback if perpendicular is degenerate
            px = 0;
            py = 1;
            pz = 0;
            pLen = 1;
        }
        px = (px / pLen) * lineWidth;
        py = (py / pLen) * lineWidth;
        pz = (pz / pLen) * lineWidth;

        // Four corners of the quad
        // Start-bottom
        addVertex(p1[0] - px, p1[1] - py, p1[2] - pz, r, g, b, a, 0, 0);
        // Start-top
        addVertex(p1[0] + px, p1[1] + py, p1[2] + pz, r, g, b, a, 0, 1);
        // End-bottom
        addVertex(p2[0] - px, p2[1] - py, p2[2] - pz, r, g, b, a, 1, 0);
        // End-top
        addVertex(p2[0] + px, p2[1] + py, p2[2] + pz, r, g, b, a, 1, 1);
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
     * Draws all lines.
     *
     * @param mvpMatrix The combined model-view-projection matrix
     */
    public void draw(@NonNull float[] mvpMatrix) {
        if (!initialized || segmentCount == 0) {
            return;
        }

        shaderProgram.use();

        // Enable blending for transparency and anti-aliasing
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(nightModeHandle, nightMode ? 1 : 0);

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
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, segmentCount * INDICES_PER_SEGMENT,
                GLES20.GL_UNSIGNED_SHORT, 0);

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Sets the line width multiplier for all lines.
     *
     * @param factor The scale factor (1.0 = normal width)
     */
    public void setLineWidthFactor(float factor) {
        this.lineWidthFactor = factor;
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
     * Returns the number of line segments currently loaded.
     *
     * @return Segment count
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    /**
     * Releases all OpenGL resources.
     */
    public void release() {
        if (initialized) {
            GLES20.glDeleteBuffers(2, vboIds, 0);
            if (shaderProgram != null) {
                shaderProgram.release();
            }
            initialized = false;
        }
    }
}

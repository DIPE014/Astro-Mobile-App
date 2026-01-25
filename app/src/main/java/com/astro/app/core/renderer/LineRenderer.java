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

    // Attribute and uniform locations (initialized to -1 = invalid)
    private int positionHandle = -1;
    private int colorHandle = -1;
    private int texCoordHandle = -1;
    private int mvpMatrixHandle = -1;
    private int nightModeHandle = -1;

    /**
     * Creates a new LineRenderer.
     */
    public LineRenderer() {
    }

    /**
     * Creates the OpenGL resources required by the renderer.
     *
     * Must be called on the GL thread. This method is a no-op if the renderer is already initialized.
     * It compiles/links the shader program, queries attribute and uniform locations, generates VBOs,
     * and marks the renderer as initialized. If shader program creation fails, initialization aborts
     * and the renderer remains not ready.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        Log.d(TAG, "Initializing LineRenderer...");

        shaderProgram = new ShaderProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        if (!shaderProgram.isValid()) {
            Log.e(TAG, "Failed to create shader program - LineRenderer will not render");
            return;
        }

        // Get attribute and uniform locations
        positionHandle = shaderProgram.getAttribLocation("aPosition");
        colorHandle = shaderProgram.getAttribLocation("aColor");
        texCoordHandle = shaderProgram.getAttribLocation("aTexCoord");
        mvpMatrixHandle = shaderProgram.getUniformLocation("uMVPMatrix");
        nightModeHandle = shaderProgram.getUniformLocation("uNightMode");

        // Log attribute/uniform locations for debugging
        Log.d(TAG, "Attribute locations: aPosition=" + positionHandle +
                ", aColor=" + colorHandle + ", aTexCoord=" + texCoordHandle);
        Log.d(TAG, "Uniform locations: uMVPMatrix=" + mvpMatrixHandle +
                ", uNightMode=" + nightModeHandle);

        // Generate VBOs
        GLES20.glGenBuffers(2, vboIds, 0);
        ShaderProgram.checkGLError("glGenBuffers");

        initialized = true;
        Log.d(TAG, "LineRenderer initialized successfully");
    }

    /**
     * Builds vertex and index buffers for the provided line primitives and prepares them for drawing.
     *
     * For each consecutive vertex pair in each LinePrimitive this method generates a quad (four vertices,
     * two triangles) representing a single line segment, updates the internal segment count, and stores
     * packed vertex and index data in the renderer's buffers. If the renderer is initialized the buffers
     * are uploaded to the GPU VBOs; otherwise the data is kept in client-side buffers until initialization.
     *
     * @param lines list of LinePrimitive objects whose vertices and per-line attributes (color, width)
     *              are used to construct the batched geometry; an empty list clears the segment count.
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
     * Convert the given line segment into a screen-facing quad and append its four vertices to the vertex buffer.
     *
     * Each vertex contains position (x,y,z), color (r,g,b,a) and texture coordinates; texture U maps start→end (0→1)
     * and V maps bottom→top (0→1).
     *
     * @param start     the segment start point in geocentric coordinates
     * @param end       the segment end point in geocentric coordinates
     * @param r         red color component (0.0–1.0)
     * @param g         green color component (0.0–1.0)
     * @param b         blue color component (0.0–1.0)
     * @param a         alpha component (0.0–1.0)
     * @param lineWidth half of the quad thickness in world units (the perpendicular offset applied to each side)
     *
     * Note: if the computed perpendicular is degenerate, a fallback perpendicular is used so the quad is still generated.
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
         * Appends a vertex to the internal vertex buffer in the expected attribute order:
         * position (x, y, z), color (r, g, b, a), then texture coordinates (u, v).
         *
         * @param x the vertex X coordinate
         * @param y the vertex Y coordinate
         * @param z the vertex Z coordinate
         * @param r red color component
         * @param g green color component
         * @param b blue color component
         * @param a alpha (opacity) component
         * @param u texture U coordinate
         * @param v texture V coordinate
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
     * Renders all prepared line segments using the provided model-view-projection matrix.
     *
     * If the renderer is not initialized or no segments are loaded, this call is a no-op.
     * The method uses the renderer's shader program and vertex/index buffers, enables blending
     * for proper anti-aliasing/transparency, and uploads the night-mode flag when enabled.
     *
     * @param mvpMatrix the combined model-view-projection matrix used to transform vertex positions
     */
    public void draw(@NonNull float[] mvpMatrix) {
        if (!initialized || segmentCount == 0) {
            return;
        }

        // Check if shader program is valid before using
        if (shaderProgram == null || !shaderProgram.isValid()) {
            Log.w(TAG, "Shader program is not valid, skipping draw");
            return;
        }

        shaderProgram.use();

        // Enable blending for transparency and anti-aliasing
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set uniforms (only if handles are valid)
        if (mvpMatrixHandle >= 0) {
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        }
        if (nightModeHandle >= 0) {
            GLES20.glUniform1i(nightModeHandle, nightMode ? 1 : 0);
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
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, segmentCount * INDICES_PER_SEGMENT,
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

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Set the global multiplier applied to line widths when constructing segment geometry.
     *
     * @param factor multiplier for line width (1.0 = original width; >1 increases width; values between 0 and 1 decrease width)
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
     * Report the number of line segments currently loaded for rendering.
     *
     * @return the number of prepared line segments
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    /**
     * Checks if the renderer is ready to draw.
     *
     * @return true if the renderer is initialized and has a valid shader program
     */
    public boolean isReady() {
        return initialized && shaderProgram != null && shaderProgram.isValid();
    }

    /**
     * Release OpenGL resources owned by this renderer.
     *
     * Deletes vertex/index buffer objects and releases the shader program if present,
     * then marks the renderer as not initialized. Calling this when the renderer is
     * not initialized is a no-op.
     *
     * <p>Must be called from the GL thread.</p>
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
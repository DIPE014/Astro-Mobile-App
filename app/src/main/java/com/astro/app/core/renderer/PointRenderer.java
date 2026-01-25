package com.astro.app.core.renderer;

import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.astro.app.data.model.PointPrimitive;
import com.astro.app.data.model.Shape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Renders point primitives (stars, planets) using OpenGL ES 2.0.
 *
 * <p>This renderer uses VBOs (Vertex Buffer Objects) and batch rendering
 * for optimal performance when displaying thousands of stars. Points are
 * rendered as textured quads that always face the camera (billboards).</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Point size based on stellar magnitude</li>
 *   <li>Customizable point shapes via fragment shader</li>
 *   <li>Night mode support (red tint)</li>
 *   <li>Batch rendering for performance</li>
 *   <li>VBO-based rendering</li>
 * </ul>
 *
 * <h3>Vertex Data Layout (per point):</h3>
 * <p>Each point consists of 4 vertices forming a quad. Each vertex has:</p>
 * <ul>
 *   <li>Position (x, y, z) - 3 floats</li>
 *   <li>Color (r, g, b, a) - 4 floats</li>
 *   <li>Texture coordinates (u, v) - 2 floats</li>
 *   <li>Point size - 1 float</li>
 * </ul>
 */
public class PointRenderer {

    private static final String TAG = "PointRenderer";

    /** Number of floats per vertex: position(3) + color(4) + texCoord(2) + size(1) */
    private static final int FLOATS_PER_VERTEX = 10;

    /** Bytes per float */
    private static final int BYTES_PER_FLOAT = 4;

    /** Stride in bytes */
    private static final int STRIDE = FLOATS_PER_VERTEX * BYTES_PER_FLOAT;

    /** Vertices per point (quad) */
    private static final int VERTICES_PER_POINT = 4;

    /** Indices per point (2 triangles) */
    private static final int INDICES_PER_POINT = 6;

    // Vertex shader for rendering points
    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform float uPointSizeFactor;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "attribute vec2 aTexCoord;\n" +
            "attribute float aPointSize;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
            "    vColor = aColor;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";

    // Fragment shader for rendering star shapes
    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
            "uniform int uShape;\n" +
            "uniform bool uNightMode;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vec2 center = vec2(0.5, 0.5);\n" +
            "    vec2 pos = vTexCoord - center;\n" +
            "    float dist = length(pos);\n" +
            "    float alpha = 1.0;\n" +
            "    \n" +
            "    // Shape rendering\n" +
            "    if (uShape == 0) {\n" +
            "        // Circle shape\n" +
            "        alpha = 1.0 - smoothstep(0.4, 0.5, dist);\n" +
            "    } else if (uShape == 1) {\n" +
            "        // Star shape with rays\n" +
            "        float angle = atan(pos.y, pos.x);\n" +
            "        float rays = abs(sin(angle * 4.0));\n" +
            "        float starDist = dist / (0.3 + 0.2 * rays);\n" +
            "        alpha = 1.0 - smoothstep(0.8, 1.0, starDist);\n" +
            "    } else if (uShape == 2) {\n" +
            "        // Diamond shape\n" +
            "        float diamond = abs(pos.x) + abs(pos.y);\n" +
            "        alpha = 1.0 - smoothstep(0.4, 0.5, diamond);\n" +
            "    } else if (uShape == 3) {\n" +
            "        // Square shape\n" +
            "        float square = max(abs(pos.x), abs(pos.y));\n" +
            "        alpha = 1.0 - smoothstep(0.4, 0.5, square);\n" +
            "    } else {\n" +
            "        // Default circle\n" +
            "        alpha = 1.0 - smoothstep(0.4, 0.5, dist);\n" +
            "    }\n" +
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
    private ByteBuffer indexBuffer;

    // Current state
    private int pointCount = 0;
    private float pointSizeFactor = 1.0f;
    private boolean nightMode = false;

    // Attribute and uniform locations (initialized to -1 = invalid)
    private int positionHandle = -1;
    private int colorHandle = -1;
    private int texCoordHandle = -1;
    private int pointSizeHandle = -1;
    private int mvpMatrixHandle = -1;
    private int pointSizeFactorHandle = -1;
    private int shapeHandle = -1;
    private int nightModeHandle = -1;

    /**
     * Creates a new PointRenderer.
     */
    public PointRenderer() {
    }

    /**
     * Prepare the renderer for use by creating its GL resources and shader program; must be called on the GL thread.
     *
     * Initializes the shader program and retrieves attribute/uniform locations, generates vertex/index buffer object IDs,
     * and marks the renderer as initialized. If already initialized this method returns immediately. If shader creation fails
     * the renderer remains uninitialized and will not render.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        Log.d(TAG, "Initializing PointRenderer...");

        shaderProgram = new ShaderProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        if (!shaderProgram.isValid()) {
            Log.e(TAG, "Failed to create shader program - PointRenderer will not render");
            return;
        }

        // Get attribute and uniform locations
        positionHandle = shaderProgram.getAttribLocation("aPosition");
        colorHandle = shaderProgram.getAttribLocation("aColor");
        texCoordHandle = shaderProgram.getAttribLocation("aTexCoord");
        pointSizeHandle = shaderProgram.getAttribLocation("aPointSize");
        mvpMatrixHandle = shaderProgram.getUniformLocation("uMVPMatrix");
        pointSizeFactorHandle = shaderProgram.getUniformLocation("uPointSizeFactor");
        shapeHandle = shaderProgram.getUniformLocation("uShape");
        nightModeHandle = shaderProgram.getUniformLocation("uNightMode");

        // Log attribute/uniform locations for debugging
        Log.d(TAG, "Attribute locations: aPosition=" + positionHandle +
                ", aColor=" + colorHandle + ", aTexCoord=" + texCoordHandle +
                ", aPointSize=" + pointSizeHandle);
        Log.d(TAG, "Uniform locations: uMVPMatrix=" + mvpMatrixHandle +
                ", uPointSizeFactor=" + pointSizeFactorHandle +
                ", uShape=" + shapeHandle + ", uNightMode=" + nightModeHandle);

        // Generate VBOs
        GLES20.glGenBuffers(2, vboIds, 0);
        ShaderProgram.checkGLError("glGenBuffers");

        initialized = true;
        Log.d(TAG, "PointRenderer initialized successfully");
    }

    /**
         * Replaces the renderer's point data with the provided list and prepares GPU buffers.
         *
         * Builds per-point vertex and index buffers (quads for billboarding) from the supplied
         * PointPrimitive list, sets the internal point count, and uploads the buffers to the
         * vertex/index VBOs when the renderer is initialized. If the list is empty, clears the
         * point count and leaves GPU resources unchanged.
         *
         * @param points the list of point primitives to render; each entry produces a quad (4 vertices, 6 indices)
         */
    public void updatePoints(@NonNull List<PointPrimitive> points) {
        if (points.isEmpty()) {
            pointCount = 0;
            return;
        }

        pointCount = points.size();
        int vertexCount = pointCount * VERTICES_PER_POINT;
        int indexCount = pointCount * INDICES_PER_POINT;

        // Allocate vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexCount * STRIDE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Allocate index buffer (short indices)
        indexBuffer = ByteBuffer.allocateDirect(indexCount * 2)
                .order(ByteOrder.nativeOrder());

        // Fill buffers
        short vertexIndex = 0;
        for (PointPrimitive point : points) {
            addPointToBuffer(point, vertexIndex);

            // Add indices for two triangles (quad)
            short baseIndex = (short) (vertexIndex * 4);
            // Triangle 1: bottom-left, top-left, bottom-right
            indexBuffer.putShort(baseIndex);
            indexBuffer.putShort((short) (baseIndex + 1));
            indexBuffer.putShort((short) (baseIndex + 2));
            // Triangle 2: top-left, top-right, bottom-right
            indexBuffer.putShort((short) (baseIndex + 1));
            indexBuffer.putShort((short) (baseIndex + 3));
            indexBuffer.putShort((short) (baseIndex + 2));

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
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity(),
                    indexBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    /**
     * Appends four vertices for a camera-facing billboard quad representing the given point to the vertex buffer.
     *
     * Each vertex contains position, color (normalized to 0–1), texture coordinates, and the point's size.
     *
     * @param point      the PointPrimitive whose geometry and attributes will be added to the buffer
     * @param pointIndex the index of this point within the point list (used to compute vertex/index offsets)
     */
    private void addPointToBuffer(PointPrimitive point, int pointIndex) {
        float[] pos = point.toVector3();
        float x = pos[0];
        float y = pos[1];
        float z = pos[2];

        // Color components (normalized)
        float r = point.getRed() / 255.0f;
        float g = point.getGreen() / 255.0f;
        float b = point.getBlue() / 255.0f;
        float a = point.getAlpha() / 255.0f;

        float size = point.getSize();

        // Calculate billboard quad vertices
        // The quad will be expanded in the vertex shader based on view direction
        // For now, we create a unit quad that will be transformed
        float halfSize = size * 0.001f; // Scale factor for world space

        // Calculate orthogonal vectors for billboarding
        // For a point on a unit sphere, the normal is the position itself
        float nx = x, ny = y, nz = z;

        // Create perpendicular vectors (u, v) to form the billboard
        // Cross product with up vector (0, 1, 0) to get right vector
        float ux, uy, uz;
        if (Math.abs(ny) > 0.99f) {
            // Point is near pole, use different up vector
            ux = 1; uy = 0; uz = 0;
        } else {
            // u = normalize(cross(normal, up))
            ux = nz;
            uy = 0;
            uz = -nx;
            float len = (float) Math.sqrt(ux * ux + uz * uz);
            ux /= len;
            uz /= len;
        }

        // v = cross(u, normal)
        float vx = uy * nz - uz * ny;
        float vy = uz * nx - ux * nz;
        float vz = ux * ny - uy * nx;

        // Scale by half size
        ux *= halfSize;
        uy *= halfSize;
        uz *= halfSize;
        vx *= halfSize;
        vy *= halfSize;
        vz *= halfSize;

        // Four corners of the quad
        // Bottom-left
        addVertex(x - ux - vx, y - uy - vy, z - uz - vz, r, g, b, a, 0, 0, size);
        // Top-left
        addVertex(x - ux + vx, y - uy + vy, z - uz + vz, r, g, b, a, 0, 1, size);
        // Bottom-right
        addVertex(x + ux - vx, y + uy - vy, z + uz - vz, r, g, b, a, 1, 0, size);
        // Top-right
        addVertex(x + ux + vx, y + uy + vy, z + uz + vz, r, g, b, a, 1, 1, size);
    }

    /**
         * Appends a single vertex's packed attributes to the active vertex buffer in the renderer's layout order.
         *
         * @param x     vertex x position in world coordinates
         * @param y     vertex y position in world coordinates
         * @param z     vertex z position in world coordinates
         * @param r     red color component (0.0–1.0)
         * @param g     green color component (0.0–1.0)
         * @param b     blue color component (0.0–1.0)
         * @param a     alpha component (0.0–1.0)
         * @param u     texture coordinate U (horizontal)
         * @param v     texture coordinate V (vertical)
         * @param size  per-vertex point size attribute used by the shader
         */
    private void addVertex(float x, float y, float z, float r, float g, float b, float a,
                           float u, float v, float size) {
        vertexBuffer.put(x);
        vertexBuffer.put(y);
        vertexBuffer.put(z);
        vertexBuffer.put(r);
        vertexBuffer.put(g);
        vertexBuffer.put(b);
        vertexBuffer.put(a);
        vertexBuffer.put(u);
        vertexBuffer.put(v);
        vertexBuffer.put(size);
    }

    /**
     * Renders all loaded points using the provided model-view-projection matrix with the circle shape.
     *
     * @param mvpMatrix the combined model-view-projection matrix used to transform point vertices to clip space
     */
    public void draw(@NonNull float[] mvpMatrix) {
        draw(mvpMatrix, Shape.CIRCLE);
    }

    /**
     * Render all loaded points as billboards using the provided model-view-projection matrix and shape.
     *
     * @param mvpMatrix the combined model-view-projection matrix used to transform point vertices
     * @param shape     the shape variant to use for fragment shading (selects shader shape mask)
     */
    public void draw(@NonNull float[] mvpMatrix, @NonNull Shape shape) {
        if (!initialized || pointCount == 0) {
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
        if (pointSizeFactorHandle >= 0) {
            GLES20.glUniform1f(pointSizeFactorHandle, pointSizeFactor);
        }
        if (shapeHandle >= 0) {
            GLES20.glUniform1i(shapeHandle, shapeToInt(shape));
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

        // Point size attribute
        if (pointSizeHandle >= 0) {
            GLES20.glEnableVertexAttribArray(pointSizeHandle);
            GLES20.glVertexAttribPointer(pointSizeHandle, 1, GLES20.GL_FLOAT, false, STRIDE,
                    9 * BYTES_PER_FLOAT);
        }

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, pointCount * INDICES_PER_POINT,
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
        if (pointSizeHandle >= 0) {
            GLES20.glDisableVertexAttribArray(pointSizeHandle);
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Map a Shape enum to the integer code expected by the fragment shader.
     *
     * @param shape the shape to convert
     * @return `0` for CIRCLE, `1` for STAR, `2` for DIAMOND, `3` for SQUARE,
     *         `4` for TRIANGLE, `5` for CROSS; defaults to `0` for unknown values
     */
    private int shapeToInt(Shape shape) {
        switch (shape) {
            case CIRCLE:
                return 0;
            case STAR:
                return 1;
            case DIAMOND:
                return 2;
            case SQUARE:
                return 3;
            case TRIANGLE:
                return 4;
            case CROSS:
                return 5;
            default:
                return 0;
        }
    }

    /**
     * Sets the point size factor for scaling all points.
     *
     * @param factor The scale factor (1.0 = normal size)
     */
    public void setPointSizeFactor(float factor) {
        this.pointSizeFactor = factor;
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
     * Get the current number of loaded points.
     *
     * @return the current number of loaded points
     */
    public int getPointCount() {
        return pointCount;
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
     * Free native GL resources held by this renderer and mark it uninitialized.
     *
     * Deletes the vertex and index VBOs and releases the compiled shader program if present.
     * This method is a no-op when the renderer is not initialized.
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
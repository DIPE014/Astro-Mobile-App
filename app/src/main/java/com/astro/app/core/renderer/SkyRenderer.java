package com.astro.app.core.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.core.layers.Layer;
import com.astro.app.core.math.Matrix4x4;
import com.astro.app.core.math.Vector3;
import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.LinePrimitive;
import com.astro.app.data.model.PointPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Main renderer for the sky map implementing GLSurfaceView.Renderer.
 *
 * <p>SkyRenderer manages the OpenGL rendering pipeline for displaying celestial
 * objects. It coordinates multiple sub-renderers (points, lines, labels) and
 * handles the transformation matrices for proper 3D projection.</p>
 *
 * <h3>Rendering Pipeline:</h3>
 * <ol>
 *   <li>Clear the screen</li>
 *   <li>Update projection and view matrices</li>
 *   <li>Render layers in depth order (back to front)</li>
 *   <li>For each layer: render lines, then points, then labels</li>
 * </ol>
 *
 * <h3>Coordinate System:</h3>
 * <p>The renderer uses a right-handed coordinate system where celestial objects
 * are placed on a unit sphere:</p>
 * <ul>
 *   <li>+X points toward RA=0h, Dec=0</li>
 *   <li>+Y points toward RA=6h, Dec=0</li>
 *   <li>+Z points toward Dec=+90 (north celestial pole)</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <p>This renderer is designed to be updated from any thread. Layer updates
 * are queued and processed on the GL thread.</p>
 *
 * @see PointRenderer
 * @see LineRenderer
 * @see LabelRenderer
 */
public class SkyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "SkyRenderer";

    // Sub-renderers
    private PointRenderer pointRenderer;
    private LineRenderer lineRenderer;
    private LabelRenderer labelRenderer;

    // Layers to render
    private final List<Layer> layers = new CopyOnWriteArrayList<>();
    private boolean layersNeedUpdate = true;

    // View state
    private int screenWidth = 1;
    private int screenHeight = 1;
    private float fieldOfView = 60.0f; // degrees
    private float nearPlane = 0.01f;
    private float farPlane = 100.0f;

    // Transformation matrices
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // View orientation
    private Vector3 lookDirection = new Vector3(1, 0, 0);
    private Vector3 upDirection = new Vector3(0, 0, 1);
    private boolean viewMatrixNeedsUpdate = true;

    // Custom transformation matrix (set externally)
    @Nullable
    private Matrix4x4 customTransformationMatrix;
    private boolean useCustomMatrix = false;

    // Night mode
    private boolean nightMode = false;

    // Background color - dark blue by default for better visibility
    // This provides a nice night sky background when not in AR mode
    private float bgRed = 0.02f;
    private float bgGreen = 0.02f;
    private float bgBlue = 0.08f;
    private float bgAlpha = 1.0f;

    // Render callbacks
    @Nullable
    private RenderCallback renderCallback;

    /**
     * Callback interface for render events.
     */
    public interface RenderCallback {
        /**
         * Called before each frame is rendered.
         *
         * @param renderer The renderer
         */
        void onPreRender(SkyRenderer renderer);

        /**
         * Called after each frame is rendered.
         *
         * @param renderer The renderer
         */
        void onPostRender(SkyRenderer renderer);
    }

    /**
     * Constructs a SkyRenderer and initializes its point, line, and label sub-renderers.
     */
    public SkyRenderer() {
        pointRenderer = new PointRenderer();
        lineRenderer = new LineRenderer();
        labelRenderer = new LabelRenderer();
    }

    /**
     * Initialize OpenGL state and sub-renderers when the GL surface is created.
     *
     * <p>Sets the clear color, enables depth testing and face culling, initializes
     * point/line/label sub-renderers, marks layer data for update, and logs GL
     * vendor/renderer/version information.</p>
     *
     * @param gl     the GL interface associated with the surface (may be unused)
     * @param config the EGL configuration used to create the surface
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated - OpenGL surface is ready!");
        Log.d(TAG, "GL_VENDOR: " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.d(TAG, "GL_RENDERER: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.d(TAG, "GL_VERSION: " + GLES20.glGetString(GLES20.GL_VERSION));

        // Set background color
        GLES20.glClearColor(bgRed, bgGreen, bgBlue, bgAlpha);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        // Enable face culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);

        // Initialize sub-renderers
        pointRenderer.initialize();
        lineRenderer.initialize();
        labelRenderer.initialize();

        // Mark layers for update
        layersNeedUpdate = true;

        Log.d(TAG, "OpenGL initialized");
    }

    /**
     * Handle a change in the GL surface size by updating viewport, internal size state, and projection.
     *
     * @param width  the new surface width in pixels; values less than 1 are treated as 1
     * @param height the new surface height in pixels; values less than 1 are treated as 1
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + "x" + height);

        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);

        GLES20.glViewport(0, 0, screenWidth, screenHeight);

        // Update projection matrix
        updateProjectionMatrix();
    }

    private static int frameCount = 0;

    /**
     * Render a single frame: update view/projection state, refresh layer data when needed, draw all layers, and invoke render callbacks.
     *
     * <p>Per-frame actions include invoking {@code renderCallback.onPreRender} and {@code onPostRender} (if set), clearing color and depth buffers
     * (temporarily using a bright red clear color for diagnostics), updating the view matrix, composing the MVP matrix (using the custom
     * transformation matrix when enabled), updating aggregated layer data when requested, rendering layers in depth order, and checking for GL errors.</p>
     *
     * @param gl the GL interface provided by GLSurfaceView
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        frameCount++;
        // Log every 60 frames to avoid spam
        if (frameCount % 60 == 1) {
            Log.d(TAG, "onDrawFrame called - frame #" + frameCount);
        }

        // Pre-render callback
        if (renderCallback != null) {
            renderCallback.onPreRender(this);
        }

        // DIAGNOSTIC: Temporarily set bright red to verify GL is working
        // If you see RED, OpenGL is working. If you see the View background color, it's not.
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);  // Bright red
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update view matrix if needed
        if (viewMatrixNeedsUpdate) {
            updateViewMatrix();
            viewMatrixNeedsUpdate = false;
        }

        // Calculate MVP matrix
        if (useCustomMatrix && customTransformationMatrix != null) {
            // Use custom transformation matrix
            float[] customMatrix = customTransformationMatrix.getFloatArray();
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, customMatrix, 0);
        } else {
            // Use calculated view matrix
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        }

        // Update layer data if needed
        if (layersNeedUpdate) {
            updateLayerData();
            layersNeedUpdate = false;
        }

        // Render layers in depth order
        renderLayers();

        // Post-render callback
        if (renderCallback != null) {
            renderCallback.onPostRender(this);
        }

        // Check for errors
        ShaderProgram.checkGLError("onDrawFrame");
    }

    /**
     * Updates the projection matrix based on current FOV and screen dimensions.
     */
    private void updateProjectionMatrix() {
        float aspectRatio = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, fieldOfView, aspectRatio, nearPlane, farPlane);
    }

    /**
     * Updates the view matrix based on look and up directions.
     */
    private void updateViewMatrix() {
        // Eye position at origin (center of celestial sphere)
        float eyeX = 0, eyeY = 0, eyeZ = 0;

        // Look at a point in the look direction
        float centerX = lookDirection.x;
        float centerY = lookDirection.y;
        float centerZ = lookDirection.z;

        // Up vector
        float upX = upDirection.x;
        float upY = upDirection.y;
        float upZ = upDirection.z;

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                centerX, centerY, centerZ,
                upX, upY, upZ);
    }

    /**
     * Aggregates primitives from visible layers in depth order and updates the sub-renderers.
     *
     * Visible layers are processed in depth order; their points, lines, and labels
     * are collected into lists and passed to the point, line, and label renderers.
     */
    private void updateLayerData() {
        List<PointPrimitive> allPoints = new ArrayList<>();
        List<LinePrimitive> allLines = new ArrayList<>();
        List<LabelPrimitive> allLabels = new ArrayList<>();

        // Sort layers by depth order
        List<Layer> sortedLayers = new ArrayList<>(layers);
        Collections.sort(sortedLayers, Comparator.comparingInt(Layer::getLayerDepthOrder));

        for (Layer layer : sortedLayers) {
            if (!layer.isVisible()) continue;

            allPoints.addAll(layer.getPoints());
            allLines.addAll(layer.getLines());
            allLabels.addAll(layer.getLabels());
        }

        // Update sub-renderers
        pointRenderer.updatePoints(allPoints);
        lineRenderer.updateLines(allLines);
        labelRenderer.updateLabels(allLabels);

        Log.d(TAG, String.format("Updated: %d points, %d lines, %d labels",
                allPoints.size(), allLines.size(), allLabels.size()));
    }

    /**
     * Renders aggregated layer primitives with depth writing disabled so transparent elements compose correctly.
     *
     * Renders in back-to-front order: lines, then points, then labels, and restores depth writing afterward.
     */
    private void renderLayers() {
        // Disable depth writing for transparent objects
        GLES20.glDepthMask(false);

        // Render in order: lines (back), points (middle), labels (front)
        lineRenderer.draw(mvpMatrix);
        pointRenderer.draw(mvpMatrix);
        labelRenderer.draw(mvpMatrix);

        // Re-enable depth writing
        GLES20.glDepthMask(true);
    }

    // ========== Public API ==========

    /**
         * Replace the renderer's current layers with the provided list and mark layers for update.
         *
         * The new list completely replaces any existing layers; layer data will be recomputed on the
         * next render pass.
         *
         * @param newLayers the layers to render (must not be null)
         */
    public void setLayers(@NonNull List<Layer> newLayers) {
        layers.clear();
        layers.addAll(newLayers);
        layersNeedUpdate = true;
    }

    /**
     * Add the given layer to the renderer and schedule an update of aggregated layer data.
     *
     * @param layer the layer to add; must not be null
     */
    public void addLayer(@NonNull Layer layer) {
        layers.add(layer);
        layersNeedUpdate = true;
    }

    /**
     * Remove the given layer from the renderer and schedule a layer data update.
     *
     * @param layer the layer to remove from rendering
     */
    public void removeLayer(@NonNull Layer layer) {
        layers.remove(layer);
        layersNeedUpdate = true;
    }

    /**
         * Remove all layers from the renderer and mark layer data for update.
         *
         * This clears the internal layer collection and sets the flag so aggregated
         * layer data will be rebuilt on the next render/update pass.
         */
    public void clearLayers() {
        layers.clear();
        layersNeedUpdate = true;
    }

    /**
     * Marks layers for update on the next frame.
     *
     * <p>Call this after modifying layer data.</p>
     */
    public void requestLayerUpdate() {
        layersNeedUpdate = true;
    }

    /**
     * Sets a custom transformation matrix to replace the internally calculated view matrix.
     *
     * @param matrix the transformation matrix to use, or `null` to disable the custom matrix and
     *               resume using the internally computed view orientation
     */
    public void setTransformationMatrix(@Nullable Matrix4x4 matrix) {
        this.customTransformationMatrix = matrix;
        this.useCustomMatrix = (matrix != null);
    }

    /**
     * Update the renderer's viewing orientation from a look direction and an up direction.
     *
     * The provided look direction will be normalized and the provided up direction will be made
     * orthogonal to the resulting look direction and normalized. This marks the view matrix
     * for recomputation and disables any previously set custom transformation matrix.
     *
     * @param lookDir the desired look direction vector (will be normalized)
     * @param upDir   the desired up direction vector (will be orthogonalized to the look direction and normalized)
     */
    public void setViewOrientation(@NonNull Vector3 lookDir, @NonNull Vector3 upDir) {
        // Normalize look direction
        float lookLen = lookDir.getLength();
        if (lookLen > 0.0001f) {
            this.lookDirection = new Vector3(
                    lookDir.x / lookLen,
                    lookDir.y / lookLen,
                    lookDir.z / lookLen);
        }

        // Orthogonalize up direction
        float dot = lookDir.dot(upDir);
        Vector3 orthogonalUp = new Vector3(
                upDir.x - dot * lookDirection.x,
                upDir.y - dot * lookDirection.y,
                upDir.z - dot * lookDirection.z);
        float upLen = orthogonalUp.getLength();
        if (upLen > 0.0001f) {
            this.upDirection = new Vector3(
                    orthogonalUp.x / upLen,
                    orthogonalUp.y / upLen,
                    orthogonalUp.z / upLen);
        }

        viewMatrixNeedsUpdate = true;
        useCustomMatrix = false;
    }

    /**
         * Set the view orientation using explicit vector components for the look and up directions.
         *
         * The provided look vector will be normalized and the up vector will be adjusted to be
         * orthogonal to the look direction. Calling this disables any custom transformation matrix
         * and schedules the internal view matrix to be updated.
         *
         * @param lookX X component of the look (forward) direction
         * @param lookY Y component of the look (forward) direction
         * @param lookZ Z component of the look (forward) direction
         * @param upX   X component of the up direction
         * @param upY   Y component of the up direction
         * @param upZ   Z component of the up direction
         */
    public void setViewOrientation(float lookX, float lookY, float lookZ,
                                   float upX, float upY, float upZ) {
        setViewOrientation(new Vector3(lookX, lookY, lookZ), new Vector3(upX, upY, upZ));
    }

    /**
     * Set the vertical field of view used for the perspective projection.
     *
     * The provided value is clamped to the range 10–120 degrees and the projection
     * matrix is updated immediately.
     *
     * @param fovDegrees desired field of view in degrees; values outside 10–120
     *                   will be clamped to that range
     */
    public void setFieldOfView(float fovDegrees) {
        this.fieldOfView = Math.max(10.0f, Math.min(120.0f, fovDegrees));
        updateProjectionMatrix();
    }

    /**
         * Get the field of view used for the perspective projection.
         *
         * Values set via {@link #setFieldOfView(float)} are clamped to 10–120 degrees.
         *
         * @return the field of view in degrees
         */
    public float getFieldOfView() {
        return fieldOfView;
    }

    /**
     * Toggle night mode to apply a red-tinted display for dark adaptation.
     *
     * When enabled, sets the internal night-mode flag, propagates the setting to
     * point, line, and label sub-renderers, and adjusts the background color to a
     * red tint suitable for dark adaptation. When disabled, restores the standard
     * dark-blue background and clears night-mode on sub-renderers.
     *
     * @param enabled true to enable night mode, false to disable it
     */
    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
        pointRenderer.setNightMode(enabled);
        lineRenderer.setNightMode(enabled);
        labelRenderer.setNightMode(enabled);

        // Adjust background color for night mode
        if (enabled) {
            bgRed = 0.05f;
            bgGreen = 0.0f;
            bgBlue = 0.0f;
        } else {
            // Dark blue background for normal mode
            bgRed = 0.02f;
            bgGreen = 0.02f;
            bgBlue = 0.08f;
        }
    }

    /**
         * Indicates whether night mode is enabled.
         *
         * @return `true` if night mode is enabled, `false` otherwise.
         */
    public boolean isNightMode() {
        return nightMode;
    }

    /**
     * Set the renderer's background (clear) color.
     *
     * @param red   Red component, in the range 0 to 1.
     * @param green Green component, in the range 0 to 1.
     * @param blue  Blue component, in the range 0 to 1.
     * @param alpha Alpha component (opacity), in the range 0 to 1.
     */
    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        bgRed = red;
        bgGreen = green;
        bgBlue = blue;
        bgAlpha = alpha;
    }

    /**
     * Sets the point size scaling factor.
     *
     * @param factor Scale factor (1.0 = normal)
     */
    public void setPointSizeFactor(float factor) {
        pointRenderer.setPointSizeFactor(factor);
    }

    /**
     * Sets the line width scaling factor.
     *
     * @param factor Scale factor (1.0 = normal)
     */
    public void setLineWidthFactor(float factor) {
        lineRenderer.setLineWidthFactor(factor);
    }

    /**
     * Sets the font scaling factor for labels.
     *
     * @param scale Scale factor (1.0 = normal)
     */
    public void setFontScale(float scale) {
        labelRenderer.setFontScale(scale);
    }

    /**
     * Sets the render callback.
     *
     * @param callback The callback, or null to remove
     */
    public void setRenderCallback(@Nullable RenderCallback callback) {
        this.renderCallback = callback;
    }

    /**
     * Retrieve the current look direction of the view.
     *
     * @return a new Vector3 containing the current look direction; modifying it does not affect the renderer
     */
    @NonNull
    public Vector3 getLookDirection() {
        return lookDirection.copyForJ();
    }

    /**
         * Get the renderer's up direction vector.
         *
         * @return A copy of the renderer's up direction Vector3; modifying the returned vector does not affect internal state.
         */
    @NonNull
    public Vector3 getUpDirection() {
        return upDirection.copyForJ();
    }

    /**
     * Get the current screen width in pixels.
     *
     * @return the current screen width in pixels
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
         * Gets the current screen height used by the renderer.
         *
         * @return the current height in pixels
         */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
         * Get the current model-view-projection (MVP) matrix.
         *
         * @return a 16-element float array containing a copy of the current MVP matrix
         */
    @NonNull
    public float[] getMvpMatrix() {
        return mvpMatrix.clone();
    }

    /**
     * Release OpenGL resources held by this renderer and clear all managed layers.
     *
     * <p>This frees resources held by the point, line, and label sub-renderers and empties the
     * internal layer list.</p>
     */
    public void release() {
        if (pointRenderer != null) {
            pointRenderer.release();
        }
        if (lineRenderer != null) {
            lineRenderer.release();
        }
        if (labelRenderer != null) {
            labelRenderer.release();
        }
        layers.clear();
    }
}
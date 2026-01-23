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
     * Creates a new SkyRenderer.
     */
    public SkyRenderer() {
        pointRenderer = new PointRenderer();
        lineRenderer = new LineRenderer();
        labelRenderer = new LabelRenderer();
    }

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
     * Collects and updates primitive data from all layers.
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
     * Renders all layers.
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
     * Sets the layers to render.
     *
     * @param newLayers The layers to render
     */
    public void setLayers(@NonNull List<Layer> newLayers) {
        layers.clear();
        layers.addAll(newLayers);
        layersNeedUpdate = true;
    }

    /**
     * Adds a layer to render.
     *
     * @param layer The layer to add
     */
    public void addLayer(@NonNull Layer layer) {
        layers.add(layer);
        layersNeedUpdate = true;
    }

    /**
     * Removes a layer.
     *
     * @param layer The layer to remove
     */
    public void removeLayer(@NonNull Layer layer) {
        layers.remove(layer);
        layersNeedUpdate = true;
    }

    /**
     * Clears all layers.
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
     * Sets a custom transformation matrix for the view.
     *
     * <p>When set, this matrix replaces the internally calculated view matrix.
     * Use this to integrate with external orientation sources.</p>
     *
     * @param matrix The transformation matrix, or null to use internal view
     */
    public void setTransformationMatrix(@Nullable Matrix4x4 matrix) {
        this.customTransformationMatrix = matrix;
        this.useCustomMatrix = (matrix != null);
    }

    /**
     * Sets the view orientation.
     *
     * @param lookDir The direction to look (will be normalized)
     * @param upDir   The up direction (will be orthogonalized)
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
     * Sets the view orientation from raw float values.
     *
     * @param lookX Look direction X
     * @param lookY Look direction Y
     * @param lookZ Look direction Z
     * @param upX   Up direction X
     * @param upY   Up direction Y
     * @param upZ   Up direction Z
     */
    public void setViewOrientation(float lookX, float lookY, float lookZ,
                                   float upX, float upY, float upZ) {
        setViewOrientation(new Vector3(lookX, lookY, lookZ), new Vector3(upX, upY, upZ));
    }

    /**
     * Sets the field of view.
     *
     * @param fovDegrees Field of view in degrees (typically 30-90)
     */
    public void setFieldOfView(float fovDegrees) {
        this.fieldOfView = Math.max(10.0f, Math.min(120.0f, fovDegrees));
        updateProjectionMatrix();
    }

    /**
     * Returns the current field of view.
     *
     * @return FOV in degrees
     */
    public float getFieldOfView() {
        return fieldOfView;
    }

    /**
     * Enables or disables night mode (red tint for dark adaptation).
     *
     * @param enabled true to enable night mode
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
     * Returns whether night mode is enabled.
     *
     * @return true if night mode is enabled
     */
    public boolean isNightMode() {
        return nightMode;
    }

    /**
     * Sets the background color.
     *
     * @param red   Red component (0-1)
     * @param green Green component (0-1)
     * @param blue  Blue component (0-1)
     * @param alpha Alpha component (0-1)
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
     * Returns the current look direction.
     *
     * @return Look direction vector
     */
    @NonNull
    public Vector3 getLookDirection() {
        return lookDirection.copyForJ();
    }

    /**
     * Returns the current up direction.
     *
     * @return Up direction vector
     */
    @NonNull
    public Vector3 getUpDirection() {
        return upDirection.copyForJ();
    }

    /**
     * Returns the screen width.
     *
     * @return Width in pixels
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Returns the screen height.
     *
     * @return Height in pixels
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Returns the current MVP matrix.
     *
     * @return The 16-element MVP matrix array
     */
    @NonNull
    public float[] getMvpMatrix() {
        return mvpMatrix.clone();
    }

    /**
     * Releases all OpenGL resources.
     *
     * <p>Call this when the renderer is no longer needed.</p>
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

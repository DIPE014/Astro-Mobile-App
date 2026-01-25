package com.astro.app.core.layers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.LinePrimitive;
import com.astro.app.data.model.PointPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract base implementation of the {@link Layer} interface.
 *
 * <p>This class provides common functionality for all layer implementations,
 * including visibility management, primitive storage, and thread-safe access
 * to the primitive lists.</p>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class uses a read-write lock to ensure thread-safe access to the
 * primitive lists. Multiple threads can read concurrently, but writes are
 * exclusive.</p>
 *
 * <h3>Subclass Implementation:</h3>
 * <p>Subclasses should override {@link #initializeLayer()} to load their data
 * and call the appropriate add methods to populate the primitive lists.</p>
 */
public abstract class AbstractLayer implements Layer {

    private static final String TAG = "AbstractLayer";

    /** Unique identifier for this layer */
    @NonNull
    protected final String layerId;

    /** Display name for this layer */
    @NonNull
    protected final String layerName;

    /** Depth order for rendering (lower = behind) */
    protected final int layerDepthOrder;

    /** Whether this layer is visible */
    private volatile boolean visible = true;

    /** Whether this layer has been initialized */
    private volatile boolean initialized = false;

    /** Point primitives (stars, planets, etc.) */
    @NonNull
    protected final List<PointPrimitive> points = new ArrayList<>();

    /** Line primitives (constellation lines, grid, etc.) */
    @NonNull
    protected final List<LinePrimitive> lines = new ArrayList<>();

    /** Label primitives (star names, constellation names, etc.) */
    @NonNull
    protected final List<LabelPrimitive> labels = new ArrayList<>();

    /** Lock for thread-safe access to primitive lists */
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs an AbstractLayer with the specified identifier, display name, and rendering depth order.
     *
     * @param layerId         Unique non-null identifier for the layer
     * @param layerName       Non-null display name for the layer
     * @param layerDepthOrder Rendering depth order; lower values render behind higher values
     */
    protected AbstractLayer(@NonNull String layerId, @NonNull String layerName, int layerDepthOrder) {
        this.layerId = layerId;
        this.layerName = layerName;
        this.layerDepthOrder = layerDepthOrder;
    }

    /**
     * Initialize the layer by populating its primitives and marking it initialized.
     *
     * This method is idempotent: if the layer is already initialized it returns immediately.
     * It acquires the layer's write lock, clears existing primitives, invokes {@link #initializeLayer()}
     * while the write lock is held, and sets the internal initialized flag.
     */
    @Override
    public void initialize() {
        if (initialized) {
            Log.w(TAG, "Layer " + layerId + " already initialized");
            return;
        }

        lock.writeLock().lock();
        try {
            clearPrimitives();
            initializeLayer();
            initialized = true;
            Log.d(TAG, "Layer " + layerId + " initialized with " +
                    points.size() + " points, " +
                    lines.size() + " lines, " +
                    labels.size() + " labels");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Called during initialization to load layer data.
     *
     * <p>Subclasses should override this method to load their data and
     * populate the primitive lists using the add methods. This method
     * is called with the write lock held.</p>
     */
    protected abstract void initializeLayer();

    /**
     * Get a snapshot of the layer's point primitives when the layer is visible.
     *
     * The returned list is a copy of the current points at call time and is not backed by the layer's internal storage.
     *
     * @return a copy of the current point primitives if the layer is visible, or an empty list if not visible
     */
    @Override
    @NonNull
    public List<PointPrimitive> getPoints() {
        if (!visible) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return new ArrayList<>(points);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Provide a snapshot of the layer's line primitives when the layer is visible.
     *
     * @return a list containing the current line primitives snapshot when the layer is visible, or an empty list when the layer is not visible
     */
    @Override
    @NonNull
    public List<LinePrimitive> getLines() {
        if (!visible) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return new ArrayList<>(lines);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Provides a snapshot of the layer's label primitives when the layer is visible.
     *
     * The returned list is a copy and is not backed by the layer's internal storage.
     *
     * @return a non-null list of current LabelPrimitive instances if the layer is visible, or an empty list otherwise.
     */
    @Override
    @NonNull
    public List<LabelPrimitive> getLabels() {
        if (!visible) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return new ArrayList<>(labels);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates whether the layer is visible to renderers and data consumers.
     *
     * Setting this to `false` causes `getPoints()`, `getLines()`, and `getLabels()` to behave as if the layer is hidden.
     *
     * @param visible `true` to make the layer visible, `false` to hide it
     */
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        Log.d(TAG, "Layer " + layerId + " visibility set to " + visible);
    }

    /**
     * Indicates whether the layer is visible.
     *
     * @return true if the layer is visible, false otherwise.
     */
    @Override
    public boolean isVisible() {
        return visible;
    }

    /**
     * Recreates the layer's primitives by clearing current primitives and repopulating them.
     *
     * <p>Acquires the layer's write lock while updating primitives to ensure thread-safe modification; upon completion the method logs the refreshed counts of points, lines, and labels.</p>
     */
    @Override
    public void redraw() {
        lock.writeLock().lock();
        try {
            clearPrimitives();
            initializeLayer();
            Log.d(TAG, "Layer " + layerId + " redrawn with " +
                    points.size() + " points, " +
                    lines.size() + " lines, " +
                    labels.size() + " labels");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get the unique identifier of this layer.
     *
     * @return the unique identifier for this layer
     */
    @Override
    @NonNull
    public String getLayerId() {
        return layerId;
    }

    /**
     * Get the display name of the layer.
     *
     * @return the layer's display name
     */
    @Override
    @NonNull
    public String getLayerName() {
        return layerName;
    }

    /**
     * Gets the layer's rendering depth order.
     *
     * @return the layer's rendering depth order; lower values render behind higher values.
     */
    @Override
    public int getLayerDepthOrder() {
        return layerDepthOrder;
    }

    /**
     * Indicates whether the layer has completed initialization.
     *
     * @return true if the layer has been initialized, false otherwise.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Add a point primitive to this layer.
     *
     * <p>Expected to be called while holding the write lock (e.g., during initialization or redraw).</p>
     *
     * @param point the point primitive to add
     */
    protected void addPoint(@NonNull PointPrimitive point) {
        points.add(point);
    }

    /**
     * Add a line primitive to this layer's internal collection.
     *
     * Expected to be called while holding the write lock (for example during initialization or redraw).
     *
     * @param line the line primitive to add
     */
    protected void addLine(@NonNull LinePrimitive line) {
        lines.add(line);
    }

    /**
     * Adds a label primitive to this layer.
     *
     * <p>This method should be called during initialization with the write lock held.</p>
     *
     * @param label Label primitive to add
     */
    protected void addLabel(@NonNull LabelPrimitive label) {
        labels.add(label);
    }

    /**
     * Adds multiple point primitives to the layer.
     *
     * @param newPoints the point primitives to append; must be called with the layer's write lock held
     */
    protected void addPoints(@NonNull List<PointPrimitive> newPoints) {
        points.addAll(newPoints);
    }

    /**
     * Add multiple line primitives to this layer's internal storage.
     *
     * @param newLines the line primitives to add; the caller is expected to hold the layer's write lock when invoking this method
     */
    protected void addLines(@NonNull List<LinePrimitive> newLines) {
        lines.addAll(newLines);
    }

    /**
     * Append the given label primitives to this layer's label storage.
     *
     * @param newLabels the label primitives to add; call while holding the layer's write lock
     */
    protected void addLabels(@NonNull List<LabelPrimitive> newLabels) {
        labels.addAll(newLabels);
    }

    /**
     * Remove all point, line, and label primitives from this layer.
     *
     * <p>Must be invoked while holding the layer's write lock to preserve thread safety.</p>
     */
    protected void clearPrimitives() {
        points.clear();
        lines.clear();
        labels.clear();
    }

    /**
     * Get the current total number of primitives in the layer.
     *
     * @return the number of point, line, and label primitives currently stored in this layer
     */
    public int getPrimitiveCount() {
        lock.readLock().lock();
        try {
            return points.size() + lines.size() + labels.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
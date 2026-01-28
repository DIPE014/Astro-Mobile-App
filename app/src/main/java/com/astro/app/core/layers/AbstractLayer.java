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
     * Creates an abstract layer with the given parameters.
     *
     * @param layerId        Unique identifier for this layer
     * @param layerName      Display name for this layer
     * @param layerDepthOrder Depth order for rendering
     */
    protected AbstractLayer(@NonNull String layerId, @NonNull String layerName, int layerDepthOrder) {
        this.layerId = layerId;
        this.layerName = layerName;
        this.layerDepthOrder = layerDepthOrder;
    }

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

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        Log.d(TAG, "Layer " + layerId + " visibility set to " + visible);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

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

    @Override
    @NonNull
    public String getLayerId() {
        return layerId;
    }

    @Override
    @NonNull
    public String getLayerName() {
        return layerName;
    }

    @Override
    public int getLayerDepthOrder() {
        return layerDepthOrder;
    }

    /**
     * Returns whether this layer has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Adds a point primitive to this layer.
     *
     * <p>This method should be called during initialization with the write lock held.</p>
     *
     * @param point Point primitive to add
     */
    protected void addPoint(@NonNull PointPrimitive point) {
        points.add(point);
    }

    /**
     * Adds a line primitive to this layer.
     *
     * <p>This method should be called during initialization with the write lock held.</p>
     *
     * @param line Line primitive to add
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
     * Adds multiple point primitives to this layer.
     *
     * @param newPoints Points to add
     */
    protected void addPoints(@NonNull List<PointPrimitive> newPoints) {
        points.addAll(newPoints);
    }

    /**
     * Adds multiple line primitives to this layer.
     *
     * @param newLines Lines to add
     */
    protected void addLines(@NonNull List<LinePrimitive> newLines) {
        lines.addAll(newLines);
    }

    /**
     * Adds multiple label primitives to this layer.
     *
     * @param newLabels Labels to add
     */
    protected void addLabels(@NonNull List<LabelPrimitive> newLabels) {
        labels.addAll(newLabels);
    }

    /**
     * Clears all primitives from this layer.
     *
     * <p>This method should be called with the write lock held.</p>
     */
    protected void clearPrimitives() {
        points.clear();
        lines.clear();
        labels.clear();
    }

    /**
     * Returns the total number of primitives in this layer.
     *
     * @return Total primitive count
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

package com.astro.app.core.layers;

import androidx.annotation.NonNull;

import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.LinePrimitive;
import com.astro.app.data.model.PointPrimitive;

import java.util.List;

/**
 * Interface for a layer in the sky map renderer.
 *
 * <p>A Layer represents a logical collection of celestial objects that should be
 * displayed together in the sky map. For instance, stars, constellations, or the
 * coordinate grid can each be represented as separate layers that can be turned
 * on/off independently.</p>
 *
 * <p>Layers provide primitives (points, lines, labels) that the renderer uses
 * to draw the sky map. Each layer manages its own visibility state and can
 * refresh its data when needed.</p>
 *
 * <h3>Layer Lifecycle:</h3>
 * <ol>
 *   <li>{@link #initialize()} - Called when the layer is added to the layer manager</li>
 *   <li>{@link #getPoints()}, {@link #getLines()}, {@link #getLabels()} - Called during rendering</li>
 *   <li>{@link #redraw()} - Called when the layer data needs to be refreshed</li>
 *   <li>{@link #setVisible(boolean)} - Called to show/hide the layer</li>
 * </ol>
 *
 * @see AbstractLayer
 */
public interface Layer {

    /**
     * Initializes the layer.
     *
     * <p>This method is called when the layer is added to the layer manager.
     * Implementations should load data and compute initial positions.
     * This method should return quickly - use a background thread if necessary.</p>
     */
    void initialize();

    /**
     * Returns the point primitives to render.
     *
     * <p>Points are used for rendering stars, planets, and other point-like
     * celestial objects.</p>
     *
     * @return List of point primitives (never null, may be empty)
     */
    @NonNull
    List<PointPrimitive> getPoints();

    /**
     * Returns the line primitives to render.
     *
     * <p>Lines are used for rendering constellation lines, coordinate grids,
     * and other linear features.</p>
     *
     * @return List of line primitives (never null, may be empty)
     */
    @NonNull
    List<LinePrimitive> getLines();

    /**
     * Returns the label primitives to render.
     *
     * <p>Labels are used for rendering star names, constellation names,
     * and other text in the sky map.</p>
     *
     * @return List of label primitives (never null, may be empty)
     */
    @NonNull
    List<LabelPrimitive> getLabels();

    /**
     * Sets the visibility of this layer.
     *
     * <p>When a layer is hidden, its primitives will not be rendered
     * but the data is still maintained for quick show/hide toggling.</p>
     *
     * @param visible true to show the layer, false to hide it
     */
    void setVisible(boolean visible);

    /**
     * Returns whether this layer is currently visible.
     *
     * @return true if visible, false if hidden
     */
    boolean isVisible();

    /**
     * Refreshes the layer data.
     *
     * <p>This method is called when the layer needs to update its primitives,
     * for example when the time changes for planets or when the user's
     * location changes.</p>
     */
    void redraw();

    /**
     * Returns the unique identifier for this layer.
     *
     * <p>This ID is used for saving/restoring layer preferences.</p>
     *
     * @return Unique layer ID
     */
    @NonNull
    String getLayerId();

    /**
     * Returns the display name of this layer.
     *
     * <p>This name is shown to the user in the layer toggle UI.</p>
     *
     * @return Human-readable layer name
     */
    @NonNull
    String getLayerName();

    /**
     * Returns the depth order for rendering.
     *
     * <p>Lower values are rendered first (behind higher values).
     * Typical values:</p>
     * <ul>
     *   <li>0 - Grid (background)</li>
     *   <li>10 - Constellations</li>
     *   <li>20 - Deep sky objects</li>
     *   <li>30 - Stars</li>
     *   <li>40 - Planets</li>
     * </ul>
     *
     * @return Depth order value
     */
    int getLayerDepthOrder();
}

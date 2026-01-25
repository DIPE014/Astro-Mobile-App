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
 * Prepare the layer for use by loading data and computing initial drawable primitives.
 *
 * <p>Called when the layer is added to the layer manager. Implementations should populate
 * internal state required for rendering (for example, compute initial positions) and avoid
 * blocking the caller; perform long-running work asynchronously if needed.</p>
 */
    void initialize();

    /**
         * Supply the point primitives to render.
         *
         * <p>Point primitives represent stars, planets, and other point-like celestial objects.</p>
         *
         * @return the list of point primitives; never null, may be empty
         */
    @NonNull
    List<PointPrimitive> getPoints();

    /**
     * Provide the line primitives for rendering.
     *
     * Lines are used for constellation lines, coordinate grids, and other linear features.
     *
     * @return a List of LinePrimitive objects to render; never null, may be empty
     */
    @NonNull
    List<LinePrimitive> getLines();

    /**
         * Provide the label primitives to render in this layer.
         *
         * <p>Labels represent star names, constellation names, and other textual annotations
         * shown on the sky map.</p>
         *
         * @return a {@code List<LabelPrimitive>} of label primitives to render; never {@code null}, may be empty
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
 * Indicates whether the layer is currently visible.
 *
 * @return `true` if the layer is visible, `false` otherwise.
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
         * Get the unique identifier for this layer.
         *
         * <p>This identifier is used to save and restore per-layer preferences and should remain stable
         * across application sessions.</p>
         *
         * @return the unique identifier for the layer, never {@code null}
         */
    @NonNull
    String getLayerId();

    /**
     * Retrieve the human-readable display name for the layer.
     *
     * <p>This name is shown to the user in the layer selection/toggle UI.</p>
     *
     * @return the human-readable layer name
     */
    @NonNull
    String getLayerName();

    /**
 * Defines the rendering depth order for this layer.
 *
 * Lower values are rendered first (appear behind layers with higher values). Typical values:
 * <ul>
 *   <li>0 - Grid (background)</li>
 *   <li>10 - Constellations</li>
 *   <li>20 - Deep sky objects</li>
 *   <li>30 - Stars</li>
 *   <li>40 - Planets</li>
 * </ul>
 *
 * @return the rendering depth order; lower values render behind higher values
 */
    int getLayerDepthOrder();
}
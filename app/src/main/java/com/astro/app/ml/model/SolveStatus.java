package com.astro.app.ml.model;

/**
 * Represents the possible outcomes of a plate solve operation.
 *
 * <p>Plate solving is the process of matching stars in an image to a star catalog
 * to determine the exact celestial coordinates the image represents.</p>
 *
 * @see com.astro.app.ml.PlateSolveResult
 * @see com.astro.app.ml.PlateSolveService
 */
public enum SolveStatus {

    /**
     * The plate solve was successful.
     *
     * <p>The image was successfully matched to the star catalog, and the
     * celestial coordinates (RA, Dec, FOV, roll) have been determined.</p>
     */
    SUCCESS,

    /**
     * No match was found in the star catalog.
     *
     * <p>The star pattern in the image could not be matched to any known
     * pattern in the database. This may occur if:
     * <ul>
     *   <li>The image contains unusual or distorted star patterns</li>
     *   <li>The field of view estimate was significantly incorrect</li>
     *   <li>The image quality is poor or contains artifacts</li>
     * </ul>
     * </p>
     */
    NO_MATCH,

    /**
     * Not enough stars were detected in the image.
     *
     * <p>Plate solving requires a minimum number of stars (typically 4 or more)
     * to form identifiable patterns. This status indicates the image had
     * insufficient star detections, possibly due to:
     * <ul>
     *   <li>Light pollution or bright sky conditions</li>
     *   <li>Image exposure too short or too long</li>
     *   <li>Camera focus issues</li>
     *   <li>Clouds or obstructions in the field of view</li>
     * </ul>
     * </p>
     */
    NOT_ENOUGH_STARS,

    /**
     * An error occurred during the plate solve operation.
     *
     * <p>This indicates a technical failure rather than a solve failure.
     * The error message in {@link com.astro.app.ml.PlateSolveResult#getErrorMessage()}
     * will contain details about what went wrong.</p>
     */
    ERROR
}

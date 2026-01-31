package com.astro.app.ml;

import androidx.annotation.NonNull;

/**
 * Callback interface for asynchronous plate solve operations.
 *
 * <p>Plate solving is performed on a background thread, and results are
 * delivered via this callback on the main thread.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * plateSolveService.solveImage(imageBytes, 70f, new PlateSolveCallback() {
 *     @Override
 *     public void onSuccess(PlateSolveResult result) {
 *         Log.d("PlateSolve", "Found " + result.getStarsMatched() + " stars");
 *         displayResult(result);
 *     }
 *
 *     @Override
 *     public void onError(String message) {
 *         Log.e("PlateSolve", "Error: " + message);
 *         showErrorDialog(message);
 *     }
 * });
 * }</pre>
 *
 * @see PlateSolveService
 * @see PlateSolveResult
 */
public interface PlateSolveCallback {

    /**
     * Called when the plate solve operation completes.
     *
     * <p>Note: This is called for all completion statuses, including
     * {@link com.astro.app.ml.model.SolveStatus#NO_MATCH} and
     * {@link com.astro.app.ml.model.SolveStatus#NOT_ENOUGH_STARS}.
     * Check {@link PlateSolveResult#getStatus()} to determine the outcome.</p>
     *
     * <p>This method is called on the main (UI) thread.</p>
     *
     * @param result The plate solve result containing status and any matched stars
     */
    void onSuccess(@NonNull PlateSolveResult result);

    /**
     * Called when an error occurs during the plate solve operation.
     *
     * <p>This indicates a technical failure (e.g., Python initialization failed,
     * database not found, out of memory) rather than a solve failure.</p>
     *
     * <p>This method is called on the main (UI) thread.</p>
     *
     * @param message A human-readable description of the error
     */
    void onError(@NonNull String message);
}

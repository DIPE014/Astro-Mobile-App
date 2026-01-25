package com.astro.app.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents the loading state of an operation with associated data and error information.
 * Used for managing UI states during asynchronous operations.
 *
 * @param <T> The type of data associated with the loading state
 */
public class LoadingState<T> {

    /**
     * Enum representing the different states of a loading operation.
     */
    public enum State {
        /** Initial state, no operation in progress */
        IDLE,
        /** Operation is in progress */
        LOADING,
        /** Operation completed successfully */
        SUCCESS,
        /** Operation failed with an error */
        ERROR
    }

    @NonNull
    private final State state;

    @Nullable
    private final T data;

    @Nullable
    private final String errorMessage;

    @Nullable
    private final Throwable exception;

    /**
     * Creates a LoadingState with the specified state, associated data, error message, and exception.
     *
     * @param state        the loading state (must not be null)
     * @param data         the associated data for the state, or null if none
     * @param errorMessage the error message for ERROR state, or null if none
     * @param exception    the Throwable associated with an error, or null if none
     */
    private LoadingState(@NonNull State state, @Nullable T data, @Nullable String errorMessage, @Nullable Throwable exception) {
        this.state = state;
        this.data = data;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    /**
     * Create a LoadingState in the IDLE state.
     *
     * @param <T> the type of associated data
     * @return a LoadingState in the IDLE state with no data, error message, or exception
     */
    @NonNull
    public static <T> LoadingState<T> idle() {
        return new LoadingState<>(State.IDLE, null, null, null);
    }

    /**
     * Create a LoadingState representing the LOADING state.
     *
     * @return a LoadingState in LOADING state with no data, error message, or exception
     */
    @NonNull
    public static <T> LoadingState<T> loading() {
        return new LoadingState<>(State.LOADING, null, null, null);
    }

    /**
     * Creates a LOADING state with existing data (for refresh scenarios).
     *
     * @param existingData The existing data to preserve while loading
     * @param <T>          The type of data
     * @return A LoadingState in LOADING state with preserved data
     */
    @NonNull
    public static <T> LoadingState<T> loading(@Nullable T existingData) {
        return new LoadingState<>(State.LOADING, existingData, null, null);
    }

    /**
         * Create a LoadingState representing a successful result.
         *
         * @param data the associated success data (may be null)
         * @param <T>  the type of the data
         * @return a LoadingState in the SUCCESS state containing the provided data
         */
    @NonNull
    public static <T> LoadingState<T> success(@Nullable T data) {
        return new LoadingState<>(State.SUCCESS, data, null, null);
    }

    /**
         * Creates a LoadingState representing an error with the provided message.
         *
         * @param errorMessage the non-null error message to attach to the ERROR state
         * @param <T>          the data type associated with the LoadingState (none present for this state)
         * @return             a `LoadingState` in `ERROR` state with the given message and no data or exception
         */
    @NonNull
    public static <T> LoadingState<T> error(@NonNull String errorMessage) {
        return new LoadingState<>(State.ERROR, null, errorMessage, null);
    }

    /**
     * Create a LoadingState representing an error using the provided throwable.
     *
     * @param throwable the exception to record with this error state
     * @param <T>       the type of associated data
     * @return          a LoadingState with state ERROR, an error message derived from the throwable, and the throwable set as the exception
     */
    @NonNull
    public static <T> LoadingState<T> error(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        return new LoadingState<>(State.ERROR, null, message, throwable);
    }

    /**
         * Create a LoadingState representing an error that carries both an error message and an exception.
         *
         * @param errorMessage the error message to attach to the state
         * @param throwable    the exception associated with the error
         * @param <T>          the payload type
         * @return the `LoadingState` in `ERROR` state containing the provided message and exception
         */
    @NonNull
    public static <T> LoadingState<T> error(@NonNull String errorMessage, @NonNull Throwable throwable) {
        return new LoadingState<>(State.ERROR, null, errorMessage, throwable);
    }

    /**
     * Creates an ERROR state with preserved data (for refresh failure scenarios).
     *
     * @param errorMessage The error message
     * @param existingData The existing data to preserve
     * @param <T>          The type of data
     * @return A LoadingState in ERROR state with preserved data
     */
    @NonNull
    public static <T> LoadingState<T> errorWithData(@NonNull String errorMessage, @Nullable T existingData) {
        return new LoadingState<>(State.ERROR, existingData, errorMessage, null);
    }

    /**
     * Convert a Result into a corresponding LoadingState.
     *
     * If the result represents success, the returned state is SUCCESS containing the result's data;
     * otherwise the returned state is ERROR containing the result's error message and exception.
     *
     * @param result the Result to convert into a LoadingState
     * @param <T>    the type of the contained data
     * @return       a LoadingState representing the given Result
     */
    @NonNull
    public static <T> LoadingState<T> fromResult(@NonNull Result<T> result) {
        if (result.isSuccess()) {
            return success(result.getData());
        } else {
            return new LoadingState<>(State.ERROR, null, result.getError(), result.getException());
        }
    }

    /**
     * Gets the current state.
     *
     * @return The current State enum value
     */
    @NonNull
    public State getState() {
        return state;
    }

    /**
     * Gets the associated data.
     *
     * @return The data, or null if not available
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * The error message associated with this loading state.
     *
     * @return the error message, or null if none is set
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the stored error message or the provided default message if none is present.
     *
     * @param defaultMessage the message to return when no error message is stored
     * @return {@code errorMessage} if present, otherwise {@code defaultMessage}
     */
    @NonNull
    public String getErrorMessageOrDefault(@NonNull String defaultMessage) {
        return errorMessage != null ? errorMessage : defaultMessage;
    }

    /**
         * The Throwable associated with the ERROR state, if any.
         *
         * @return the Throwable for the current ERROR state, or {@code null} if none
         */
    @Nullable
    public Throwable getException() {
        return exception;
    }

    /**
     * Determine whether the current loading state is IDLE.
     *
     * @return `true` if the current state is IDLE, `false` otherwise.
     */
    public boolean isIdle() {
        return state == State.IDLE;
    }

    /**
     * Returns whether the current state is LOADING.
     *
     * @return `true` if the current state is LOADING, `false` otherwise.
     */
    public boolean isLoading() {
        return state == State.LOADING;
    }

    /**
     * Determines whether the loading state is SUCCESS.
     *
     * @return true if the current state is SUCCESS, false otherwise.
     */
    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    /**
     * Whether the current loading state is ERROR.
     *
     * @return `true` if the state is ERROR, `false` otherwise.
     */
    public boolean isError() {
        return state == State.ERROR;
    }

    /**
     * Indicates whether associated data is present regardless of the loading state.
     *
     * @return `true` if the stored data is not null, `false` otherwise.
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Invoke the corresponding method on the provided handler for the current loading state.
     *
     * <p>The handler is called as follows:
     * <ul>
     *   <li>IDLE → {@code onIdle()}</li>
     *   <li>LOADING → {@code onLoading(data)}</li>
     *   <li>SUCCESS → {@code onSuccess(data)}</li>
     *   <li>ERROR → {@code onError(errorMessage or "Unknown error", data)}</li>
     * </ul>
     *
     * @param handler the handler whose callback matching the current state will be invoked
     */
    public void handle(@NonNull StateHandler<T> handler) {
        switch (state) {
            case IDLE:
                handler.onIdle();
                break;
            case LOADING:
                handler.onLoading(data);
                break;
            case SUCCESS:
                handler.onSuccess(data);
                break;
            case ERROR:
                handler.onError(errorMessage != null ? errorMessage : "Unknown error", data);
                break;
        }
    }

    /**
     * Interface for handling different loading states.
     *
     * @param <T> The type of data
     */
    public interface StateHandler<T> {
        /**
 * Invoked when the loading state is IDLE.
 */
void onIdle();
        /**
 * Called when the operation enters the LOADING state.
 *
 * @param existingData previously available data to continue displaying while loading, or {@code null} if none
 */
void onLoading(@Nullable T existingData);
        /**
 * Called when the operation completes successfully.
 *
 * @param data the resulting data, or {@code null} if no data is available
 */
void onSuccess(@Nullable T data);
        /**
 * Called when the loading operation finishes with an error.
 *
 * @param message       the error message describing the failure
 * @param existingData  the associated data preserved from before the error, or {@code null} if none
 */
void onError(@NonNull String message, @Nullable T existingData);
    }

    /**
     * Simple state handler with default empty implementations.
     *
     * @param <T> The type of data
     */
    public static abstract class SimpleStateHandler<T> implements StateHandler<T> {
        /**
         * Called when the loading state is IDLE.
         *
         * <p>Default no-op implementation; override to handle the idle state.</p>
         */
        @Override
        public void onIdle() {}

        /**
         * Invoked when the loading state is active; provides any existing data preserved while loading.
         *
         * @param existingData previously loaded data that may be shown during the loading state, or {@code null} if none
         */
        @Override
        public void onLoading(@Nullable T existingData) {}

        /**
         * Invoked when the operation completes successfully.
         *
         * <p>Default no-op implementation; override to handle the successful result.</p>
         *
         * @param data the result data for the successful operation, may be null
         */
        @Override
        public void onSuccess(@Nullable T data) {}

        /**
         * Invoked when the operation finishes in the ERROR state.
         *
         * @param message       a human-readable error message describing the failure
         * @param existingData  optional previously loaded data associated with this error, may be null
         */
        @Override
        public void onError(@NonNull String message, @Nullable T existingData) {}
    }

    /**
     * Produce a concise string representation of this LoadingState for debugging.
     *
     * @return the string containing the current `state`, `data`, and `errorMessage`
     */
    @Override
    @NonNull
    public String toString() {
        return "LoadingState{" +
                "state=" + state +
                ", data=" + data +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
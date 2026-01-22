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

    private LoadingState(@NonNull State state, @Nullable T data, @Nullable String errorMessage, @Nullable Throwable exception) {
        this.state = state;
        this.data = data;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    /**
     * Creates an IDLE loading state.
     *
     * @param <T> The type parameter
     * @return A LoadingState in IDLE state
     */
    @NonNull
    public static <T> LoadingState<T> idle() {
        return new LoadingState<>(State.IDLE, null, null, null);
    }

    /**
     * Creates a LOADING state.
     *
     * @param <T> The type parameter
     * @return A LoadingState in LOADING state
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
     * Creates a SUCCESS state with the given data.
     *
     * @param data The successful result data
     * @param <T>  The type of data
     * @return A LoadingState in SUCCESS state
     */
    @NonNull
    public static <T> LoadingState<T> success(@Nullable T data) {
        return new LoadingState<>(State.SUCCESS, data, null, null);
    }

    /**
     * Creates an ERROR state with the given error message.
     *
     * @param errorMessage The error message
     * @param <T>          The type parameter
     * @return A LoadingState in ERROR state
     */
    @NonNull
    public static <T> LoadingState<T> error(@NonNull String errorMessage) {
        return new LoadingState<>(State.ERROR, null, errorMessage, null);
    }

    /**
     * Creates an ERROR state from a Throwable.
     *
     * @param throwable The exception that caused the error
     * @param <T>       The type parameter
     * @return A LoadingState in ERROR state
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
     * Creates an ERROR state with both message and exception.
     *
     * @param errorMessage The error message
     * @param throwable    The exception
     * @param <T>          The type parameter
     * @return A LoadingState in ERROR state
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
     * Creates a LoadingState from a Result object.
     *
     * @param result The Result to convert
     * @param <T>    The type of data
     * @return A LoadingState representing the Result
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
     * Gets the error message.
     *
     * @return The error message, or null if not in error state
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the error message with a default fallback.
     *
     * @param defaultMessage The default message if none exists
     * @return The error message or the default
     */
    @NonNull
    public String getErrorMessageOrDefault(@NonNull String defaultMessage) {
        return errorMessage != null ? errorMessage : defaultMessage;
    }

    /**
     * Gets the exception if available.
     *
     * @return The exception, or null if not available
     */
    @Nullable
    public Throwable getException() {
        return exception;
    }

    /**
     * Checks if the state is IDLE.
     *
     * @return true if IDLE
     */
    public boolean isIdle() {
        return state == State.IDLE;
    }

    /**
     * Checks if the state is LOADING.
     *
     * @return true if LOADING
     */
    public boolean isLoading() {
        return state == State.LOADING;
    }

    /**
     * Checks if the state is SUCCESS.
     *
     * @return true if SUCCESS
     */
    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    /**
     * Checks if the state is ERROR.
     *
     * @return true if ERROR
     */
    public boolean isError() {
        return state == State.ERROR;
    }

    /**
     * Checks if data is available (regardless of state).
     *
     * @return true if data is not null
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Handles the current state with appropriate callbacks.
     *
     * @param handler The handler with callbacks for each state
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
        void onIdle();
        void onLoading(@Nullable T existingData);
        void onSuccess(@Nullable T data);
        void onError(@NonNull String message, @Nullable T existingData);
    }

    /**
     * Simple state handler with default empty implementations.
     *
     * @param <T> The type of data
     */
    public static abstract class SimpleStateHandler<T> implements StateHandler<T> {
        @Override
        public void onIdle() {}

        @Override
        public void onLoading(@Nullable T existingData) {}

        @Override
        public void onSuccess(@Nullable T data) {}

        @Override
        public void onError(@NonNull String message, @Nullable T existingData) {}
    }

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

package com.astro.app.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic Result wrapper class for handling success and error states.
 * Provides a clean way to handle operations that can either succeed with data
 * or fail with an error.
 *
 * @param <T> The type of data contained in a successful result
 */
public class Result<T> {

    @Nullable
    private final T data;

    @Nullable
    private final String errorMessage;

    @Nullable
    private final Throwable exception;

    private final boolean isSuccess;

    private Result(@Nullable T data, @Nullable String errorMessage, @Nullable Throwable exception, boolean isSuccess) {
        this.data = data;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.isSuccess = isSuccess;
    }

    /**
     * Creates a successful Result with the given data.
     *
     * @param data The data to wrap in the successful result
     * @param <T>  The type of data
     * @return A successful Result containing the data
     */
    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(data, null, null, true);
    }

    /**
     * Creates an error Result with the given error message.
     *
     * @param errorMessage The error message describing what went wrong
     * @param <T>          The type parameter (will be null for error results)
     * @return An error Result containing the error message
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String errorMessage) {
        return new Result<>(null, errorMessage, null, false);
    }

    /**
     * Creates an error Result from a Throwable.
     *
     * @param throwable The exception that caused the error
     * @param <T>       The type parameter (will be null for error results)
     * @return An error Result containing the exception information
     */
    @NonNull
    public static <T> Result<T> error(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        return new Result<>(null, message, throwable, false);
    }

    /**
     * Creates an error Result with both a custom message and the original exception.
     *
     * @param errorMessage Custom error message
     * @param throwable    The exception that caused the error
     * @param <T>          The type parameter (will be null for error results)
     * @return An error Result containing both the message and exception
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String errorMessage, @NonNull Throwable throwable) {
        return new Result<>(null, errorMessage, throwable, false);
    }

    /**
     * Checks if this Result represents a successful operation.
     *
     * @return true if the operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Checks if this Result represents a failed operation.
     *
     * @return true if the operation failed, false otherwise
     */
    public boolean isError() {
        return !isSuccess;
    }

    /**
     * Gets the data from a successful Result.
     *
     * @return The data, or null if this is an error Result
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Gets the data from a successful Result, throwing an exception if this is an error Result.
     *
     * @return The data
     * @throws IllegalStateException if this is an error Result
     */
    @NonNull
    public T getDataOrThrow() {
        if (!isSuccess || data == null) {
            throw new IllegalStateException("Cannot get data from error result: " + errorMessage);
        }
        return data;
    }

    /**
     * Gets the error message from an error Result.
     *
     * @return The error message, or null if this is a successful Result
     */
    @Nullable
    public String getError() {
        return errorMessage;
    }

    /**
     * Gets the error message, with a default value if none exists.
     *
     * @param defaultMessage The default message to return if no error message exists
     * @return The error message or the default message
     */
    @NonNull
    public String getErrorOrDefault(@NonNull String defaultMessage) {
        return errorMessage != null ? errorMessage : defaultMessage;
    }

    /**
     * Gets the exception that caused the error, if available.
     *
     * @return The exception, or null if not available or this is a successful Result
     */
    @Nullable
    public Throwable getException() {
        return exception;
    }

    /**
     * Maps the data in a successful Result to a new type.
     *
     * @param mapper The function to transform the data
     * @param <R>    The new type
     * @return A new Result with the transformed data, or the same error Result
     */
    @NonNull
    public <R> Result<R> map(@NonNull Mapper<T, R> mapper) {
        if (isSuccess) {
            // Note: mapper must handle null input if data is null
            return Result.success(mapper.map(data));
        } else {
            return new Result<>(null, errorMessage, exception, false);
        }
    }

    /**
     * Executes the appropriate callback based on the Result state.
     *
     * @param onSuccess Callback to execute if successful
     * @param onError   Callback to execute if error
     */
    public void fold(@NonNull OnSuccess<T> onSuccess, @NonNull OnError onError) {
        if (isSuccess) {
            onSuccess.onSuccess(data);
        } else {
            onError.onError(errorMessage != null ? errorMessage : "Unknown error", exception);
        }
    }

    /**
     * Functional interface for mapping data.
     *
     * @param <T> Input type
     * @param <R> Output type
     */
    public interface Mapper<T, R> {
        R map(T input);
    }

    /**
     * Callback interface for successful results.
     *
     * @param <T> The type of data
     */
    public interface OnSuccess<T> {
        void onSuccess(@Nullable T data);
    }

    /**
     * Callback interface for error results.
     */
    public interface OnError {
        void onError(@NonNull String message, @Nullable Throwable exception);
    }

    @Override
    @NonNull
    public String toString() {
        if (isSuccess) {
            return "Result.Success{data=" + data + "}";
        } else {
            return "Result.Error{message=" + errorMessage + ", exception=" + exception + "}";
        }
    }
}

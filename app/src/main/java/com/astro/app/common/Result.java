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

    /**
     * Initializes a Result instance with the given payload and error information.
     *
     * @param data        the success payload; may be null for successful results or when unavailable
     * @param errorMessage the error message for a failed result; may be null
     * @param exception   the exception associated with a failed result; may be null
     * @param isSuccess   true when this instance represents a successful result, false when it represents an error
     */
    private Result(@Nullable T data, @Nullable String errorMessage, @Nullable Throwable exception, boolean isSuccess) {
        this.data = data;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.isSuccess = isSuccess;
    }

    /**
     * Create a successful Result that wraps the provided data.
     *
     * @param data the data to wrap; may be null
     * @return a Result representing success containing the provided `data` (error message and exception are null)
     */
    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(data, null, null, true);
    }

    /**
     * Create an error Result that carries the provided error message and no data.
     *
     * @param errorMessage a non-null description of the failure
     * @param <T>          the result data type (unused for error results)
     * @return              a Result representing failure with the given message
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String errorMessage) {
        return new Result<>(null, errorMessage, null, false);
    }

    /**
     * Create an error Result from a Throwable.
     *
     * If the throwable has no message or an empty message, the throwable's class simple name
     * is used as the error message.
     *
     * @param throwable the exception that caused the error
     * @param <T>       the result data type (will be null for error results)
     * @return a Result representing failure with the derived error message and the provided exception
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
         * Gets the contained data if this Result represents success.
         *
         * @return the contained data, or {@code null} if this Result represents an error
         */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Return the contained data when this Result represents success.
     *
     * @return the contained data
     * @throws IllegalStateException if this Result represents an error or the contained data is null
     */
    @NonNull
    public T getDataOrThrow() {
        if (!isSuccess || data == null) {
            throw new IllegalStateException("Cannot get data from error result: " + errorMessage);
        }
        return data;
    }

    /**
     * Provides the error message for a failed Result.
     *
     * @return the error message if this Result represents an error, or null if it represents success
     */
    @Nullable
    public String getError() {
        return errorMessage;
    }

    /**
         * Gets the stored error message if present, otherwise returns the provided default.
         *
         * @param defaultMessage the value to return when no error message is present
         * @return the stored error message if present, otherwise {@code defaultMessage}
         */
    @NonNull
    public String getErrorOrDefault(@NonNull String defaultMessage) {
        return errorMessage != null ? errorMessage : defaultMessage;
    }

    / **
     * Provides the exception associated with an error, if present.
     *
     * @return the Throwable that caused the error, or null if no exception is available or this Result represents success
     * /
    @Nullable
    public Throwable getException() {
        return exception;
    }

    /**
         * Transform the contained data to a different type when this Result represents success.
         *
         * @param mapper the transformation to apply to the contained data when this Result is a success and has non-null data
         * @param <R>    the target data type
         * @return a successful Result containing the transformed data if this Result is success with non-null data; otherwise an error Result preserving the original error message and exception
         */
    @NonNull
    public <R> Result<R> map(@NonNull Mapper<T, R> mapper) {
        if (isSuccess && data != null) {
            return Result.success(mapper.map(data));
        } else {
            return new Result<>(null, errorMessage, exception, false);
        }
    }

    /**
     * Invoke the appropriate callback for this Result: call {@code onSuccess} with the contained data when the Result represents success; otherwise call {@code onError} with a non-null error message and the associated exception (if any).
     *
     * @param onSuccess callback invoked for success; receives the contained data (may be null)
     * @param onError   callback invoked for error; receives a non-null message and an optional exception
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
        /**
 * Transforms an input value of type T into a value of type R.
 *
 * @param input the value to transform
 * @return the transformed value
 */
R map(T input);
    }

    /**
     * Callback interface for successful results.
     *
     * @param <T> The type of data
     */
    public interface OnSuccess<T> {
        /**
 * Called when a result represents success.
 *
 * @param data the successful value, which may be null if there is no data
 */
void onSuccess(@Nullable T data);
    }

    /**
     * Callback interface for error results.
     */
    public interface OnError {
        /**
 * Invoked when an error occurs, providing a human-readable message and an optional exception.
 *
 * @param message   a non-null error message describing what went wrong
 * @param exception the underlying cause of the error, or {@code null} if none is available
 */
void onError(@NonNull String message, @Nullable Throwable exception);
    }

    /**
     * Provide a concise string describing the Result's state and contents.
     *
     * @return a string in the form {@code Result.Success{data=...}} when successful,
     *         or {@code Result.Error{message=..., exception=...}} when an error occurred.
     */
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
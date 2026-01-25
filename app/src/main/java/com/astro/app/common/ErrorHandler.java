package com.astro.app.common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Centralized error handling utility for the application.
 * Provides logging, user-friendly message conversion, and error reporting capabilities.
 */
public class ErrorHandler {

    private static final String TAG = "ErrorHandler";

    // Error type constants for categorization
    public static final int ERROR_TYPE_NETWORK = 1;
    public static final int ERROR_TYPE_TIMEOUT = 2;
    public static final int ERROR_TYPE_SERVER = 3;
    public static final int ERROR_TYPE_AUTHENTICATION = 4;
    public static final int ERROR_TYPE_VALIDATION = 5;
    public static final int ERROR_TYPE_UNKNOWN = 0;

    @Nullable
    private static ErrorHandler instance;

    @Nullable
    private ErrorReporter errorReporter;

    @Nullable
    private Context applicationContext;

    private boolean isDebugMode = false;

    /**
     * Prevents external instantiation to enforce the singleton pattern for ErrorHandler.
     */
    private ErrorHandler() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of ErrorHandler.
     *
     * @return The ErrorHandler instance
     */
    @NonNull
    public static synchronized ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }

    /**
     * Initializes the ErrorHandler with application context.
     *
     * @param context The application context
     * @return The ErrorHandler instance for chaining
     */
    @NonNull
    public ErrorHandler init(@NonNull Context context) {
        this.applicationContext = context.getApplicationContext();
        return this;
    }

    /**
     * Enables or disables debug logging for the ErrorHandler.
     *
     * @param debugMode true to enable verbose debug logging, false to disable it
     * @return the ErrorHandler instance for method chaining
     */
    @NonNull
    public ErrorHandler setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
        return this;
    }

    /**
         * Sets the ErrorReporter used to report errors to external analytics or crash services.
         *
         * @param reporter the ErrorReporter to use, or {@code null} to disable external reporting
         * @return this ErrorHandler instance for method chaining
         */
    @NonNull
    public ErrorHandler setErrorReporter(@Nullable ErrorReporter reporter) {
        this.errorReporter = reporter;
        return this;
    }

    /**
     * Handles an exception by logging and optionally reporting it.
     *
     * @param tag       The log tag
     * @param throwable The exception to handle
     */
    public void handleError(@NonNull String tag, @NonNull Throwable throwable) {
        handleError(tag, null, throwable);
    }

    /**
     * Logs the provided throwable and forwards it to the configured error reporter with an optional context message.
     *
     * @param tag       log tag identifying the source
     * @param message   optional contextual message to include with logs and reports; may be null
     * @param throwable the exception to log and report
     */
    public void handleError(@NonNull String tag, @Nullable String message, @NonNull Throwable throwable) {
        // Log the error
        logError(tag, message, throwable);

        // Report to analytics/crash reporting
        reportError(throwable, message);
    }

    /**
     * Log an error using the provided tag, optional message, and throwable.
     *
     * In debug mode this logs the full throwable (including stack trace); otherwise it logs the message
     * and the throwable's message only.
     *
     * @param tag       the log tag to categorize the message
     * @param message   an optional human-readable message; if null a default message is used
     * @param throwable the exception to log
     */
    public void logError(@NonNull String tag, @Nullable String message, @NonNull Throwable throwable) {
        String logMessage = message != null ? message : "An error occurred";

        if (isDebugMode) {
            Log.e(tag, logMessage, throwable);
        } else {
            Log.e(tag, logMessage + ": " + throwable.getMessage());
        }
    }

    /**
     * Logs an error message without an exception.
     *
     * @param tag     The log tag
     * @param message The error message
     */
    public void logError(@NonNull String tag, @NonNull String message) {
        Log.e(tag, message);
    }

    /**
     * Logs a warning message.
     *
     * @param tag     The log tag
     * @param message The warning message
     */
    public void logWarning(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
    }

    /**
     * Forward an exception and optional context to the configured ErrorReporter for analytics/crash reporting.
     *
     * If no ErrorReporter is configured this method performs no external reporting. When debug mode is enabled
     * a debug log entry is written indicating the reported error or provided context.
     *
     * @param throwable the exception to report
     * @param context   optional contextual message to accompany the error
     */
    public void reportError(@NonNull Throwable throwable, @Nullable String context) {
        if (errorReporter != null) {
            errorReporter.reportError(throwable, context);
        }

        // Placeholder for analytics integration
        // TODO: Integrate with Firebase Crashlytics, Sentry, or other crash reporting service
        if (isDebugMode) {
            Log.d(TAG, "Error reported: " + (context != null ? context : throwable.getMessage()));
        }
    }

    /**
     * Reports a non-fatal error for tracking purposes.
     *
     * @param message The error message
     */
    public void reportNonFatal(@NonNull String message) {
        if (errorReporter != null) {
            errorReporter.reportNonFatal(message);
        }

        if (isDebugMode) {
            Log.d(TAG, "Non-fatal reported: " + message);
        }
    }

    /**
         * Map an exception to a concise, user-facing error message.
         *
         * @param throwable the exception to convert into a user-facing message
         * @return a concise, user-friendly message describing the error suitable for display to end users
         */
    @NonNull
    public String getUserFriendlyMessage(@NonNull Throwable throwable) {
        // Network-related errors
        if (throwable instanceof UnknownHostException) {
            return "Unable to connect to the server. Please check your internet connection.";
        }

        if (throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException) {
            return "The request timed out. Please try again.";
        }

        if (throwable instanceof IOException) {
            return "A network error occurred. Please check your connection and try again.";
        }

        // Security/Authentication errors
        if (throwable instanceof SecurityException) {
            return "You don't have permission to perform this action.";
        }

        // Null pointer and illegal state
        if (throwable instanceof NullPointerException || throwable instanceof IllegalStateException) {
            return "Something went wrong. Please try again.";
        }

        // Illegal argument
        if (throwable instanceof IllegalArgumentException) {
            String message = throwable.getMessage();
            if (message != null && !message.isEmpty()) {
                return "Invalid input: " + message;
            }
            return "Invalid input provided. Please check and try again.";
        }

        // Default message
        String message = throwable.getMessage();
        if (message != null && !message.isEmpty() && message.length() < 100) {
            return message;
        }

        return "An unexpected error occurred. Please try again.";
    }

    /**
         * Produce a user-facing error message for the given throwable, using a fallback if generation fails.
         *
         * @param throwable      the exception to convert into a user-friendly message
         * @param defaultMessage the message to return if conversion throws an exception
         * @return a user-friendly message derived from the throwable, or `defaultMessage` if conversion fails
         */
    @NonNull
    public String getUserFriendlyMessage(@NonNull Throwable throwable, @NonNull String defaultMessage) {
        try {
            return getUserFriendlyMessage(throwable);
        } catch (Exception e) {
            return defaultMessage;
        }
    }

    /**
     * Categorizes a Throwable into one of the ErrorHandler error-type constants.
     *
     * @param throwable the exception to classify
     * @return one of the ERROR_TYPE_* constants indicating the error category (for example:
     *         ERROR_TYPE_NETWORK, ERROR_TYPE_TIMEOUT, ERROR_TYPE_AUTHENTICATION,
     *         ERROR_TYPE_VALIDATION, or ERROR_TYPE_UNKNOWN)
     */
    public int getErrorType(@NonNull Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            return ERROR_TYPE_NETWORK;
        }

        if (throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException) {
            return ERROR_TYPE_TIMEOUT;
        }

        if (throwable instanceof IOException) {
            return ERROR_TYPE_NETWORK;
        }

        if (throwable instanceof SecurityException) {
            return ERROR_TYPE_AUTHENTICATION;
        }

        if (throwable instanceof IllegalArgumentException) {
            return ERROR_TYPE_VALIDATION;
        }

        return ERROR_TYPE_UNKNOWN;
    }

    /**
     * Determines whether the given throwable represents a recoverable error that the user can retry.
     *
     * @param throwable the exception to classify
     * @return true if the error is recoverable (network or timeout), false otherwise
     */
    public boolean isRecoverable(@NonNull Throwable throwable) {
        int errorType = getErrorType(throwable);
        return errorType == ERROR_TYPE_NETWORK || errorType == ERROR_TYPE_TIMEOUT;
    }

    /**
     * Convert an exception into a Result.Error containing a user-friendly message.
     *
     * @param throwable the exception to convert
     * @return a Result representing an error with a user-friendly message and the original throwable
     */
    @NonNull
    public <T> Result<T> toResult(@NonNull Throwable throwable) {
        handleError(TAG, throwable);
        return Result.error(getUserFriendlyMessage(throwable), throwable);
    }

    /**
     * Creates a LoadingState in error state from the given throwable using a user-friendly message.
     *
     * @param throwable the exception to convert into an error state
     * @param <T> the content type of the LoadingState
     * @return a LoadingState in error state containing a user-friendly message and the original throwable
     */
    @NonNull
    public <T> LoadingState<T> toLoadingState(@NonNull Throwable throwable) {
        handleError(TAG, throwable);
        return LoadingState.error(getUserFriendlyMessage(throwable), throwable);
    }

    /**
     * Interface for custom error reporting implementations.
     */
    public interface ErrorReporter {
        /**
 * Send the given exception to the configured crash/analytics service with optional contextual information.
 *
 * @param throwable the exception to report
 * @param context   optional short context (e.g., tag, operation, or message) to accompany the report
 */
        void reportError(@NonNull Throwable throwable, @Nullable String context);

        /**
 * Sends a non-fatal diagnostic message to the configured error-reporting backend for tracking.
 *
 * @param message descriptive text about the non-fatal issue to record
 */
        void reportNonFatal(@NonNull String message);
    }

    /**
     * Default no-op error reporter for development/testing.
     */
    public static class NoOpErrorReporter implements ErrorReporter {
        /**
         * Discards the given throwable and optional context without reporting it.
         *
         * @param throwable the error to report (ignored)
         * @param context optional contextual information about the error (ignored)
         */
        @Override
        public void reportError(@NonNull Throwable throwable, @Nullable String context) {
            // No-op implementation
        }

        /**
         * Report a non-fatal issue for tracking or analytics.
         *
         * <p>The default implementation does nothing; override to forward the message to a crash
         * reporting or analytics service.</p>
         *
         * @param message a descriptive message for the non-fatal issue
         */
        @Override
        public void reportNonFatal(@NonNull String message) {
            // No-op implementation
        }
    }
}
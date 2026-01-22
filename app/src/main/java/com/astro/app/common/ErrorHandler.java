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
     * Sets the debug mode flag.
     *
     * @param debugMode true to enable debug mode
     * @return The ErrorHandler instance for chaining
     */
    @NonNull
    public ErrorHandler setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
        return this;
    }

    /**
     * Sets a custom error reporter for analytics/crash reporting.
     *
     * @param reporter The ErrorReporter implementation
     * @return The ErrorHandler instance for chaining
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
     * Handles an exception with a custom message.
     *
     * @param tag       The log tag
     * @param message   Optional custom message
     * @param throwable The exception to handle
     */
    public void handleError(@NonNull String tag, @Nullable String message, @NonNull Throwable throwable) {
        // Log the error
        logError(tag, message, throwable);

        // Report to analytics/crash reporting
        reportError(throwable, message);
    }

    /**
     * Logs an error with the given information.
     *
     * @param tag       The log tag
     * @param message   Optional message
     * @param throwable The exception
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
     * Reports an error to the configured error reporter (analytics/crash reporting).
     *
     * @param throwable The exception to report
     * @param context   Optional context message
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
     * Converts an exception to a user-friendly error message.
     *
     * @param throwable The exception
     * @return A user-friendly error message
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
     * Converts an exception to a user-friendly message with a default fallback.
     *
     * @param throwable      The exception
     * @param defaultMessage The default message if conversion fails
     * @return A user-friendly error message
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
     * Gets the error type for categorization purposes.
     *
     * @param throwable The exception
     * @return The error type constant
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
     * Checks if the error is recoverable (user can retry).
     *
     * @param throwable The exception
     * @return true if the error is recoverable
     */
    public boolean isRecoverable(@NonNull Throwable throwable) {
        int errorType = getErrorType(throwable);
        return errorType == ERROR_TYPE_NETWORK || errorType == ERROR_TYPE_TIMEOUT;
    }

    /**
     * Creates a Result.Error from an exception with a user-friendly message.
     *
     * @param throwable The exception
     * @param <T>       The type parameter
     * @return A Result.Error with user-friendly message
     */
    @NonNull
    public <T> Result<T> toResult(@NonNull Throwable throwable) {
        handleError(TAG, throwable);
        return Result.error(getUserFriendlyMessage(throwable), throwable);
    }

    /**
     * Creates a LoadingState.Error from an exception with a user-friendly message.
     *
     * @param throwable The exception
     * @param <T>       The type parameter
     * @return A LoadingState in error state
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
         * Reports an error to the analytics/crash reporting service.
         *
         * @param throwable The exception to report
         * @param context   Optional context information
         */
        void reportError(@NonNull Throwable throwable, @Nullable String context);

        /**
         * Reports a non-fatal issue for tracking.
         *
         * @param message The message to report
         */
        void reportNonFatal(@NonNull String message);
    }

    /**
     * Default no-op error reporter for development/testing.
     */
    public static class NoOpErrorReporter implements ErrorReporter {
        @Override
        public void reportError(@NonNull Throwable throwable, @Nullable String context) {
            // No-op implementation
        }

        @Override
        public void reportNonFatal(@NonNull String message) {
            // No-op implementation
        }
    }
}

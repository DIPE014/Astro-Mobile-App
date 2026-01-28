package com.astro.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

/**
 * Reusable error dialog for displaying error messages with optional retry functionality.
 * Can be used as a DialogFragment for lifecycle-aware display.
 */
public class ErrorDialog extends DialogFragment {

    private static final String TAG = "ErrorDialog";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE_BUTTON = "positive_button";
    private static final String ARG_NEGATIVE_BUTTON = "negative_button";
    private static final String ARG_SHOW_RETRY = "show_retry";

    @Nullable
    private String title;

    @Nullable
    private String message;

    @Nullable
    private String positiveButtonText;

    @Nullable
    private String negativeButtonText;

    private boolean showRetry = false;

    @Nullable
    private OnRetryClickListener retryClickListener;

    @Nullable
    private OnDismissListener dismissListener;

    /**
     * Creates a new instance of ErrorDialog with a message.
     *
     * @param message The error message to display
     * @return A new ErrorDialog instance
     */
    @NonNull
    public static ErrorDialog newInstance(@NonNull String message) {
        return newInstance(null, message);
    }

    /**
     * Creates a new instance of ErrorDialog with a title and message.
     *
     * @param title   The dialog title
     * @param message The error message
     * @return A new ErrorDialog instance
     */
    @NonNull
    public static ErrorDialog newInstance(@Nullable String title, @NonNull String message) {
        ErrorDialog dialog = new ErrorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Creates a new instance of ErrorDialog with retry option.
     *
     * @param title   The dialog title
     * @param message The error message
     * @return A new ErrorDialog instance with retry button
     */
    @NonNull
    public static ErrorDialog newInstanceWithRetry(@Nullable String title, @NonNull String message) {
        ErrorDialog dialog = new ErrorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putBoolean(ARG_SHOW_RETRY, true);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Restore listeners from host activity/fragment after configuration changes
        if (context instanceof OnRetryClickListener && retryClickListener == null) {
            retryClickListener = (OnRetryClickListener) context;
        }
        if (context instanceof OnDismissListener && dismissListener == null) {
            dismissListener = (OnDismissListener) context;
        }
        // Also check parent fragment
        if (getParentFragment() instanceof OnRetryClickListener && retryClickListener == null) {
            retryClickListener = (OnRetryClickListener) getParentFragment();
        }
        if (getParentFragment() instanceof OnDismissListener && dismissListener == null) {
            dismissListener = (OnDismissListener) getParentFragment();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
            positiveButtonText = getArguments().getString(ARG_POSITIVE_BUTTON);
            negativeButtonText = getArguments().getString(ARG_NEGATIVE_BUTTON);
            showRetry = getArguments().getBoolean(ARG_SHOW_RETRY, false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage(message);

        // Set title if provided
        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
        } else {
            builder.setTitle("Error");
        }

        // Set positive button
        String okText = positiveButtonText != null ? positiveButtonText : "OK";
        builder.setPositiveButton(okText, (dialog, which) -> {
            if (dismissListener != null) {
                dismissListener.onDismiss();
            }
        });

        // Set retry button if enabled
        if (showRetry) {
            String retryText = negativeButtonText != null ? negativeButtonText : "Retry";
            builder.setNegativeButton(retryText, (dialog, which) -> {
                if (retryClickListener != null) {
                    retryClickListener.onRetry();
                }
            });
        }

        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Note: dismissListener is called from button click, not here
        // to differentiate between OK click and outside dismiss
    }

    /**
     * Sets the retry click listener.
     *
     * @param listener The listener
     * @return The ErrorDialog instance
     */
    @NonNull
    public ErrorDialog setOnRetryClickListener(@Nullable OnRetryClickListener listener) {
        this.retryClickListener = listener;
        return this;
    }

    /**
     * Sets the dismiss listener.
     *
     * @param listener The listener
     * @return The ErrorDialog instance
     */
    @NonNull
    public ErrorDialog setOnDismissListener(@Nullable OnDismissListener listener) {
        this.dismissListener = listener;
        return this;
    }

    /**
     * Shows the error dialog.
     *
     * @param fragmentManager The FragmentManager to use
     */
    public void show(@NonNull FragmentManager fragmentManager) {
        if (!isAdded() && !fragmentManager.isStateSaved()) {
            show(fragmentManager, TAG);
        }
    }

    /**
     * Dismisses the error dialog safely.
     */
    public void dismissSafely() {
        if (isAdded() && !isStateSaved()) {
            dismissAllowingStateLoss();
        }
    }

    /**
     * Listener interface for retry button clicks.
     */
    public interface OnRetryClickListener {
        void onRetry();
    }

    /**
     * Listener interface for dialog dismissal.
     */
    public interface OnDismissListener {
        void onDismiss();
    }

    /**
     * Builder class for creating ErrorDialog instances with a fluent API.
     */
    public static class Builder {

        @Nullable
        private String title;

        @NonNull
        private String message = "An error occurred";

        @Nullable
        private String positiveButtonText;

        @Nullable
        private String negativeButtonText;

        private boolean showRetry = false;

        @Nullable
        private OnRetryClickListener retryClickListener;

        @Nullable
        private OnDismissListener dismissListener;

        /**
         * Sets the dialog title.
         *
         * @param title The title
         * @return The Builder instance
         */
        @NonNull
        public Builder setTitle(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param message The message
         * @return The Builder instance
         */
        @NonNull
        public Builder setMessage(@NonNull String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the positive button text.
         *
         * @param text The button text
         * @return The Builder instance
         */
        @NonNull
        public Builder setPositiveButtonText(@NonNull String text) {
            this.positiveButtonText = text;
            return this;
        }

        /**
         * Sets the negative/retry button text.
         *
         * @param text The button text
         * @return The Builder instance
         */
        @NonNull
        public Builder setNegativeButtonText(@NonNull String text) {
            this.negativeButtonText = text;
            return this;
        }

        /**
         * Enables the retry button.
         *
         * @param showRetry true to show retry button
         * @return The Builder instance
         */
        @NonNull
        public Builder setShowRetry(boolean showRetry) {
            this.showRetry = showRetry;
            return this;
        }

        /**
         * Sets the retry click listener.
         *
         * @param listener The listener
         * @return The Builder instance
         */
        @NonNull
        public Builder setOnRetryClickListener(@Nullable OnRetryClickListener listener) {
            this.retryClickListener = listener;
            this.showRetry = listener != null;
            return this;
        }

        /**
         * Sets the dismiss listener.
         *
         * @param listener The listener
         * @return The Builder instance
         */
        @NonNull
        public Builder setOnDismissListener(@Nullable OnDismissListener listener) {
            this.dismissListener = listener;
            return this;
        }

        /**
         * Builds the ErrorDialog instance.
         *
         * @return A new ErrorDialog
         */
        @NonNull
        public ErrorDialog build() {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            args.putString(ARG_MESSAGE, message);
            args.putString(ARG_POSITIVE_BUTTON, positiveButtonText);
            args.putString(ARG_NEGATIVE_BUTTON, negativeButtonText);
            args.putBoolean(ARG_SHOW_RETRY, showRetry);
            dialog.setArguments(args);
            dialog.setOnRetryClickListener(retryClickListener);
            dialog.setOnDismissListener(dismissListener);
            return dialog;
        }

        /**
         * Builds and shows the ErrorDialog.
         *
         * @param fragmentManager The FragmentManager to use
         * @return The shown ErrorDialog
         */
        @NonNull
        public ErrorDialog show(@NonNull FragmentManager fragmentManager) {
            ErrorDialog dialog = build();
            dialog.show(fragmentManager);
            return dialog;
        }
    }

    /**
     * Simple error dialog that can be used without FragmentManager.
     * Use this for simple use cases where lifecycle management is not critical.
     */
    public static class Simple {

        /**
         * Shows a simple error dialog.
         *
         * @param context The context
         * @param message The error message
         */
        public static void show(@NonNull Context context, @NonNull String message) {
            show(context, "Error", message, null);
        }

        /**
         * Shows a simple error dialog with title.
         *
         * @param context The context
         * @param title   The title
         * @param message The error message
         */
        public static void show(@NonNull Context context, @NonNull String title, @NonNull String message) {
            show(context, title, message, null);
        }

        /**
         * Shows a simple error dialog with dismiss callback.
         *
         * @param context        The context
         * @param title          The title
         * @param message        The error message
         * @param dismissListener The dismiss listener
         */
        public static void show(@NonNull Context context, @NonNull String title, @NonNull String message,
                                @Nullable OnDismissListener dismissListener) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (dismissListener != null) {
                            dismissListener.onDismiss();
                        }
                    })
                    .show();
        }

        /**
         * Shows a simple error dialog with retry option.
         *
         * @param context        The context
         * @param title          The title
         * @param message        The error message
         * @param retryListener  The retry listener
         */
        public static void showWithRetry(@NonNull Context context, @NonNull String title,
                                         @NonNull String message, @Nullable OnRetryClickListener retryListener) {
            showWithRetry(context, title, message, retryListener, null);
        }

        /**
         * Shows a simple error dialog with retry option and dismiss callback.
         *
         * @param context         The context
         * @param title           The title
         * @param message         The error message
         * @param retryListener   The retry listener
         * @param dismissListener The dismiss listener
         */
        public static void showWithRetry(@NonNull Context context, @NonNull String title,
                                         @NonNull String message,
                                         @Nullable OnRetryClickListener retryListener,
                                         @Nullable OnDismissListener dismissListener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (dismissListener != null) {
                            dismissListener.onDismiss();
                        }
                    });

            if (retryListener != null) {
                builder.setNegativeButton("Retry", (dialog, which) -> retryListener.onRetry());
            }

            builder.show();
        }
    }
}

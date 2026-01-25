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
     * Constructs an ErrorDialog configured to display the provided message.
     *
     * @param message the error message to display in the dialog
     * @return an ErrorDialog configured with the provided message
     */
    @NonNull
    public static ErrorDialog newInstance(@NonNull String message) {
        return newInstance(null, message);
    }

    /**
     * Create an ErrorDialog configured with the given title and message.
     *
     * @param title   the dialog title, or null to use the default title
     * @param message the error message to display
     * @return an ErrorDialog configured with the provided title and message
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
     * Create an ErrorDialog configured to show a retry button.
     *
     * @param title   optional dialog title; pass null to use the default title
     * @param message error message to display
     * @return an ErrorDialog configured to display a retry option
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

    /**
     * Populates dialog configuration fields (title, message, button texts, and retry flag) from the fragment arguments if present.
     *
     * @param savedInstanceState the previously saved state, if any (not used by this method)
     */
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

    /**
     * Create the AlertDialog displayed by this DialogFragment.
     *
     * @return the Dialog configured with the fragment's message, a title (or "Error" if no title was provided),
     *         a positive "OK" button that invokes the dismiss listener if set, and an optional "Retry" button
     *         that invokes the retry listener if enabled and set.
     */
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

    /**
     * Called when the dialog is dismissed.
     *
     * <p>This override forwards to the superclass but does not invoke the configured
     * OnDismissListener. The dialog's dismiss listener (if any) is invoked only when
     * the positive (OK) button is clicked to distinguish an explicit confirmation
     * from dismissals caused by outside touches or the back button.</p>
     *
     * @param dialog the dialog that was dismissed
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Note: dismissListener is called from button click, not here
        // to differentiate between OK click and outside dismiss
    }

    /**
         * Configure a listener invoked when the dialog's Retry button is clicked.
         *
         * @param listener the listener to invoke when Retry is clicked, or `null` to clear the listener
         * @return this ErrorDialog instance for method chaining
         */
    @NonNull
    public ErrorDialog setOnRetryClickListener(@Nullable OnRetryClickListener listener) {
        this.retryClickListener = listener;
        return this;
    }

    /**
         * Assigns a listener to be invoked when the dialog's positive ("OK") button is clicked.
         *
         * @param listener listener to notify on positive-button click, or null to remove the listener
         * @return this ErrorDialog instance
         */
    @NonNull
    public ErrorDialog setOnDismissListener(@Nullable OnDismissListener listener) {
        this.dismissListener = listener;
        return this;
    }

    /**
     * Displays the dialog with the provided FragmentManager when it is not already added and the manager's state has not been saved.
     *
     * @param fragmentManager the FragmentManager to use to show the dialog
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
        /**
 * Called when the user requests a retry of the failed operation.
 *
 * Implementers should start or schedule the retry logic to handle the failure.
 */
void onRetry();
    }

    /**
     * Listener interface for dialog dismissal.
     */
    public interface OnDismissListener {
        /**
 * Invoked when the dialog is dismissed.
 *
 * <p>Called after the dialog has been dismissed by the user or programmatically.</p>
 */
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
         * Set the dialog title for the ErrorDialog being built.
         *
         * @param title the title to display, or {@code null} to use the default title
         * @return this Builder instance for method chaining
         */
        @NonNull
        public Builder setTitle(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the dialog's error message.
         *
         * @param message the message text to display in the dialog
         * @return this Builder instance
         */
        @NonNull
        public Builder setMessage(@NonNull String message) {
            this.message = message;
            return this;
        }

        /**
                 * Set the text shown on the dialog's positive (confirmation) button.
                 *
                 * @param text the label to display on the positive button
                 * @return this Builder instance for method chaining
                 */
        @NonNull
        public Builder setPositiveButtonText(@NonNull String text) {
            this.positiveButtonText = text;
            return this;
        }

        /**
                 * Set the negative (retry) button label used by the dialog.
                 *
                 * @param text the label to display on the negative/retry button
                 * @return this Builder instance for chaining
                 */
        @NonNull
        public Builder setNegativeButtonText(@NonNull String text) {
            this.negativeButtonText = text;
            return this;
        }

        /**
                 * Configure whether the dialog should include a Retry button.
                 *
                 * @param showRetry true to show the Retry button, false to hide it
                 * @return the Builder instance for chaining
                 */
        @NonNull
        public Builder setShowRetry(boolean showRetry) {
            this.showRetry = showRetry;
            return this;
        }

        /**
         * Assigns a retry callback and enables the retry option when a non-null listener is provided.
         *
         * @param listener the callback invoked when the retry button is pressed; pass `null` to disable retry
         * @return this Builder instance
         */
        @NonNull
        public Builder setOnRetryClickListener(@Nullable OnRetryClickListener listener) {
            this.retryClickListener = listener;
            this.showRetry = listener != null;
            return this;
        }

        /**
                 * Configure a listener to be invoked when the dialog is dismissed.
                 *
                 * @param listener a listener to notify on dialog dismiss; may be {@code null} to clear any previously set listener
                 * @return the Builder instance
                 */
        @NonNull
        public Builder setOnDismissListener(@Nullable OnDismissListener listener) {
            this.dismissListener = listener;
            return this;
        }

        /**
         * Creates an ErrorDialog configured with this Builder's settings.
         *
         * @return the configured ErrorDialog instance
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
         * Show a simple error dialog titled "Error" with the given message and an OK button.
         *
         * @param context the Context used to create and display the dialog
         * @param message the error message to display
         */
        public static void show(@NonNull Context context, @NonNull String message) {
            show(context, "Error", message, null);
        }

        /**
         * Show a simple error dialog with the given title and message.
         *
         * The dialog displays an OK button and does not invoke a dismiss callback.
         *
         * @param context the Context used to build and show the dialog
         * @param title   the dialog title
         * @param message the error message to display
         */
        public static void show(@NonNull Context context, @NonNull String title, @NonNull String message) {
            show(context, title, message, null);
        }

        /**
         * Display an AlertDialog with the given title and message and an OK button.
         *
         * @param context         the Context used to build the dialog
         * @param title           the dialog title
         * @param message         the dialog message
         * @param dismissListener callback invoked when the OK button is pressed (may be null)
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
         * Displays a simple error dialog with an OK button and a Retry button.
         *
         * @param context       Context used to build and show the dialog
         * @param title         Dialog title
         * @param message       Error message shown in the dialog
         * @param retryListener Invoked when the user taps the Retry button; may be null
         */
        public static void showWithRetry(@NonNull Context context, @NonNull String title,
                                         @NonNull String message, @Nullable OnRetryClickListener retryListener) {
            showWithRetry(context, title, message, retryListener, null);
        }

        /**
         * Display a simple error AlertDialog with the given title and message, an OK button that triggers an optional dismiss callback, and an optional Retry button.
         *
         * @param context         the Context used to build and show the dialog
         * @param title           the dialog title
         * @param message         the error message shown in the dialog
         * @param retryListener   invoked when the Retry button is pressed; if `null`, the Retry button is not shown
         * @param dismissListener invoked when the OK button is pressed; may be `null`
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
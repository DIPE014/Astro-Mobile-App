package com.astro.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.astro.app.R;

/**
 * Reusable loading dialog that displays a progress indicator with an optional message.
 * Can be used as a DialogFragment for lifecycle-aware display or as a standalone Dialog.
 */
public class LoadingDialog extends DialogFragment {

    private static final String TAG = "LoadingDialog";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_CANCELABLE = "cancelable";

    @Nullable
    private String message;

    private boolean isCancelableOnTouchOutside = false;

    @Nullable
    private TextView messageTextView;

    /**
     * Create a LoadingDialog configured with the provided message and non-cancelable by default.
     *
     * @param message the message to display in the dialog, or null to hide the message view
     * @return the configured LoadingDialog with cancelable set to false
     */
    @NonNull
    public static LoadingDialog newInstance(@Nullable String message) {
        return newInstance(message, false);
    }

    /**
     * Creates a new instance of LoadingDialog with a custom message and cancelable option.
     *
     * @param message    The message to display
     * @param cancelable Whether the dialog can be canceled
     * @return A new LoadingDialog instance
     */
    @NonNull
    public static LoadingDialog newInstance(@Nullable String message, boolean cancelable) {
        LoadingDialog dialog = new LoadingDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putBoolean(ARG_CANCELABLE, cancelable);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Initializes the dialog's message and cancelable behavior from the fragment arguments and applies the cancelable setting.
     *
     * @param savedInstanceState previously saved state, if any
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE);
            isCancelableOnTouchOutside = getArguments().getBoolean(ARG_CANCELABLE, false);
        }
        setCancelable(isCancelableOnTouchOutside);
    }

    /**
     * Create and configure the loading dialog UI with the currently set message and cancel behavior.
     *
     * The returned dialog contains the inflated loading layout, shows the message if one was provided,
     * applies the fragment's cancelable setting, and sets a transparent window background when available.
     *
     * @return the AlertDialog configured as the loading dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);

        messageTextView = view.findViewById(R.id.loadingMessage);
        if (messageTextView != null) {
            if (message != null && !message.isEmpty()) {
                messageTextView.setText(message);
                messageTextView.setVisibility(View.VISIBLE);
            } else {
                messageTextView.setVisibility(View.GONE);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(isCancelableOnTouchOutside);

        AlertDialog dialog = builder.create();

        // Make dialog background transparent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    /**
     * Updates the displayed loading message and its visibility.
     *
     * Sets the internal message value and, if the dialog's view has been created, updates the message
     * TextView: the view is set to visible and updated when `message` is non-empty, and hidden when
     * `message` is null or empty.
     *
     * @param message the message to display; if null or empty the message view will be hidden
     */
    public void setMessage(@Nullable String message) {
        this.message = message;
        if (messageTextView != null) {
            if (message != null && !message.isEmpty()) {
                messageTextView.setText(message);
                messageTextView.setVisibility(View.VISIBLE);
            } else {
                messageTextView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Shows the dialog using the provided FragmentManager when it is not already added and the manager's state is not saved.
     *
     * @param fragmentManager the FragmentManager used to perform the show operation
     */
    public void show(@NonNull FragmentManager fragmentManager) {
        if (!isAdded() && !fragmentManager.isStateSaved()) {
            show(fragmentManager, TAG);
        }
    }

    /**
     * Dismisses the dialog only if it is currently added and the FragmentManager state is not saved.
     *
     * If those conditions are not met, the method does nothing to avoid IllegalStateException.
     */
    public void dismissSafely() {
        if (isAdded() && !isStateSaved()) {
            dismissAllowingStateLoss();
        }
    }

    /**
     * Builder class for creating LoadingDialog instances with a fluent API.
     */
    public static class Builder {

        @Nullable
        private String message;

        private boolean cancelable = false;

        /**
         * Sets the loading message.
         *
         * @param message The message to display
         * @return The Builder instance
         */
        @NonNull
        public Builder setMessage(@Nullable String message) {
            this.message = message;
            return this;
        }

        /**
         * Configure whether the created LoadingDialog can be canceled by user actions.
         *
         * @param cancelable `true` to allow canceling the dialog, `false` to prevent it
         * @return this Builder instance
         */
        @NonNull
        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * Create a LoadingDialog configured with the builder's settings.
         *
         * @return a {@link LoadingDialog} instance configured with the builder's message and cancelable flag
         */
        @NonNull
        public LoadingDialog build() {
            return LoadingDialog.newInstance(message, cancelable);
        }

        /**
         * Builds a LoadingDialog with the builder's configuration and shows it using the provided FragmentManager.
         *
         * @param fragmentManager the FragmentManager used to show the dialog
         * @return the shown LoadingDialog
         */
        @NonNull
        public LoadingDialog show(@NonNull FragmentManager fragmentManager) {
            LoadingDialog dialog = build();
            dialog.show(fragmentManager);
            return dialog;
        }
    }

    /**
     * Simple loading dialog that can be used without FragmentManager.
     * Use this for simple use cases where lifecycle management is not critical.
     */
    public static class Simple {

        @Nullable
        private Dialog dialog;

        @NonNull
        private final Context context;

        @Nullable
        private String message;

        @Nullable
        private TextView messageTextView;

        /**
         * Constructs a lightweight, non-lifecycle-managed loading dialog using the provided context.
         *
         * @param context the Context used to create and display the dialog; must be non-null (typically an Activity context)
         */
        public Simple(@NonNull Context context) {
            this.context = context;
        }

        /**
         * Set the loading message displayed by this Simple dialog.
         *
         * If `message` is `null` or empty, the message view is hidden; if the dialog is already shown,
         * the visible message is updated immediately.
         *
         * @param message the message to display, or `null`/empty to hide the message
         * @return this Simple instance for chaining
         */
        @NonNull
        public Simple setMessage(@Nullable String message) {
            this.message = message;
            if (messageTextView != null) {
                if (message != null && !message.isEmpty()) {
                    messageTextView.setText(message);
                    messageTextView.setVisibility(View.VISIBLE);
                } else {
                    messageTextView.setVisibility(View.GONE);
                }
            }
            return this;
        }

        /**
         * Displays a non-lifecycle loading dialog using the stored Context.
         *
         * If a dialog is already showing this method does nothing. Otherwise it creates a new Dialog,
         * inflates the loading layout, applies a transparent background and wrap-content sizing,
         * sets non-cancelable behavior, updates the message view visibility/text from the current
         * message value, and shows the dialog.
         */
        public void show() {
            if (dialog != null && dialog.isShowing()) {
                return;
            }

            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_loading);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
            }

            messageTextView = dialog.findViewById(R.id.loadingMessage);
            if (messageTextView != null) {
                if (message != null && !message.isEmpty()) {
                    messageTextView.setText(message);
                    messageTextView.setVisibility(View.VISIBLE);
                } else {
                    messageTextView.setVisibility(View.GONE);
                }
            }

            dialog.show();
        }

        /**
         * Dismisses the loading dialog.
         */
        public void dismiss() {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        }

        /**
         * Reports whether the underlying dialog is currently visible to the user.
         *
         * @return {@code true} if the dialog exists and is showing, {@code false} otherwise.
         */
        public boolean isShowing() {
            return dialog != null && dialog.isShowing();
        }
    }
}
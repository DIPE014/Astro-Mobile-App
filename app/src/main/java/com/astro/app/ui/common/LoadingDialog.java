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
     * Creates a new instance of LoadingDialog with a custom message.
     *
     * @param message The message to display
     * @return A new LoadingDialog instance
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE);
            isCancelableOnTouchOutside = getArguments().getBoolean(ARG_CANCELABLE, false);
        }
        setCancelable(isCancelableOnTouchOutside);
    }

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
     * Updates the loading message.
     *
     * @param message The new message to display
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
     * Shows the loading dialog.
     *
     * @param fragmentManager The FragmentManager to use
     */
    public void show(@NonNull FragmentManager fragmentManager) {
        if (!isAdded() && !fragmentManager.isStateSaved()) {
            show(fragmentManager, TAG);
        }
    }

    /**
     * Dismisses the loading dialog safely.
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
         * Sets whether the dialog can be canceled.
         *
         * @param cancelable true if cancelable
         * @return The Builder instance
         */
        @NonNull
        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * Builds the LoadingDialog instance.
         *
         * @return A new LoadingDialog
         */
        @NonNull
        public LoadingDialog build() {
            return LoadingDialog.newInstance(message, cancelable);
        }

        /**
         * Builds and shows the LoadingDialog.
         *
         * @param fragmentManager The FragmentManager to use
         * @return The shown LoadingDialog
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
         * Creates a new Simple loading dialog.
         *
         * @param context The context
         */
        public Simple(@NonNull Context context) {
            this.context = context;
        }

        /**
         * Sets the loading message.
         *
         * @param message The message
         * @return The Simple instance
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
         * Shows the loading dialog.
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
         * Checks if the dialog is currently showing.
         *
         * @return true if showing
         */
        public boolean isShowing() {
            return dialog != null && dialog.isShowing();
        }
    }
}

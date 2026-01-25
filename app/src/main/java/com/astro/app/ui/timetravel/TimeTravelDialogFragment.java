package com.astro.app.ui.timetravel;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.astro.app.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Material Design 3 dialog for selecting a date and time for time travel.
 *
 * <p>Allows users to:</p>
 * <ul>
 *   <li>Select a specific date using a date picker</li>
 *   <li>Select a specific time using a time picker</li>
 *   <li>Use quick presets (tonight, sunrise, sunset, etc.)</li>
 *   <li>Return to real time</li>
 * </ul>
 */
public class TimeTravelDialogFragment extends DialogFragment {

    private static final String TAG = "TimeTravelDialog";

    /** Callback interface for time travel selection */
    public interface TimeTravelCallback {
        void onTimeTravelSelected(int year, int month, int day, int hour, int minute);
        void onReturnToRealTime();
    }

    private TimeTravelCallback callback;
    private Calendar selectedDateTime;
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public static TimeTravelDialogFragment newInstance() {
        return new TimeTravelDialogFragment();
    }

    public void setCallback(TimeTravelCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AstroApp_Dialog_TimeTravel);
        selectedDateTime = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_time_travel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        MaterialButton btnPickDate = view.findViewById(R.id.btnPickDate);
        MaterialButton btnPickTime = view.findViewById(R.id.btnPickTime);
        MaterialButton btnTonight = view.findViewById(R.id.btnTonight);
        MaterialButton btnSunrise = view.findViewById(R.id.btnSunrise);
        MaterialButton btnSunset = view.findViewById(R.id.btnSunset);
        MaterialButton btnMidnight = view.findViewById(R.id.btnMidnight);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnRealTime = view.findViewById(R.id.btnRealTime);
        MaterialButton btnGoToTime = view.findViewById(R.id.btnGoToTime);

        // Update displayed date/time
        updateDateTimeDisplay();

        // Date picker button
        btnPickDate.setOnClickListener(v -> showDatePicker());

        // Time picker button
        btnPickTime.setOnClickListener(v -> showTimePicker());

        // Quick presets
        btnTonight.setOnClickListener(v -> setTonight());
        btnSunrise.setOnClickListener(v -> setSunrise());
        btnSunset.setOnClickListener(v -> setSunset());
        btnMidnight.setOnClickListener(v -> setMidnight());

        // Action buttons
        btnCancel.setOnClickListener(v -> dismiss());

        btnRealTime.setOnClickListener(v -> {
            if (callback != null) {
                callback.onReturnToRealTime();
            }
            dismiss();
        });

        btnGoToTime.setOnClickListener(v -> {
            if (callback != null) {
                callback.onTimeTravelSelected(
                        selectedDateTime.get(Calendar.YEAR),
                        selectedDateTime.get(Calendar.MONTH) + 1,
                        selectedDateTime.get(Calendar.DAY_OF_MONTH),
                        selectedDateTime.get(Calendar.HOUR_OF_DAY),
                        selectedDateTime.get(Calendar.MINUTE)
                );
            }
            dismiss();
        });
    }

    private void showDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                R.style.Theme_AstroApp_DatePicker,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private void showTimePicker() {
        TimePickerDialog picker = new TimePickerDialog(
                requireContext(),
                R.style.Theme_AstroApp_TimePicker,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false // 12-hour format
        );
        picker.show();
    }

    private void updateDateTimeDisplay() {
        Date date = selectedDateTime.getTime();
        tvSelectedDate.setText(dateFormat.format(date));
        tvSelectedTime.setText(timeFormat.format(date));
    }

    private void setTonight() {
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 21);
        selectedDateTime.set(Calendar.MINUTE, 0);
        updateDateTimeDisplay();
    }

    private void setSunrise() {
        // Approximate sunrise at 6 AM (actual calculation would use location and date)
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 6);
        selectedDateTime.set(Calendar.MINUTE, 0);
        updateDateTimeDisplay();
    }

    private void setSunset() {
        // Approximate sunset at 6 PM (actual calculation would use location and date)
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 18);
        selectedDateTime.set(Calendar.MINUTE, 0);
        updateDateTimeDisplay();
    }

    private void setMidnight() {
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 0);
        selectedDateTime.set(Calendar.MINUTE, 0);
        updateDateTimeDisplay();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}

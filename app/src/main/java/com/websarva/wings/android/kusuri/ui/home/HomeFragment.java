package com.websarva.wings.android.kusuri.ui.home;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.MedicationDates;
import com.websarva.wings.android.kusuri.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MaterialCalendarView calendarView;
    private CalendarDay selectedDate;
    private SharedPreferences preferences;

    private MedicationDao medicationDao;
    private LinearLayout checklistLayout;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = getActivity().getSharedPreferences("calendar_prefs", 0);
        Log.d("HomeFragment", "All preferences: " + preferences.getAll());

        calendarView = binding.calendarView;
        checklistLayout = binding.checklist;



//        loadSavedDots();

        CalendarDay today = CalendarDay.today();
        calendarView.setDateSelected(today, true);
        selectedDate = today;



        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = date;
            loadMedicationCheckBoxesForSelectedDate(date);
        });

        medicationDao = AppDatabase.getDatabase(requireContext()).medicationDao();

        new Thread(() -> {
            List<MedicationDates> medicationDatesList = medicationDao.getAllMedicationDates();
            List<CalendarDay> daysToDecorate = new ArrayList<>();

            for (MedicationDates dates : medicationDatesList) {
                long startTimestamp = dates.startdate;
                long endTimestamp = dates.enddate;

                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(startTimestamp);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(endTimestamp);

                while (startCal.compareTo(endCal) <= 0) {
                    int year = startCal.get(Calendar.YEAR);
                    int month = startCal.get(Calendar.MONTH) + 1;
                    int day = startCal.get(Calendar.DAY_OF_MONTH);

                    daysToDecorate.add(CalendarDay.from(year, month, day));
                    startCal.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            getActivity().runOnUiThread(() -> {
                calendarView.removeDecorators();
                for (CalendarDay day : daysToDecorate) {
                    calendarView.addDecorator(new DotDecorator(day, Color.RED));
                }
                calendarView.invalidateDecorators();
                loadSavedDots();

            });
        }).start();

        updateCalendarMarker();
    }

    private void loadMedicationCheckBoxesForSelectedDate(CalendarDay selectedDate) {
        String dateKey = getDateKey(selectedDate);

        new Thread(() -> {
            List<Medication> medicationList = medicationDao.getMedicationForDate(convertToTimestamp(selectedDate));

            Log.d("loadMedicationCheckBoxesForSelectedDate", "medicationList = " + medicationList);

            getActivity().runOnUiThread(() -> {
                checklistLayout.removeAllViews();

                for (Medication medication : medicationList) {
                    int frequency = medication.frequency;

                    List<Boolean> savedStates = loadChecklistState(dateKey + "_" + medication.id, frequency);

                    for (int i = 0; i < frequency; i++) {
                        LinearLayout checkBoxLayout = new LinearLayout(getContext());
                        checkBoxLayout.setOrientation(LinearLayout.HORIZONTAL);

                        CheckBox checkBox = new CheckBox(getContext());
                        checkBox.setText(medication.name + " " + (i + 1));
                        checkBox.setChecked(savedStates.get(i));

                        final int index = i;
                        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            savedStates.set(index, isChecked);
                            saveChecklistState(dateKey + "_" + medication.id, savedStates);
                            updateCalendarMarker();
                        });

                        checkBoxLayout.addView(checkBox);
                        checklistLayout.addView(checkBoxLayout);
                    }
                }
            });
        }).start();
    }

    //チェックリストの状態をSharedPreferencesに保存し、その日付がすべてチェック完了されているかどうかに応じてカレンダーに青いドットか赤ドットかを決める
    private void saveChecklistState(String dateKey, List<Boolean> states) {
        SharedPreferences.Editor editor = preferences.edit();
        boolean allChecked = true;

        for (int i = 0; i < states.size(); i++) {
            editor.putBoolean(dateKey + "_check_" + i, states.get(i));
            if (!states.get(i)) {
                allChecked = false;
            }
        }

        // 全てチェックされている場合、青いドットとして保存
        if (allChecked) {
            editor.putBoolean(dateKey + "_blue_dot", true);
            addDateToSavedDates(dateKey);
        } else {
            editor.remove(dateKey + "_blue_dot");
        }

        editor.apply();
    }

    private void addDateToSavedDates(String dateKey) {
        Set<String> savedDates = preferences.getStringSet("saved_dates", new HashSet<>());
        savedDates.add(dateKey);
        preferences.edit().putStringSet("saved_dates", savedDates).commit();
    }


    private List<Boolean> loadChecklistState(String dateKey, int itemCount) {
        List<Boolean> states = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            states.add(preferences.getBoolean(dateKey + "_check_" + i, false));
        }
        return states;
    }

    public long convertToTimestamp(CalendarDay calendarDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendarDay.getYear(), calendarDay.getMonth() - 1, calendarDay.getDay());
        return calendar.getTimeInMillis();
    }

    private void updateCalendarMarker() {
        if (selectedDate == null) return;

        new Thread(() -> {
            List<CalendarDay> daysToDecorate = new ArrayList<>();
            List<CalendarDay> completedDays = new ArrayList<>();

            List<MedicationDates> medicationDatesList = medicationDao.getAllMedicationDates();

            for (MedicationDates dates : medicationDatesList) {
                long startTimestamp = dates.startdate;
                long endTimestamp = dates.enddate;

                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(startTimestamp);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(endTimestamp);

                while (startCal.compareTo(endCal) <= 0) {
                    int year = startCal.get(Calendar.YEAR);
                    int month = startCal.get(Calendar.MONTH) + 1;
                    int day = startCal.get(Calendar.DAY_OF_MONTH);
                    CalendarDay dayObj = CalendarDay.from(year, month, day);

                    daysToDecorate.add(dayObj);

                    String dayKey = getDateKey(dayObj);
                    //カレンダーの日付がすべてチェックされている場合に青いドットをカレンダーに追加し、SharedPreferencesにも保存
                    if (areAllChecklistsComplete(dayKey)) {
                        completedDays.add(dayObj);

                        // 青いドットとして保存
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(dayKey + "_blue_dot", true);
                        editor.apply();
                    } else {
                        // 赤いドットとして保存
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.remove(dayKey + "_blue_dot");
                        editor.apply();
                    }

                    startCal.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            getActivity().runOnUiThread(() -> {
                calendarView.removeDecorators();

                for (CalendarDay day : daysToDecorate) {
                    calendarView.addDecorator(new DotDecorator(day, Color.RED));
                }

                for (CalendarDay day : completedDays) {
                    calendarView.addDecorator(new DotDecorator(day, Color.BLUE));
                }

                calendarView.invalidateDecorators();
                calendarView.invalidate();
            });
        }).start();
    }


    private boolean areAllChecklistsComplete(String dateKey) {
        List<Medication> medications = medicationDao.getMedicationForDate(convertToTimestamp(getCalendarDayFromKey(dateKey)));

        for (Medication medication : medications) {
            int frequency = medication.frequency;
            List<Boolean> states = loadChecklistState(dateKey + "_" + medication.id, frequency);

            for (boolean state : states) {
                if (!state) {
                    return false;
                }
            }
        }
        return true;
    }

    //SharedPreferencesに保存されている情報を基に、カレンダーの日付に対応するドットを設定
    private void loadSavedDots() {
        Set<String> savedDates = preferences.getStringSet("saved_dates", new HashSet<>());

        Log.d("HomeFragment", "savedDates = " + savedDates);

        for (String savedDateKey : savedDates) {
            CalendarDay day = getCalendarDayFromKey(savedDateKey);

//            Log.d("HomeFragment", "day = " + day);
            boolean ret = preferences.getBoolean(savedDateKey + "_blue_dot", false);
//            Log.d("HomeFragment", "preferences.getBoolean() = " + ret);

            if (preferences.getBoolean(savedDateKey + "_blue_dot", false)) {
                // 青いドット
                calendarView.addDecorator(new DotDecorator(day, Color.BLUE));
            } else {
                // 赤いドット
                calendarView.addDecorator(new DotDecorator(day, Color.RED));
            }
        }

        // ドットの更新を確実に反映させる
        calendarView.invalidateDecorators();
    }


    private String getDateKey(CalendarDay date) {
//        return date.getYear() + "-" + date.getMonth() + "-" + date.getDay();

        // CalendarDayをCalendar型に変換
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());

        // SimpleDateFormatを使用して"yyyy-MM-dd"形式の文字列に変換
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(calendar.getTime());
    }

    private CalendarDay getCalendarDayFromKey(String dateKey) {
        String[] parts = dateKey.split("-");
//        Log.d("HomeFragment", "dateKey = " + dateKey);
//        Log.d("HomeFragment", "parts = " + parts);
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        // 日付部分（"13_1"）をさらに分割
        String[] dayParts = parts[2].split("_");
        int day = Integer.parseInt(dayParts[0]);  // "13" を取得
        return CalendarDay.from(year, month, day);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
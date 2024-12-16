package com.websarva.wings.android.kusuri.ui.notifications;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.ReminderDao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {
    private List<Medication> medicationList;
    private final MedicationDao medicationDao;
    private final Context context;

    //カレンダーのプリファレンシス
    private SharedPreferences preferences;

    public MedicationAdapter(List<Medication> medicationList, MedicationDao medicationDao, Context context) {
        this.medicationList = medicationList;
        this.medicationDao = medicationDao;
        this.context = context;

        preferences = this.context.getSharedPreferences("calendar_prefs", 0);

    }

//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView textView1;
//        TextView textView2;
//
//        public ViewHolder(View itemView) {
//            super(itemView);
//            textView1 = itemView.findViewById(R.id.healthcare_id);
//            textView2 = itemView.findViewById(R.id.healthcare_date);
//        }
//    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication_list, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = medicationList.get(position);
        holder.medicationName.setText(medication.name);
        holder.medicationDate.setText(medication.getFormattedCreationDate());

        // 行の位置によって背景色を交互に設定
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));  // 偶数行: 白
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F1EFF8"));  // 奇数行: 薄い紫
        }

        // 削除ボタンのクリックリスナー
        holder.deleteButton.setOnClickListener(
                v -> showDeleteConfirmationDialog(medication, position)
        );

        // 編集ボタンのクリックリスナー
        holder.editButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("medicationId", medication.id);  // IDを渡す
            Navigation.findNavController(v)
                    .navigate(R.id.action_medicationListFragment_to_editMedicationFragment, bundle);
        });


    }

    @Override
    public int getItemCount() {
        return medicationList.size();
    }

    private void showDeleteConfirmationDialog(Medication medication, int position) {
        new AlertDialog.Builder(context)
                .setTitle("削除確認")
                .setMessage("このデータを削除してもよろしいですか？")
                .setPositiveButton("OK", (dialog, which) -> deleteMedication(medication, position))
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void deleteMedication(Medication medication, int position) {
        new Thread(() -> {
            // リマインダーの削除処理
            cancelMedicationReminder(medication);

            // メディケーションの削除処理
            medicationDao.deleteMedication(medication);

            removeBlueDotForDateRange(medication);

            // UIスレッドでの更新処理
            ((FragmentActivity) context).runOnUiThread(() -> {
                medicationList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "データが削除されました", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }


    private void removeBlueDotForDateRange(Medication medication) {
        // 開始日と終了日をCalendarにセット
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(medication.startdate);  // startdateはタイムスタンプ

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(medication.enddate);  // enddateもタイムスタンプ

        // SharedPreferencesのエディタを取得
        SharedPreferences.Editor editor = preferences.edit();

        // 日付の範囲を反復処理
        while (!startCalendar.after(endCalendar)) {
            // 日付を"yyyy-MM-dd"形式で取得
            String dateKey = getDateKeyFromCalendar(startCalendar);

            // 正規表現でキーの形式を指定
            String regex = dateKey + "(_\\d+)?_blue_dot";

            Log.d("HomeFragment", "removeBlueDotForDateRange regex = " + regex);

            Pattern pattern = Pattern.compile(regex);

            // SharedPreferencesの全データを取得
            Map<String, ?> allEntries = (Map<String, ?>) preferences.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();

                Log.d("HomeFragment", "removeBlueDotForDateRange all key = " + key);

                // キーが正規表現に一致する場合は削除
                if (pattern.matcher(key).matches()) {
                    editor.remove(key);

                    Log.d("HomeFragment", "removeBlueDotForDateRange remove key = " + key);
                }
            }


            // savedDates キーに保存されている値を取得
            Set<String> savedDates = preferences.getStringSet("saved_dates", new HashSet<>());
            Log.d("HomeFragment", "removeBlueDotForDateRange savedDates = " + savedDates);

            regex = dateKey + "(_\\d+)?";
            Log.d("HomeFragment", "removeBlueDotForDateRange savedDates regex = " + regex);
            Pattern pattern2 = Pattern.compile(regex);

            // 削除対象を探す
            Set<String> updatedDates = savedDates.stream()
                    .filter(date -> !pattern2.matcher(date).matches())
                    .collect(Collectors.toSet());
            editor.putStringSet("saved_dates", updatedDates);

            Log.d("HomeFragment", "removeBlueDotForDateRange updatedDates = " + updatedDates);

            // カレンダーを1日進める
            startCalendar.add(Calendar.DAY_OF_MONTH, 1);

            // 変更を保存
            editor.apply();
        }



        // 変更を保存
        editor.apply();
    }

    private String getDateKeyFromCalendar(Calendar calendar) {
        // 日付を"yyyy-MM-dd"形式で取得
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    // リマインダーをキャンセルするメソッド
    private void cancelMedicationReminder(Medication medication) {
        Log.d("MedicationReminder", "Attempting to cancel reminder for medication");

        int daycnt = 1;
        int cnt = 1;
        for (int i = 0; i < medication.frequency; i++) {


            // 開始日時を指定した時間で設定
            Calendar currentDate = Calendar.getInstance();
            currentDate.setTimeInMillis(medication.startdate); // タイムスタンプを日時に変換

            // 終了日時をカレンダーで設定
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTimeInMillis(medication.enddate); // タイムスタンプを日時に変換


            while (!currentDate.after(endCalendar)) {
                cancelDailyNotification(cnt, daycnt, medication.id); // 通知を設定

                int year = currentDate.get(Calendar.YEAR);
                int month = currentDate.get(Calendar.MONTH) + 1; // 0始まりなので+1
                int day = currentDate.get(Calendar.DAY_OF_MONTH);
                Log.d("MedicationReminder", "設定された日付: " + year + "年" + month + "月" + day + "日");

                currentDate.add(Calendar.DATE, 1); // 次の日に進む
                Log.d("MedicationReminder", daycnt*cnt + "回目キャンセル: cnt　= " + cnt + " daycnt = " + daycnt + " medication.id = " + medication.id);

                daycnt++;

            }
            cnt++;
        }


    }

    private void cancelDailyNotification(int cnt, int daycnt, long medication_id) {
        Log.d("MedicationReminder", "cancelDailyNotification cnt　= " + cnt + " daycnt = " + daycnt + " medication_id = " + medication_id);

        int id = (int)medication_id*1000 + daycnt*10 + cnt;
        // PendingIntentを再作成し、キャンセルする
        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                id,  // 同じIDを使用
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // AlarmManagerを取得して、リマインダーをキャンセル
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);  // PendingIntentをキャンセル
            Log.d("MedicationReminder", "Reminder cancelled for medication: " + id);
        }
    }


    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView medicationName, medicationDate;
        Button deleteButton, editButton;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medicationName = itemView.findViewById(R.id.medication_name);
            medicationDate =  itemView.findViewById(R.id.medication_date);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);


        }
    }

}
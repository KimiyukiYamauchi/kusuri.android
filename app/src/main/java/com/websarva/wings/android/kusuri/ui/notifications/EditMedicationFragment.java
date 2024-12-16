package com.websarva.wings.android.kusuri.ui.notifications;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.Reminder;
import com.websarva.wings.android.kusuri.ReminderDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;

public class EditMedicationFragment extends Fragment {
    private EditText medicationNameInput, medicationDosageInput, medicationFrequencyInput,
            medicationStartDateInput, medicationEndDateInput, medicationMemoInput;
    private Spinner medicationTimingSpinner, medicationDosageSpinner, notificationSpinner;
    private Button pickerbutton, registerButton, cancelButton;    //タイムピッカー・登録・キャンセルボタン
    private LinearLayout timePickerLayout; // タイムピッカーを表示するレイアウト
    private ArrayList<TimePickerDialog> timePickers = new ArrayList<>(); // タイムピッカーのリスト
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView cameraImageView, selectImageView, deleteImageView, photoImageView; //カメラのイメージ　ギャラりー　削除ボタン　写真表示のイメージ
    private boolean isPhotoDeleted = true;         //写真が削除されたか
    private Uri _imageUri;
    private MedicationDao medicationDao;
    private Medication currentMedication;
    private ReminderDao reminderDao;
    private List<Reminder>  currentReminders;
    private AppDatabase db;

    //リマインダー機能
    private EditText startDateEditText, endDateEditText;    //服用開始・服用終了
    private TimePicker timePicker;                          //タイムピッカー
    private Calendar startDate, endDate, selectedTime;
    private int hourofday, minute;
    private List<int[]> selectedTimes;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_medication, container, false);


    }

//    @NonNull
//    @Override
//    public EditMedicationFragment.MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_medication_list, parent, false);
//        return new MedicationAdapter.MedicationViewHolder(view);
//    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context =getContext();

        db = AppDatabase.getDatabase(getActivity());
        medicationDao = AppDatabase.getDatabase(requireContext()).medicationDao();
        reminderDao = AppDatabase.getDatabase(requireContext()).reminderDao();
        medicationNameInput = view.findViewById(R.id.medicine_name_edit_ud);     //おくすり名
        medicationFrequencyInput = view.findViewById(R.id.doscount_input_ud);  //服用回数
        medicationDosageInput = view.findViewById(R.id.dosage_input_ud);    //服用量
        medicationDosageSpinner = view.findViewById(R.id.dosage_spinner_ud);//錠・包
        medicationTimingSpinner = view.findViewById(R.id.MDtiming_spinner_ud); //服薬タイミング
        medicationStartDateInput = view.findViewById(R.id.medication_startdate_ud);//服用期間（開始）
        medicationEndDateInput = view.findViewById(R.id.medication_enddate_ud);//服用期間（終了）
        medicationMemoInput = view.findViewById(R.id.memo_edit_ud);//メモ
        notificationSpinner = view.findViewById(R.id.notification_spinner_ud);//通知（リマインダー）
        cameraImageView = view.findViewById(R.id.camera_image_view_ud); //写真登録ボタン

        deleteImageView = view.findViewById(R.id.delete_image_view_ud); //写真削除ボタン
        photoImageView = view.findViewById(R.id.photo_image_view_ud);//お薬写真
        Button saveButton = view.findViewById(R.id.register_button_ud);
        Button cancelButton = view.findViewById(R.id.cancel_button_ud);


        int medicationId = getArguments().getInt("medicationId", -1);

        // IDからMedicationを取得し、各フィールドに設定
        new Thread(() -> {
            currentMedication = medicationDao.getMedicationById(medicationId);
            currentReminders = reminderDao.getRemindersByMedicationId(medicationId);

            if (currentMedication != null) {
                getActivity().runOnUiThread(() -> {
                    medicationNameInput.setText(currentMedication.name);
                    medicationFrequencyInput.setText(String.valueOf(currentMedication.frequency));
                    medicationDosageInput.setText(String.valueOf(currentMedication.dosage));
                    medicationStartDateInput.setText(String.valueOf(currentMedication.getStartDate()));
                    medicationEndDateInput.setText(String.valueOf(currentMedication.getEndDate()));
                    medicationMemoInput.setText(String.valueOf(currentMedication.memo));


                    //　Spinnerの項目から一致するインデックスを探して設定（錠・包）
                    String dosageSpinner = currentMedication.dosageSpinner;
                    for (int i = 0; i < medicationDosageSpinner.getCount(); i++) {
                        if (medicationDosageSpinner.getItemAtPosition(i).toString().equals(dosageSpinner)) {
                            medicationDosageSpinner.setSelection(i);
                            break;
                        }
                    }

                    //　Spinnerの項目から一致するインデックスを探して設定（服薬タイミング）
                    String timingSpinner = currentMedication.timing;
                    for (int i = 0; i < medicationTimingSpinner.getCount(); i++) {
                        if (medicationTimingSpinner.getItemAtPosition(i).toString().equals(timingSpinner)) {
                            medicationTimingSpinner.setSelection(i);
                            break;
                        }
                    }

                    //　Spinnerの項目から一致するインデックスを探して設定（通知）
                    String reminder = currentMedication.reminder;
                    for (int i = 0; i < notificationSpinner.getCount(); i++) {
                        if (notificationSpinner.getItemAtPosition(i).toString().equals(reminder)) {
                            notificationSpinner.setSelection(i);
                            break;
                        }
                    }

                    // 保存されたURIを取得して画像を表示
                    if (currentMedication.imageUri != null && !currentMedication.imageUri.isEmpty()) {
                        isPhotoDeleted = false;
                        try {
                             _imageUri = Uri.parse(currentMedication.imageUri);
                            photoImageView.setImageURI(_imageUri);
                            // クリックリスナーを設定（ポップアップ表示用）
                            photoImageView.setOnClickListener(v -> {
                                Log.d("PhotoDebug", "PhotoImageView clicked (saved image)");
                                if (_imageUri != null && !isPhotoDeleted) {
                                    onImageTap(_imageUri); // ポップアップ表示
                                }else{
                                    Log.d("PhotoDebug", "Image has been deleted"); // ログ追加
                                    Toast.makeText(getActivity(), "写真はありません", Toast.LENGTH_SHORT).show();  //追加
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "保存された画像がありません", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();

        // 服用開始日入力欄をクリックしたときにDatePickerを表示
        medicationStartDateInput.setOnClickListener(
                v -> showDatePickerDialog(medicationStartDateInput, currentMedication.startdate));
        // 服用終了日入力欄をクリックしたときにDatePickerを表示
        medicationEndDateInput.setOnClickListener(
                v -> showDatePickerDialog(medicationEndDateInput, currentMedication.enddate));



        //服用量の入力制限
        medicationDosageInput.setFilters(new InputFilter[]{ new medicationDosageFilter() });
        //服用回数の入力制限
        medicationFrequencyInput.setFilters(new InputFilter[]{ new medicationFrequencyFilter() });
        //タイムピッカーを設定
        medicationFrequencyInput = view.findViewById(R.id.doscount_input_ud);
        timePickerLayout = view.findViewById(R.id.time_picker_layout_ud);
        pickerbutton = view.findViewById(R.id.picker_button_ud);

        pickerbutton.setOnClickListener(v -> {
            try {
                int doseCount = Integer.parseInt(medicationFrequencyInput.getText().toString());
                if (doseCount > 0) {
                    showTimePickers();
                } else {
                    Toast.makeText(getActivity(), "回数を正しく入力してください", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "回数を正しく入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        //

        //写真を撮るボタンのクリックリスナー
        cameraImageView.setOnClickListener(v -> dispatchTakePictureIntent());
        //写真を選択ボタンのクリックリスナー
        selectImageView = view.findViewById(R.id.select_image_view_ud);
        selectImageView.setOnClickListener(v -> openGallery());
        // 画像を削除ボタンのクリックリスナー
        deleteImageView.setOnClickListener(v -> {
                    //追加
                    if (_imageUri == null){
                        Toast.makeText(getActivity(), "写真はありません", Toast.LENGTH_SHORT).show();  //追加
                    }else {
                        new AlertDialog.Builder(getActivity())
                                .setTitle("確認")
                                .setMessage("本当に削除しますか？")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    photoImageView.setImageDrawable(null);
                                    isPhotoDeleted = true;
                                })
                                .setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
        });

        //ポップアップのクリックリスナー
        photoImageView.setOnClickListener(v -> {
            Log.d("PhotoDebug", "PhotoImageView clicked");

            if (_imageUri != null && !isPhotoDeleted) {
                Log.d("PhotoDebug", "Image exists and not deleted");
                onImageTap(_imageUri); // ポップアップ表示
            } else if (isPhotoDeleted) {
                Log.d("PhotoDebug", "Image has been deleted");
                Toast.makeText(getActivity(), "画像は削除されました。新しい画像を撮影してください。", Toast.LENGTH_LONG).show();
            } else {
                Log.d("PhotoDebug", "Image not captured yet");
                Toast.makeText(getActivity(), "画像がまだ撮影されていません。", Toast.LENGTH_SHORT).show();
            }
        });





        // 保存ボタンでデータを更新
        saveButton.setOnClickListener(v -> {

            Log.d("EditMedicationFragment", "EditMedicationFragment saveButton.setOnClickListener");
            String name = medicationNameInput.getText().toString();
            String frequency = medicationFrequencyInput.getText().toString();
            String dosage = medicationDosageInput.getText().toString();
            String dosageSpinner = medicationDosageSpinner.getSelectedItem().toString();
            String timingSpinner = medicationTimingSpinner.getSelectedItem().toString();
            String startdate = medicationStartDateInput.getText().toString();
            String enddate = medicationEndDateInput.getText().toString();
            String memo = medicationMemoInput.getText().toString();
            String notification = notificationSpinner.getSelectedItem().toString();
            // 写真のURIをチェックし登録
            if (_imageUri != null) {
                currentMedication.imageUri = _imageUri.toString(); // URIを文字列として保存
            } else {
                currentMedication.imageUri = null; // 空の場合はnullを設定
            }
            long currentMedicationLong = currentMedication.id;
//            long startdateLong = currentMedication.startdate;
//            long enddateLong = currentMedication.enddate;

            if (name.isEmpty()) {
                Toast.makeText(getActivity(), "おくすり名を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (frequency.isEmpty()) {
                Toast.makeText(getActivity(), "服用回数を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dosage.isEmpty()) {
                Toast.makeText(getActivity(), "服用量を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startdate.isEmpty()) {
                Toast.makeText(getActivity(), "服用開始日を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (enddate.isEmpty()) {
                Toast.makeText(getActivity(), "服用終了日を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            //終了日が開始日以前に設定されている場合、トーストを表示する
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            try {
                // 開始日と終了日をDate型に変換
                Date startDate = dateFormat.parse(startdate);
                Date endDate = dateFormat.parse(enddate);

                // 日付の比較
                if (startDate != null && endDate != null && endDate.before(startDate)) {
                    Toast.makeText(getActivity(), "終了日は開始日以降の日付を設定してください", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                // 日付のパースに失敗した場合（通常はここには到達しない）
                Toast.makeText(getActivity(), "日付形式が正しくありません。", Toast.LENGTH_SHORT).show();
                return;
            }


            // タイムピッカーが設定されているかチェック
            if (this.selectedTimes == null || this.selectedTimes.isEmpty()) {
                Toast.makeText(getActivity(), "時間を設定してください", Toast.LENGTH_SHORT).show();
                return;
            }


        try {

            // 日付をタイムスタンプ(long)に変換する
            long startdateLong = convertDateToTimestamp(startdate);
            long enddateLong = convertDateToTimestamp2(enddate);

            // 通知が「あり」の場合に通知の設定をする
            if (notification.equals("あり")){
                //リマインダーの削除
                cancelMedicationReminder(currentMedication);
                scheduleDailyNotifications(currentMedicationLong, startdateLong, enddateLong);
            }else{
                cancelMedicationReminder(currentMedication);
            }

        } catch (NumberFormatException e) {
            if (this != null) {
                Toast.makeText(getActivity(), "全ての項目に値を入力してください。", Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e){
            if (this != null) {
                Toast.makeText(getActivity(), "日付の形式が正しくありません。", Toast.LENGTH_LONG).show();
            }
        }

        Toast.makeText(getActivity(), "おくすりを登録しました", Toast.LENGTH_LONG).show();

//        finish();  // Activityを閉じる

            new Thread(() -> {
                currentMedication.name = name;
                currentMedication.frequency = Integer.parseInt(frequency);
                currentMedication.dosage = Integer.parseInt(dosage);
                currentMedication.dosageSpinner = dosageSpinner;
                currentMedication.timing = timingSpinner;
                try {
                    currentMedication.startdate = convertDateToTimestamp(startdate);
                    currentMedication.enddate = convertDateToTimestamp2(enddate);
                } catch (ParseException ex) {
                    currentMedication.startdate = System.currentTimeMillis();
                    currentMedication.enddate = System.currentTimeMillis();
                }

                currentMedication.memo = memo;
                currentMedication.reminder = notification;



                medicationDao.updateMedication(currentMedication);

                saveRemindersToDatabase(currentMedication.id);
            }).start();

            getActivity().getSupportFragmentManager().popBackStack();
            Toast.makeText(getActivity(), "お薬情報を更新しました", Toast.LENGTH_LONG).show();

        });
        // キャンセルボタンのクリックイベント
        cancelButton.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().popBackStack();// 画面を閉じる
            Toast.makeText(getActivity(), "キャンセルしました", Toast.LENGTH_SHORT).show();
        });

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
        Intent intent = new Intent(this.getActivity(), MedicationReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getActivity(),
                id,  // 同じIDを使用
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // AlarmManagerを取得して、リマインダーをキャンセル
        AlarmManager alarmManager = (AlarmManager) this.getActivity().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);  // PendingIntentをキャンセル
            Log.d("MedicationReminder", "Reminder cancelled for medication: " + id);
        }
    }


    // 入力された服薬回数分だけタイムピッカーを表示する
    private void showTimePickers() {
        // タイムピッカーのリストをリセット
        timePickerLayout.removeAllViews();
        timePickers.clear();

        int doseCount = Integer.parseInt(medicationFrequencyInput.getText().toString());

        // selectedTimes 配列を調整
        if (this.selectedTimes == null) {
            this.selectedTimes = new ArrayList<>();
        }
        // 入力された doseCount に応じて selectedTimes のサイズを変更
        if (doseCount > this.selectedTimes.size()) {
            // リストのサイズが不足している場合、デフォルトの時間を追加
            for (int i = this.selectedTimes.size(); i < doseCount; i++) {
                this.selectedTimes.add(new int[]{8, 0}); // デフォルトの時間（8:00）
            }
        } else if (doseCount < this.selectedTimes.size()) {
            // リストが大きすぎる場合、余分な要素を削除
            while (this.selectedTimes.size() > doseCount) {
                this.selectedTimes.remove(this.selectedTimes.size() - 1);
            }
        }

        // 現在の設定に応じてリストを調整
        for (int i = 0; i < Math.min(doseCount, currentMedication.frequency); i++) {
            int hour = currentReminders.get(i).HourOfDay;
            int minutes = currentReminders.get(i).Minute;
            this.selectedTimes.set(i, new int[]{hour, minutes});
        }


        for (int i = 0; i < doseCount; i++) {
            final int index = i;
            // タイムピッカーのレイアウトを作成
            View timePickerView = getLayoutInflater().inflate(R.layout.time_picker_item, timePickerLayout, false);

            TextView timeLabel = timePickerView.findViewById(R.id.time_label);
            timeLabel.setText("服薬 " + (index + 1) + " 回目");

            Button timePickerButton = timePickerView.findViewById(R.id.time_picker_button);
            int [] hourminutes = selectedTimes.get(i);
            int hour = hourminutes[0];
            int minutes = hourminutes[1];
            timePickerButton.setText(String.format("%02d:%02d", hour, minutes));

//            timePickerButton.setText();
            timePickerButton.setOnClickListener(v -> {
                // TimePickerDialogのインスタンスを生成
                TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                        (view, hourOfDay, minute) -> {
                            // 選択された時間を保存
                            this.selectedTimes.set(index, new int[]{hourOfDay, minute});

                            // 時間を表示
                            String time = String.format("%02d:%02d", hourOfDay, minute);
                            timePickerButton.setText(time);

//                            this.hourofday = hourOfDay;
//                            this.minute = minute;
                        },
                        this.selectedTimes.get(index)[0],
                        this.selectedTimes.get(index)[1],
                        true); // デフォルトで8時0分に設定
                timePickerDialog.show();
            });

            // タイムピッカーレイアウトに追加
            timePickerLayout.addView(timePickerView);
        }
    }

    // DatePickerDialogを表示し、選択した日付をEditTextにセットする
    private void showDatePickerDialog(EditText dateInput, long timestamp) {
        // 現在の日付を取得
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // DatePickerDialogを表示
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), (view, year1, month1, dayOfMonth) -> {
            // 選択された日付を "yyyy/MM/dd" の形式でEditTextにセット
            String selectedDate = year1 + "/" + (month1 + 1) + "/" + dayOfMonth;
            dateInput.setText(selectedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

    //通知の処理
    private void scheduleDailyNotifications(long medication_id, long startDate, long endDate) {
        Log.d("MedicationReminder", "scheduleDailyNotifications ");

        int daycnt = 1;
        int cnt = 1;
//        int i = 0; // デバッグ用カウント
        for (int[] time : selectedTimes) {
            int hourofday = time[0];
            int minute = time[1];

            Log.d("MedicationReminder", "hourofday: " + hourofday + " minute: " + minute);

            // 開始日時を指定した時間で設定
            Calendar currentDate = Calendar.getInstance();
            currentDate.setTimeInMillis(startDate); // タイムスタンプを日時に変換
            currentDate.set(Calendar.HOUR_OF_DAY, hourofday);
            currentDate.set(Calendar.MINUTE, minute);
            currentDate.set(Calendar.SECOND, 0);

            // 終了日時をカレンダーで設定
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTimeInMillis(endDate); // タイムスタンプを日時に変換


            while (!currentDate.after(endCalendar)) {

                //追加
                // 現在日時を取得
                Calendar now = Calendar.getInstance();

                // 現在日時よりも過去の場合はスキップ
                if (currentDate.before(now)) {
                    Log.d("MedicationReminder", "スキップ: 通知時間が現在の日時を過ぎています " + currentDate.getTime());
                    currentDate.add(Calendar.DATE, 1); // 次の日に進む
                    continue;
                }

                setDailyNotification(cnt, daycnt, medication_id, currentDate); // 通知を設定

                int year = currentDate.get(Calendar.YEAR);
                int month = currentDate.get(Calendar.MONTH) + 1; // 0始まりなので+1
                int day = currentDate.get(Calendar.DAY_OF_MONTH);
                Log.d("MedicationReminder", "設定された日付: " + year + "年" + month + "月" + day + "日");

                currentDate.add(Calendar.DATE, 1); // 次の日に進む
                Log.d("MedicationReminder", cnt*daycnt + "回目通知設定: "
                        + hourofday + ":" + minute);

                daycnt++;

            }
            cnt++;
        }
//        getActivity().getSupportFragmentManager().popBackStack();// 画面を閉じる

    }


    private void setDailyNotification(int cnt, int daycnt, long medication_id, Calendar notificationDate) {
        Log.d("MedicationReminder", "setDailyNotification " + notificationDate.getTimeInMillis());

        // AlarmManager を設定
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), MedicationReminderReceiver.class);
        int id = (int)medication_id*1000 + daycnt*10 + cnt;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getActivity(),
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationDate.getTimeInMillis(), pendingIntent);

            // ログに出力して設定値を確認
            Log.d("MedicationReminder", "id = " + id);
            Log.d("MedicationReminder", "setDailyNotification notificationDate.getTimeInMillis() = " +notificationDate.getTimeInMillis());
        }
    }

    private void saveRemindersToDatabase(long medicationId) {
        // 選択された時刻リストをリマインダーに変換
        Log.d("ReminderSave", "saveReminders medicationId = " + medicationId);

        List<Reminder> reminders = new ArrayList<>();
        for (int[] time : selectedTimes) {
            Reminder reminder = new Reminder();
            reminder.medicationId = (int)medicationId;
            reminder.HourOfDay = time[0];
            reminder.Minute = time[1];
            reminders.add(reminder);
        }

        new Thread(() -> {
            ReminderDao reminderDao = db.reminderDao();
            reminderDao.insertAll(reminders);
            Log.d("ReminderSave", "Reminders saved for medicationId: " + medicationId);
        }).start();
    }

    //通知ここまで


    //タイムスタンプに直してデータベースに登録
    private long convertDateToTimestamp(String dateStr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        Date date = dateFormat.parse(dateStr);
//        return date != null ? date.getTime() : 0;  // nullの場合は0を返す

        if (date == null) {
            return 0; // nullの場合は0を返す
        }

        // Calendarを使ってタイムスタンプを取得
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // パースしたDateをセット
        return calendar.getTimeInMillis(); // タイムスタンプを取得して返す
    }

    //タイムスタンプに直してデータベースに登録
    private long convertDateToTimestamp2(String dateStr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        Date date = dateFormat.parse(dateStr);
//        return date != null ? date.getTime() : 0;  // nullの場合は0を返す

        if (date == null) {
            return 0; // nullの場合は0を返す
        }

        // Calendarを使ってタイムスタンプを取得
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // パースしたDateをセット

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar.getTimeInMillis(); // タイムスタンプを取得して返す
    }

    // ↓ギャラリーを開くメソッド
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }

    // ギャラリーから画像を選択するランチャー
    ActivityResultLauncher<Intent> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    // ギャラリーから選んだ画像のURIを取得
                    Uri selectedImageUri = result.getData().getData();
                    // _imageUriに設定
                    _imageUri = selectedImageUri;
                    // 削除状態をリセット
                    isPhotoDeleted = false;

                    // ImageViewに選択した画像を表示
                    photoImageView.setImageURI(selectedImageUri);
                }
            });


    //　↓カメラ機能の処理
    // Cameraアクティビティを起動するためのランチャーオブジェクト
    ActivityResultLauncher<Intent> _cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) { // 修正済み
                            if (_imageUri != null) {
                                photoImageView.setImageURI(_imageUri);
                                Log.e("EditMedicationFragment", "Uri" + _imageUri);
                                isPhotoDeleted = false; // 削除状態をリセット
                                Toast.makeText(getActivity(), "画像を設定しました", Toast.LENGTH_SHORT).show();
                                // メディアスキャナーに通知
                                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                scanIntent.setData(_imageUri);
                                requireActivity().sendBroadcast(scanIntent);

                            } else {
                                Log.e("EditMedicationFragment", "Image URI is null");
                            }
                        }
                    }
            );

    //　↓カメラを起動するためのメソッド
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {

            // 日時データを「yyyyMMddHHmmss」の形式に整形するフォーマッタを生成
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            // 現在の日時を取得
            Date now = new Date(System.currentTimeMillis());
            // 取得した日時データを「yyyyMMddHHmmss」形式に整形した文字列を生成
            String nowStr = dateFormat.format(now);
            // ストレージに格納する画像のファイル名を生成。ファイルの一意を確保するために
            // タイムスタンプの値を利用
            String fileName = "CameraIntentSamplePhoto_" + nowStr + ".jpg";

            // ContentValuesオブジェクトを生成
            ContentValues values = new ContentValues();
            // 画像ファイル名を設定
            values.put(MediaStore.Images.Media.TITLE, fileName);
            // 画像ファイルの種類を設定
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            // ContentResolverオブジェクトを生成
            ContentResolver resolver = requireActivity().getContentResolver();
            // ContentResolverを使ってURIオブジェクトを生成
            _imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // Intentオブジェクトを生成
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Extra情報として、_imageUriを設定
            intent.putExtra(MediaStore.EXTRA_OUTPUT, _imageUri);
            // アクティビティを起動
            _cameraLauncher.launch(intent);
        }
    }
    // Cameraアクティビティから戻ってきたときの処理が記述されたコールバッククラス
    private class ActivityResultCallbackFromCamera implements
            ActivityResultCallback<ActivityResult> {

        @Override
        public void onActivityResult(ActivityResult result) {
            // カメラアプリで撮影成功の場合
            if (result.getResultCode() == requireActivity().RESULT_OK) {
                isPhotoDeleted = false;  // 削除状態をリセット
                // 画像を表示するImageViewを取得
                ImageView ivCamera = photoImageView;
                // フィールドの画像URIをImageViewに設定
                ivCamera.setImageURI(_imageUri);

                // メディアスキャナーに通知
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(_imageUri);
                requireActivity().sendBroadcast(scanIntent);

            }

            if (result.getResultCode() == requireActivity().RESULT_OK) {
                isPhotoDeleted = false;  // 削除状態をリセット
                // 画像を表示するImageViewを取得
                ImageView ivCamera = photoImageView;
                // フィールドの画像URIをImageViewに設定
                ivCamera.setImageURI(_imageUri);

                // メディアスキャナーに通知
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(_imageUri);
                requireActivity().sendBroadcast(scanIntent);
            }
        }
    }

    // カメラ画像タップ時にダイアログで拡大画像を表示する処理
    public void onImageTap(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(getActivity(), "画像が存在しません。", Toast.LENGTH_SHORT).show();
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.popup_image_view, null);

        // ダイアログ内のImageViewに画像を設定
        ImageView ivDialogPhoto = dialogView.findViewById(R.id.popup_image_view);
        ivDialogPhoto.setImageURI(imageUri);

        // AlertDialogを生成して表示
        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton("閉じる", null)
                .create()
                .show();
    }


    //  InputFilterを使って服用量の入力制限
    public class medicationDosageFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

            // 整数部1桁 (例: "2"など)
            if (newInput.matches("^\\d{0,1}")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }

    //  InputFilterを使って服用回数の入力制限
    public class medicationFrequencyFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

            // 整数部1桁 (例: "2"など)
            if (newInput.matches("^[1-5]$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            Toast.makeText(getActivity(), "1～5の値を入力してください", Toast.LENGTH_SHORT).show();
            return "";  // 無効な入力を制限
        }
    }
}




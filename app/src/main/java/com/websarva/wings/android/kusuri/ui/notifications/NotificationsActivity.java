package com.websarva.wings.android.kusuri.ui.notifications;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.MainActivity;
import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.MedicationWithRemainders;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.Reminder;
import com.websarva.wings.android.kusuri.ReminderDao;
import com.websarva.wings.android.kusuri.ui.home.HomeFragment;

import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.TimePickerDialog;
import android.widget.LinearLayout;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;

public class NotificationsActivity extends AppCompatActivity {

    private EditText medicineNameEdit;              //おくすり名
    private EditText doscountEdit;                  //服薬回数
    private LinearLayout timePickerLayout; // タイムピッカーを表示するレイアウト
    private ArrayList<TimePickerDialog> timePickers = new ArrayList<>(); // タイムピッカーのリスト
    private EditText  dosageEdit;                   //服用量
    private Spinner timingSpinner;                  //服薬タイミング
    private Spinner medicationDosageSpinner;        //錠・包
    private EditText medicationStartDateInput;      //服薬開始
    private EditText medicationEndDateInput;        //服薬終了
    private EditText memoEdit;                      //メモ
    private Spinner notificationSpinner;            //通知
    private Button pickerbutton, registerButton, cancelButton;    //タイムピッカー・登録・キャンセルボタン
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView cameraImageView, deleteImageView, photoImageView;     //カメラのイメージ　ギャラリー　削除　写真ボタンのイメージ
    private Bitmap capturedImageBitmap;
    private boolean isPhotoDeleted = true;         //写真が削除されたか

    //リマインダー機能
    private EditText startDateEditText, endDateEditText;    //服用開始・服用終了
    private TimePicker timePicker;                          //タイムピッカー
    private Calendar startDate, endDate, selectedTime;
    private int hourofday, minute;
    private List<int[]> selectedTimes;

    //カレンダーのプリファレンシス
    private SharedPreferences preferences;

    // Cameraアクティビティを起動するためのランチャーオブジェクト
    ActivityResultLauncher<Intent> _cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallbackFromCamera()
            );
    private Uri _imageUri;
    private ImageView selectImageView;
    private AppDatabase db;
    private MedicationDao medicationDao;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        initializeViews(); // 各ビューの初期化
        setupSpinners();   // Spinnerにアダプターを設定

        // 服用開始日入力欄をクリックしたときにDatePickerを表示
        medicationStartDateInput.setOnClickListener(v -> showDatePickerDialog(medicationStartDateInput));
        // 服用終了日入力欄をクリックしたときにDatePickerを表示
        medicationEndDateInput.setOnClickListener(v -> showDatePickerDialog(medicationEndDateInput));

        // 登録ボタンのクリックイベント
        registerButton.setOnClickListener(v -> {
            String medicineName = medicineNameEdit.getText().toString();                 //おくすり名
            String dosageCount = doscountEdit.getText().toString();                      //服用回数
            String dosage = dosageEdit.getText().toString();                             //服用量
            String dosage_jo_ho = medicationDosageSpinner.getSelectedItem().toString();  //錠・包
            String md_timing = timingSpinner.getSelectedItem().toString();              //服薬タイミング
            String startDateLong = medicationStartDateInput.getText().toString();       //服薬開始日
            String endDateLong = medicationEndDateInput.getText().toString();           //服薬終了日
            String memo = memoEdit.getText().toString();
            String notification = notificationSpinner.getSelectedItem().toString();

            if (medicineName.isEmpty()) {
                Toast.makeText(this, "おくすり名を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dosageCount.isEmpty()) {
                Toast.makeText(this, "服用回数を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dosage.isEmpty()) {
                Toast.makeText(this, "服用量を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDateLong.isEmpty() ) {
                Toast.makeText(this, "服用開始日を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (endDateLong.isEmpty()) {
                Toast.makeText(this, "服用終了日を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            //終了日が開始日以前に設定されている場合、トーストを表示する
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            try {
                // 開始日と終了日をDate型に変換
                Date startDate = dateFormat.parse(startDateLong);
                Date endDate = dateFormat.parse(endDateLong);

                // 日付の比較
                if (startDate != null && endDate != null && endDate.before(startDate)) {
                    Toast.makeText(this, "終了日は開始日以降の日付を設定してください", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                // 日付のパースに失敗した場合（通常はここには到達しない）
                Toast.makeText(this, "日付形式が正しくありません。", Toast.LENGTH_SHORT).show();
                return;
            }


            // タイムピッカーが設定されているかチェック
            if (this.selectedTimes == null || this.selectedTimes.isEmpty()) {
                Toast.makeText(this, "時間を設定してください", Toast.LENGTH_SHORT).show();
                return;
            }


            try {

                // 日付をタイムスタンプ(long)に変換する
                long StartDateLong = convertDateToTimestamp(startDateLong);
                long EndDateLong = convertDateToTimestamp2(endDateLong);

                // Medication オブジェクトを作成して保存
                Medication medication = new Medication();
                medication.name = medicineName;
                medication.dosage = Integer.parseInt(dosage);
                medication.dosageSpinner = dosage_jo_ho;
                medication.frequency = Integer.parseInt(dosageCount);
                medication.timing = md_timing;
                medication.startdate = StartDateLong;
                medication.enddate = EndDateLong;
                medication.memo = memo;
                medication.reminder = notification;  // リマインダー設定

                removeBlueDotForDateRange(medication);

                // 写真のURIも登録
                if (_imageUri != null) {
                    medication.imageUri = _imageUri.toString(); // URIを文字列として保存
                }

                // データベースに薬情報を挿入（バックグラウンドスレッドで処理）
                new Thread(() -> {
                    Log.d("ReminderSave", "medicationDao.insertMedication(medication);");
                    long medication_id = medicationDao.insertMedication(medication);

                    // 有効なIDが生成された場合にReminderを保存
                    if (medication_id > 0) {
                        saveReminders(medication_id);
                    } else {
                        Log.e("DatabaseError", "Failed to retrieve valid Medication ID");
                    }

                    // 通知が「あり」の場合に通知の設定をする
                    if (notification.equals("あり")){
                        scheduleDailyNotifications(medication_id, StartDateLong, EndDateLong);
                    }

                }).start();
            } catch (NumberFormatException e) {
                if (this != null) {
                    Toast.makeText(this, "全ての項目に値を入力してください。", Toast.LENGTH_LONG).show();
                }
            } catch (ParseException e){
                if (this != null) {
                    Toast.makeText(this, "日付の形式が正しくありません。", Toast.LENGTH_LONG).show();
                }
            }

            Toast.makeText(NotificationsActivity.this, "おくすりを登録しました", Toast.LENGTH_LONG).show();

            finish();  // Activityを閉じる
        });


        //タイムピッカーを設定
        doscountEdit = findViewById(R.id.doscount_input);
        timePickerLayout = findViewById(R.id.time_picker_layout);
        pickerbutton = findViewById(R.id.picker_button);

        pickerbutton.setOnClickListener(v -> {
            try {
                int doseCount = Integer.parseInt(doscountEdit.getText().toString());
                if (doseCount > 0) {
                    showTimePickers(doseCount);
                } else {
                    Toast.makeText(NotificationsActivity.this, "回数を正しく入力してください", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(NotificationsActivity.this, "回数を正しく入力してください", Toast.LENGTH_SHORT).show();
            }
        });


        //通知機能
        startDateEditText = findViewById(R.id.medication_startdate);
        endDateEditText = findViewById(R.id.medication_enddate);
        registerButton = findViewById(R.id.register_button);
        // 開始日と終了日用の DatePicker 設定
        startDateEditText.setOnClickListener(v -> showDatePickerDialog(true));
        endDateEditText.setOnClickListener(v -> showDatePickerDialog(false));

        //写真撮影ボタンの処理
        cameraImageView.setOnClickListener(v -> dispatchTakePictureIntent());
        //写真追加ボタンの処理（ギャラリー）
        selectImageView = findViewById(R.id.select_image_view);
        selectImageView.setOnClickListener(v -> openGallery());
        // 写真を削除ボタンの処理
        deleteImageView.setOnClickListener(v -> {
            //追加
            if (_imageUri == null){
                Toast.makeText(NotificationsActivity.this, "写真はありません", Toast.LENGTH_SHORT).show();  //追加
            }else {

                new AlertDialog.Builder(NotificationsActivity.this)
                        .setTitle("確認")
                        .setMessage("本当に削除しますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            photoImageView.setImageDrawable(null);
                            isPhotoDeleted = true;
                            Toast.makeText(NotificationsActivity.this, "写真が削除されました", Toast.LENGTH_SHORT).show();    //追加
                        })
                        .setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        initializeViews();

        photoImageView.setOnClickListener(v -> {
            Log.d("PhotoDebug", "PhotoImageView clicked"); // ログ追加
            if (_imageUri != null && !isPhotoDeleted) {
                Log.d("PhotoDebug", "Image exists and not deleted"); // ログ追加
                onImageTap(_imageUri); // ポップアップ表示
            } else if (isPhotoDeleted) {
                Log.d("PhotoDebug", "Image has been deleted"); // ログ追加
                Toast.makeText(NotificationsActivity.this, "写真はありません", Toast.LENGTH_SHORT).show();  //追加
            }
        });

        // キャンセルボタンのクリックイベント
        cancelButton.setOnClickListener(v -> {
            finish();  // 画面を閉じる
            Toast.makeText(this, "キャンセルしました", Toast.LENGTH_SHORT).show();
        });

        db = AppDatabase.getDatabase(this);
        medicationDao = db.medicationDao();

        preferences = getSharedPreferences("calendar_prefs", 0);

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

            // 該当するdateKeyの"_blue_dot"キーを削除
            editor.remove(dateKey + "_blue_dot");

            // カレンダーを1日進める
            startCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }



        // 変更を保存
        editor.apply();
    }

    private String getDateKeyFromCalendar(Calendar calendar) {
        // 日付を"yyyy-MM-dd"形式で取得
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }



    // 入力された服薬回数分だけタイムピッカーを表示する
    private void showTimePickers(int doseCount) {
        // タイムピッカーのリストをリセット
        timePickerLayout.removeAllViews();
        timePickers.clear();

        // 各タイムピッカーの時間を保存するリスト
        this.selectedTimes = new ArrayList<>();
        for (int i = 0; i < doseCount; i++) {
            // 初期値としてデフォルトの時間（8:00）を設定
            this.selectedTimes.add(new int[]{8, 0});
        }

        for (int i = 0; i < doseCount; i++) {
            final int index = i;
            // タイムピッカーのレイアウトを作成
            View timePickerView = getLayoutInflater().inflate(R.layout.time_picker_item, timePickerLayout, false);

            TextView timeLabel = timePickerView.findViewById(R.id.time_label);
            timeLabel.setText("服薬 " + (index + 1) + " 回目");

            Button timePickerButton = timePickerView.findViewById(R.id.time_picker_button);

            timePickerButton.setOnClickListener(v -> {
                // TimePickerDialogのインスタンスを生成
                TimePickerDialog timePickerDialog = new TimePickerDialog(NotificationsActivity.this,
                        (view, hourOfDay, minute) -> {
                            // 選択された時間を保存
                            this.selectedTimes.set(index, new int[]{hourOfDay, minute});
                            // 時間を表示
                            String time = String.format("%02d:%02d", hourOfDay, minute);
                            timePickerButton.setText(time);

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


    //通知機能
    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    if (isStartDate) {
                        startDate = Calendar.getInstance();
                        startDate.set(year, month, dayOfMonth);
                        startDateEditText.setText(year + "/" + (month + 1) + "/" + dayOfMonth);
                    } else {
                        endDate = Calendar.getInstance();
                        endDate.set(year, month, dayOfMonth);
                        endDateEditText.setText(year + "/" + (month + 1) + "/" + dayOfMonth);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }


    private void scheduleDailyNotifications(long medication_id, long startDate, long endDate) {
        Log.d("MedicationReminder", "scheduleDailyNotifications");

        int daycnt = 1;
        int cnt = 1;
        int i = 0; // デバッグ用カウント
        for (int[] time : selectedTimes) {
            int hourofday = time[0];
            int minute = time[1];

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

        finish();
    }

    private void setDailyNotification(int cnt, int daycnt, long medication_id, Calendar notificationDate) {
        Log.d("MedicationReminder", "setDailyNotification " + notificationDate.getTimeInMillis());

        // AlarmManager を設定
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MedicationReminderReceiver.class);
        int id = (int)medication_id*1000 + daycnt*10 + cnt;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
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

    private void saveReminders(long medicationId) {
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
    //通知機能ここまで


    //カメラを起動するためのメソッド
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

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
            ContentResolver resolver = getContentResolver();
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


    //写真を取るボタンの処理
    // Cameraアクティビティから戻ってきたときの処理が記述されたコールバッククラス
    private class ActivityResultCallbackFromCamera implements
            ActivityResultCallback<ActivityResult> {

        @Override
        public void onActivityResult(ActivityResult result) {
            // カメラアプリで撮影成功の場合
            if (result.getResultCode() == RESULT_OK) {
                isPhotoDeleted = false;  // 削除状態をリセット
                // 画像を表示するImageViewを取得
                ImageView ivCamera = findViewById(R.id.photo_image_view);
                // フィールドの画像URIをImageViewに設定
                ivCamera.setImageURI(_imageUri);

                // メディアスキャナーに通知
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(_imageUri);
                sendBroadcast(scanIntent);
            }
        }
    }

    // カメラ画像タップ時にダイアログで拡大画像を表示する処理
    public void onImageTap(Uri view) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.popup_image_view, null);
        // ダイアログ内のImageViewに画像を設定
        ImageView ivDialogPhoto = dialogView.findViewById(R.id.popup_image_view);
        ivDialogPhoto.setImageURI(_imageUri);

        // AlertDialogを生成して表示
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("閉じる", null)
                .create()
                .show();
    }


    //写真を選択ボタンの処理
    // ギャラリーを開くメソッド
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }

    // ギャラリーから画像を選択するランチャー
    ActivityResultLauncher<Intent> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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

    // DatePickerDialogを表示し、選択した日付をEditTextにセットする
    private void showDatePickerDialog(EditText dateInput) {
        // 現在の日付を取得
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // DatePickerDialogを表示
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            // 選択された日付を "yyyy/MM/dd" の形式でEditTextにセット
            String selectedDate = year1 + "/" + (month1 + 1) + "/" + dayOfMonth;
            dateInput.setText(selectedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

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

    //おくすり登録画面から画面部品の取得
    private void initializeViews() {
        medicineNameEdit = findViewById(R.id.medicine_name_edit);               // 薬の名前
        doscountEdit = findViewById(R.id.doscount_input);                       // 服用回数
        dosageEdit = findViewById(R.id.dosage_input);                           // 服用量
        medicationDosageSpinner = findViewById(R.id.dosage_spinner);            // 錠・包
        timingSpinner = findViewById(R.id.MDtiming_spinner);                 //服薬タイミング
        medicationStartDateInput = findViewById(R.id.medication_startdate);     //服薬開始日
        medicationEndDateInput = findViewById(R.id.medication_enddate);         //服薬終了日
        memoEdit = findViewById(R.id.memo_edit);                                // メモ
        notificationSpinner = findViewById(R.id.notification_spinner);          // 通知
        registerButton = findViewById(R.id.register_button);                    //登録ボタン
        cancelButton = findViewById(R.id.cancel_button);                        //キャンセルボタン

        // ここに cameraImageView を追加して初期化
        cameraImageView = findViewById(R.id.camera_image_view);
        photoImageView = findViewById(R.id.photo_image_view);
        deleteImageView = findViewById(R.id.delete_image_view);

        //各項目の入力制限
        dosageEdit.setFilters(new InputFilter[]{ new medicationDosageFilter() });
        doscountEdit.setFilters(new InputFilter[]{ new medicationFrequencyFilter() });
    }

    //錠・包と、リマインダーのドロップダウンリストの画面部品を取得
    private void setupSpinners() {
        setupSpinner(medicationDosageSpinner, R.array.dosage_options);
        setupSpinner(notificationSpinner, R.array.notification_options);
    }

    private void setupSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
            Toast.makeText(NotificationsActivity.this, "1～5の値を入力してください", Toast.LENGTH_SHORT).show();
            return "";  // 無効な入力を制限
        }
    }
}
package com.websarva.wings.android.kusuri.ui.notifications;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.MainActivity;
import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.ui.home.HomeFragment;

import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
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
    private EditText  dosageEdit;                   //服用量の入力
    private Spinner medicationDosageSpinner;        //錠・包
    private EditText doscountEdit;                  //服薬回数
    private LinearLayout timePickerLayout; // タイムピッカーを表示するレイアウト
    private ArrayList<TimePickerDialog> timePickers = new ArrayList<>(); // タイムピッカーのリスト
    private EditText medicationStartDateInput;      //服薬開始
    private EditText medicationEndDateInput;        //服薬終了
    private EditText memoEdit;                      //メモ
    private Spinner notificationSpinner;            //通知
    private Button pickerbutton, registerButton, cancelButton;    //タイムピッカー・登録・キャンセルボタン
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView cameraImageView, photoImageView, deleteImageView;     //カメラのイメージ　写真　削除ボタンのイメージ
    private Bitmap capturedImageBitmap;
    private boolean isPhotoDeleted = false;         //写真が削除されたか
    private Uri photoUri;

    //リマインダー機能
    private EditText startDateEditText, endDateEditText;    //服用開始・服用終了
    private TimePicker timePicker;                          //タイムピッカー
    private Calendar startDate, endDate, selectedTime;
    private int hourofday, minute;

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
            String dosage = dosageEdit.getText().toString();                             //服用量
            String dosage_jo_ho = medicationDosageSpinner.getSelectedItem().toString();  //錠・包
            String dosageCount = doscountEdit.getText().toString();                      //服用回数
            String startDateLong = medicationStartDateInput.getText().toString();       //服薬開始日
            String endDateLong = medicationEndDateInput.getText().toString();           //服薬終了日
            String memo = memoEdit.getText().toString();
            String notification = notificationSpinner.getSelectedItem().toString();

            if (medicineName.isEmpty()) {
                Toast.makeText(this, "おくすり名を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            try {

                // 日付をタイムスタンプ(long)に変換する
                long StartDateLong = convertDateToTimestamp(startDateLong);
                long EndDateLong = convertDateToTimestamp(endDateLong);

                // Medication オブジェクトを作成して保存
                Medication medication = new Medication();
                medication.name = medicineName;
                medication.dosage = Integer.parseInt(dosage);
                medication.dosageSpinner = dosage_jo_ho;
                medication.frequency = Integer.parseInt(dosageCount);
                medication.startdate = StartDateLong;
                medication.enddate = EndDateLong;
                medication.memo = memo;
                medication.reminder = notification;  // リマインダー設定



                // データベースに薬情報を挿入（バックグラウンドスレッドで処理）
                new Thread(() -> {
                    medicationDao.insertMedication(medication);
//            runOnUiThread(this::displayMedications);  // メインスレッドでリストを更新

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
//        timePicker = findViewById(R.id.time_picker_button); // TimePicker のインスタンスを取得
        registerButton = findViewById(R.id.register_button);

        // 開始日と終了日用の DatePicker 設定
        startDateEditText.setOnClickListener(v -> showDatePickerDialog(true));
        endDateEditText.setOnClickListener(v -> showDatePickerDialog(false));

//        registerButton.setOnClickListener(v -> scheduleDailyNotifications());

        //カメラ機能
        photoImageView = findViewById(R.id.photo_image_view);
        deleteImageView = findViewById(R.id.delete_image_view);

        // 画像削除ボタンの処理
        deleteImageView.setOnClickListener(v -> {
            new AlertDialog.Builder(NotificationsActivity.this)
                    .setTitle("確認")
                    .setMessage("本当に削除しますか？")
                    .setPositiveButton("OK", (dialog, which) -> {
                        photoImageView.setImageDrawable(null);
                        isPhotoDeleted = true;
                        Toast.makeText(NotificationsActivity.this, "画像が削除されました", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        initializeViews();

        cameraImageView.setOnClickListener(v -> dispatchTakePictureIntent());

        photoImageView.setOnClickListener(v -> {
            if (capturedImageBitmap != null && !isPhotoDeleted) {
                // 画像があればポップアップで表示
                showImagePopup(capturedImageBitmap);
            } else if (isPhotoDeleted) {
                Toast.makeText(NotificationsActivity.this, "画像は削除されました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(NotificationsActivity.this, "画像がまだ撮影されていません", Toast.LENGTH_SHORT).show();
            }
        });


        // キャンセルボタンのクリックイベント
        cancelButton.setOnClickListener(v -> {
            finish();  // 画面を閉じる
            Toast.makeText(this, "キャンセルしました", Toast.LENGTH_SHORT).show();
        });

        db = AppDatabase.getDatabase(this);
        medicationDao = db.medicationDao();

    }

    // 入力された服薬回数分だけタイムピッカーを表示する
    private void showTimePickers(int doseCount) {
        // タイムピッカーのリストをリセット
        timePickerLayout.removeAllViews();
        timePickers.clear();

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
                            // 時間を表示
                            String time = String.format("%02d:%02d", hourOfDay, minute);
                            timePickerButton.setText(time);

                            this.hourofday = hourOfDay;
                            this.minute = minute;
                        }, 8, 0, true); // デフォルトで8時0分に設定
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

    private void scheduleDailyNotifications() {
        Log.d("MedicationReminder", "scheduleDailyNotifications");
        // TimePicker から時刻を取得
        selectedTime = Calendar.getInstance();


//        selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
//        selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
        selectedTime.set(Calendar.HOUR_OF_DAY, hourofday);
        selectedTime.set(Calendar.MINUTE, minute);
        selectedTime.set(Calendar.SECOND, 0);



        // 開始日から終了日まで毎日の通知をスケジュール
//        Calendar currentDate = (Calendar) startDate.clone();
        Calendar currentDate = (Calendar) selectedTime.clone();


//        Log.d("MedicationReminder", "scheduleDailyNotifications currentDate =" + currentDate + " endDate = " + endDate);

        int i = 1;
//        while (!currentDate.after(endDate)) {
//            setDailyNotification(currentDate);
//            currentDate.add(Calendar.DATE, 1);
//            Log.d("MedicationReminder",  (i++) + "回目");
//        }

//        setDailyNotification(currentDate);
        setDailyNotification(selectedTime);
        Log.d("MedicationReminder",  (i++) + "回目 " + hourofday + ":" + minute);

        finish();
    }

    private void setDailyNotification(Calendar notificationDate) {
        Log.d("MedicationReminder", "setDailyNotification " + notificationDate.getTimeInMillis());
        // 通知時間を設定
//        notificationDate.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
//        notificationDate.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
//        notificationDate.set(Calendar.SECOND, 0);

        // AlarmManager を設定
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MedicationReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) notificationDate.getTimeInMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationDate.getTimeInMillis(), pendingIntent);

            // ログに出力して設定値を確認
            Log.d("MedicationReminder", "setDailyNotification notificationDate.getTimeInMillis() = " +notificationDate.getTimeInMillis());
        }
    }
    //通知機能ここまで


    //カメラを起動するためのメソッド
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // ファイルを作成し、そのURIを取得する
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.websarva.wings.android.kusuri.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    //撮影した画像をアプリケーションの外部ストレージに保存j
    private File createImageFile() {
        // 現在の日付と時刻で一意のファイル名を生成
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    //撮影後の結果を取得するメソッド
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 高解像度の画像を設定したURIから取得してImageViewに表示
            photoImageView.setImageURI(photoUri);

            // capturedImageBitmapに画像を設定
            try {
                capturedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 画像が削除されていない状態にする
            isPhotoDeleted = false;
        }
    }


    // ポップアップを表示
    private void showImagePopup(Bitmap image) {
        if (image == null) {
            Toast.makeText(this, "画像が無効です", Toast.LENGTH_SHORT).show();
            return;
        }

        // レイアウトのインフレート
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_image_view, null);
        ImageView popupImageView = popupView.findViewById(R.id.popup_image_view);

        // 画像をセット
        popupImageView.setImageBitmap(image);

        // ポップアップの設定
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView)
                .setPositiveButton("閉じる", null) // 閉じるボタン
                .show();
    }

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
        return date != null ? date.getTime() : 0;  // nullの場合は0を返す
    }

    //おくすり登録画面から画面部品の取得
    private void initializeViews() {
        medicineNameEdit = findViewById(R.id.medicine_name_edit);               // 薬の名前
        dosageEdit = findViewById(R.id.dosage_input);                           // 服用量
        medicationDosageSpinner = findViewById(R.id.dosage_spinner);            // 錠・包
        doscountEdit = findViewById(R.id.doscount_input);                       // 服用回数
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
            if (newInput.matches("[1-5]")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }
}
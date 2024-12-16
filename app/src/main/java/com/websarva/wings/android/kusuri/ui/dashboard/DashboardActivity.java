package com.websarva.wings.android.kusuri.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.HealthCare;
import com.websarva.wings.android.kusuri.HealthCareDao;
import com.websarva.wings.android.kusuri.Medication;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.ui.notifications.NotificationsActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView dateTextView;

    private EditText tempEditText;           //体温
    private EditText weightEditText;        //体重
    private EditText bpUpEditText;           //血圧（上）
    private EditText bpDownEditText;        //血圧（下）
    private Spinner hcTimingSpinner;        //食前・食後
    private EditText sugarEditText;          //血糖値

    private Button registerButton, deleteButton;    //登録・キャンセルボタン
    private ScrollView scrollView;
    private String currentDate;

    private AppDatabase db;
    private HealthCareDao healthCareDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        HCinitializeViews(); // 各ビューの初期化

        scrollView = findViewById(R.id.scrollView);
        // 本日の日付表示
        dateTextView = findViewById(R.id.dateTextView);
        currentDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        dateTextView.setText("本日 : " + currentDate);

        // 登録ボタンのクリックリスナー
        registerButton.setOnClickListener(v -> {
            String temp = tempEditText.getText().toString().trim();
            String weight = weightEditText.getText().toString().trim();
            String bpUp = bpUpEditText.getText().toString().trim();
            String bpDown = bpDownEditText.getText().toString().trim();
            String hcTiming = hcTimingSpinner.getSelectedItem().toString();
            String sugar = sugarEditText.getText().toString().trim();

            // 入力チェック：すべて空ならエラーメッセージを表示
            if (temp.isEmpty() && bpUp.isEmpty() && bpDown.isEmpty() && weight.isEmpty() && sugar.isEmpty()) {
                Toast.makeText(DashboardActivity.this, "登録する情報を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            // 血圧の整合性チェック
            if (!bpUp.isEmpty() && !bpDown.isEmpty()) {
                int bpUpValue = Integer.parseInt(bpUp);
                int bpDownValue = Integer.parseInt(bpDown);
                if (bpUpValue < bpDownValue) {
                    Toast.makeText(DashboardActivity.this, "血圧の上の値は下の値以上である必要があります", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            try {
            // キャストしてHealthcare オブジェクトを作成し保存
                HealthCare healthCare = new HealthCare();
                if (!temp.isEmpty()) {
                    healthCare.temperature = Double.parseDouble(temp);

                }
                if (!weight.isEmpty()) {
                    healthCare.weight = Double.parseDouble(weight);
                }

                if (!bpUp.isEmpty() && !bpDown.isEmpty()) {
                    healthCare.pressureUp = Integer.parseInt(bpUp);
                }

                if (!bpDown.isEmpty()) {
                    healthCare.pressureDown = Integer.parseInt(bpDown);
                }

                healthCare.hc_timing = hcTiming;

                if (!sugar.isEmpty()) {
                    healthCare.sugar = Integer.parseInt(sugar);
                }

            // データベースに健康情報を挿入（バックグラウンドスレッドで処理）
            new Thread(() -> {
                healthCareDao.insertHealthCare(healthCare);
//            runOnUiThread(this::displayHealthCare);  // メインスレッドでリストを更新

            }).start();
        } catch (NumberFormatException e) {
            if (this != null) {
                Toast.makeText(this, "全ての項目に値を入力してください。", Toast.LENGTH_LONG).show();
            }
        }
            Toast.makeText(DashboardActivity.this, "登録しました", Toast.LENGTH_SHORT).show();
            finish();  // Activityを閉じる
    });

        // キャンセルボタンのクリックリスナー
        deleteButton.setOnClickListener(v -> {
            finish();
            Toast.makeText(this, "キャンセルしました", Toast.LENGTH_SHORT).show();
        });

        db = AppDatabase.getDatabase(this);
        healthCareDao = db.healthCareDao();


        // キーボード表示時のスクロール設定
        View rootView = findViewById(R.id.scrollView);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
            if (heightDiff > 100) { // キーボードが表示されている場合
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {
                    scrollView.post(() -> scrollView.smoothScrollTo(0, focusedView.getBottom()));
                }
            }
        });


    }

    //服薬登録画面から画面部品の取得
    private void HCinitializeViews() {
        tempEditText = findViewById(R.id.tempEditText);                       //体温
        weightEditText = findViewById(R.id.weightEditText);                   //体重
        bpUpEditText = findViewById(R.id.bpUpEditText);                       //血圧(上)
        bpDownEditText = findViewById(R.id.bpDownEditText);                   //血圧（下）
        hcTimingSpinner = findViewById(R.id.HCtiming_spinner_ud);                //タイミング
        sugarEditText = findViewById(R.id.sugarEditText);                     //血糖値
        registerButton = findViewById(R.id.registerButton);                   //登録ボタン
        deleteButton = findViewById(R.id.deleteButton);                       //キャンセルボタン

        //各項目の入力制限
        tempEditText.setFilters(new InputFilter[]{ new healthcareTemp()});
        bpUpEditText.setFilters(new InputFilter[]{ new  healthcareBpUp()});
        bpDownEditText.setFilters(new InputFilter[]{ new healthcareBpDown()});
        weightEditText.setFilters(new InputFilter[]{ new healthcareWeight()});
        sugarEditText.setFilters(new InputFilter[]{ new healthcareSuger()});

    }

    //  InputFilterを使って体温の入力制限
    public class healthcareTemp implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());


            // 整数部3桁・小数部1桁のパターン (例: "123.4"、"99.9"など)
            if (newInput.matches("^\\d{0,3}(\\.\\d?)?$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }

    //  InputFilterを使って血圧（上）の入力制限
    public class healthcareBpUp implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());


            // 整数部3桁・小数部1桁のパターン (例: "123.4"、"99.9"など)
            if (newInput.matches("^\\d{0,3}(\\.\\d?)?$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }

    //  InputFilterを使って血圧（下）の入力制限
    public class healthcareBpDown implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

            // 整数部3桁のパターン (例: "123")
            if (newInput.matches("^\\d{0,3}(\\\\d?)?$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }

    //  InputFilterを使って体重の入力制限
    public class healthcareWeight implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

            // 整数部3桁・小数部1桁のパターン (例: "123.4"、"99.9"など)
            if (newInput.matches("^\\d{0,3}(\\.\\d?)?$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }

    //  InputFilterを使って血糖値の入力制限
    public class healthcareSuger implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String newInput = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

            // 整数部3桁のパターン (例: "123")
            if (newInput.matches("^\\d{0,3}(\\\\d?)?$")) {
                return null;  // 入力が有効な場合は変更なし
            }
            return "";  // 無効な入力を制限
        }
    }
}

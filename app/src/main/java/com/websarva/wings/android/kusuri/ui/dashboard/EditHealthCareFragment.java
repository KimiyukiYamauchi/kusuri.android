package com.websarva.wings.android.kusuri.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.HealthCare;
import com.websarva.wings.android.kusuri.HealthCareDao;
import com.websarva.wings.android.kusuri.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditHealthCareFragment extends Fragment {

    private TextView dateTextView;           //当日日時
    private TextView beforeDateTextView;           //前回登録日時
    private EditText tempEditText;           //体温
    private EditText weightEditText;        //体重
    private EditText bpUpEditText;           //血圧（上）
    private EditText bpDownEditText;        //血圧（下）
    private Spinner hcTimingSpinner;        //食前・食後
    private EditText sugarEditText;          //血糖値

    private HealthCareDao healthCareDao;
    private HealthCare currentHealthCare;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_healthcare, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        healthCareDao = AppDatabase.getDatabase(requireContext()).healthCareDao();
        dateTextView = view.findViewById(R.id.dateTextView_ud);
        beforeDateTextView = view.findViewById(R.id.beforeDateTextView_ud);
        tempEditText = view.findViewById(R.id.tempEditText_ud);
        weightEditText = view.findViewById(R.id.weightEditText_ud);
        bpUpEditText = view.findViewById(R.id.bpUpEditText_ud);
        bpDownEditText = view.findViewById(R.id.bpDownEditText_ud);
        hcTimingSpinner = view.findViewById(R.id.HCtiming_spinner_ud);
        sugarEditText = view.findViewById(R.id.sugarEditText_ud);
        Button saveButton = view.findViewById(R.id.registerButton_ud);
        Button cancelButton = view.findViewById(R.id.deleteButton_ud);

        int healthCareId = getArguments().getInt("healthCareId", -1);
        // IDからHealthCareを取得し、各フィールドに設定


        new Thread(() -> {
            currentHealthCare = healthCareDao.getHealthCareById(healthCareId);
            if (currentHealthCare != null) {
                getActivity().runOnUiThread(() -> {
                    // 本日の日付表示
                    String currentDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
                    dateTextView.setText("本日 : " + currentDate);

                    //　前回登録した日時
                    long timestamp = currentHealthCare.createdAt;
                    Date date = new Date(timestamp);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    String beforeDate = dateFormat.format(date);
                    beforeDateTextView.setText("登録日: " + beforeDate);

                    tempEditText.setText(String.valueOf(currentHealthCare.temperature));
                    weightEditText.setText(String.valueOf(currentHealthCare.weight));
                    bpUpEditText.setText(String.valueOf(currentHealthCare.pressureUp));
                    bpDownEditText.setText(String.valueOf(currentHealthCare.pressureDown));
                    //　Spinnerの項目から一致するインデックスを探して設定（食前・食後）
                    String dosageSpinner = currentHealthCare.hc_timing;
                    for (int i = 0; i < hcTimingSpinner.getCount(); i++) {
                        if (hcTimingSpinner.getItemAtPosition(i).toString().equals(dosageSpinner)) {
                            hcTimingSpinner.setSelection(i);
                            break;
                        }
                    }
                    sugarEditText.setText(String.valueOf(currentHealthCare.sugar));
                });
            }
        }).start();

        // 保存ボタンでデータを更新
        saveButton.setOnClickListener(v -> {
            Log.d("EditHealthCareFragment", "EditHealthCareFragment saveButton.setOnClickListener");
//            double temperature = Double.parseDouble(tempEditText.getText().toString());
//            double weight = Double.parseDouble(weightEditText.getText().toString());
//            int presserUp = Integer.parseInt(bpUpEditText.getText().toString());
//            int presserDown = Integer.parseInt(bpDownEditText.getText().toString());
//            String hcTiming = hcTimingSpinner.getSelectedItem().toString();
//            int suger = Integer.parseInt(sugarEditText.getText().toString());

            String tempText = tempEditText.getText().toString();
            double temperature = tempText.isEmpty() ? 0.0 : Double.parseDouble(tempText);

            String weightText = weightEditText.getText().toString();
            double weight = weightText.isEmpty() ? 0.0 : Double.parseDouble(weightText);

            String bpUpText = bpUpEditText.getText().toString();
            int presserUp = bpUpText.isEmpty() ? 0 : Integer.parseInt(bpUpText);

            String bpDownText = bpDownEditText.getText().toString();
            int presserDown = bpDownText.isEmpty() ? 0 : Integer.parseInt(bpDownText);

            String sugarText = sugarEditText.getText().toString();
            int sugar = sugarText.isEmpty() ? 0 : Integer.parseInt(sugarText);

            String hcTiming = hcTimingSpinner.getSelectedItem().toString();

            // 血圧の整合性チェック

                if (presserUp < presserDown) {
                    Toast.makeText(getActivity(), "血圧の上の値は下の値以上である必要があります", Toast.LENGTH_LONG).show();
                    return;
                }

            new Thread(() -> {
                Log.d("EditHealthCareFragment", "Thread");
//                currentHealthCare.createdAt =  System.currentTimeMillis();  //登録した日の日時を再設定
                currentHealthCare.temperature = temperature;
                currentHealthCare.weight = weight;
                currentHealthCare.pressureUp = presserUp;
                currentHealthCare.pressureDown = presserDown;
                currentHealthCare.hc_timing = hcTiming;
                currentHealthCare.sugar = sugar;
                healthCareDao.updateHealthCare(currentHealthCare);

                getActivity().runOnUiThread(() -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                    Toast.makeText(getActivity(), "健康状態を更新しました", Toast.LENGTH_LONG).show();
                });
            }).start();
        });

        // キャンセルボタンのクリックイベント
        cancelButton.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().popBackStack();// 画面を閉じる
            Toast.makeText(getActivity(), "キャンセルしました", Toast.LENGTH_SHORT).show();
        });
    }

}

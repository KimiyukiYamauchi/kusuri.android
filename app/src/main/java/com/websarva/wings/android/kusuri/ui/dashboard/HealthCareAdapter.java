package com.websarva.wings.android.kusuri.ui.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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

import com.websarva.wings.android.kusuri.HealthCare;
import com.websarva.wings.android.kusuri.HealthCareDao;
import com.websarva.wings.android.kusuri.R;

import java.util.List;

public class HealthCareAdapter extends RecyclerView.Adapter<HealthCareAdapter.HealthcareViewHolder> {
    private List<HealthCare> _healthCareList;
    private final HealthCareDao healthCareDao;
    private final Context context;
    //コンストラクタ
    public HealthCareAdapter(List<HealthCare> healthCareList,HealthCareDao healthCareDao, Context context) {
        //引数のリストデータをフィールドに格納
        this._healthCareList = healthCareList;
        this.healthCareDao = healthCareDao;
        this.context = context;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView1;
        TextView textView2;

        public ViewHolder(View itemView) {
            super(itemView);
            textView1 = itemView.findViewById(R.id.medication_name);
            textView2 = itemView.findViewById(R.id.medication_date);
        }
    }


    @NonNull
    @Override
    public HealthcareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //レイアウトインフレーターの取得
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_healthcare_list, parent, false);  // レイアウト名が正しいか確認

        //生成したビューホルダをリターンで返す
        return new HealthcareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HealthcareViewHolder holder, int position) {
        HealthCare healthCare = _healthCareList.get(position);
        holder.healthCareId.setText(healthCare.getFormattedCreationDate());
        holder.healthCareDate.setText(healthCare.getFormattedCreationTime());

        // 行の位置によって背景色を交互に設定
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));  // 偶数行: 白
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F1EFF8"));  // 奇数行: 薄い紫
        }

        // 削除ボタンのクリックリスナー
        holder.deleteButton.setOnClickListener(
                v -> showDeleteConfirmationDialog(healthCare, position)
        );


        // 編集ボタンのクリックリスナー
        holder.editButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("healthCareId", healthCare.id);// IDを渡す
            Navigation.findNavController(v)
                    .navigate(R.id.action_healthCareFragment_to_editHealthCareFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return _healthCareList.size();
    }
    private void showDeleteConfirmationDialog(HealthCare healthCare, int position) {
        new AlertDialog.Builder(context)
                .setTitle("削除確認")
                .setMessage("このデータを削除してもよろしいですか？")
                .setPositiveButton("OK", (dialog, which) -> deleteHealthCare(healthCare, position))
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void deleteHealthCare(HealthCare healthCare, int position) {
        new Thread(() -> {
            healthCareDao.deleteHealthCare(healthCare);
            ((FragmentActivity) context).runOnUiThread(() -> {
                _healthCareList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "データが削除されました", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    static class HealthcareViewHolder extends RecyclerView.ViewHolder {
        TextView healthCareId, healthCareDate;
        Button deleteButton, editButton;

        public HealthcareViewHolder(@NonNull View itemView) {
            super(itemView);
            healthCareId = itemView.findViewById(R.id.healthcare_id);
            healthCareDate = itemView.findViewById(R.id.healthcare_date);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}

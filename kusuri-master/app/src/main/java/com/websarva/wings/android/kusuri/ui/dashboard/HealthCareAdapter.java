package com.websarva.wings.android.kusuri.ui.dashboard;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.HealthCare;
import com.websarva.wings.android.kusuri.R;

import java.util.List;

public class HealthCareAdapter extends RecyclerView.Adapter<HealthCareAdapter.HealthcareViewHolder> {
    private List<HealthCare> _healthCareList;
    //コンストラクタ
    public HealthCareAdapter(List<HealthCare> healthCareList) {
        //引数のリストデータをフィールドに格納
        this._healthCareList = healthCareList;
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
    }

    @Override
    public int getItemCount() {
        return _healthCareList.size();
    }

    static class HealthcareViewHolder extends RecyclerView.ViewHolder {
        TextView healthCareId, healthCareDate;

        public HealthcareViewHolder(@NonNull View itemView) {
            super(itemView);
            healthCareId = itemView.findViewById(R.id.healthcare_id);
            healthCareDate = itemView.findViewById(R.id.healthcare_date);
        }
    }
}

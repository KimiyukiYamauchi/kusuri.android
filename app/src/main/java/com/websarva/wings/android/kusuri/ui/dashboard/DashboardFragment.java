package com.websarva.wings.android.kusuri.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.HealthCareDao;
import com.websarva.wings.android.kusuri.HealthCareViewModel;
import com.websarva.wings.android.kusuri.R;
import com.websarva.wings.android.kusuri.databinding.FragmentDashboardBinding;
import com.websarva.wings.android.kusuri.ui.notifications.MedicationAdapter;

public class DashboardFragment extends Fragment {
    private AppDatabase db;
    private HealthCareDao healthCareDao;
    HealthCareViewModel HCViewModel;
    private FragmentDashboardBinding binding;
    private TextView medListTextView;
    private  HealthCareAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext());
        healthCareDao = db.healthCareDao();
        HCViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HealthCareViewModel(healthCareDao);
            }
        }).get(HealthCareViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // TextViewの初期設定
        medListTextView = root.findViewById(R.id.da_medListView);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //リサイクラービューのデータ表示処理を記述する
        //リサイクラービューの画面部品を取得
        RecyclerView HCrecyclerView = view.findViewById(R.id.recycler_view_hc);
        //レイアウトマネージャーのオブジェクト作成
        LinearLayoutManager layout = new LinearLayoutManager(getContext());
        HCrecyclerView.setLayoutManager(layout);
        DividerItemDecoration decoration = new DividerItemDecoration(getActivity(),layout.getOrientation());
        HCrecyclerView.addItemDecoration(decoration);

        // LiveData を観察してデータが変わったらリサイクラービューにアダプターにセット
        HCViewModel.getHealthCareList().observe(getViewLifecycleOwner(), healthCareList -> {
            Log.d("DashboardFragment", "healthCareList = " + healthCareList);
            adapter = new HealthCareAdapter(healthCareList, healthCareDao, requireContext());
            HCrecyclerView.setAdapter(adapter);
        });

        // 登録ボタンの設定
        Button button = binding.getRoot().findViewById(R.id.bt_da_reg);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DashboardActivity.class);
            startActivityForResult(intent, 1); // 結果を取得するリクエストコード1
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}

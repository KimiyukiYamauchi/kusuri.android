package com.websarva.wings.android.kusuri.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.websarva.wings.android.kusuri.AppDatabase;
import com.websarva.wings.android.kusuri.MedicationDao;
import com.websarva.wings.android.kusuri.MedicationViewModel;
import com.websarva.wings.android.kusuri.R;

public class MedicationListFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private AppDatabase db;
    private MedicationDao medicationDao; // データベースアクセス用
    private MedicationAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MedicationListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Database インスタンスを取得し、ViewModel を設定
        db = AppDatabase.getDatabase(requireContext());
        medicationDao = db.medicationDao();
        MedicationViewModel viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new MedicationViewModel(medicationDao);
            }
        }).get(MedicationViewModel.class);

        // LiveData を観察してデータが変わったらアダプターにセット
        viewModel.getMedicationList().observe(getViewLifecycleOwner(), medicationList -> {
            adapter = new MedicationAdapter(medicationList);
            recyclerView.setAdapter(adapter);
        });
        return view;
    }
}
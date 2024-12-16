package com.websarva.wings.android.kusuri;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class MedicationViewModel extends ViewModel {
    private MedicationDao medicationDao;
    private LiveData<List<Medication>> medicationList;

    public MedicationViewModel(MedicationDao medicationDao) {
        this.medicationDao = medicationDao;
        medicationList = medicationDao.getAllMedicationsByCreationDate();
    }

    public LiveData<List<Medication>> getMedicationList() {
        return medicationList;
    }
}
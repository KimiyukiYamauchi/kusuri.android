package com.websarva.wings.android.kusuri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class HealthCareViewModel extends ViewModel {
    private HealthCareDao healthCareDao;
    private LiveData<List<HealthCare>> healthCareList;

    public HealthCareViewModel(HealthCareDao healthCareDao) {
        this.healthCareDao = healthCareDao;
        healthCareList = healthCareDao.getAllHealthCareByCreationDate();
    }

    public LiveData<List<HealthCare>> getHealthCareList() {
        return healthCareList;
    }
}
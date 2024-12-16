package com.websarva.wings.android.kusuri;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HealthCareDao {
    //体調をデータベースに挿入
    @Insert
    void insertHealthCare(HealthCare healthcare);

    //体調のすべてのレコードを取得
    @Query("SELECT * FROM HealthCare")
    List<HealthCare> getAllHealthCare();

    // 作成日の降順で薬を取得
    @Query("SELECT * FROM HealthCare ORDER BY  createdAt DESC")
    LiveData<List<HealthCare>> getAllHealthCareByCreationDate();
}

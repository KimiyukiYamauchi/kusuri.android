package com.websarva.wings.android.kusuri;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    // 指定された健康情報を削除
    @Delete
    void deleteHealthCare(HealthCare healthCare);


    // 指定IDの健康情報を取得
    @Query("SELECT * FROM HealthCare WHERE id = :healthCareId")
    HealthCare getHealthCareById(int healthCareId);

    // 指定された健康情報を更新
    @Update
    void updateHealthCare(HealthCare healthCare);

}

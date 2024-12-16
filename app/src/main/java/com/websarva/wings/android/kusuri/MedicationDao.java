package com.websarva.wings.android.kusuri;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;


import java.util.List;

@Dao
public interface MedicationDao {
    // 薬をデータベースに挿入
    @Insert
    long insertMedication(Medication medication);

    // 薬のすべてのレコードを取得
    @Query("SELECT * FROM Medication")
    List<Medication> getAllMedications();

    // 作成日の降順で薬を取得
    @Query("SELECT * FROM Medication ORDER BY  createdAt DESC")
    LiveData<List<Medication>> getAllMedicationsByCreationDate();

    // 指定された薬を削除
    @Delete
    void deleteMedication(Medication medication);


    // 指定IDの薬を取得
    @Query("SELECT * FROM Medication WHERE id = :medicationId")
    Medication getMedicationById(int medicationId);

    // 指定された薬を更新
    @Update
    void updateMedication(Medication medication);

    // Medicationテーブルのすべてのデータから開始日と終了日を取得
    @Query("SELECT startdate, enddate FROM Medication")
    List<MedicationDates> getAllMedicationDates();

    @Query("SELECT * FROM medication WHERE startdate <= :selectedDate AND enddate >= :selectedDate")
    List<Medication> getMedicationForDate(long selectedDate);

    @Transaction
    @Query("SELECT * FROM Medication WHERE id = :id")
    MedicationWithRemainders getMedicationWithRemainders(int id);

    @Transaction
    @Query("SELECT * FROM Medication")
    List<MedicationWithRemainders> getAllMedicationsWithRemainders();

    @Query("DELETE FROM Reminder WHERE medicationId = :medicationId")
    void deleteRemaindersByMedicationId(int medicationId);

}

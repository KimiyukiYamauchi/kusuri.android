package com.websarva.wings.android.kusuri;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReminderDao {

    // Reminderの挿入
    @Insert
    void insert(Reminder reminder);

    // Reminderをリストで挿入
    @Insert
    void insertAll(List<Reminder> reminders);

    // 特定のmedicationIdに紐づくReminderを取得
    @Query("SELECT * FROM Reminder WHERE medicationId = :medicationId")
    List<Reminder> getRemindersByMedicationId(int medicationId);

    // Reminderをすべて取得
    @Query("SELECT * FROM Reminder")
    List<Reminder> getAllReminders();

    // Reminderを削除
    @Delete
    void delete(Reminder reminder);
}

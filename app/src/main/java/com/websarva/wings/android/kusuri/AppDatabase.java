package com.websarva.wings.android.kusuri;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Medication.class,HealthCare.class, Reminder.class}, version = 12, exportSchema = false)
//@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MedicationDao medicationDao();
    public abstract HealthCareDao healthCareDao();
    public abstract ReminderDao reminderDao();
    // シングルトンパターンでデータベースインスタンスを取得
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context){
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")  // 1つのデータベース名に統一
                            .fallbackToDestructiveMigration()  // マイグレーションを行わず、データを破壊
                            .build();

                }
            }
        }
        return INSTANCE;
    }




}


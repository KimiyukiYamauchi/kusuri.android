package com.websarva.wings.android.kusuri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "Reminder",
        foreignKeys = @ForeignKey(
        entity = Medication.class,
        parentColumns = "id",
        childColumns = "medicationId",
        onDelete = ForeignKey.CASCADE // 親データが削除された場合、子データも削除
),
indices = {@androidx.room.Index("medicationId")} // 外部キーにインデックスを付与して高速化
)

public class Reminder {
    @PrimaryKey(autoGenerate = true)
    public int id; // 識別ID

    @ColumnInfo(name = "medicationId")
    public int medicationId;  // 外部キーとして薬のIDを参照

    @ColumnInfo(name = "hourOfDay")
    public int HourOfDay;  // 時間 (例: 14)

    @ColumnInfo(name = "minute")
    public int Minute;  // 分 (例: 30)
}



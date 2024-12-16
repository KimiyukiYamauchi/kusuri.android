package com.websarva.wings.android.kusuri;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class MedicationWithRemainders {
    @Embedded
    public Medication medication; // 親エンティティ

    @Relation(
            parentColumn = "id", // 親テーブルのキー
            entityColumn = "medicationId" // 子テーブルの外部キー
    )
    public List<Reminder> reminders; // 子エンティティのリスト
}

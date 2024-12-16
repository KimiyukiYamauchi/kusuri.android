package com.websarva.wings.android.kusuri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity
public class Medication {
    @PrimaryKey(autoGenerate = true)
    public int id;             // 自動生成されるID
    public String name;        // 薬の名前
    public int dosage;         // 服用量
    public String dosageSpinner; //錠・包
    public int frequency;      // 服用頻度
    public long startdate;     // 服用開始日（Unixタイムスタンプ）
    public long enddate;       // 服用終了日（Unixタイムスタンプ）
    public String memo;
    public String reminder;   // リマインダ

    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    public long createdAt;     // 登録日時（Unixタイムスタンプ）

    // デフォルトのコンストラクタで現在のタイムスタンプを設定
    public Medication() {
        this.name = "";
        this.dosage = 0;
        this.frequency = 0;
        this.startdate = 0;
        this.enddate = 0;
        this.reminder = "false";
        this.createdAt = System.currentTimeMillis(); // 現在のタイムスタンプを設定
    }

    // 日付をフォーマットして返すメソッド
    public String getFormattedCreationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }
}

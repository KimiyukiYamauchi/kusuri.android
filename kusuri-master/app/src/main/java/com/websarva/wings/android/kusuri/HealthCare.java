package com.websarva.wings.android.kusuri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity
public class HealthCare {
    @PrimaryKey(autoGenerate = true)
    public int id;                  //自動生成されるID
    public double temperature;        //体温
    public int pressureUp;            //血圧（上）
    public int pressureDown;            //血圧（下）
    public double weight;              //体重
    public int sugar;

    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    public long createdAt;     // 登録日時（Unixタイムスタンプ）

    // デフォルトのコンストラクタで現在のタイムスタンプを設定
    public HealthCare() {
        this.id = 0;
        this.temperature = 0;
        this.pressureUp = 0;
        this.pressureDown = 0;
        this.weight = 0;
        this.sugar = 0;
        this.createdAt = System.currentTimeMillis(); // 現在のタイムスタンプを設定
    }

    // IDを取得するメソッド
    public int getId() {
        return id;
    }

    // 日付をフォーマットして返すメソッド
    public String getFormattedCreationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }

    // 日付をフォーマットして返すメソッド
    public String getFormattedCreationTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }
}

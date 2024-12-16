package com.websarva.wings.android.kusuri;

public class MedicationDates {
    public long startdate; // 服用開始日
    public long enddate;   // 服用終了日

    // コンストラクタ（任意）
    public MedicationDates(long startdate, long enddate) {
        this.startdate = startdate;
        this.enddate = enddate;
    }
}

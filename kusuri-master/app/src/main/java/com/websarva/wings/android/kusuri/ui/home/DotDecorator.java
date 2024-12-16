package com.websarva.wings.android.kusuri.ui.home;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

public class DotDecorator implements DayViewDecorator {
    private CalendarDay date;
    private int color;

    // コンストラクタ
    public DotDecorator(CalendarDay date, int color) {
        this.date = date;
        this.color = color;
    }

    // shouldDecorate メソッドのオーバーライド
    @Override
    public boolean shouldDecorate(CalendarDay day) {
        // 指定の日付をデコレートするかどうかを判断
        return day.equals(date);
    }

    // decorate メソッドのオーバーライド
    @Override
    public void decorate(DayViewFacade view) {
        // ドットを描画
        view.addSpan(new DotSpan(14, color));  // ドットの半径10, 色を指定
    }
}

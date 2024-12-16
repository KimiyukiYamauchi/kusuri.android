package com.websarva.wings.android.kusuri.ui.notifications;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.websarva.wings.android.kusuri.R;

public class MedicationReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 通知チャンネルを作成 (API 26+ 必要)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("medication_channel", "服用通知", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 通知の作成
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "medication_channel")
                .setSmallIcon(R.drawable.ic_notification) // 適切な通知アイコンを設定
                .setContentTitle("服用リマインダー")
                .setContentText("お薬を服用する時間です")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // 通知の送信
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

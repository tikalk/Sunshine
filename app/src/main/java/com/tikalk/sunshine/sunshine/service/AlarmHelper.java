package com.tikalk.sunshine.sunshine.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by oren on 21/01/16.
 */
public class AlarmHelper {
    public static final String ONE_TIME = "single_alarm";
    public static void setAlarm(Context context, Class<? extends BroadcastReceiver> receiverClass)
    {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context,receiverClass);
        intent.putExtra(ONE_TIME, Boolean.FALSE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        //After after 5 seconds
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis(), 1000 * 5 , pi);
    }

    public static void cancelAlarm(Context context, Class<? extends BroadcastReceiver> receiverClass)
    {
        Intent intent = new Intent(context,  receiverClass);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    public static void setOnetimeTimer(Context context, Class<? extends BroadcastReceiver> receiverClass){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiverClass);
        intent.putExtra(ONE_TIME, Boolean.TRUE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi);
    }
}

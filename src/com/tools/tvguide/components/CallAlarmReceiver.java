package com.tools.tvguide.components;

import com.tools.tvguide.activities.AlarmAlertActivity;
import com.tools.tvguide.managers.AppEngine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallAlarmReceiver extends BroadcastReceiver 
{
    private static final String TAG = "CallAlarmReceiver";
    @Override
    public void onReceive(Context context, Intent intent) 
    {
//        Log.d(TAG, "onReceive: action=" + intent.getAction());
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) 
        {
//            Log.d(TAG, "onReceive boot completed");
            // 重新计算闹铃时间
            if (AppEngine.getInstance().getContext() == null)
                AppEngine.getInstance().setContext(context);
            AppEngine.getInstance().getAlarmHelper().resetAllAlarms();
            return;
        }
        
        intent.setClass(context, AlarmAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

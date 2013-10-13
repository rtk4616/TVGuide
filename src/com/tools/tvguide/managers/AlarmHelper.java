package com.tools.tvguide.managers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.tools.tvguide.components.CallAlarmReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmHelper
{
    public static final String TAG = "AlarmHelper";
    private Context mContext;
    private LinkedHashMap<String, HashMap<String, String>> mRecords;
    private final String SEPERATOR = "#";
    private boolean mSettingChanged = false;
    private String FILE_ALARM_HELPER = "alarm_settings.txt";
    
    public AlarmHelper(Context context)
    {
        assert(context != null);
        mContext = context;
        loadAlarmSettings();
    }
    
    private void checkInitialized() 
    {
        if (mRecords == null)
            throw new IllegalStateException("mRecords is null");
    }
    
    public void addAlarm(String channelId, String channelName, String program, long triggerAtMillis)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(triggerAtMillis);
//        Log.d(TAG, "addAlarm: channel=" + channelName + ", program=" + program + ", triggerTime=" + calendar.getTime().toString());
        String key = makeKey(channelId, channelName, program);
        if (isAlarmSet(channelId, channelName, program))
            removeAlarm(channelId, channelName, program);
        
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("channel_id", channelId);
        info.put("channel_name", channelName);
        info.put("program", program);
        if (hasConflictWithOthers(triggerAtMillis))
        {
            long threshold = 5;
            long tuning = key.hashCode() % 20000;
            if (Math.abs(tuning) < threshold)
            {
                if (tuning >= 0)
                    tuning += threshold;
                else
                    tuning += threshold * -1;
            }
            triggerAtMillis += tuning;
            calendar.setTimeInMillis(triggerAtMillis);
//            Log.d(TAG, "addAlarm: has conflict, ajust=" + tuning + ", to" + calendar.getTime().toString());
        }
        info.put("time", Long.toString(triggerAtMillis));
        mRecords.put(key, info);
        
        // 指定闹钟设置的时间到时，要运行的CallAlarm.class  
        Intent intent = new Intent(mContext, CallAlarmReceiver.class);
        intent.putExtra("channel_id", channelId);
        intent.putExtra("channel_name", channelName);
        intent.putExtra("program", program);
        
        PendingIntent sender = PendingIntent.getBroadcast(mContext, key.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, sender);
        mSettingChanged = true;
    }
    
    public void removeAlarm(String channelId, String channelName, String program)
    {
        String key = makeKey(channelId, channelName, program);
        mRecords.remove(key);
        
        Intent intent = new Intent(mContext, CallAlarmReceiver.class);
        intent.putExtra("channel_id", channelId);
        intent.putExtra("channel_name", channelName);
        intent.putExtra("program", program);
        
        PendingIntent sender = PendingIntent.getBroadcast(mContext, key.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        mSettingChanged = true;
        saveAlarmSettings();        // removeAlarm 可能会在主程序未启动时调用到，所以不能依靠shutDown()中来存储
    }
    
    public HashMap<String, HashMap<String, String>> getAllRecords()
    {
        return mRecords;
    }
    
    public long getAlarmTimeAtMillis(String channelId, String channelName, String program)
    {
        if (isAlarmSet(channelId, channelName, program))
            return Long.valueOf(mRecords.get(makeKey(channelId, channelName, program)).get("time"));
        return -1;
    }
    
    public boolean isAlarmSet(String channelId, String channelName, String program)
    {
        return mRecords.containsKey(makeKey(channelId, channelName, program));
    }
    
    public void resetAllAlarms()
    {
        checkInitialized();
        LinkedHashMap<String, HashMap<String, String>> tmpMap = new LinkedHashMap<String, HashMap<String,String>>(mRecords);
        Iterator<Entry<String, HashMap<String, String>>> iter = tmpMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Entry<String, HashMap<String, String>> entry = iter.next();
            HashMap<String, String> info = entry.getValue();
            String channelId = info.get("channel_id");
            String channelName = info.get("channel_name");
            String program = info.get("program");
            long triggerAtMillis = Long.valueOf(info.get("time")).longValue();
            addAlarm(channelId, channelName, program, triggerAtMillis);
        }
    }
    
    private String makeKey(String channelId, String channelName, String program)
    {
        return channelId + SEPERATOR + channelName + SEPERATOR + program;
    }
    
    private boolean hasConflictWithOthers(long triggerAtMillis)
    {
        Iterator<Entry<String, HashMap<String, String>>> iter = mRecords.entrySet().iterator();
        while (iter.hasNext())
        {
            Entry<String, HashMap<String, String>> entry = iter.next();
            HashMap<String, String> info = entry.getValue();
            if (info.get("time") != null)
            {
                long triggerTime = Long.valueOf(info.get("time")).longValue();
                if (triggerTime == triggerAtMillis)
                    return true;
            }
        }
        return false;
    }
    
    public void shutDown()
    {
        if (mSettingChanged)
        {
            saveAlarmSettings();
        }
    }
    
    private void saveAlarmSettings()
    {
        try
        {
            FileOutputStream fos = mContext.openFileOutput(FILE_ALARM_HELPER, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mRecords);
            oos.flush();
            oos.close();
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadAlarmSettings()
    {
        boolean loadSuccess = true;
        try
        {
            FileInputStream fis = mContext.openFileInput(FILE_ALARM_HELPER);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            if (obj instanceof HashMap<?, ?>)
            {
                mRecords =  (LinkedHashMap<String, HashMap<String, String>>) obj;
            }
            else 
            {
                loadSuccess = false;
            }
            ois.close();
            fis.close();
        }
        catch (StreamCorruptedException e)
        {
            loadSuccess = false;
            e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            loadSuccess = false;
            e.printStackTrace();
        }
        catch (IOException e)
        {
            loadSuccess = false;
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            loadSuccess = false;
            e.printStackTrace();
        }
        
        if (loadSuccess == false)
        {
            mRecords = new LinkedHashMap<String, HashMap<String, String>>();
        }
    }
}

package com.tools.tvguide.activities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.tools.tvguide.R;
import com.tools.tvguide.adapters.DateAdapter;
import com.tools.tvguide.adapters.ResultProgramAdapter;
import com.tools.tvguide.adapters.DateAdapter.DateData;
import com.tools.tvguide.adapters.ResultProgramAdapter.IItemView;
import com.tools.tvguide.components.AlarmSettingDialog;
import com.tools.tvguide.components.AlarmSettingDialog.OnAlarmSettingListener;
import com.tools.tvguide.components.MyProgressDialog;
import com.tools.tvguide.data.Channel;
import com.tools.tvguide.managers.AppEngine;
import com.tools.tvguide.managers.CollectManager;
import com.tools.tvguide.managers.ContentManager;
import com.tools.tvguide.views.DetailLeftGuide;
import com.tools.tvguide.views.DetailLeftGuide.OnChannelSelectListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChannelDetailActivity extends Activity 
{
    private static final String TAG = "ChannelDetailActivity";
    private String mChannelName;
    private String mChannelId;
    private List<Channel> mChannelList;
    private TextView mChannelNameTextView;
    private TextView mDateTextView;
    private ListView mProgramListView;
    private BaseAdapter mProgramListViewAdapter;
    private ListView mDateChosenListView;
    private DateAdapter mDateAdapter;
    private int mOnPlayingIndex;
    private Timer mTimer;
    private DetailLeftGuide mLeftMenu;
    private ImageView mFavImageView;
    
    private List<HashMap<String, String>> mProgramList;             // Key: time, title
    private List<HashMap<String, String>> mOnPlayingProgram;        // Key: time, title
    private MyProgressDialog mProgressDialog;
    private int mCurrentSelectedDay;
    private List<ResultProgramAdapter.IListItem> mItemDataList;
    
    private enum SelfMessage {MSG_UPDATE_PROGRAMS, MSG_UPDATE_ONPLAYING_PROGRAM};
    private final int TIMER_SCHEDULE_PERIOD = 5 * 60 * 1000;        // 5 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);
        
        // configure the SlidingMenu
        SlidingMenu menu = new SlidingMenu(this);
//        menu.setMode(SlidingMenu.LEFT_RIGHT);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
//        menu.setMenu(R.layout.channel_detail_left);
        mLeftMenu = new DetailLeftGuide(this);
        menu.setMenu(mLeftMenu);
//        menu.setSecondaryMenu(R.layout.channel_detail_right);
//        menu.setSecondaryShadowDrawable(R.drawable.shadowright);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        
        mChannelId = getIntent().getStringExtra("id");
        mChannelName = getIntent().getStringExtra("name");
        mChannelList = (List<Channel>) getIntent().getSerializableExtra("channel_list");
        if (mChannelList == null)
        	mChannelList = new ArrayList<Channel>();
        mProgramList = new ArrayList<HashMap<String,String>>();
        mOnPlayingProgram = new ArrayList<HashMap<String,String>>();
        mProgressDialog = new MyProgressDialog(this);
        mCurrentSelectedDay = getProxyDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        mItemDataList = new ArrayList<ResultProgramAdapter.IListItem>();
        mOnPlayingIndex = -1;
     
        initViews();
        updateAll();
        
        mTimer = new Timer(true);
        mTimer.schedule(new TimerTask() 
        {
            @Override
            public void run() 
            {
                updateOnplayingProgram();
            }
        }, TIMER_SCHEDULE_PERIOD, TIMER_SCHEDULE_PERIOD);
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mTimer.cancel();
    }
    
    @Override
    public void onNewIntent (Intent intent)
    {
        setIntent(intent);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        
        // 检测屏幕的方向：纵向或横向  
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            // 当前为横屏， 在此处添加额外的处理代码 
        }
        else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            // 当前为竖屏， 在此处添加额外的处理代码  
        }
    }
    
    public void initViews()
    {
        mChannelNameTextView = (TextView) findViewById(R.id.channeldetail_channel_name_tv);
        mDateTextView = (TextView) findViewById(R.id.channeldetail_date_tv);
        mProgramListView = (ListView) findViewById(R.id.channeldetail_program_listview);
        mDateChosenListView = (ListView) findViewById(R.id.channeldetail_date_chosen_listview);
        mFavImageView = (ImageView) findViewById(R.id.channeldetail_fav_iv);
        
        List<DateData> dateList = new ArrayList<DateAdapter.DateData>();
        dateList.add(new DateData(getResources().getString(R.string.Mon)));
        dateList.add(new DateData(getResources().getString(R.string.Tue)));
        dateList.add(new DateData(getResources().getString(R.string.Wed)));
        dateList.add(new DateData(getResources().getString(R.string.Thu)));
        dateList.add(new DateData(getResources().getString(R.string.Fri)));
        dateList.add(new DateData(getResources().getString(R.string.Sat)));
        dateList.add(new DateData(getResources().getString(R.string.Sun)));
        mDateAdapter = new DateAdapter(this, dateList);
        mDateChosenListView.setAdapter(mDateAdapter);
        mDateAdapter.setCurrentIndex(mCurrentSelectedDay - 1);
        
        mLeftMenu.setChannelList(mChannelList);
        for (int i=0; i<mChannelList.size(); ++i)
        {
        	if (mChannelList.get(i).id.equals(mChannelId))
        	{
        		mLeftMenu.setCurrentIndex(i);
        		mLeftMenu.setSelection(i);
        	}
        }
        mLeftMenu.setOnChannelSelectListener(new OnChannelSelectListener() 
        {
            @Override
            public void onChannelSelect(Channel channel) 
            {
                mChannelId = channel.id;
                mChannelName = channel.name;
                updateAll();
            }
        });
        
        mDateChosenListView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) 
            {
                mDateAdapter.setCurrentIndex(position);
                mCurrentSelectedDay = position + 1;
                updateProgramList();
            }
        });
        
        mProgramListView.setOnScrollListener(new OnScrollListener() 
        {            
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) 
            {
                foldDateListView();
            }
            
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) 
            {
            }
        });
        
        mProgramListView.setOnItemClickListener(new OnItemClickListener() 
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) 
            {
                foldDateListView();
                
                final String time = (String) mItemDataList.get(position).getExtraInfo().get("time");
                final String title = (String) mItemDataList.get(position).getExtraInfo().get("title");
                final String program = getProgramString(time, title);
                
                String hour = time.split(":")[0];
                String minute = time.split(":")[1];
                
                AlarmSettingDialog alarmSettingDialog = new AlarmSettingDialog(ChannelDetailActivity.this, mCurrentSelectedDay, Integer.parseInt(hour),
                                Integer.parseInt(minute), mChannelId, mChannelName, program);
                
                alarmSettingDialog.setAlarmSettingListener(new OnAlarmSettingListener() 
                {
                    @Override
                    public void onAlarmSetted(boolean success) 
                    {
                        updateItem(position, true, success, false);
                    }
                });
                
                alarmSettingDialog.show();
            }
        });
        
        Toast.makeText(ChannelDetailActivity.this, getResources().getString(R.string.alarm_tips_can_set), Toast.LENGTH_SHORT).show();
    }
    
    public void onClick(View view)
    {
        switch (view.getId()) 
        {
            case R.id.channeldetail_date_iv:
                toggleDateListView();
                break;
            case R.id.channeldetail_fav_iv:
                toggleFavIcon();
                break;
            default:
                break;
        }
    }
    
    private void toggleDateListView()
    {
        if (mDateChosenListView.getVisibility() == View.GONE)
            unfoldDateListView();
        else
            foldDateListView();
    }
    
    private void foldDateListView()
    {
        if (mDateChosenListView.getVisibility() == View.VISIBLE)
        {
            Animation pushRightOut = AnimationUtils.loadAnimation(this, R.anim.push_right_out);
            mDateChosenListView.startAnimation(pushRightOut);
            mDateChosenListView.setVisibility(View.GONE);
        }
    }
    
    private void unfoldDateListView()
    {
        if (mDateChosenListView.getVisibility() == View.GONE)
        {
            Animation pushRightIn = AnimationUtils.loadAnimation(this, R.anim.push_right_in);
            pushRightIn.setFillAfter(true);
            mDateChosenListView.startAnimation(pushRightIn);
            mDateChosenListView.setVisibility(View.VISIBLE);
        }
    }
    
    private void toggleFavIcon()
    {
        
        CollectManager manager = AppEngine.getInstance().getCollectManager();
        if (manager.isChannelCollected(mChannelId))
        {
            collectChannel(false);
            mFavImageView.setImageResource(R.drawable.btn_fav);
        }
        else
        {
            collectChannel(true);
            mFavImageView.setImageResource(R.drawable.btn_fav_checked);
        }
    }
    
    public void collectChannel(boolean doCollect)
    {
        CollectManager manager = AppEngine.getInstance().getCollectManager();
        if (doCollect)
        {
            HashMap<String, Object> info = new HashMap<String, Object>();
            info.put("name", mChannelName);
            manager.addCollectChannel(mChannelId, info);
            Toast.makeText(this, R.string.collect_success, Toast.LENGTH_SHORT).show();
        }
        else
        {
            if (manager.isChannelCollected(mChannelId))
                manager.removeCollectChannel(mChannelId);
            Toast.makeText(this, R.string.cancel_collect, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateAll()
    {
    	updateTitle();
    	updateFavIcon();
    	updateProgramList();
    }
    
    private void updateTitle()
    {
        mChannelNameTextView.setText(mChannelName);
    }
    
    private void updateFavIcon()
    {
    	if (AppEngine.getInstance().getCollectManager().isChannelCollected(mChannelId))
            mFavImageView.setImageResource(R.drawable.btn_fav_checked);
    	else
    		mFavImageView.setImageResource(R.drawable.btn_fav);
    }

    private void updateProgramList()
    {
        mProgramList.clear();
        mOnPlayingProgram.clear();
        boolean isSyncLoad = AppEngine.getInstance().getContentManager().loadProgramsByChannel2(mChannelId, mCurrentSelectedDay, mProgramList, 
                                mOnPlayingProgram, new ContentManager.LoadListener() 
        {
            @Override
            public void onLoadFinish(int status) 
            {
                uiHandler.sendEmptyMessage(SelfMessage.MSG_UPDATE_PROGRAMS.ordinal());
            }
        });
        if (isSyncLoad == false)
            mProgressDialog.show();
    }
    
    private void updateOnplayingProgram()
    {
        mOnPlayingProgram.clear();
        AppEngine.getInstance().getContentManager().loadOnPlayingProgramByChannel(mChannelId, mOnPlayingProgram, new ContentManager.LoadListener() 
        {    
            @Override
            public void onLoadFinish(int status) 
            {
                uiHandler.sendEmptyMessage(SelfMessage.MSG_UPDATE_ONPLAYING_PROGRAM.ordinal());
            }
        });
    }

    @Override
    public void onBackPressed() 
    {
        finish();
    };
    
    public void back(View view)
    {
        if (view instanceof Button)
        {
            // The same effect with press back key
            finish();
        }
    }
    
    private Handler uiHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            SelfMessage selfMsg = SelfMessage.values()[msg.what];
            switch (selfMsg) 
            {
                case MSG_UPDATE_PROGRAMS:
                    mItemDataList.clear();
                    mProgressDialog.dismiss();
                    for (int i=0; i<mProgramList.size(); ++i)
                    {
                        final String time = mProgramList.get(i).get("time");
                        final String title = mProgramList.get(i).get("title");
                        
                        ResultProgramAdapter.Item item = new ResultProgramAdapter.Item();
                        item.time = time;
                        item.title = title;
                        ResultProgramAdapter.ContentItem contentItem = new ResultProgramAdapter.ContentItem(item, R.layout.hot_program_item, R.id.hot_program_name_tv);
                        
                        HashMap<String, String> extraInfo = new HashMap<String, String>();
                        extraInfo.put("time", time);
                        extraInfo.put("title", title);
                        contentItem.setExtraInfo(extraInfo);
                                                
                        if (time.equals(mOnPlayingProgram.get(0).get("time")) && title.equals(mOnPlayingProgram.get(0).get("title"))
                            && mCurrentSelectedDay == getProxyDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)))
                        {
                            mOnPlayingIndex = i;
                            contentItem.setItemView(new ResultProgramAdapter.IItemView()
                            {    
                                @Override
                                public View getView(Context context, View convertView, LayoutInflater inflater) 
                                {
                                    return getContentItemView(context, convertView, inflater, mOnPlayingIndex, true, false, true);
                                }
                            });
                        }
                        contentItem.setClickable(true);
                        mItemDataList.add(contentItem);
                    }
                    addTimeLable();
                    mProgramListViewAdapter = new ResultProgramAdapter(ChannelDetailActivity.this, mItemDataList);
                    mProgramListView.setAdapter(mProgramListViewAdapter);
                    if (mOnPlayingIndex != -1)                        
                        mProgramListView.setSelection(mOnPlayingIndex);
                    
                    // 显示闹钟图标
                    for (int i=0; i<mItemDataList.size(); ++i)
                    {
                        if (mItemDataList.get(i).getExtraInfo() == null)	// Label Item
                            continue;
                     
                        if (i < mOnPlayingIndex)
                    		updateItem(i, false, false, false);
                        
                        String time = (String) mItemDataList.get(i).getExtraInfo().get("time");
                        String title = (String) mItemDataList.get(i).getExtraInfo().get("title");
                        String program = getProgramString(time, title);
                        if (AppEngine.getInstance().getAlarmHelper().isAlarmSet(mChannelId, mChannelName, program, mCurrentSelectedDay))
                            updateItem(i, true, true, false);
                    }
                    
                    foldDateListView();
                    break;
                case MSG_UPDATE_ONPLAYING_PROGRAM:
                    for (int i=0; i<mItemDataList.size(); ++i)
                    {
                        if (mItemDataList.get(i).getExtraInfo() == null)
                            continue;
                        
                        String time = (String) mItemDataList.get(i).getExtraInfo().get("time");
                        String title = (String) mItemDataList.get(i).getExtraInfo().get("title");
                        if (time.equals(mOnPlayingProgram.get(0).get("time")) && title.equals(mOnPlayingProgram.get(0).get("title"))
                            && mCurrentSelectedDay == getProxyDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)))
                        {
                            if (mOnPlayingIndex != i)
                            {
                                updateItem(mOnPlayingIndex, true, false, false);
                                mOnPlayingIndex = i;
                                updateItem(mOnPlayingIndex, true, false, true);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            
        }
    };
    
    private void addTimeLable()
    {
        if (mProgramList.size() == 0)
            return;
        
        if (mProgramList.size() == 1)
        {
            mItemDataList.add(0, new ResultProgramAdapter.LabelItem(getResources().getString(R.string.full_day), R.layout.hot_channel_item, R.id.hot_channel_name_tv));
            return;
        }
        
        final String morning = "00:00";
        final String midday = "12:00";
        final String evening = "18:00";
        int hasMorning = 0;
        int hasMidday = 0;
        int hasEvening = 0;
        int onPlayingAddon = 0;
        
        for (int i=0; i<mProgramList.size()-1; ++i)
        {
            String time1 = mProgramList.get(i).get("time");
            String time2 = mProgramList.get(i+1).get("time");
            
            if (i == 0 && compareTime(time1, midday) < 0)
            {
                mItemDataList.add(i, new ResultProgramAdapter.LabelItem(getResources().getString(R.string.morning), R.layout.hot_channel_item, R.id.hot_channel_name_tv));
                hasMorning = 1;
                if (mOnPlayingIndex >= i)
                    onPlayingAddon++;
            }
            else if (compareTime(time1, midday) < 0 && compareTime(time2, midday) >= 0 && compareTime(time2, evening) < 0)
            {
                mItemDataList.add(i + 1 + hasMorning, new ResultProgramAdapter.LabelItem(getResources().getString(R.string.midday), R.layout.hot_channel_item, R.id.hot_channel_name_tv));
                hasMidday = 1;
                if (mOnPlayingIndex >= i)
                    onPlayingAddon++;
            }
            else if (compareTime(time1, evening) < 0 && compareTime(time2, evening) >= 0)
            {
                mItemDataList.add(i + 1 + hasMorning + hasMidday, new ResultProgramAdapter.LabelItem(getResources().getString(R.string.evening), R.layout.hot_channel_item, R.id.hot_channel_name_tv));
                hasEvening = 1;
                if (mOnPlayingIndex >= i)
                    onPlayingAddon++;
            }
        }
        
        mOnPlayingIndex += onPlayingAddon;
    }
    
    /*
     * Trasfer the day to the server host day: Monday~Sunday -> 1~7
     */
    private int getProxyDay(int day)
    {
        assert(day >=1 && day <=7);
        int hostDay = 0;
        switch (day)
        {
            case Calendar.MONDAY:
                hostDay = 1;
                break;
            case Calendar.TUESDAY:
                hostDay = 2;
                break;
            case Calendar.WEDNESDAY:
                hostDay = 3;
                break;
            case Calendar.THURSDAY:
                hostDay = 4;
                break;
            case Calendar.FRIDAY:
                hostDay = 5;
                break;
            case Calendar.SATURDAY:
                hostDay = 6;
                break;
            case Calendar.SUNDAY:
                hostDay = 7;
                break;
        }
        return hostDay;
    }
    
    private int compareTime(String time1, String time2)
    {
        assert(time1 != null && time2 != null);
        
        try 
        {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            Date date1 = df.parse(time1);
            Date date2 = df.parse(time2);
            
            if (date1.getTime() > date2.getTime())
                return 1;
            else if (date1.getTime() < date2.getTime())
                return -1;
            else
                return 0;
        } 
        catch (ParseException e) 
        {
            e.printStackTrace();
        }
        
        return 0;
    }
       
    private void updateItem(final int position, final boolean hasIndicator, final boolean hasAlarm, final boolean onplaying)
    {
        if (mItemDataList == null || mProgramListViewAdapter == null)
            return;
        
        mItemDataList.get(position).setItemView(new IItemView() 
        {
            @Override
            public View getView(Context context, View convertView, LayoutInflater inflater) 
            {
                return getContentItemView(context, convertView, inflater, position, hasIndicator, hasAlarm, onplaying);
            }
        });
        mProgramListViewAdapter.notifyDataSetChanged();
    }
    
    private String getProgramString(String time, String title)
    {
        return time + ":　" + title;
    }
    
    private View getContentItemView(Context context, View convertView, LayoutInflater inflater, int position, 
    		boolean hasIndicator, boolean hasAlarm, boolean onplaying)
    {
        assert (inflater != null);
        convertView = inflater.inflate(R.layout.detail_program_item, null);
        TextView programNameTextView = (TextView) convertView.findViewById(R.id.detail_program_name_tv);
        ImageView indicator = (ImageView) convertView.findViewById(R.id.detail_program_indicator_iv);
        ImageView alarmIcon = (ImageView) convertView.findViewById(R.id.detail_alarm_icon_iv);
        
        if (onplaying && mOnPlayingProgram.size() > 0)
        {
            SpannableString ss = new SpannableString(getProgramString(mOnPlayingProgram.get(0).get("time"), mOnPlayingProgram.get(0).get("title")) + "  (正在播放)");
            ss.setSpan(new ForegroundColorSpan(Color.RED), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            programNameTextView.setText(ss);
        }
        else if (mItemDataList.get(position).getExtraInfo() != null)
        {
            String time = (String) mItemDataList.get(position).getExtraInfo().get("time");
            String title = (String) mItemDataList.get(position).getExtraInfo().get("title");
            programNameTextView.setText(getProgramString(time, title));
        }
        if (hasAlarm)
        {
            indicator.setVisibility(View.GONE);
            alarmIcon.setVisibility(View.VISIBLE);
        }
        else
        {
        	if (hasIndicator)
        		indicator.setVisibility(View.VISIBLE);
        	else
        		indicator.setVisibility(View.GONE);
            alarmIcon.setVisibility(View.GONE);
        }
        
        return convertView;
    }
}

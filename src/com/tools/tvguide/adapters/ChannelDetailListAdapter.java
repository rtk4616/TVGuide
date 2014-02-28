package com.tools.tvguide.adapters;

import java.util.ArrayList;
import java.util.List;

import com.tools.tvguide.R;
import com.tools.tvguide.data.Program;
import com.tools.tvguide.utils.Utility;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChannelDetailListAdapter extends BaseAdapter  
{
    private Context mContext;
    private List<IListItem> mItemList;
    private Program mOnplayingProgram;
    private List<Program> mAlarmProgramList;
    
    enum ItemType {Label, Content};

    public ChannelDetailListAdapter(Context context, List<Program> programList)
    {
        assert (context != null && programList != null);
        mContext = context;
        mItemList = new ArrayList<IListItem>();
        mAlarmProgramList = new ArrayList<Program>();
        mOnplayingProgram = new Program();

        final String midday = "12:00";
        final String evening = "18:00";
        for (int i=0; i<programList.size(); ++i)
        {
            boolean addMidday = false;
            boolean addEvening = false;
            
            if (i < programList.size() - 1)
            {
                String time1 = programList.get(i).time;
                String time2 = programList.get(i+1).time;
                
                if (i == 0 && Utility.compareTime(time1, midday) < 0)
                {
                    LabelItem labelItem = new LabelItem();
                    labelItem.setTag(mContext.getResources().getString(R.string.morning));
                    mItemList.add(labelItem);
                }
                else if (Utility.compareTime(time1, midday) < 0 && Utility.compareTime(time2, midday) >= 0 && Utility.compareTime(time2, evening) < 0)
                {
                    addMidday = true;
                }
                else if (Utility.compareTime(time1, evening) < 0 && Utility.compareTime(time2, evening) >= 0)
                {
                    addEvening = true;
                }
            }
            
            ContentItem contentItem = new ContentItem();
            contentItem.setTag(programList.get(i));
            mItemList.add(contentItem);
            
            if (addMidday)
            {
                LabelItem labelItem = new LabelItem();
                labelItem.setTag(mContext.getResources().getString(R.string.midday));
                mItemList.add(labelItem);
            }
            else if (addEvening)
            {
                LabelItem labelItem = new LabelItem();
                labelItem.setTag(mContext.getResources().getString(R.string.evening));
                mItemList.add(labelItem);
            }
        }
    }
    
    public int setOnplayingProgram(Program program)
    {
        assert (program != null);
        mOnplayingProgram = program;
        notifyDataSetChanged();
        
        assert (mItemList != null);
        for (int i=0; i<mItemList.size(); ++i)
        {
            if (mItemList.get(i).getType() == ItemType.Content)
            {
                if (((Program)mItemList.get(i).getTag()).equals(mOnplayingProgram))
                {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public int addAlarmProgram(Program program)
    {
        assert (program != null);
        assert (mItemList != null);
        for (int i=0; i<mItemList.size(); ++i)
        {
            if (mItemList.get(i).getType() == ItemType.Content)
            {
                if (((Program)mItemList.get(i).getTag()).equals(program))
                {
                    mAlarmProgramList.add(program);
                    notifyDataSetChanged();
                    return i;
                }
            }
        }
        return -1;
    }
    
    public int removeAlarmProgram(Program program)
    {
        assert (program != null);
        assert (mItemList != null);
        for (int i=0; i<mAlarmProgramList.size(); ++i)
        {
            if (mAlarmProgramList.get(i).equals(program))
            {
                mAlarmProgramList.remove(i);
                notifyDataSetChanged();
                return i;
            }
        }
        return -1;
    }
    
    public boolean isAlarmProgram(Program program)
    {
        for (int i=0; i<mAlarmProgramList.size(); ++i)
        {
            if (mAlarmProgramList.get(i).equals(program))
            {
                return true;
            }
        }
        return false;
    }
    
    public Program getProgram(int position)
    {
        if (mItemList == null)
            return null;
        
        if (position < 0 || position >= mItemList.size())
            return null;
        
        if (mItemList.get(position).getType() != ItemType.Content)
            return null;
        
        return (Program) mItemList.get(position).getTag();
    }
    
    @Override
    public int getCount() 
    {
        assert (mItemList != null);
        return mItemList.size();
    }

    @Override
    public Object getItem(int position) 
    {
        assert (mItemList != null);
        return mItemList.get(position);
    }

    @Override
    public long getItemId(int position) 
    {
        return position;
    }
    
    @Override
    public boolean isEnabled(int position) 
    {
        return mItemList.get(position).isClickable();
    }
    
    @Override
    public int getViewTypeCount()
    {
        return ItemType.values().length;
    }
    
    @Override
    public int getItemViewType(int position)
    {
        return mItemList.get(position).getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        assert (mItemList != null);
        return mItemList.get(position).getView(mContext, convertView, LayoutInflater.from(mContext));
    }
    
    private String getProgramString(String time, String title)
    {
        return time + ":　" + title;
    }
    
    private interface IListItem
    {
        public int getLayout();
        public boolean isClickable();
        public View getView(Context context, View convertView, LayoutInflater inflater);
        public ItemType getType();
        public void setTag(Object obj);
        public Object getTag();
    }
    
    private class LabelItem implements IListItem
    {
        private Object mTag;
        
        @Override
        public int getLayout() 
        {
            return R.layout.hot_channel_tvsou_item;
        }

        @Override
        public boolean isClickable() 
        {
            return false;
        }

        @Override
        public View getView(Context context, View convertView, LayoutInflater inflater) 
        {
            if (convertView == null)
                convertView = inflater.inflate(getLayout(), null);
            
            TextView title = (TextView) convertView.findViewById(R.id.hot_channel_name_tv);
            title.setText((String) mTag);
            return convertView;
        }

        @Override
        public ItemType getType() 
        {
            return ItemType.Label;
        }

        @Override
        public void setTag(Object obj) 
        {
            mTag = obj;
        }

        @Override
        public Object getTag() 
        {
            return mTag;
        }
        
    }
    
    private class ContentItem implements IListItem
    {
        private Object mTag;
        
        @Override
        public int getLayout() 
        {
            return R.layout.channel_detail_program_item;
        }

        @Override
        public boolean isClickable() 
        {
            return true;
        }

        @Override
        public View getView(Context context, View convertView, LayoutInflater inflater) 
        {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.channel_detail_program_item, null);
            
            TextView programNameTextView = (TextView) convertView.findViewById(R.id.detail_program_name_tv);
            ImageView indicator = (ImageView) convertView.findViewById(R.id.detail_program_indicator_iv);
            ImageView alarmIcon = (ImageView) convertView.findViewById(R.id.detail_alarm_icon_iv);
            LinearLayout introduceLayout = (LinearLayout) convertView.findViewById(R.id.detail_program_introduce_ll);
            TextView introduceTextView = (TextView) convertView.findViewById(R.id.detail_program_introduce_tv);
            
            Program program = (Program) mTag;
            if (program.equals(mOnplayingProgram))
            {
                SpannableString ss = new SpannableString(getProgramString(program.time, program.title) + "  (正在播放)");
                ss.setSpan(new ForegroundColorSpan(Color.RED), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                programNameTextView.setText(ss);
            }
            else
                programNameTextView.setText(getProgramString(program.time, program.title));
            
            // 角标处理：一般情况：有链接的节目有角标，无链接的节目无角标
            if (program.hasLink())
                indicator.setVisibility(View.VISIBLE);
            else
                indicator.setVisibility(View.INVISIBLE);
            
            // 针对设定过闹钟的节目的处理：显示闹钟图标，为防止图标重叠，此时不显示角标（即便是有链接的情况）
            alarmIcon.setVisibility(View.INVISIBLE);
            if (program.equals(mOnplayingProgram) == false)
            {
                for (int i=0; i<mAlarmProgramList.size(); ++i)
                {
                    if (program.equals(mAlarmProgramList.get(i)))
                    {
                        alarmIcon.setVisibility(View.VISIBLE);
                        indicator.setVisibility(View.INVISIBLE);
                        continue;
                    }
                }
            }
            
            if (program.hasTrailer())
            {
                introduceLayout.setVisibility(View.VISIBLE);
                introduceTextView.setText(program.trailer);
            }
            else
            {
                introduceLayout.setVisibility(View.GONE);
                introduceTextView.setText("");
            }
            
            return convertView;
        }

        @Override
        public ItemType getType() 
        {
            return ItemType.Content;
        }

        @Override
        public void setTag(Object obj) 
        {
            mTag = obj;
        }

        @Override
        public Object getTag() 
        {
            return mTag;
        }
        
    }

}

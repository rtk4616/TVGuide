package com.tools.tvguide.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.tools.tvguide.R;
import com.tools.tvguide.adapters.ResultPageAdapter;
import com.tools.tvguide.managers.AppEngine;
import com.tools.tvguide.managers.UrlManager;
import com.tools.tvguide.managers.ProgramHtmlManager.HotProgramsCallback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

public class HotActivity2 extends Activity implements Callback 
{
    private LayoutInflater mInflater;
    private ViewPager mViewPager;
    private RadioGroup mTabsGroup;
    private ResultPageAdapter mPageAdapter;
    private List<HashMap<String, String>> mProgramInfoList;
    private Handler mUiHandler;
    
    enum TabIndex {Drama, Tvcolumn, Movie}
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot2);
        
        mInflater = LayoutInflater.from(this);
        mViewPager = (ViewPager) findViewById(R.id.hot_view_pager);
        mTabsGroup = (RadioGroup) findViewById(R.id.hot_tabs_group);
        mProgramInfoList = new ArrayList<HashMap<String,String>>();
        mUiHandler = new Handler(this);
        
        mPageAdapter = new ResultPageAdapter();
        for (int i=0; i<mTabsGroup.getChildCount(); ++i)
        {
            LinearLayout loadingLayout = (LinearLayout)mInflater.inflate(R.layout.center_text_tips, null);
            ((TextView) loadingLayout.findViewById(R.id.center_tips_text_view)).setText(getResources().getString(R.string.loading_string));
            mPageAdapter.addView(loadingLayout);
        }
        mViewPager.setAdapter(mPageAdapter);
        
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() 
        {
            @Override
            public void onPageSelected(int position) 
            {
                if (position == TabIndex.Drama.ordinal())
                    mTabsGroup.check(R.id.hot_tab_drama);
                else if (position == TabIndex.Tvcolumn.ordinal())
                    mTabsGroup.check(R.id.hot_tab_tvcolumn);
                else if (position == TabIndex.Movie.ordinal())
                    mTabsGroup.check(R.id.hot_tab_movie);
                update();
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) 
            {
            }
            
            @Override
            public void onPageScrollStateChanged(int state) 
            {
            }
        });
        
        update();
    }

    public void onClickTabs(View view)
    {
        switch (view.getId())
        {
            case R.id.hot_tab_drama:
                mViewPager.setCurrentItem(TabIndex.Drama.ordinal());
                update();
                break;
            case R.id.hot_tab_tvcolumn:
                mViewPager.setCurrentItem(TabIndex.Tvcolumn.ordinal());
                update();
                break;
            case R.id.hot_tab_movie:
                mViewPager.setCurrentItem(TabIndex.Movie.ordinal());
                update();
                break;
        }
    }
    
    private void update()
    {
        TabIndex index = TabIndex.values()[mViewPager.getCurrentItem()];
        String hotUrl = "";
        switch (index)
        {
            case Drama:
                hotUrl = UrlManager.URL_HOT_DRAMA;
                break;
            case Tvcolumn:
                hotUrl = UrlManager.URL_HOT_TVCOLUMN;
                break;
            case Movie:
                hotUrl = UrlManager.URL_HOT_MOVIE;
                break;
        }
        
        AppEngine.getInstance().getProgramHtmlManager().getHotProgramsAsync(0, hotUrl, new HotProgramsCallback() 
        {
            @Override
            public void onProgramsLoaded(int requestId, List<HashMap<String, String>> programInfoList) 
            {
                if (!programInfoList.isEmpty())
                {
                    mProgramInfoList.clear();
                    mProgramInfoList.addAll(programInfoList);
                    mUiHandler.sendEmptyMessage(0);
                }
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) 
    {
        
        return false;
    }
}

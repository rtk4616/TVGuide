package com.tools.tvguide.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.tools.tvguide.R;
import com.tools.tvguide.adapters.ResultProgramAdapter;
import com.tools.tvguide.components.MyProgressDialog;
import com.tools.tvguide.managers.AppEngine;
import com.tools.tvguide.managers.HotHtmlManager;
import com.tools.tvguide.utils.NetworkManager;

public class HotActivityTvsou extends BaseActivity implements Callback 
{
    private ListView mListView;
    private ArrayList<ResultProgramAdapter.IListItem> mItemList;
    private ArrayList<ResultProgramAdapter.IListItem> mItemDataList;
    private Handler mUpdateHandler;
    private MyProgressDialog mProgressDialog;
    private boolean mHasUpdated = false;
    private Handler mUiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_tvsou);
        
        mListView = (ListView)findViewById(R.id.hot_list_view);
        mItemList = new ArrayList<ResultProgramAdapter.IListItem>();
        mItemDataList = new ArrayList<ResultProgramAdapter.IListItem>();
        mProgressDialog = new MyProgressDialog(this);
        mUiHandler = new Handler(this);
        
        mListView.setOnItemClickListener(new OnItemClickListener() 
        {
            @SuppressWarnings("unchecked")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
            {
                if (mItemList == null)
                    return;
                HashMap<String, HashMap<String, String>> programInfo = (HashMap<String, HashMap<String, String>>) mItemList.get(position).getExtraInfo();
                if (programInfo == null)
                    return;
                
                String name = programInfo.get("programInfo").get("name");
                String link = programInfo.get("programInfo").get("link");
                Intent intent = new Intent(HotActivityTvsou.this, ProgramActivityTvsou.class);
                intent.putExtra("name", name);
                intent.putExtra("link", link);
                startActivity(intent);
            }
        });
        
        createUpdateThreadAndHandler();
        updateResult();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        // 发现之前获取失败，则重新获取
        if (mHasUpdated && mItemList.size() == 0)
            updateResult();
    }

    private void createUpdateThreadAndHandler()
    {
        mUpdateHandler = new Handler(NetworkManager.getInstance().getNetworkThreadLooper());
    }
    
    private void updateResult()
    {
        mProgressDialog.show();
        mUpdateHandler.post(new Runnable()
        {
            public void run()
            {
                List<HotHtmlManager.HotEntry> entryList = AppEngine.getInstance().getHotHtmlManager().getEntryList();
                for (int i=0; i<entryList.size(); ++i)
                {
                    mItemDataList.add(new ResultProgramAdapter.LabelItem(entryList.get(i).channelName, R.layout.hot_channel_tvsou_item, R.id.hot_channel_name_tv));
                    for (int j=0; j<entryList.get(i).programList.size(); ++j)
                    {
                        String title = entryList.get(i).programList.get(j).get("name");
                        ResultProgramAdapter.Item item = new ResultProgramAdapter.Item();
                        item.title = title;
                        item.hasLink = true;
                        ResultProgramAdapter.ContentItem contentItem = new ResultProgramAdapter.ContentItem(item, R.layout.hot_program_tvsou_item, R.id.hot_program_name_tv);
                        contentItem.setClickable(true);
                        HashMap<String, HashMap<String, String>> extraInfo = new HashMap<String, HashMap<String, String>>();
                        extraInfo.put("programInfo", entryList.get(i).programList.get(j));
                        contentItem.setExtraInfo(extraInfo);
                        mItemDataList.add(contentItem);
                    }
                }
                mUiHandler.sendEmptyMessage(0);
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) 
    {
        mProgressDialog.dismiss();
        mItemList.clear();
        if (mItemDataList.size() == 0)
            Toast.makeText(HotActivityTvsou.this, getResources().getString(R.string.server_is_busy), Toast.LENGTH_LONG).show();
        for (int i=0; i<mItemDataList.size(); ++i)
        {
            mItemList.add(mItemDataList.get(i));
        }
        
        mListView.setAdapter(new ResultProgramAdapter(HotActivityTvsou.this, mItemList));
        mHasUpdated = true;
        return true;
    }
}

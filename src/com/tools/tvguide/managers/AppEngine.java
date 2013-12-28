package com.tools.tvguide.managers;

import android.content.Context;

public class AppEngine 
{
    private static final String                     TAG                         = "AppEngine";
    private static AppEngine                        mInstance;
    private Context                                 mContext;
    private Context                                 mApplicationContext;
    private CollectManager                          mUserSettingManager;
    private LoginManager                            mLoginManager;
    private ContentManager                          mContentManager;
    private CacheManager                            mCacheManager;
    private AlarmHelper                             mAlarmHelper;
    private UrlManager                              mUrlManager;
    private DnsManager                              mDnsManager;
    private UpdateManager                           mUpdateManager;
    private BootManager                             mBootManager;
    private HotHtmlManager                          mHotHtmlManager;
    
    /********************************* Manager定义区，所有受AppEngine管理的Manger统一定义 **********************************/
    
    public static AppEngine getInstance()
    {
        if (mInstance == null)
            mInstance = new AppEngine();
        return mInstance;
    }
    
    public void setContext(Context context)
    {
        mContext = context;
    }

    public void setApplicationContext(Context context)
    {
        mApplicationContext = context;
    }

    public Context getApplicationContext()
    {
        return mApplicationContext;
    }

    public Context getContext()
    {
        return mContext;
    }
    
    public CollectManager getCollectManager()
    {
        if (mUserSettingManager == null)
            mUserSettingManager = new CollectManager(mApplicationContext);
        return mUserSettingManager;
    }
    
    public LoginManager getLoginManager()
    {
        if (mLoginManager == null)
            mLoginManager = new LoginManager(mApplicationContext);
        return mLoginManager;
    }
    
    public ContentManager getContentManager()
    {
        if (mContentManager == null)
            mContentManager = new ContentManager(mApplicationContext);
        return mContentManager;
    }
    
    public CacheManager getCacheManager()
    {
        if (mCacheManager == null)
            mCacheManager = new CacheManager(mApplicationContext);
        return mCacheManager;
    }
    
    public AlarmHelper getAlarmHelper()
    {
        if (mAlarmHelper == null)
            mAlarmHelper = new AlarmHelper(mApplicationContext);
        return mAlarmHelper;
    }
    
    public UrlManager getUrlManager()
    {
        if (mUrlManager == null)
            mUrlManager = new UrlManager(mApplicationContext);
        return mUrlManager;
    }
    
    public DnsManager getDnsManager()
    {
        if (mDnsManager == null)
            mDnsManager = new DnsManager(mApplicationContext);
        return mDnsManager;
    }
    
    public UpdateManager getUpdateManager()
    {
        if (mUpdateManager == null)
            mUpdateManager = new UpdateManager(mApplicationContext);
        return mUpdateManager;
    }
    
    public BootManager getBootManager()
    {
        if (mBootManager == null)
            mBootManager = new BootManager(mApplicationContext);
        return mBootManager;
    }
    
    public HotHtmlManager getHotHtmlManager()
    {
        if (mHotHtmlManager == null)
            mHotHtmlManager = new HotHtmlManager(mApplicationContext);
        return mHotHtmlManager;
    }
    
    public void prepareBeforeExit()
    {
        if (mUserSettingManager != null)
            mUserSettingManager.shutDown();
        
        if (mAlarmHelper != null)
            mAlarmHelper.shutDown();
        
        if (mContentManager != null)
            mContentManager.shutDown();
        
        exit();
    }
    
    public void exit()
    {
        mInstance = null;
    }
}

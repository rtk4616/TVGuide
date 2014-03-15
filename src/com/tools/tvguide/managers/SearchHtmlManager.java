package com.tools.tvguide.managers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.tools.tvguide.components.UANetDataGetter;
import com.tools.tvguide.data.Channel;
import com.tools.tvguide.data.Program;
import com.tools.tvguide.data.SearchResultCategory;
import com.tools.tvguide.data.SearchResultCategory.Type;
import com.tools.tvguide.data.SearchResultDataEntry;
import com.tools.tvguide.utils.HtmlUtils;

import android.content.Context;
import android.util.Log;

public class SearchHtmlManager 
{
    private static final String QUERY_URL = "http://m.tvmao.com/query.jsp";
    private Context mContext;
    
    public SearchHtmlManager(Context context)
    {
        assert (context != null);
        mContext = context;
    }
    
    public interface SearchResultCallback
    {
        public void onCategoriesLoaded(int requestId, List<SearchResultCategory> categoryList);
        public void onEntriesLoaded(int requestId, SearchResultCategory.Type categoryType, List<SearchResultDataEntry> entryList);
        public void onProgramScheduleLoadeded(int requestId, int page, List<HashMap<String, Object>> scheduleList);
    }
    
    public void search(final int requestId, final String keyword, final SearchResultCallback callback)
    {
        assert (callback != null);
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    Document doc = Jsoup.connect(QUERY_URL).data("key", keyword).data("submit", "搜索").timeout(20).post();
                    String protocol = new URL(QUERY_URL).getProtocol();
                    String host = new URL(QUERY_URL).getHost();
                    String prefix = protocol + "://" + host;
                    
                    // -------------- 获取结果分类 --------------
                    // 返回结果
                    List<SearchResultCategory> categoryList = new ArrayList<SearchResultCategory>();
                    Elements categoryElements = doc.select("dl[id=query_tab] dd");
                    for (int i=0; i<categoryElements.size(); ++i)
                    {
                        Element categoryElement = categoryElements.get(i);
                        Element categoryLink = categoryElement.select("a").first();
                        if (categoryLink != null)
                        {
                            SearchResultCategory category = new SearchResultCategory();
                            category.name = categoryLink.ownText();
                            category.type = SearchResultCategory.getType(category.name);
                            category.link = getAbsoluteUrl(categoryLink.attr("href"));
                            categoryList.add(category);
                        }
                    }
                    callback.onCategoriesLoaded(requestId, categoryList);
                    
                    // -------------- 获取资源信息 --------------
                    if (categoryList.isEmpty())
                        return;
                    
                    for (int i=0; i<categoryList.size(); ++i)
                    {
                        List<SearchResultDataEntry> entryList = new ArrayList<SearchResultDataEntry>();
                        List<HashMap<String, Object>> schedule = new ArrayList<HashMap<String,Object>>();
                        
                        Document document = null;
                        if (i == 0) { // 当前页
                            document = doc;
                        } else {
                            String html = new UANetDataGetter(categoryList.get(i).link).getStringData();
                            if (html != null)
                                document = Jsoup.parse(html);
                            else 
                                continue;
                        }
                        
                        switch (categoryList.get(i).type) 
                        {
                            case Channel:
                                getChannels(document, entryList);
                                break;
                            case Tvcolumn:
                                getTvcolumns(document, entryList);
                                break;
                            case Movie:
                                getMovies(document, entryList);
                                break;
                            case ProgramSchedule:
                                String nextPageLink = null;
                                Document nextPageDocument = document;
                                int page = 0;
                                do {
                                    nextPageLink = getProgramSchedule(nextPageDocument, schedule);
                                    callback.onProgramScheduleLoadeded(requestId, page, schedule);
                                    
                                    if (nextPageLink != null) {
                                        String html = new UANetDataGetter(nextPageLink).getStringData();
                                        if (html != null) {
                                            nextPageDocument = Jsoup.parse(html);
                                            schedule = new ArrayList<HashMap<String,Object>>();
                                            page++;
                                        }
                                    }
                                } while (nextPageLink != null);
                                break;
                            default:
                                break;
                        }
                        
                        if (categoryList.get(i).type != Type.ProgramSchedule)
                            callback.onEntriesLoaded(requestId, categoryList.get(i).type, entryList);
                    }
                    
                    Log.d("", "");
                } 
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void getChannels(Document doc, List<SearchResultDataEntry> channels)
    {
        if (doc == null || channels == null)
            return;
        
        Elements channelBlocks = doc.select("ul[id=t_q_tab_channel] li");
        for (int i=0; i<channelBlocks.size(); ++i)
        {
            Element channelElement = channelBlocks.get(i);
            SearchResultDataEntry entry = new SearchResultDataEntry();
            
            Element imageElement = channelElement.select("img").first();
            if (imageElement != null)
                entry.imageLink = getAbsoluteUrl(imageElement.attr("src"));
            Elements links = channelElement.select("a");
            if (links.size() > 1)   // 包含文字
            {
                entry.name = links.get(1).ownText();
                entry.detailLink = getAbsoluteUrl(links.get(1).attr("href"));
            }
            
            channels.add(entry);
        }
    }
    
    private void getTvcolumns(Document doc, List<SearchResultDataEntry> columns)
    {
        if (doc == null || columns == null)
            return;
        
        Elements columnBlocks = doc.select("table[id=t_q_tab_tvc] tr");
        for (int i=0; i<columnBlocks.size(); ++i)
        {
            Element columnElement = columnBlocks.get(i);
            SearchResultDataEntry entry = new SearchResultDataEntry();
            
            Element imageElement = columnElement.select("img").first();
            if (imageElement != null)
                entry.imageLink = getAbsoluteUrl(imageElement.attr("src"));
            Elements links = columnElement.select("a");
            if (links.size() > 1)
            {
                entry.name = links.get(1).ownText();
                entry.detailLink = getAbsoluteUrl(links.get(1).attr("href"));
            }
            
            columns.add(entry);
        }
    }
    
    private void getMovies(Document doc, List<SearchResultDataEntry> movies)
    {
        if (doc == null || movies == null)
            return;
        
        Elements columnBlocks = doc.select("table[id=t_q_tab_movie] tr");
        for (int i=0; i<columnBlocks.size(); ++i)
        {
            Element columnElement = columnBlocks.get(i);
            SearchResultDataEntry entry = new SearchResultDataEntry();
            
            Element imageElement = columnElement.select("img").first();
            if (imageElement != null)
                entry.imageLink = getAbsoluteUrl(imageElement.attr("src"));
            Elements links = columnElement.select("a");
            if (links.size() > 1)
            {
                entry.name = links.get(1).ownText();
                entry.detailLink = getAbsoluteUrl(links.get(1).attr("href"));
            }
            
            movies.add(entry);
        }
    }
    
    /**
     * 获取节目表
     * @param doc
     * @param schedules
     * @return 下一页的链接
     */
    private String getProgramSchedule(Document doc, List<HashMap<String, Object>> schedules)
    {
        if (doc == null || schedules == null)
            return null;
        
        Elements liElements = doc.select("ul li");
        HashMap<String, Object> schedule = null;
        List<Program> programList = null;
        
        for (int i=0; i<liElements.size(); ++i)
        {
            Element li = liElements.get(i);
            Element linkElement = li.select("a").first();
            if (linkElement != null)
            {
                if (linkElement.attr("href").contains("program"))   // 频道
                {
                    Channel channel = new Channel();
                    channel.name = linkElement.ownText();
                    channel.tvmaoId = HtmlUtils.filterTvmaoId(linkElement.attr("href"));
                    channel.tvmaoLink = getAbsoluteUrl(linkElement.attr("href"));
                    
                    if (schedule != null)
                        schedules.add(schedule);
                        
                    schedule = new HashMap<String, Object>();
                    programList = new ArrayList<Program>();
                    schedule.put("channel", channel);
                    schedule.put("programs", programList);
                }
                else    // 节目
                {
                    Program program = new Program();
                    Element dateElement = li.select("span.date").first();
                    if (dateElement != null)
                        program.date = dateElement.ownText();
                    
                    Element timeElement = li.select("span.time").first();
                    if (timeElement != null)
                        program.time = timeElement.ownText();
                    
                    program.title = linkElement.ownText();
                    program.link = getAbsoluteUrl(linkElement.attr("href"));
                    
                    if (programList != null)
                        programList.add(program);
                }
            }
            
            if (i == liElements.size() -1)  // 最后一个
            {
                if (schedule != null)
                    schedules.add(schedule);
            }
        }
        
        String nextPageLink = null;
        Element nextPageElement = doc.select("div.page a").first();
        if (nextPageElement != null && nextPageElement.text().contains("下一页"))
        {
            nextPageLink = getAbsoluteUrl(nextPageElement.attr("href"));
        }
        
        return nextPageLink;
    }
    
    private String getAbsoluteUrl(String url)
    {
        if (url == null)
            return null;
        
        try 
        {
            String protocol = new URL(QUERY_URL).getProtocol();
            String host = new URL(QUERY_URL).getHost();
            String prefix = protocol + "://" + host;
            
            if (!url.contains("http://"))
                url = prefix + url;
        } 
        catch (MalformedURLException e) 
        {
            e.printStackTrace();
        }
        
        return url;
    }
    
    
}


package com.tools.tvguide.managers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;

import com.tools.tvguide.data.ProgramType;
import com.tools.tvguide.utils.CacheControl;
import com.tools.tvguide.utils.HtmlUtils;

public class ProgramHtmlManager 
{
    private Context mContext;
    
    public ProgramHtmlManager(Context context)
    {
        assert (context != null);
        mContext = context;
    }
    
    public interface ProgramDetailCallback
    {
        void onTitleLoaded(int requestId, String title);
        void onProfileLoaded(int requestId, String profile);
        void onSummaryLoaded(int requestId, String summary);
        void onPictureLinkParsed(int requestId, String link);
        void onEpisodeLinkParsed(int requestId, String link);
    }
    
    public interface ProgramEpisodesCallback
    {
        void onEntriesLoaded(int requestId, List<HashMap<String, String>> entryList);
        void onEpisodesLoaded(int requestId, List<HashMap<String, String>> episodeList);
    }
    
    public interface HotProgramsCallback
    {
        void onProgramsLoaded(int requestId, List<HashMap<String, String>> programInfoList);
    }
    
    public void getProgramDetailAsync(final int requestId, final String programUrl, final ProgramDetailCallback callback)
    {
        assert (callback != null);
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    String host = new URL(programUrl).getHost();
                    if (host.equals("www.tvmao.com"))
                        getProgramDetailFromFullWeb(requestId, programUrl, callback);
                    else if (host.equals("m.tvmao.com"))
                        getProgramDetailFromSimpleWeb(requestId, programUrl, callback);
                } 
                catch (MalformedURLException e) 
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void getProgramEpisodesAsync(final int requestId, final String url, final ProgramEpisodesCallback callback)
    {
        assert (callback != null);
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    Document doc = HtmlUtils.getDocument(url, CacheControl.Memory);
                    String protocol = new URL(url).getProtocol();
                    String host = new URL(url).getHost();
                    String prefix = protocol + "://" + host;
                    
                    // -------------- 获取Tab链接 --------------
                    // 返回结果
                    List<HashMap<String, String>> entryList = new ArrayList<HashMap<String,String>>();
                    Element entriesElement = doc.select("div.section-wrap div.epipage").first();
                    if (entriesElement != null)
                    {
                        Elements entryElements = entriesElement.select("a");
                        for (int i=0; i<entryElements.size(); ++i)
                        {
                            String name = entryElements.get(i).text().trim();
                            String link = prefix + entryElements.get(i).attr("href");
                            
                            if (!name.equals("") && !link.equals("") && !link.contains("javascript:;"))
                            {
                                HashMap<String, String> entry = new HashMap<String, String>();
                                entry.put("name", name);
                                entry.put("link", link);
                                entryList.add(entry);
                            }
                        }
                    }
                    callback.onEntriesLoaded(requestId, entryList);
                    
                    // -------------- 获取当前分集信息 --------------
                    // 返回结果
                    List<HashMap<String, String>> episodeList = new ArrayList<HashMap<String,String>>();
                    Element articleElement = doc.select("div.section-wrap article").first();
                    if (articleElement != null)
                    {
                        Elements titleElements = articleElement.getElementsByAttribute("id");
                        for (int i=0; i<titleElements.size(); ++i)
                        {
                            Element titleElement = titleElements.get(i);
                            Element plotElement = titleElement.nextElementSibling();
                            
                            if (titleElement != null && plotElement != null)
                            {
                                String title = titleElement.text().trim();
                                String omit = "在线观看";
                                if (title.contains(omit)) {
                                    title = title.substring(0, title.indexOf(omit));
                                }
                                
                                String plot = plotElement.text() + "\n";
                                while (plotElement.nextElementSibling() != null
                                        && plotElement.nextElementSibling().nodeName().equals("p")) {
                                    plotElement = plotElement.nextElementSibling();
                                    plot += plotElement.text() + "\n";
                                }
                                plot += "\n";
                                
                                HashMap<String, String> episode = new HashMap<String, String>();
                                episode.put("title", title);
                                episode.put("plot", plot);
                                episodeList.add(episode);
                            }
                        }
                    }
                    callback.onEpisodesLoaded(requestId, episodeList);
                    
                } 
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void getProgramDetailFromFullWeb(final int requestId, final String programUrl, final ProgramDetailCallback callback)
    {
        try 
        {
            Document doc = HtmlUtils.getDocument(programUrl, CacheControl.Memory);
            String protocol = new URL(programUrl).getProtocol();
            String host = new URL(programUrl).getHost();
            String prefix = protocol + "://" + host;
            
            // -------------- 获取Title --------------
            // 返回结果
            String title = "";
            Element titleElement = doc.select("h1.lt[itemprop=name]").first();
            if (titleElement != null)
            {
                title = titleElement.text().trim();
            }
            if (title != null && !title.equals(""))
                callback.onTitleLoaded(requestId, title);
            
            // -------------- 获取Profile --------------
            // 返回结果
            String profile = "";
            Elements profileElements = doc.select("div.abstract-wrap table.obj_meta tbody tr");
            for (int i=0; i<profileElements.size(); ++i)
            {
                Element profileElement = profileElements.get(i);
                String key = "";
                String value = "";
                
                Element keyElement = profileElement.select("td").first();
                if (keyElement != null)
                    key = keyElement.ownText();
                
                Element valueElement = profileElement.select("td").last();
                if (valueElement != null)
                    value = valueElement.text();
                
                profile += key + value + "\n";
            }
            
            callback.onProfileLoaded(requestId, profile);
            
            // -------------- 获取图片链接 --------------
            // 返回结果
            String picLink = null;
            Element divElement = doc.getElementById("mainpic");
            if (divElement != null)
            {
                Element imgElement = divElement.select("img[src]").first();
                if (imgElement != null)
                    picLink = imgElement.attr("abs:src");
            }
            if (picLink != null)
                callback.onPictureLinkParsed(requestId, picLink);
            
            // -------------- 获取剧情概要 --------------
            // 返回结果
            String summary = "";
            Element descriptionElement = doc.select("div.section-wrap div.lessmore div.more_c").first();
            if (descriptionElement != null)
            {
                Elements pargfs = descriptionElement.select("p");
                for (int i=0; i<pargfs.size(); ++i)
                {
                    if (pargfs.get(i).text().trim().equals(""))
                        continue;
                    summary += "　　" + pargfs.get(i).text() + "\n\n";
                }
            }
            
            callback.onSummaryLoaded(requestId, summary);
            
            // -------------- 获取分集链接 --------------
            String episodesLink = null;
            Element dlElement = doc.select("dl.menu_tab").first();
            if (dlElement != null)
            {
                Elements links = dlElement.select("a[href]");
                for (int i=0; i<links.size(); ++i)
                {
                    String text = links.get(i).ownText();
                    if (text.contains("剧情")) {
                        episodesLink = prefix + links.get(i).attr("href");
                        break;
                    }
                }
            }
            if (episodesLink != null)
                callback.onEpisodeLinkParsed(requestId, episodesLink);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    private void getProgramDetailFromSimpleWeb(final int requestId, final String programUrl, final ProgramDetailCallback callback)
    {
        try 
        {
            Document doc = HtmlUtils.getDocument(programUrl, CacheControl.Memory);
            String protocol = new URL(programUrl).getProtocol();
            String host = new URL(programUrl).getHost();
            String prefix = protocol + "://" + host;
            
            ProgramType type = ProgramType.Tvcolumn;
            if (programUrl.contains("tvcolumn")) {
                type = ProgramType.Tvcolumn;
            } else if (programUrl.contains("drama")) {
                type = ProgramType.Drama;
            } else if (programUrl.contains("movie")) {
                type = ProgramType.Movie;
            }
            
            // -------------- 获取Profile --------------
            // 返回结果
            String profile = "";
            if (type == ProgramType.Tvcolumn) {
                Elements profileElements = doc.select("table.mtblmetainfo table.obj_meta tbody tr");
                for (int i=0; i<profileElements.size(); ++i) {
                    Element profileElement = profileElements.get(i);
                    String key = "";
                    String value = "";
                    
                    Element keyElement = profileElement.select("td").first();
                    if (keyElement != null)
                        key = keyElement.ownText();
                    
                    Element valueElement = profileElement.select("td span").first();
                    if (valueElement != null)
                        value = valueElement.ownText();
                    
                    profile += key + value + "\n";
                }
            } else if (type == ProgramType.Drama) {
                Elements pElements = doc.select("div[class=bg_deepgray] div.bg_light div.blank p");
                if (pElements != null) {
                    for (Element p : pElements) {
                        profile += p.ownText() + "\n";
                    }
                }
            }
            
            callback.onProfileLoaded(requestId, profile);
            
            // -------------- 获取图片链接 --------------
            // 返回结果
            String picLink = null;
            Element imgElement = doc.select("div[class=bg_deepgray] div.bg_light img").first();
            if (imgElement != null)
            {
                picLink = imgElement.attr("src");
            }
            if (picLink != null)
                callback.onPictureLinkParsed(requestId, picLink);
                        
            // -------------- 获取剧情概要 --------------
            // 返回结果
            String summary = "";
            if (type == ProgramType.Tvcolumn) {
                Elements summaryElements = doc.select("div.section p");
                if (summaryElements != null) {
                    Element descriptionElement = summaryElements.first();
                    if (descriptionElement != null) {
                        summary += descriptionElement.ownText() + "\n";
                        final int MaxLines = 1000;
                        int i = 0;
                        Element nextElement = descriptionElement.nextElementSibling();
                        while (nextElement != null && nextElement.nodeName().equals("p") && (i++ < MaxLines))
                        {
                            summary += nextElement.text() + "\n";
                            nextElement = nextElement.nextElementSibling();
                        }
                    }
                }
            } else if (type == ProgramType.Drama) {
                Elements summaryElements = doc.select("div[class=bg_deepgray] div.bg_light div[class=desc less_c]");
                if (summaryElements != null) {
                    summary = summaryElements.first().ownText() + "\n";
                }
            }
            
            callback.onSummaryLoaded(requestId, summary);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void getHotProgramsAsync(final int requestId, final String hotUrl,
            final ProgramType type, final HotProgramsCallback callback)
    {
        assert (callback != null);
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    Document doc = HtmlUtils.getDocument(hotUrl, CacheControl.DiskToday);
                    String protocol = new URL(hotUrl).getProtocol();
                    String host = new URL(hotUrl).getHost();
                    String prefix = protocol + "://" + host;
                    
                    // -------------- 获取整个列表 --------------
                    // 返回结果
                    List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
                    switch (type) {
                        case Tvcolumn:
                            programList = parseHotTvcolumn(prefix, doc);
                            break;
                        case Drama:
                            programList = parseHotDrama(prefix, doc);
                            break;
                        case Movie:
                            programList = parseHotMovie(prefix, doc);
                            break;
                    }
                    callback.onProgramsLoaded(requestId, programList);
                }
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private List<HashMap<String, String>> parseHotTvcolumn(String linkPrefix, Document doc) {
        List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
        Elements programElements = doc.select("ul[class=mt10 clear tvclst] li");
        if (programElements == null) {
            return programList;
        }
        return parseProgramDetail(linkPrefix, programElements);
    }
    
    private List<HashMap<String, String>> parseHotDrama(String linkPrefix, Document doc) {
        List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
        Elements programElements = doc.select("ul[class=tv_fixed tvli] li");
        if (programElements == null) {
            return programList;
        }
        return parseProgramDetail(linkPrefix, programElements);
    }
    
    private List<HashMap<String, String>> parseHotMovie(String linkPrefix, Document doc) {
        List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
        Elements programElements = doc.select("ul[class=libd clear mb10 mt10] li");
        if (programElements == null) {
            return programList;
        }
        return parseProgramDetail(linkPrefix, programElements);
    } 
    
    private List<HashMap<String, String>> parseProgramDetail(String linkPrefix, Elements programElements) {
        List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
        for (Element programElement : programElements) {
            HashMap<String, String> programInfo = new HashMap<String, String>();
            // -------------- 获取名称, 链接, 图片 --------------
            Elements links = programElement.select("a");
            if (links != null) {
                String name = links.first().attr("title");
                String link = links.first().attr("href");
                if (!link.contains("://"))    // not absolute path
                    link = linkPrefix + link;
                programInfo.put("name", name);
                programInfo.put("link", link);
                
                // 图片链接
                Elements imgs = programElement.select("img");
                if (imgs != null) {
                    String imgSrc = imgs.first().attr("src");
                    programInfo.put("picture_link", imgSrc);
                }
            }
            
            // -------------- 获取Profile --------------
            String profile = "";
            Elements profileElements = programElement.select("p");
            if (profileElements != null) {
                for (Element profileElement : profileElements) {
                    profile += profileElement.text() + "\n";
                }
            }
            programInfo.put("profile", profile);
            programList.add(programInfo);
        }
        return programList;
    }
}



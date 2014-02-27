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

import com.tools.tvguide.utils.HtmlUtils;

import android.content.Context;

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
                    Document doc = HtmlUtils.getDocument(url);
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
                            
                            if (!name.equals("") && !link.equals(""))
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
                            Element plotElement = titleElement.siblingElements().first();
                            
                            if (titleElement != null && plotElement != null)
                            {
                                String title = titleElement.text().trim();
                                String plot = "";
                                
                                Elements pargphs = plotElement.select("p");
                                for (int j=0; j<pargphs.size(); ++j)
                                    plot += pargphs.get(j).text().trim() + "\n\n";
                                
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
            Document doc = HtmlUtils.getDocument(programUrl);
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
            Element dlElement = doc.select("dl.hdtab").first();
            if (dlElement != null)
            {
                Elements links = dlElement.select("a[href]");
                for (int i=0; i<links.size(); ++i)
                {
                    String text = links.get(i).ownText();
                    if (text.startsWith("分集剧情"))
                        episodesLink = prefix + links.get(i).attr("href");
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
            Document doc = HtmlUtils.getDocument(programUrl);
            String protocol = new URL(programUrl).getProtocol();
            String host = new URL(programUrl).getHost();
            String prefix = protocol + "://" + host;
            
            // -------------- 获取Profile --------------
            // 返回结果
            String profile = "";
            Elements profileElements = doc.select("table.mtblmetainfo table.obj_meta tbody tr");
            for (int i=0; i<profileElements.size(); ++i)
            {
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
            
            callback.onProfileLoaded(requestId, profile);
            
            // -------------- 获取图片链接 --------------
            // 返回结果
            String picLink = null;
            Element imgElement = doc.select("table.mtblmetainfo td.td1 img").first();
            if (imgElement != null)
            {
                picLink = imgElement.attr("src");
            }
            if (picLink != null)
                callback.onPictureLinkParsed(requestId, picLink);
                        
            // -------------- 获取剧情概要 --------------
            // 返回结果
            String summary = "";
            String descriptionLink = programUrl + "/detail";
            Document descriptionDoc = Jsoup.connect(descriptionLink).get();
            Element descriptionElement = descriptionDoc.select("p.desc").first();
            if (descriptionElement != null)
            {
                final int MaxLines = 1000;
                int i = 0;
                Element nextElement = descriptionElement.nextElementSibling();
                while (nextElement.nodeName().equals("p") && (i++ < MaxLines))
                {
                    summary += nextElement.text() + "\n";
                    nextElement = nextElement.nextElementSibling();
                }
            }
            
            callback.onSummaryLoaded(requestId, summary);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void getHotProgramsAsync(final int requestId, final String hotUrl, final HotProgramsCallback callback)
    {
        assert (callback != null);
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    Document doc = HtmlUtils.getDocument(hotUrl);
                    String protocol = new URL(hotUrl).getProtocol();
                    String host = new URL(hotUrl).getHost();
                    String prefix = protocol + "://" + host;
                    
                    // -------------- 获取整个列表 --------------
                    // 返回结果
                    List<HashMap<String, String>> programList = new ArrayList<HashMap<String,String>>();
                    Elements tableElements = doc.select("div.clear table");
                    for (int i=0; i<tableElements.size(); ++i)
                    {
                        HashMap<String, String> programInfo = new HashMap<String, String>();
                        Element tableElement = tableElements.get(i);

                        // -------------- 获取Program名称和链接 --------------
                        // 返回结果
                        String programLink = "";
                        String programName = "";
                        Element programLinkElement = tableElement.select("tbody td.td2 a").first();
                        if (programLinkElement != null)
                        {
                            programLink = programLinkElement.attr("href");
                            if (!programLink.contains(protocol + "://"))    // not absolute path
                                programLink = prefix + programLink;
                            programInfo.put("link", programLink);
                            
                            programName = programLinkElement.text();
                            programInfo.put("name", programName);
                        }
                        
                        // -------------- 获取Profile --------------
                        // 返回结果
                        String profile = "";
                        Element profileElement = tableElement.select("tbody td.td2 div").first();
                        if (profileElement != null)
                        {
                            String tmpProfile = HtmlUtils.omitHtmlElement(profileElement.html());
                            
                            // 去除空行
                            String lines[] = tmpProfile.split("\n");
                            for (int t=0; t<lines.length; ++t)
                            {
                                String line = lines[t].trim();
                                if (line.length() > 0)   // 不是空行
                                {
                                    if (line.equals(programName) 
                                        || line.contains("评论") 
                                        || line.contains("更多"))    // 除去某些行
                                        continue;
                                    
                                    if (line.equals("主演:") || line.equals("主演："))   // 解决“主演：”和演员名不在同一行的问题
                                        profile += line + " ";
                                    else
                                        profile += line + "\n";
                                }
                            }
                            
                            programInfo.put("profile", profile);
                        }
                        
                        // -------------- 获取图片链接 --------------
                        // 返回结果
                        String picLink = null;
                        Element imgElement = tableElement.select("tbody td.td1 img").first();
                        if (imgElement != null)
                        {
                            picLink = imgElement.attr("src");
                            programInfo.put("picture_link", picLink);
                        }
                        
                        programList.add(programInfo);
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
}



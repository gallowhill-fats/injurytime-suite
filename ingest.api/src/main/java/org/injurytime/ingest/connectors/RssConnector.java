/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.injurytime.ingest.connectors;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.injurytime.ingest.api.RawItem;
import org.injurytime.ingest.api.SourceConnector;
import org.jsoup.Jsoup;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RssConnector implements SourceConnector {

    private final List<String> urls;

    public RssConnector(List<String> urls)
    {
        this.urls = urls;
    }

    @Override
    public String id()
    {
        return "rss";
    }

    @Override
    public List<RawItem> fetchNew() throws Exception
    {
        List<RawItem> out = new ArrayList<>();
        var input = new SyndFeedInput();
        for (String u : urls)
        {
            var feed = input.build(new XmlReader(new URL(u)));
            for (SyndEntry e : feed.getEntries())
            {
                String html = e.getDescription() != null ? e.getDescription().getValue() : null;
                String text = html != null ? Jsoup.parse(html).text() : null;
                out.add(new RawItem("rss", e.getUri() != null ? e.getUri() : e.getLink(), e.getLink(), e.getTitle(), html, text));
            }
        }
        return out;
    }
}

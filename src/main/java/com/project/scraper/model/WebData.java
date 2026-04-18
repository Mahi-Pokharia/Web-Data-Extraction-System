package com.project.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/*Data model representing the scraped content extracted from a web page.*/
public class WebData implements Serializable {

    @JsonProperty("url")
    private String url;

    @JsonProperty("title")
    private String title;

    @JsonProperty("headings")
    private List<String> headings;

    @JsonProperty("links")
    private List<String> links;

    public WebData() {}

    public WebData(String url, String title, List<String> headings, List<String> links) {
        this.url      = url;
        this.title    = title;
        this.headings = headings;
        this.links    = links;
    }

    public String getUrl(){return url;}
    public void setUrl(String url){this.url = url;}

    public String getTitle(){return title;}
    public void setTitle(String title){this.title = title;}

    public List<String> getHeadings(){return headings;}
    public void setHeadings(List<String> headings){this.headings = headings;}

    public List<String> getLinks(){return links;}
    public void setLinks(List<String> links){this.links = links;}

    @Override
    public String toString(){
        return "WebData{url='%s', title='%s', headings=%d, links=%d}"
                .formatted(url, title,
                        headings == null ? 0 : headings.size(),
                        links    == null ? 0 : links.size());
    }
}

package com.proj.spider.crawler.service;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.SilentJavaScriptErrorListener;
import com.proj.spider.crawler.model.LinkBetweenURL;
import com.proj.spider.crawler.model.Location;
import com.proj.spider.crawler.model.URL;
import com.proj.spider.crawler.model.Word;
import com.proj.spider.crawler.repository.LinkBetweenURLRepository;
import com.proj.spider.crawler.repository.LocationRepository;
import com.proj.spider.crawler.repository.URLRepository;
import com.proj.spider.crawler.repository.WordRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Service
public class CrawlerService {

    WordRepository wordRepository;
    URLRepository urlRepository;
    LocationRepository locationRepository;
    LinkBetweenURLRepository linkBetweenURLRepository;
    WebClient webClient;

    public CrawlerService(WordRepository wordRepository,
                          URLRepository urlRepository,
                          LocationRepository locationRepository,
                          LinkBetweenURLRepository linkBetweenURLRepository) {
        this.wordRepository = wordRepository;
        this.urlRepository = urlRepository;
        this.locationRepository = locationRepository;
        this.linkBetweenURLRepository = linkBetweenURLRepository;

        webClient = new WebClient(); // гдет надо будет закрыть
        webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(3000);
    }

    public Document getPage(URL url) throws IOException {
        HtmlPage myPage = webClient.getPage(url.getValue());
        return Jsoup.parse(myPage.asXml());
    }

    public Queue<URL> getLinks(Document document, URL url) throws URISyntaxException {
        URI uri = new URI(url.getValue());
        String domain = uri.getHost();

        Queue<URL> urls = new LinkedList<>();
        Elements aTags = document.select("a");

        for (Element aTag : aTags) {
            String link;

            if (aTag.hasAttr("href") && !aTag.attr("href").equals("")) {
                link = aTag.attr("href");
            } else continue;

            if (link.charAt(0) == '/') {
                link = "https://" + domain + link;
            }

            URL URLEntity = getURL(link);

            linkBetweenURLRepository.save(new LinkBetweenURL(url, URLEntity));

            urls.offer(URLEntity);

        }

        return urls;
    }

    public URL getURL(String url) {
        URL curUrl = urlRepository.findByValue(url);
        if (curUrl == null) {
            curUrl = new URL(url);
            urlRepository.save(curUrl);
        }
        return curUrl;
    }

    public String getText(Document document) {
        String bodyText = document.select("body").text();
        bodyText = bodyText.replaceAll("[^\\p{IsCyrillic}+]", " "); // what
        return bodyText;
    }

    public void indexPage(Document document, URL url) {
        StringTokenizer stringTokenizer = new StringTokenizer(getText(document));
        Set<Word> newWords = new HashSet<>();

        for (int position = 0; position < stringTokenizer.countTokens(); position++) {
            String wordToken = stringTokenizer.nextToken();
            Word word = wordRepository.findByValue(wordToken);
            if (word == null) {
                word = new Word(wordToken);
                newWords.add(word);
            }
            word.getLocation().add(new Location(url, word, position));
            wordRepository.save(word);
        }

        wordRepository.saveAll(newWords);
    }

    public boolean isIndexed(URL url) {
        return locationRepository.existsByUrl(url);
    }

}

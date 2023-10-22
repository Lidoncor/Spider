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

        webClient = new WebClient();
        webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(1000);
    }

    public void crawl(Set<String> urls, int depth) {
        Set<String> nextPages = new HashSet<>();

        for (int i = 0; i < depth; i++) {
            System.out.println("DEPTH " + i);// todo remove
            for (String url : urls) {
                System.out.println("NOW " + url);// todo remove
                if (isIndexed(url)) {
                    System.out.println("INDEXED " + url); // todo remove
                    continue;
                }

                URL urlEntity = saveURL(url);

                Document document;
                try {
                    document = getPage(url);
                    nextPages.addAll(getLinks(document, urlEntity));
                } catch (Exception ex) {
                    continue;
                }

                indexPage(document, urlEntity);

            }

            urls.addAll(nextPages);
            nextPages.clear();

        }
        System.out.println("DONE"); // todo remove
        webClient.close();
    }


    public Document getPage(String url) throws IOException {
        HtmlPage myPage = webClient.getPage(url);
        return Jsoup.parse(myPage.asXml());
    }

    public Set<String> getLinks(Document document, URL url) throws URISyntaxException {
        URI uri = new URI(url.getValue());
        String domain = uri.getHost();

        Set<String> urls = new HashSet<>();
        Elements aTags = document.select("a");

        for (Element aTag : aTags) {
            String link;

            if (aTag.hasAttr("href") && !aTag.attr("href").equals("")) {
                link = aTag.attr("href");
            } else {
                continue;
            }

            if (link.charAt(0) == '/') {
                link = "https://" + domain + link;
            }

            if (isIndexed(link)) {
                continue;
            }

            URL url_test = saveURL(link);

            linkBetweenURLRepository.save(new LinkBetweenURL(url, url_test));

            //URL URLEntity = getOrCreateURL(link);

            //linkBetweenURLRepository.save(new LinkBetweenURL(url, URLEntity));

            urls.add(link);

        }

        return urls;
    }

    public URL saveURL(String url) {
        URL urlEntity = new URL(url);
        urlRepository.save(urlEntity);
        return urlEntity;
    }

    public String getText(Document document) {
        String bodyText = document.select("body").text();
        bodyText = bodyText.replaceAll("[^\\p{IsCyrillic}+]", " ");
        return bodyText;
    }

    public void indexPage(Document document, URL url) {
        StringTokenizer stringTokenizer = new StringTokenizer(getText(document));
        for (int position = 0; position < stringTokenizer.countTokens(); position++) {
            String wordToken = stringTokenizer.nextToken();
            Word word = wordRepository.findByValue(wordToken);
            if (word == null) {
                word = new Word(wordToken);
            }
            word.getLocation().add(new Location(url, word, position));
            wordRepository.save(word);
        }
    }

    public boolean isIndexed(String url) {
        return locationRepository.existsByUrlValue(url);
    }

}

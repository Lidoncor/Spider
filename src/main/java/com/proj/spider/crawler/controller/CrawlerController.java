package com.proj.spider.crawler.controller;

import com.proj.spider.crawler.model.URL;
import com.proj.spider.crawler.service.CrawlerService;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.Queue;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/crawl")
    public void crawl(@RequestParam(value = "url", required = true) String url,
                      @RequestParam(value = "depth", required = true) int depth) {

        Queue<URL> currentURLS = new LinkedList<>();
        Queue<URL> nextURLS = new LinkedList<>();

        Document document;
        URL currentURL;

        currentURL = crawlerService.getURL(url);
        try {
            document = crawlerService.getPage(currentURL);
            nextURLS.addAll(crawlerService.getLinks(document, currentURL));
        } catch (Exception ex) { }

        for (int i = 0; i < depth; i++) {
            currentURLS.addAll(nextURLS);
            nextURLS.clear();

            while (currentURLS.size() != 0) {
                System.out.println(currentURLS.size());

                currentURL = currentURLS.poll();

                if (crawlerService.isIndexed(currentURL)) {
                    continue;
                }

                try {
                    document = crawlerService.getPage(currentURL);
                    nextURLS.addAll(crawlerService.getLinks(document, currentURL));
                } catch (Exception ex) {
                    continue;
                }

                crawlerService.indexPage(document, currentURL);
            }
        }
    }
}

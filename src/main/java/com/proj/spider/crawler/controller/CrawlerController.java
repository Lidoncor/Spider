package com.proj.spider.crawler.controller;

import com.proj.spider.crawler.service.CrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/crawler")
public class CrawlerController {

    CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/crawl")
    public void crawl(@RequestParam(value = "urls", required = true) Set<String> url,
                      @RequestParam(value = "depth", required = true) int depth) {
        crawlerService.crawl(url, depth);
    }

}

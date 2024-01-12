package com.proj.spider.crawler.controller;

import com.proj.spider.crawler.service.CrawlerService;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/crawler")
public class CrawlerController {
    CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/crawl")
    @Transactional
    public void crawl(@RequestParam(value = "urls") Set<String> url,
                      @RequestParam(value = "depth") int depth) {
        crawlerService.crawl(url, depth);
    }
}

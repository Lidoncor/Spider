package com.proj.spider.searcher.controller;

import com.proj.spider.searcher.service.SearcherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/searcher")
public class SearcherController {
    SearcherService searcherService;

    public SearcherController(SearcherService searcherService) {
        this.searcherService = searcherService;
    }

    @GetMapping("/search")
    public String search(Model model, @RequestParam String query) {
       return searcherService.search(model, query);
    }
}

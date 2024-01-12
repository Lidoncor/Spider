package com.proj.spider.repository;

import com.proj.spider.crawler.model.URL;
import org.springframework.data.repository.CrudRepository;

public interface URLRepository extends CrudRepository<URL, Integer> {
   URL findById(int id);
}
